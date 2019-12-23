package com.istat.freedev.processor;

import com.istat.freedev.processor.interfaces.RunnableDispatcher;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by istat on 04/10/16.
 */

public class Processor {
    public final static String DEFAULT_PROCESSOR_TAG = "com.istat.freedev.processor.DEFAULT";
    final static ConcurrentHashMap<String, Processor> processorQueue = new ConcurrentHashMap<String, Processor>() {
        {
            put(DEFAULT_PROCESSOR_TAG, new Processor(DEFAULT_PROCESSOR_TAG));
        }
    };
    final static ProcessManager defaultProcessManager = new ProcessManager();
    final ProcessManager processManager;
    String nameSpace;

    public final static ProcessManager getDefaultProcessManager() {
        return defaultProcessManager;
    }

    public final static Processor getDefault() {
        return processorQueue.get(DEFAULT_PROCESSOR_TAG);
    }

    Processor(String nameSpace) {
        this(nameSpace, null);
    }

    Processor(String nameSpace, RunnableDispatcher runnableDispatcher) {
        this.nameSpace = nameSpace;
        this.processManager = new ProcessManager(nameSpace, runnableDispatcher);
    }

    public RunnableDispatcher getDispatcher() {
        return getProcessManager().getDispatcher();
    }

    //TODO si il est possible de trouver un meilleur nom qu eboot
    public final static Processor boot(String processorTag) {
        return boot(processorTag, null);
    }

    public final static Processor boot(String processorTag, RunnableDispatcher runnableDispatcher) {
        if (processorQueue.contains(processorTag)) {
            Processor processor = processorQueue.get(processorTag);
            if (processor.getDispatcher() == runnableDispatcher) {
                return processor;
            }
        }
        Processor processor = new Processor(processorTag, runnableDispatcher);
        processorQueue.put(processorTag, processor);
        return processor;
    }

    public final static Processor get(String processorTag) {
        if (processorQueue.contains(processorTag)) {
            return processorQueue.get(processorTag);
        }
        return null;
    }


    public final Process execute(Process process, Object... vars) {
        return getProcessManager().execute(process, vars);
    }

    public final Process execute(String PID, Process process, Object... vars) throws ProcessManager.ProcessException {
        return getProcessManager().execute(PID, process, vars);
    }

    public final int shutDown() {
        onShutdown();
        int runningProcess = release();
        processorQueue.remove(this);
        return runningProcess;
    }


    public final int release() {
        onRelease();
        int runningProcess = getProcessManager().getRunningProcessCount();
        getProcessManager().release();
        return runningProcess;
    }

    protected void onRelease() {
    }

    protected void onShutdown() {
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

    public final static ProcessManager getProcessManager(String processorTag) {
        Processor processor = null;
        if (processorQueue.contains(processorTag)) {
            processor = processorQueue.get(processorTag);
        }
        if (processor != null) {
            return processor.getProcessManager();
        }
        return null;
    }

    public final boolean hasWork() {
        return getProcessCount() > 0;
    }

    @Deprecated
    public final static int shutDownAll() {
        Iterator<String> iterator = processorQueue.keySet().iterator();
        int count = 0;
        while (iterator.hasNext()) {
            String name = iterator.next();
            processorQueue.get(name).shutDown();
            count++;
        }
        processorQueue.put(DEFAULT_PROCESSOR_TAG, new Processor(DEFAULT_PROCESSOR_TAG));
        return count;
    }

    @Deprecated
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

    public String getNameSpace() {
        return nameSpace;
    }


}