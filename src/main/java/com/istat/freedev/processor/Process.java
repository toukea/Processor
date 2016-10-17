package com.istat.freedev.processor;

import android.text.TextUtils;

import com.istat.freedev.processor.interfaces.ProcessExecutionListener;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by istat on 04/10/16.
 */

public abstract class Process {
    public final static int WHEN_SUCCESS = 0, WHEN_ERROR = 1, WHEN_FAIL = 2, WHEN_ANYWAY = 3, WHEN_ABORTED = 4;
    Object result;
    String id;
    ProcessExecutionListener executionListener;
    ProcessExecutionListener mListener;

    public void setExecutionListener(ProcessExecutionListener executionListener) {
        this.executionListener = executionListener;
    }

    void execute(Object... vars) {
        onExecute(vars);
    }

    protected abstract void onExecute(Object... vars);

    protected abstract void onResume();

    protected abstract void onPaused();

    protected abstract void onStopped();

    protected abstract void onCancel();

    protected abstract void onRestart();

    public abstract boolean isRunning();

    public abstract boolean isCompleted();

    public abstract boolean isPaused();

    public <T> T getResult() {
        return null;
    }

    public Object getResultEntity() {
        return null;
    }

    public final boolean hasResult() {
        return result != null;
    }

    protected final void setResult(Object result) {
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
        onRestart();
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
        // TODO make something god here
        boolean cancelled = cancel();

        return cancelled;
    }

    public final boolean compromise() {
        // TODO make something god here
        boolean cancelled = cancel();

        return cancelled;

    }

    boolean hasId() {
        return !TextUtils.isEmpty(getId());
    }


    public static class ProcessError extends RuntimeException {
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

    class EventDispatcher implements ProcessExecutionListener {
        // TODO make something god here
        @Override
        public void onSucceed(Process process) {

        }

        @Override
        public void onError(Process process) {

        }

        @Override
        public void onFail(Process process, Exception e) {

        }

        @Override
        public void onAborted(Process process) {

        }

    }

    protected final void notifyProcessCompleted() {

    }

    protected final void notifyProcessFailled(Exception e) {
        dispatchFailEvent(e);
    }

    protected final void dispatchFailEvent(Exception e) {

    }
}
