package com.istat.freedev.processor;

import android.text.TextUtils;

import com.istat.freedev.processor.interfaces.ProcessExecutionListener;
import com.istat.freedev.processor.tools.Toolkit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by istat on 04/10/16.
 */

public abstract class Process<Result, Error extends Process.ProcessError> {
    public final static int FLAG_SYS_DEFAUlT = 0;
    public final static int FLAG_SYS_CANCELABLE = 1;
    public final static int FLAG_USER_CANCELABLE = 1;
    public final static int FLAG_BACKGROUND = 2;
    public final static int FLAG_DETACHED = 3;
    int flag;
    public final static int WHEN_SUCCESS = 0, WHEN_ERROR = 1, WHEN_FAIL = 2, WHEN_ANYWAY = 3, WHEN_ABORTED = 4;
    Result result;
    Error error;
    Exception exception;
    String id;
    ProcessExecutionListener executionListener;
    private long startingTime = -1, completionTime = -1;
    protected Object[] executionVariables = new Object[0];


    public void setFlag(int flag) {
        this.flag = flag;
    }

    protected void setExecutionListener(ProcessExecutionListener<Result, Error> executionListener) {
        this.executionListener = executionListener;
    }

    void execute(Object... vars) {
        this.executionVariables = vars;
        startingTime = System.currentTimeMillis();
        onExecute(vars);
    }

    protected abstract void onExecute(Object... vars);

    protected abstract void onResume();

    protected abstract void onPaused();

    protected abstract void onStopped();

    protected abstract void onCancel();

    public abstract boolean isRunning();

    public abstract boolean isCompleted();

    public abstract boolean isPaused();

    public Error getError() {
        return error;
    }

    public Exception getFailCause() {
        return exception;
    }

    public <T extends Error> T optError() {
        try {
            return (T) error;
        } catch (Exception e) {
            return null;
        }
    }

    public Result getResult() {
        return result;
    }

    public <T> T optResult() {
        try {
            return (T) result;
        } catch (Exception e) {
            return null;
        }
    }

    public final boolean hasResult() {
        return result != null;
    }

    protected final void setResult(Result result) {
        this.result = result;
    }

    public final String getId() {
        return id;
    }

    final void setId(String id) {
        this.id = id;
    }

    public final void pause() {
        onPaused();
    }

    public final void resume() {
        onResume();
    }

    public final void restart() {
        cancel();
        execute(this.executionVariables);
    }

    public final void stop() {
        onStopped();
    }

    public final boolean cancel() {
        boolean running = isRunning();
        if (running) {
            onCancel();
        }
        return running;
    }

    public final boolean compromiseWhen(int... when) {
        boolean cancelled = cancel();
        return cancelled;
    }

    public final boolean compromise() {
        boolean cancelled = cancel();

        return cancelled;

    }

    boolean hasId() {
        return !TextUtils.isEmpty(getId());
    }


    public static class ProcessError extends ProcessManager.ProcessException {

        public ProcessError(String message) {
            super(message);
        }

        public ProcessError(Exception exception) {
            super(exception);
        }
    }

    final ConcurrentLinkedQueue<Runnable> executedRunnable = new ConcurrentLinkedQueue<Runnable>();
    final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Runnable>> runnableTask = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Runnable>>();

    public <T extends Process> T runWhen(Runnable runnable, int... when) {
        for (int value : when) {
            addFuture(runnable, value);
        }
        return (T) this;
    }

    private void addFuture(Runnable runnable, int conditionTime) {
        if (!isFutureContain(runnable, conditionTime)) {
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(conditionTime);
            if (runnableList == null) {
                runnableList = new ConcurrentLinkedQueue<Runnable>();
            }
            runnableList.add(runnable);
            runnableTask.put(conditionTime, runnableList);
        }
    }

    private boolean isFutureContain(Runnable run, int conditionTime) {
        ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(conditionTime);
        if (runnableList == null || runnableList.isEmpty()) {
            return false;
        }
        return runnableList.contains(run);
    }

    public final long getStartingTime() throws ProcessManager.ProcessException {
        if (startingTime < 0) {
            throw new ProcessManager.ProcessException("Oups, it seem than this process is not yet started.");
        }
        return startingTime;
    }

    public final long getCompletionTime() throws ProcessManager.ProcessException {
        if (completionTime < 0) {
            throw new ProcessManager.ProcessException("Oups, it seem than this process is not yet completed.");
        }
        return completionTime;
    }

    public final long getLinvingTime() {
        if (startingTime < 0) {
            return 0;
        }
        return System.currentTimeMillis() - startingTime;
    }

    private void executeWhen(ConcurrentLinkedQueue<Runnable> runnableList) {
        for (Runnable runnable : runnableList) {
            if (!executedRunnable.contains(runnable)) {
                runnable.run();
                executedRunnable.add(runnable);
            }
        }
    }

    protected final void notifyProcessCompleted(boolean state) {
        if (executionListener != null) {
            executionListener.onCompleted(this, state);
        }
        executedRunnable.clear();
        ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_ANYWAY);
        executeWhen(runnableList);
    }

    protected final void notifyProcessSuccess(Result result) {
        notifyProcessCompleted(true);
        if (executionListener != null) {
            executionListener.onSuccess(this, result);
        }
        ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_SUCCESS);
        executeWhen(runnableList);
    }


    protected final void notifyProcessError(ProcessError error) {
        notifyProcessCompleted(false);
        if (executionListener != null) {
            executionListener.onError(this, error);
        }
        ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_ERROR);
        executeWhen(runnableList);
    }

    protected final void notifyProcessFailed(Exception e) {
        notifyProcessCompleted(false);
        if (executionListener != null) {
            executionListener.onFail(this, e);
        }
        ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_FAIL);
        executeWhen(runnableList);
    }


    protected final void notifyProcessAborted() {
        if (executionListener != null) {
            executionListener.onAborted(this);
        }
        ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_ABORTED);
        executeWhen(runnableList);
    }

    public boolean hasError() {
        return error != null;
    }

    public boolean isFailed() {
        return exception != null;
    }

    public boolean isSuccess() {
        return !hasError() && !isFailed();
    }

    public void attach(ProcessExecutionListener<Result, Error> listener) {
        Toolkit.attachAsProcessWhen(this, listener);
    }


}
