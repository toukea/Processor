package com.istat.freedev.processor;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by istat on 04/10/16.
 */

public final class Processor {
    public final static String DEFAULT_PROCESSOR_TAG = "com.istat.freedev.processor.DEFAULT";
    final static ConcurrentHashMap<String, Processor> processorQueue = new ConcurrentHashMap<String, Processor>() {
        {
            put(DEFAULT_PROCESSOR_TAG, new Processor());
        }
    };
    final static ProcessManager defaultProcessManager = new ProcessManager();
    final ProcessManager processManager = new ProcessManager();

    public final static ProcessManager getDefaultProcessManager() {
        return defaultProcessManager;
    }

    public final static Processor getDefault() {
        return processorQueue.get(DEFAULT_PROCESSOR_TAG);
    }

    public final static Processor from(String processorTag) {
        if (processorQueue.contains(processorTag)) {
            return processorQueue.get(processorTag);
        }
        Processor processor = new Processor();
        processorQueue.put(processorTag, processor);
        return processor;
    }

    public final Process execute(Process process, Object... vars) {
        return getProcessManager().execute(process, vars);
    }

    public final Process execute(Process process, String PID, Object... vars) throws ProcessManager.ProcessException {
        return getProcessManager().execute(process, PID, vars);
    }

    public int shutDown() {
        int runningProcess = release();
        processorQueue.remove(this);
        return runningProcess;
    }

    public int release() {
        int runningProcess = getProcessManager().getRunningProcessCount();
        getProcessManager().cancelAll();
        return runningProcess;
    }

    public ProcessManager getProcessManager() {
        return processManager;
    }

    public final static int count() {
        return processorQueue.size();
    }

    public final int getProcessCount() {
        return getProcessManager().getRunningProcessCount();
    }

    public final boolean hasWork() {
        return getProcessCount() > 0;
    }

    public final static int shutDownAll() {
        Iterator<String> iterator = processorQueue.keySet().iterator();
        int count = 0;
        while (iterator.hasNext()) {
            String name = iterator.next();
            processorQueue.get(name).shutDown();
            count++;
        }
        processorQueue.put(DEFAULT_PROCESSOR_TAG, new Processor());
        return count;
    }

    public final static int releaseAll() {
        Iterator<String> iterator = processorQueue.keySet().iterator();
        int count = 0;
        while (iterator.hasNext()) {
            String name = iterator.next();
            processorQueue.get(name).release();
            count++;
        }
        return count;
    }
}