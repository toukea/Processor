package com.istat.freedev.processor.utils;

import com.istat.freedev.processor.Process;
import com.istat.freedev.processor.ProcessManager;
import com.istat.freedev.processor.Processor;

public final class ProcessWrapper<T, Error extends Throwable> extends Process<T, Error> {
    Process<T, Error> wrappedProcess;
    private ProcessWrapper() {

    }

    public Process<T, Error> getWrappedProcess() {
        return wrappedProcess;
    }

    public static <T, Error extends Throwable> Process<T, Error> wrapRunningProcess(Process<T, Error> process, String wrapperPid) throws ProcessManager.ProcessException, IllegalStateException {
        if (!process.isRunning()) {
            throw new IllegalStateException("The given process is not yet running. you schould run it furst before trying to wrap it using the method wrapRunningProcess");
        }
        ProcessWrapper<T, Error> wrapper = new ProcessWrapper<>();
        process.getManager().execute(wrapperPid, wrapper, process);
        return wrapper;
    }

    public static <T, Error extends Throwable> Process<T, Error> wrapRunningProcess(Process<T, Error> process) throws IllegalStateException {
        if (!process.isRunning()) {
            throw new IllegalStateException("The given process is not yet running. you schould run it furst before trying to wrap it using the method wrapRunningProcess");
        }
        ProcessWrapper<T, Error> wrapper = new ProcessWrapper<>();
        process.getManager().execute(wrapper, process);
        return wrapper;
    }

    public static <T, Error extends Throwable> Process<T, Error> wrapAndExecute(Process<T, Error> process) {
        if (process.isRunning()) {
            throw new IllegalStateException("The given process is already running. You can not submit an already running process to the method wrapAndExecute");
        }
        ProcessWrapper<T, Error> wrapper = new ProcessWrapper<>();
        Processor.getDefaultProcessManager().execute(wrapper, process);
        return wrapper;
    }


    @Override
    protected void onExecute(ExecutionVariables executionVariables) throws Exception {
        this.wrappedProcess = executionVariables.getVariable(0);
        wrappedProcess.then(new PromiseCallback<T>() {
            @Override
            public void onPromise(T data) {
                notifySucceed(data);
            }
        }).failure(new PromiseCallback<Throwable>() {
            @Override
            public void onPromise(Throwable data) {
                notifyFailed(data);
            }
        }).error(new PromiseCallback<Error>() {
            @Override
            public void onPromise(Error data) {
                notifyError(data);
            }
        }).abortion(new PromiseCallback<Void>() {
            @Override
            public void onPromise(Void data) {
                notifyAborted();
            }
        });
    }
}
