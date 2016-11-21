package com.istat.freedev.processor;

import android.text.TextUtils;

import com.istat.freedev.processor.interfaces.ProcessExecutionListener;
import com.istat.freedev.processor.tools.ProcessTools;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
    public final static int WHEN_SUCCESS = 0, WHEN_ERROR = 1, WHEN_FAIL = 2, WHEN_ANYWAY = 3, WHEN_ABORTED = 4, WHEN_STARTED = 5;
    Result result;
    Error error;
    Exception exception;
    String id;
    final ConcurrentLinkedQueue<ProcessExecutionListener<Result, Error>> executionListeners = new ConcurrentLinkedQueue<ProcessExecutionListener<Result, Error>>();
    private long startingTime = -1, completionTime = -1;
    protected Object[] executionVariables = new Object[0];


    public void setFlag(int flag) {
        this.flag = flag;
    }

    protected void addExecutionListener(ProcessExecutionListener<Result, Error> executionListener) {
        this.executionListeners.add(executionListener);
    }

    public Object[] getExecutionVariables() {
        return executionVariables;
    }

    void execute(Object... vars) {
        geopardise = false;
        try {
            this.executionVariables = vars;
            startingTime = System.currentTimeMillis();
            onExecute(vars);
            notifyProcessStarted();
        } catch (Exception e) {
            notifyProcessStarted();
            notifyProcessFailed(e);
        }
    }

    protected abstract void onExecute(Object... vars);

    protected abstract void onResume();

    protected abstract void onPaused();

    protected abstract void onStopped();

    protected abstract void onCancel();

    public abstract boolean isRunning();

    public abstract boolean isCompleted();

    public abstract boolean isPaused();

    protected void onRestart(int mode) {
    }

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
        restart(RESTART_MODE_ABORT);
    }

    public final static int RESTART_MODE_GEOPARDISE = 0, RESTART_MODE_ABORT = 1;

    public final void restart(int mode) {
        onRestart(mode);
        if (RESTART_MODE_GEOPARDISE == mode) {
            geopardise();
        } else {
            cancel();
        }
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

    public <T extends Process> T sendWhen(final MessageCarrier message, int... when) {
        return sendWhen(message, new Object[0], when);
    }

    public <T extends Process> T sendWhen(final MessageCarrier message, final Object[] messages, int... when) {
        for (int value : when) {
            addFuture(new Runnable() {
                @Override
                public void run() {
                    message.handleMessage(messages);
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

    protected void notifyProcessStarted() {
        if (!geopardise) {
            for (ProcessExecutionListener<Result, Error> executionListener : executionListeners) {
                executionListener.onStart(this);
            }
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_STARTED);
            executeWhen(runnableList);
        }
    }

    protected final void notifyProcessCompleted(boolean state) {
        if (!geopardise) {
            for (ProcessExecutionListener<Result, Error> executionListener : executionListeners) {
                executionListener.onCompleted(this, state);
            }
            executedRunnable.clear();
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_ANYWAY);
            executeWhen(runnableList);
        }
    }

    protected final void notifyProcessSuccess(Result result) {
        if (!geopardise) {
            this.result = result;
            notifyProcessCompleted(true);
            for (ProcessExecutionListener<Result, Error> executionListener : executionListeners) {
                executionListener.onSuccess(this, result);
            }
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_SUCCESS);
            executeWhen(runnableList);
        }
    }


    protected final void notifyProcessError(Error error) {
        if (!geopardise) {
            this.error = error;
            notifyProcessCompleted(false);
            for (ProcessExecutionListener<Result, Error> executionListener : executionListeners) {
                executionListener.onError(this, error);
            }
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_ERROR);
            executeWhen(runnableList);
        }
    }

    protected final void notifyProcessFailed(Exception e) {
        if (!geopardise) {
            this.exception = e;
            notifyProcessCompleted(false);
            for (ProcessExecutionListener<Result, Error> executionListener : executionListeners) {
                executionListener.onFail(this, e);
            }
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_FAIL);
            executeWhen(runnableList);
        }
    }


    protected final void notifyProcessAborted() {
        if (!geopardise) {
            for (ProcessExecutionListener<Result, Error> executionListener : executionListeners) {
                executionListener.onAborted(this);
            }
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(WHEN_ABORTED);
            executeWhen(runnableList);
        }
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
        ProcessTools.attachToProcessCycle(this, listener);
    }

    public int removeAllExecutionListener() {
        int listenerCount = executionListeners.size();
        executionListeners.clear();
        return listenerCount;
    }

    public boolean removeExecutionListener(ProcessExecutionListener listener) {
        boolean removed = executionListeners.contains(listener);
        if (removed) {
            executionListeners.remove(listener);
        }
        return removed;
    }

    public boolean cancelWhen(Runnable runnable) {
        Iterator<Integer> iterator = runnableTask.keySet().iterator();
        while (iterator.hasNext()) {
            Integer when = iterator.next();
            ConcurrentLinkedQueue<Runnable> runnables = runnableTask.get(when);
            if (runnables != null) {
                boolean removed = runnables.contains(runnable);
                if (removed) {
                    runnables.remove(runnable);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean cancelWhen(int When) {
        boolean removed = runnableTask.contains(When);
        runnableTask.remove(When);
        return removed;
    }

    public abstract static class MessageCarrier {
        List<Object> messages;

        void handleMessage(Object... messages) {
            Collections.addAll(this.messages, messages);
            onHandleMessage(messages);
        }

        public abstract void onHandleMessage(Object... messages);

        public List<Object> getMessages() {
            return messages;
        }
    }


}
