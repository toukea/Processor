package com.istat.freedev.processor;

import android.text.TextUtils;

import com.istat.freedev.processor.interfaces.ProcessListener;

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

public final class ProcessManager {
    static final ConcurrentHashMap<String, Process> globalProcessQueue = new ConcurrentHashMap<String, Process>();
    final ConcurrentHashMap<String, Process> processQueue = new ConcurrentHashMap<String, Process>();
    final ConcurrentLinkedQueue<ProcessListener> processListeners = new ConcurrentLinkedQueue();
    private static final int SIZE_GENERATED_PID = 16;

    /**
     * Execute process with specific execution variables
     *
     * @param process
     * @param vars
     * @return
     */
    public final Process execute(Process process, Object... vars) {
        String id;
        if (!process.hasId()) {
            id = System.currentTimeMillis() + "";
            while (isRunningPID(id)) {
                id = generateProcessId();
            }
            process.setId(id);
        }
        process.execute(vars);
        notifyProcessStarted(process, vars);
        return process;
    }

    /**
     * Obtenir tout les process de drive encore en vie.
     *
     * @return
     */
    public List<Process> getRunningRProcess() {
        Iterator<String> iterator = processQueue.keySet().iterator();
        List<Process> list = new ArrayList<Process>();
        while (iterator.hasNext()) {
            String id = iterator.next();
            Process process = getProcessById(id);
            list.add(process);
        }
        return list;
    }

    /**
     * whether or not current processManager has running process.
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
     * @throws ProcessException if given id is already used inside the processManager
     */
    public final Process execute(Process process, String PID, Object... vars) throws ProcessException {
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
        return livingProcess;
    }

    ProcessManager() {

    }

    /**
     * Opt a process started by the processManager and which is always running using his PID.
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
     * switch a Running Process id, from an older to a new PID.
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
        if (TextUtils.isEmpty(initialPID)) {
            throw new ProcessException("Oups, you atempt to set a NULL value for  Process with ID=" + initialPID);
        }
        if (!processQueue.containsKey(initialPID)) {
            throw new ProcessException("Oups, not running process associated to id=" + initialPID + " processManager can't switch id with new PID= " + updatePID);
        }
        if (processQueue.containsKey(updatePID)) {
            throw new ProcessException("Oups, ConcurrentProcessId a running process is alrady associated to this id=" + updatePID + ". ");
        }
        Process process = processQueue.get(initialPID);
        process.setId(updatePID);
        processQueue.put(updatePID, process);
        return process;
    }

    /**
     * Enregistrer un process Listener afin d'être notifié du lancement et de la "Termination" d'un {@link Process}
     *
     * @param listener
     */
    public final void registerProcessListener(ProcessListener listener) {
        if (!processListeners.contains(listener)) {
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
            throw new Exception("this listener is not registered to drive processManager.");
        }
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

    private void notifyProcessStarted(final Process process, Object[] vars) {
        String id = process.getId();
        globalProcessQueue.put(id, process);
        processQueue.put(id, process);
        for (ProcessListener listener : processListeners) {
            listener.onProcessCompleted(process, id);
        }
        process.runWhen(new Runnable() {
            @Override
            public void run() {
                notifyProcessCompleted(process);
            }
        }, Process.WHEN_ANYWAY);
    }


    private void notifyProcessCompleted(Process process) {
        globalProcessQueue.remove(process.getId());
        processQueue.remove(process.getId());
        for (ProcessListener listener : processListeners) {
            String processId = "";
            if (process != null) {
                processId = process.getId();
            }
            listener.onProcessCompleted(process, processId);
        }
    }

    public final int getRunningProcessCount() {
        return processQueue.size();
    }

    public final static int gertGlobalRunningProcessCount() {
        return globalProcessQueue.size();
    }

    public static class ProcessException extends Exception {
        public ProcessException(String message) {
            super(message);
        }

        public ProcessException(Throwable e) {
            super(e);
        }
    }
}
