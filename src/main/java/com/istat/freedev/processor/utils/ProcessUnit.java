package com.istat.freedev.processor.utils;

import com.istat.freedev.processor.ProcessManager;
import com.istat.freedev.processor.Processor;
import com.istat.freedev.processor.Process;
import com.istat.freedev.processor.interfaces.ProcessListener;

/**
 * Created by istat on 07/02/17.
 */

public class ProcessUnit {
    Processor processor;

    protected ProcessUnit() {
        String nameSpace = this.getClass().getCanonicalName();
        processor = Processor.from(nameSpace);
    }

    protected ProcessUnit(String nameSpace) {
        processor = Processor.from(nameSpace);
    }

    public Processor getProcessor() {
        return processor;
    }

    public ProcessManager getProcessManager() {
        return processor.getProcessManager();
    }

    public String getNameSpace() {
        return this.processor.getNameSpace();
    }

    public boolean cancel() {
        return getProcessManager().release() > 0;
    }

    public ProcessUnit registerProcessListener(ProcessListener listener) {

        getProcessManager().registerProcessListener(listener);
        return this;
    }

    public boolean unRegisterProcessListener(ProcessListener listener) {
        try {
            getProcessManager().unRegisterProcessListener(listener);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isComputing() {
        return getProcessManager().hasRunningProcess();
    }

    public <R, E extends Throwable> Process<R, E> execute(Process<R, E> process, Object... vars) {
        return getProcessManager().execute(process, vars);
    }

    public <R, E extends Throwable> Process<R, E> execute(String PID, Process<R, E> process, Object... vars) throws ProcessManager.ProcessException {
        return getProcessManager().execute(PID, process, vars);
    }
}
