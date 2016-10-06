package com.istat.freedev.processor;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by istat on 04/10/16.
 */

public final class ProcessManager {

    final static ConcurrentHashMap<String, Process> processQueue = new ConcurrentHashMap<>();
    final static ConcurrentLinkedQueue<ProcessListener> processListeners = new ConcurrentLinkedQueue();

    public final Process execute(Process process, Object... vars) {
        process.execute(vars);
        return process;
    }

    ProcessManager() {

    }

    public Process getProcessById(String PID) {
        return processQueue.get(PID);
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
        for (int i = 0; i < 16; i++) {
            int index = random
                    .nextInt(ID_PROPOSITION_CHAR.length - 1);
            id += ID_PROPOSITION_CHAR[index];
        }
        return id;
    }

    private void notifyProcessStarted(Process process, Object[] vars) {
        String id = System.currentTimeMillis() + "";
        while (getProcessById(id) != null) {
            id = generateProcessId();
        }
        process.setId(id);
        for (ProcessListener listener : processListeners) {
            Object result = null;
            if (process != null) {
                result = process.getResult();
            }
            listener.onProcessCompleted(process, id, result);
        }
    }


    private void notifyProcessCompleted(Process process) {
        for (ProcessListener listener : processListeners) {
            String processId = "";
            Object result = null;
            if (process != null) {
                processId = process.getId();
                result = process.getResult();
            }
            listener.onProcessCompleted(process, processId, result);
        }
    }

    public static interface ProcessListener {
        public void onProcessStarted(Process process, String id);

        public void onProcessCompleted(Process process, String id, Object result);
    }

    public final static class ProcessException extends IllegalAccessException {
        public ProcessException(String message) {
            super(message);
        }
    }
}
