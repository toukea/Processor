package com.istat.freedev.processor.interfaces;

import android.text.TextUtils;

import com.istat.freedev.processor.Process;
import com.istat.freedev.processor.ProcessManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by istat on 23/09/17.
 */

public interface Processable<Result, Error extends Throwable> {
    ProcessManager getManager();

    public void addProcessCallback(ProcessCallback<Result, Error> executionListener) {
        if (executionListener != null) {
            this.processCallbacks.add(executionListener);
        }
    }

    public Process.ExecutionVariables getExecutionVariables() {
        return new Process.ExecutionVariables();
    }

    public <T> T getExecutionVariable(int index) {
        return getExecutionVariables().getVariable(index);
    }

    final void execute(ProcessManager manager, Object... vars) {
        this.manager = manager;
        geopardise = false;
        try {
            this.executionVariableArray = vars;
            startingTime = System.currentTimeMillis();
//            memoryRunnableTask.putAll(runnableTask);
            onExecute(getExecutionVariables());
            notifyProcessStarted();
        } catch (Exception e) {
            notifyProcessStarted();
            notifyProcessFailed(e);
        }
    }

    final void reset() {
        geopardise = false;
        executedRunnable.clear();
//        runnableTask.putAll(memoryRunnableTask);
        try {
            onExecute(getExecutionVariables());
        } catch (Exception e) {
            notifyProcessStarted();
            notifyProcessFailed(e);
        }
    }

    protected abstract void onExecute(Process.ExecutionVariables executionVariables);

    protected abstract void onResume();

    protected abstract void onPaused();

    protected abstract void onStopped();

    protected abstract void onCancel();

    public abstract boolean isRunning();

    public abstract boolean isCompleted();

    public abstract boolean isPaused();

    public boolean isCanceled() {
        return canceled;
    }

    protected void onRestart(int mode) {
    }

    public Error getError() {
        return error;
    }

    public Throwable getFailCause() {
        return exception;
    }

    public <T> T getErrortAs(Class<T> cLass) {
        if (error != null) {
            if (cLass.isAssignableFrom(error.getClass())) {
                return (T) error;
            }
            if (CharSequence.class.isAssignableFrom(cLass)) {
                return (T) error.toString();
            }
        }
        return null;
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

    public <T> T getResultAs(Class<T> cLass) {
        if (result != null) {
            if (cLass.isAssignableFrom(result.getClass())) {
                return (T) result;
            }
            if (CharSequence.class.isAssignableFrom(cLass)) {
                return (T) result.toString();
            }
        }
        return null;
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
        restart(RESTART_MODE_ABORT);
    }

    public final static int RESTART_MODE_GEOPARDISE = 0, RESTART_MODE_ABORT = 1;
    private final static int TIME_MILLISEC_WAIT_FOR_RESTART = 100;

    public final void restart(int mode) {
        onRestart(mode);
        if (isRunning()) {
            if (RESTART_MODE_GEOPARDISE == mode) {
                geopardise = true;
            }
            cancel();
        }
        final Object[] executionVars = this.executionVariableArray;
        getManager().postDelayed(new Runnable() {
            @Override
            public void run() {
//                execute(Process.this.manager, executionVars);
                reset();
            }
        }, TIME_MILLISEC_WAIT_FOR_RESTART);

    }


    public final void stop() {
        onStopped();
    }

    public final boolean cancel() {
        boolean running = isRunning();
        if (running) {
            canceled = true;
            onCancel();
        }
        return running;
    }

    public final boolean compromiseWhen(int... when) {
        boolean running = isRunning();
        if (when != null || when.length == 0) {
            when = new int[]{WHEN_ABORTED, WHEN_ANYWAY, WHEN_ERROR, WHEN_FAIL, WHEN_STARTED, WHEN_SUCCESS};
        }
        for (int i : when) {
            if (runnableTask.containsKey(i)) {
                try {
                    runnableTask.get(i).clear();
                } catch (Exception e) {

                } finally {
                    runnableTask.remove(i);
                }
            }
        }
        return running;
    }

    boolean geopardise = false;

    public final boolean hasBeenGeopardise() {
        return geopardise;
    }

    public final boolean geopardise() {
        geopardise = true;
        compromiseWhen();
        boolean cancelled = cancel();
        return cancelled;

    }

    boolean hasId() {
        return !TextUtils.isEmpty(getId());
    }


    final ConcurrentLinkedQueue<Runnable> executedRunnable = new ConcurrentLinkedQueue<Runnable>();
    //    final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Runnable>> memoryRunnableTask = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Runnable>>();
    final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Runnable>> runnableTask = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Runnable>>();

    public <T extends Process> T runWhen(Runnable runnable, int... when) {
//        if(!isRunning()){
//            throw new IllegalStateException("Oups, current Process is not running. It has to be running before adding any runWhen or promise");
//        }
        for (int value : when) {
            addFuture(runnable, value);
        }
        return (T) this;
    }


    public <T extends Process> T sendWhen(final Process.MessageCarrier message, int... when) {
        return sendWhen(message, new Object[0], when);
    }

    public <T extends Process> T sendWhen(final Process.MessageCarrier carrier, final Object[] messages, int... when) {
        for (int value : when) {
            addFuture(new Runnable() {
                @Override
                public void run() {
                    carrier.process = Process.this;
                    carrier.handleMessage(messages);
                }
            }, value);
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
        if (!geopardise && runnableList != null && runnableList.size() > 0) {
            for (Runnable runnable : runnableList) {
                if (!executedRunnable.contains(runnable)) {
                    runnable.run();
                    executedRunnable.add(runnable);
                }
            }
        }
    }

    protected void onCompleted(boolean state, Result result, Error error) {

    }

    protected void onSucceed(Result result) {

    }

    protected void onError(Error error) {

    }

    protected void onFailed(Throwable e) {

    }

    protected void onAborted() {

    }

    protected void notifyProcessStarted() {
        if (!geopardise) {
            this.state = WHEN_STARTED;
            for (ProcessCallback<Result, Error> executionListener : processCallbacks) {
                executionListener.onStart(this);
            }
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_STARTED);
            executeWhen(runnableList);
        }
    }

    final void notifyProcessCompleted(boolean state) {
        if (!geopardise) {
            for (ProcessCallback<Result, Error> executionListener : processCallbacks) {
                executionListener.onCompleted(this, this.result, state);
            }
            executedRunnable.clear();
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_ANYWAY);
            executeWhen(runnableList);
            onCompleted(state, result, error);
        }
    }

    protected final void notifyProcessPartialSuccess(Result result) {
        if (!geopardise) {
            this.state = WHEN_SUCCESS;
            this.result = result;
            for (ProcessCallback<Result, Error> executionListener : processCallbacks) {
                executionListener.onSuccess(this, result);
            }
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_SUCCESS);
            executeWhen(runnableList);
            onSucceed(result);
        }
    }

    protected final void notifyProcessSuccess(Result result) {
        if (!geopardise) {
            this.state = WHEN_SUCCESS;
            this.result = result;
            notifyProcessCompleted(true);
            for (ProcessCallback<Result, Error> executionListener : processCallbacks) {
                executionListener.onSuccess(this, result);
            }
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_SUCCESS);
            executeWhen(runnableList);
            onSucceed(result);
        }
    }

    protected final void notifyProcessError(Error error) {
        if (!geopardise) {
            this.state = WHEN_ERROR;
            this.error = error;
            notifyProcessCompleted(false);
            for (ProcessCallback<Result, Error> executionListener : processCallbacks) {
                executionListener.onError(this, error);
            }
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_ERROR);
            executeWhen(runnableList);
            onError(error);
        }
    }


    protected final void notifyProcessFailed(Throwable e) {
        if (!geopardise) {
            this.state = WHEN_FAIL;
            this.exception = e;
            notifyProcessCompleted(false);
            for (ProcessCallback<Result, Error> executionListener : processCallbacks) {
                executionListener.onFail(this, e);
            }
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_FAIL);
            executeWhen(runnableList);
            onFailed(e);
        }
    }


    protected final void notifyProcessAborted() {
        if (!geopardise) {
            this.state = WHEN_ABORTED;
            for (ProcessCallback<Result, Error> executionListener : processCallbacks) {
                executionListener.onAborted(this);
            }
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_ABORTED);
            executeWhen(runnableList);
            onAborted();
        }
    }


    protected final void notifyDelayedProcessAborted(int delay) {
        if (delay <= 0) {
            notifyProcessAborted();
        } else {
            manager.postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifyProcessAborted();
                }
            }, delay);
        }
    }

    protected final void notifyDelayedProcessFailed(final Exception e, int delay) {
        if (delay <= 0) {
            notifyProcessFailed(e);
        } else {
            manager.postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifyProcessFailed(e);
                }
            }, delay);
        }
    }

    protected final void notifyDelayedProcessError(final Error error, int delay) {
        if (delay <= 0) {
            notifyProcessError(error);
        } else {
            manager.postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifyProcessError(error);
                }
            }, delay);
        }
    }

    boolean hasError();

    boolean isFailed();

    boolean isSuccess();

    int getState();
}
