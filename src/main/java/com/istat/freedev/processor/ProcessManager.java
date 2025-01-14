package com.istat.freedev.processor;

import android.text.TextUtils;

import com.istat.freedev.processor.interfaces.ProcessListener;
import com.istat.freedev.processor.interfaces.RunnableDispatcher;
import com.istat.freedev.processor.utils.ToolKits;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by istat on 04/10/16.
 */
//TODO thing about ProcessManager.Plugin
//TODO permettre de lancer des Process qui embarque en eux l'execution de plusieurs autre process (avec possibilité de créer des sous ProcessManager dans lequel il tourne ces sous Process)
public final class ProcessManager {
    static final ConcurrentHashMap<String, Process> globalProcessQueue = new ConcurrentHashMap();
    final ConcurrentHashMap<String, Process> processQueue = new ConcurrentHashMap();
    final ConcurrentLinkedQueue<ProcessListener> processListeners = new ConcurrentLinkedQueue();
    private static final int SIZE_GENERATED_PID = 16;
    private final String nameSpace;

    public <T extends Process> T execute(final T process, Object[] vars, String PID) throws ProcessException {
        if (isRunningPID(PID)) {
            throw new ProcessException("Sorry, a running process with same PID already running");
        }
        process.setId(PID);
        return execute(process, vars);
    }

    /**
     * Execute process with specific execution variables
     *
     * @param process
     * @param vars
     * @return
     */
    public <T extends Process> T execute(final T process, Object... vars) {
        String id = process.getId();
        synchronized (this) {
            if (ToolKits.isEmpty(id)) {
                id = System.currentTimeMillis() + "";
                while (isRunningPID(id)) {
                    id = generateProcessId();
                }
                process.setId(id);
            }
            setPID(id, process);
        }
        synchronized (id) {
            post(() -> notifyProcessEnqueued(process));
            process.execute(this, vars);
            return process;
        }
    }

    /**
     * Obtenir tout les process de drive encore en vie.
     *
     * @return
     */
    public List<Process> getRunningProcess() {
        Iterator<String> iterator = processQueue.keySet().iterator();
        List<Process> list = new ArrayList<>();
        while (iterator.hasNext()) {
            String id = iterator.next();
            Process<?, ?> process = getProcessById(id);
            if (process != null) {
                if (process.isRunning()) {
                    list.add(process);
                } else {
                    processQueue.remove(process);
                }
            }
        }
        return list;
    }

    public <T extends Process<?, ?>> List<T> getRunningProcess(Class<T> filterClass) {
        return getRunningProcess(filterClass, false);
    }

    public <T extends Process<?, ?>> List<T> getRunningProcess(Class<T> filterClass, boolean acceptAssignableClass) {
        Iterator<String> iterator = processQueue.keySet().iterator();
        List<T> list = new ArrayList<T>();
        while (iterator.hasNext()) {
            String id = iterator.next();
            Process<?, ?> process = getProcessById(id);
            if (process != null) {
                if (process.isRunning()) {
                    if (filterClass == null || filterClass == process.getClass() || (acceptAssignableClass && filterClass.isAssignableFrom(process.getClass()))) {
                        list.add((T) process);
                    }
                } else {
                    processQueue.remove(process);
                }
            }
        }
        return list;
    }

    /**
     * whether or not current manager has running process.
     *
     * @return
     */
    public boolean hasRunningProcess() {
        return processQueue.size() > 0;
    }

    /**
     * Execute process with specific execution variables and id.
     *
     * @param process
     * @param PID
     * @param vars
     * @return
     * @throws ProcessException if given id is already used inside the manager
     */
    public final <T extends Process> T execute(String PID, T process, Object... vars) throws ProcessException {
        if (isRunningPID(PID)) {
            throw new ProcessException("Sorry, a running process with same PID alrady running");
        }
        process.setId(PID);
        return execute(process, vars);
    }

    /**
     * Cancell all running process.
     *
     * @return
     */
    public int cancelAll() {
        int livingProcess = processQueue.size();
        Enumeration<Process> enumProcess = processQueue.elements();
        while (enumProcess.hasMoreElements()) {
            enumProcess.nextElement().cancel();
        }
        mDispatcher.release();
        return livingProcess;
    }

    public int release() {
        unRegisterAllProcessListener();
        return cancelAll();
    }

    ProcessManager() {
        this(Processor.DEFAULT_PROCESSOR_TAG, null);
    }

//    ProcessManager(String nameSpace) {
//        this(nameSpace, null);
//    }

    ProcessManager(String nameSpace, RunnableDispatcher dispatcher) {
        this.nameSpace = nameSpace;
        this.mDispatcher = dispatcher != null ? dispatcher : RunnableDispatcher.DEFAULT;
    }

    public String getNameSpace() {
        return nameSpace;
    }

    /**
     * Opt a process started by the manager and which is always running using his PID.
     *
     * @param PID
     * @return
     */
    public Process getProcessById(String PID) {
        Process process = processQueue.get(PID);
        if (process != null) {
            if (process.isRunning()) {
                return process;
            } else {
                processQueue.remove(process.getId());
            }
        }
        return null;
    }

    public <T extends Process<?, ?>> T getProcessById(String PID, Class<T> cLass) {
        Process<?,?> p = getProcessById(PID);
        if (p != null && cLass == p.getClass()) {
            return (T) p;
        }
        return null;
    }

    /**
     * Determine si un esemble de processus est en cours d'execution a partir de leurs IDs.
     *
     * @param PID
     * @return
     */
    public boolean isRunning(String... PID) {
        for (String pid : PID) {
            if (getProcessById(pid) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine si au mois un des  processus est en cours d'execution a partir de leurs IDs.
     *
     * @param PID
     * @return
     */
    public boolean isLeastOneRunning(String... PID) {
        for (String pid : PID) {
            if (getProcessById(pid) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determinate if Process is currently running.
     *
     * @param PID
     * @return
     */
    public boolean isRunningPID(String PID) {
        return getProcessById(PID) != null;
    }

    public boolean cancelProcess(String PID) {
        Process process = getProcessById(PID);
        boolean out = false;
        if (process != null) {
            out = process.cancel();
        }
        return out;
    }

    /**
     * cancel one or more process.
     *
     * @param ids
     * @return number of canceled process.
     */
    public int cancel(String... ids) {
        int count = 0;
        for (String id : ids) {
            try {
                cancelProcess(id);
                count++;
            } catch (Exception e) {

            }
        }
        return count;

    }

    /**
     * Permet de modifier le PID d'un process qui est cours de fonctionnement.
     *
     * @param PID
     * @param process
     * @throws ProcessException lancé si le process n'est plus en cours de fonctionnement ou si son id ne peut être modifier pour le moment.
     */
    public void setProcessId(String PID, Process process) throws ProcessException {
        switchProcessId(process.getId(), PID);
    }

    /**
     * switch a Running Process id, boot an older to a new PID.
     *
     * @param initialPID initial running process ID
     * @param updatePID  new Process ID to set.
     * @return
     * @throws ProcessException
     */
    public Process switchProcessId(String initialPID, String updatePID) throws ProcessException {
        if (TextUtils.isEmpty(initialPID)) {
            throw new ProcessException("Oups, you looking for a Process with PID=NULL, can't exist one.");
        }
        if (TextUtils.isEmpty(updatePID)) {
            throw new ProcessException("Oups, you atempt to set a NULL value for  Process with ID=" + initialPID);
        }
        if (!processQueue.containsKey(initialPID)) {
            throw new ProcessException("Oups, not running process associated to id=" + initialPID + " manager can't switch id with new PID= " + updatePID);
        }
        if (processQueue.containsKey(updatePID)) {
            throw new ProcessException("Oups, ConcurrentProcessId a running process is alrady associated to this id=" + updatePID + ". ");
        }
        Process process = processQueue.get(initialPID);
        setPID(updatePID, process);
        processQueue.remove(initialPID);
        globalProcessQueue.remove(initialPID);
        return process;
    }

    private void setPID(String id, Process process) {
        process.setId(id);
        if ((process.getFlags() & Process.FLAG_DETACHED) != Process.FLAG_DETACHED) {
            processQueue.put(id, process);
            globalProcessQueue.put(id, process);
        }
    }

    /**
     * Enregistrer un process Listener afin d'être notifié du lancement et de la "Termination" d'un {@link Process}
     *
     * @param listener
     */
    public final void registerProcessListener(ProcessListener listener) {
        if (listener != null && !processListeners.contains(listener)) {
            processListeners.add(listener);
        }
    }

    /**
     * Désenregistrer un Listener qui a été enregistré via {@link ProcessManager#registerProcessListener(ProcessListener)}
     *
     * @param listener
     * @throws Exception si le Listener n'a pas été enregistré ou ne peut pas être désenregistré pour le moment.
     */
    public final void unRegisterProcessListener(ProcessListener listener) throws Exception {
        if (processListeners.contains(listener)) {
            processListeners.remove(listener);
        } else {
            throw new Exception("this listener is not registered to drive manager.");
        }
    }

    private final int unRegisterAllProcessListener() {
        int listenerSize = processListeners.size();
        processListeners.clear();
        return listenerSize;
    }

    static String[] ID_PROPOSITION_CHAR = {"0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};

    private String generateProcessId() {
        Random random = new Random();
        String id = "";
        for (int i = 0; i < SIZE_GENERATED_PID; i++) {
            int index = random
                    .nextInt(ID_PROPOSITION_CHAR.length - 1);
            id += ID_PROPOSITION_CHAR[index];
        }
        return id;
    }

    void notifyProcessEnqueued(final Process process/*, Object[] vars*/) {
        for (ProcessListener listener : processListeners) {
            listener.onProcessStateChanged(process, process.getId(), process.getState());
            listener.onProcessEnqueued(process, process.getId());
        }
    }

    void notifyProcessStarted(final Process process/*, Object[] vars*/) {
        for (ProcessListener listener : processListeners) {
            listener.onProcessStateChanged(process, process.getId(), process.getState());
            listener.onProcessStarted(process, process.getId());
        }
    }


    void notifyProcessFinished(Process process) {
        globalProcessQueue.remove(process.getId());
        processQueue.remove(process.getId());
        for (ProcessListener listener : processListeners) {
            String processId = "";
            if (process != null) {
                processId = process.getId();
            }
            listener.onProcessStateChanged(process, processId, process.getState());
            listener.onProcessFinished(process, processId);
        }
    }

    void notifyProcessStateChanged(Process process) {
        for (ProcessListener listener : processListeners) {
            String processId = "";
            if (process != null) {
                processId = process.getId();
            }
            listener.onProcessStateChanged(process, processId, process.getState());
        }
    }

    public final int getRunningProcessCount() {
        return processQueue.size();
    }

    public final static int getGlobalRunningProcessCount() {
        return globalProcessQueue.size();
    }

    public RunnableDispatcher getDispatcher() {
        return mDispatcher;
    }

    public static class ProcessException extends Exception {
        public ProcessException(String message) {
            super(message);
        }

        public ProcessException(Throwable e) {
            super(e);
        }
    }

    private final RunnableDispatcher mDispatcher;

    public final boolean post(Runnable runnable) {
        if (mDispatcher == null) {
            return false;
        }
        mDispatcher.dispatch(runnable, 0);
        return true;
    }

    public final boolean postDelayed(Runnable runnable, long delayed) {
        if (mDispatcher == null) {
            return false;
        }
        mDispatcher.dispatch(runnable, delayed);
        return false;
    }

    public final void unPost(Runnable runnable) {
        if (mDispatcher == null) {
            return;
        }
        mDispatcher.cancel(runnable);
        return;
    }

    private static boolean isAndroidOs() {
        String osName = System.getProperty("os.name").toLowerCase();
        if ("linux".equals(osName)) {
            if (System.getProperty("java.specification.vendor", "linux").toLowerCase().contains("android")) {
                return true;
            }
        }
        return false;
    }
}
