package com.istat.freedev.processor;

/**
 * Created by istat on 04/10/16.
 */

public abstract class Process {
    Object result;
    String id;
    ExecutionListener mListener;

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

    protected final void setResult(Object result) {
        this.result = result;
    }

    public String getId() {
        return id;
    }

    void setId(String id) {
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

    public static interface ExecutionListener {
        public void onSucced(Process process);
    }
}
