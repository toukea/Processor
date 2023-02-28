package com.istat.freedev.processor;

import com.istat.freedev.processor.interfaces.ProcessCallback;
import com.istat.freedev.processor.utils.ProcessTools;
import com.istat.freedev.processor.utils.ToolKits;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by istat on 04/10/16.
 */

public abstract class Process<Result, Error extends Throwable> {
    public final static int FLAG_DETACHED = 1;
    public final static int FLAG_DONT_CLEAR_ON_FINISH = 2;
    public final static int FLAG_NOT_CANCELABLE = 4;
    int flags;
    public final static int
            STATE_IDLE = -1,
            STATE_STARTED = 7,
            STATE_PROCESSING = 31,
            STATE_SUCCESS = 255,
            STATE_ERROR = 127,
            STATE_FAILED = 111,
            STATE_ABORTED = 95,
            STATE_PENDING = 15,
            STATE_DROPPED = 1,
            STATE_FLAG_FINISHED = 65;
    Result result;
    Error error;
    Throwable exception;
    String id;
    final ConcurrentHashMap<PromiseCallback, Runnable> promiseRunnableMap = new ConcurrentHashMap<>();
    final ConcurrentLinkedQueue<ProcessCallback<Result, Error>> processCallbacks = new ConcurrentLinkedQueue();
    private long startingTime = -1, finishTime = -1;
    private Object[] executionVariableArray = new Object[0];
    ProcessManager manager;
    int state = STATE_IDLE;
    boolean canceled = false;
    boolean running = false;

    public ProcessManager getManager() {
        return manager;
    }

    public boolean isManaged() {
        return manager != null;
    }

    public void setFlags(int flag) {
        if (isRunning()) {
            throw new IllegalStateException("Process is already started");
        }
        this.flags = flag;
    }

    public void addFlags(int flags) {
        if (isRunning()) {
            throw new IllegalStateException("Process is already started");
        }
        this.flags |= flags;
    }

    public int getFlags() {
        return flags;
    }

    public void addCallback(ProcessCallback<Result, Error> executionListener) {
        if (executionListener != null && executionListener != null) {
            this.processCallbacks.add(executionListener);
        }
    }

    public boolean removeCallback(ProcessCallback executionListener) {
        if (executionListener == null) {
            return false;
        }
        return this.processCallbacks.remove(executionListener);
    }

    public void removeCallbacks() {
        this.processCallbacks.clear();
    }

    public ExecutionVariables getExecutionVariables() {
        return new ExecutionVariables();
    }

    public <T> T getExecutionVariable(int index) {
        return getExecutionVariables().getVariable(index);
    }

    final void execute(ProcessManager manager, Object... vars) {
        this.manager = manager;
        jeopardise = false;
        try {
            this.executionVariableArray = vars;
            startingTime = System.currentTimeMillis();
//            memoryRunnableTask.putAll(runnableTask);
            notifyStarted();
            onExecute(getExecutionVariables());
            if (running) {
                notifyStateChanged(STATE_PROCESSING, false);
            }
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    final void reset() {
        jeopardise = false;
        executedRunnable.clear();
//        runnableTask.putAll(memoryRunnableTask);
        try {
            onExecute(getExecutionVariables());
        } catch (Exception e) {
            notifyStarted();
            notifyFailed(e);
        }
    }

    protected void onStarted() {

    }

    protected abstract void onExecute(ExecutionVariables executionVariables) throws Exception;

    protected void onResume() {
    }

    protected void onPaused() {
    }

    protected void onStopped() {
    }

    protected void onCancel() {
    }

    protected void onHotSwapping(ExecutionVariables executionVariables) {

    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Called when the process is now finished, include when the process was canceled
     *
     * @return
     */
    public boolean isCompleted() {
        return !running && !canceled && (state == STATE_SUCCESS || exception != null || error != null);
    }

    /**
     * Called when the process is now finished, but was not canceled
     *
     * @return
     */
    public boolean isFinished() {
        return !running && (result != null ||
                exception != null ||
                error != null ||
                state == STATE_SUCCESS ||
                state == STATE_ERROR ||
                state == STATE_FAILED);
    }

    public boolean isPaused() {
        return false;
    }

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

    public <T> T getErrorAs(Class<T> cLass) {
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

    public void doHotSwapping(Object... payload) {
        executionVariableArray = payload;
        onHotSwapping(getExecutionVariables());
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
                jeopardise = true;
            }
            cancel();
        }
//        final Object[] executionVars = this.executionVariableArray;
        getManager().postDelayed(new Runnable() {
            @Override
            public void run() {
//                execute(Process.getManager(), executionVars);
                reset();
            }
        }, TIME_MILLISEC_WAIT_FOR_RESTART);

    }


    public final void stop() {
        onStopped();
    }

    public final boolean cancel() {
        if ((flags & FLAG_NOT_CANCELABLE) == FLAG_NOT_CANCELABLE) {
            return false;
        }
        try {
            if (running) {
                canceled = true;
                onCancel();
                notifyAborted();
            }
            this.running = false;
            this.processCallbacks.clear();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

//    public boolean compromise(int When) {
//        boolean removed = runnableTask.contains(When);
//        runnableTask.remove(When);
//        return removed;
//    }

    public final boolean compromise(int... when) {
        boolean running = isRunning();
        if (when == null || when.length == 0) {
            when = new int[]{STATE_ABORTED, STATE_FLAG_FINISHED, STATE_ERROR, STATE_FAILED, STATE_STARTED, STATE_SUCCESS};
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

    boolean jeopardise = false;

    public final boolean hasBeenJeopardise() {
        return jeopardise;
    }

    //TODO bien réfléchir au comportement de cette method
    public final boolean jeopardise() {
        jeopardise = true;
        compromise();
        boolean cancelled = cancel();
        return cancelled;

    }

    boolean hasId() {
        return !ToolKits.isEmpty(getId());
    }


    final ConcurrentLinkedQueue<Runnable> executedRunnable = new ConcurrentLinkedQueue<Runnable>();
    //    final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Runnable>> memoryRunnableTask = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Runnable>>();
    final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Runnable>> runnableTask = new ConcurrentHashMap();


    public <T extends Process<Result, Error>> T promise(final PromiseCallback<T> callback, int[] when, long timeout) {
        T process = promise(callback, when);
        postDelayed(() -> {
            compromise(callback);
        }, timeout);
        return process;
    }

    public <T extends Process> T promise(final PromiseCallback<T> callback, int... when) {
        if (callback != null) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        callback.onPromise((T) Process.this);
                    }
                }
            };
            promiseRunnableMap.put(callback, runnable);
            return promise(runnable, when);
        }
        return (T) this;
    }

    private void removeAttachedPromise(Runnable runnable) {

    }

    public <T extends Process> T promise(Runnable runnable, int... when) {
        if (runnable == null) {
            return (T) this;
        }
        if (isCompleted()) {
            for (int time : when) {
                if (time == STATE_FLAG_FINISHED || state == time) {
                    runnable.run();
                }
            }
            return (T) this;
        }
        for (int value : when) {
            addPromise(runnable, value);
        }
        return (T) this;
    }

    public <T extends Process<Result, Error>> T then(final PromiseCallback<Result> promise, long timeout) {
        T process = then(promise);
        postDelayed(() -> {
            compromise(promise);
        }, timeout);
        return process;
    }

    public <T extends Process<Result, Error>> T then(final PromiseCallback<Result> promise) {
        if (promise == null) {
            return (T) this;
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                promise.onPromise(getResult());
            }
        };
        if (hasSucceed()) {
//            throw new IllegalStateException("Oups, current Process is not running. It has to be running before adding any promise or promise");
            runnable.run();
            return (T) this;
        }
        addPromise(runnable, STATE_SUCCESS);
        return (T) this;
    }

    public <T extends Process<Result, Error>> T error(final PromiseCallback<Error> promise, long timeout) {
        T process = error(promise);
        postDelayed(() -> {
            compromise(promise);
        }, timeout);
        return process;
    }

    public <T extends Process> T error(final PromiseCallback<Error> promise) {
        if (promise == null) {
            return (T) this;
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                promise.onPromise(getError());
            }
        };
        if (hasError()) {
//            throw new IllegalStateException("Oups, current Process is not running. It has to be running before adding any promise or promise");
            runnable.run();
            return (T) this;
        }
        addPromise(runnable, STATE_ERROR);
        return (T) this;
    }

    public <T extends Process<Result, Error>> T failure(final PromiseCallback<Throwable> promise, long timeout) {
        T process = failure(promise);
        postDelayed(() -> {
            compromise(promise);
        }, timeout);
        return process;
    }

    public <T extends Process<Result, Error>> T failure(final PromiseCallback<Throwable> promise) {
        if (promise == null) {
            return (T) this;
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                promise.onPromise(getFailCause());
            }
        };
        if (hasException()) {
//            throw new IllegalStateException("Oups, current Process is not running. It has to be running before adding any promise or promise");
            runnable.run();
            return (T) this;
        }
        addPromise(runnable, STATE_FAILED);
        return (T) this;
    }

    public <T extends Process<Result, Error>> T catchException(final PromiseCallback<Throwable> promise) {
        if (promise == null) {
            return (T) this;
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Throwable error = getException();
//                if (error == null) {
//                    error = new InterruptedException("Process has been canceled");
//                }
                promise.onPromise(error);
            }
        };
        if (hasError() || hasException()) {
//            throw new IllegalStateException("Oups, current Process is not running. It has to be running before adding any promise or promise");
            runnable.run();
            return (T) this;
        }
        addPromise(runnable, STATE_FAILED);
        addPromise(runnable, STATE_ERROR);
//        addFuture(runnable, STATE_ABORTED);
        return (T) this;
    }

    public <T extends Process<Result, Error>> T abortion(final PromiseCallback<Void> promise, long timeout) {
        T process = abortion(promise);
        postDelayed(() -> {
            compromise(promise);
        }, timeout);
        return process;
    }

    public <T extends Process<Result, Error>> T abortion(final PromiseCallback<Void> promise) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                promise.onPromise(null);
            }
        };
        if (isCanceled()) {
//            throw new IllegalStateException("Oups, current Process is not running. It has to be running before adding any promise or promise");
            runnable.run();
            return (T) this;
        }
        addPromise(runnable, STATE_ABORTED);
        return (T) this;
    }


    public <T extends Process<Result, Error>> T finish(final PromiseCallback<Process> promise, long timeout) {
        T process = finish(promise);
        postDelayed(() -> {
            compromise(promise);
        }, timeout);
        return process;
    }

    public <T extends Process<Result, Error>> T finish(final PromiseCallback<Process> promise) {
        if (promise == null) {
            return (T) this;
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                promise.onPromise(Process.this);
            }
        };
        if (isFinished()) {
//            throw new IllegalStateException("Oups, current Process is not running. It has to be running before adding any promise or promise");
            runnable.run();
            return (T) this;
        }
        addPromise(runnable, STATE_FLAG_FINISHED);
        return (T) this;
    }

    public <T extends Process<Result, Error>> T chain(final Process<?, ? extends Throwable> promise) {
        return then(new PromiseCallback<Result>() {
            @Override
            public void onPromise(Result data) {
                promise.execute(manager, data);
            }
        });
    }

    public Throwable getException() {
        return getError() != null ? getError() : getFailCause();
    }

    public interface PromiseCallback<T> {
        void onPromise(T data);
    }


    public <T extends Process<Result, Error>> T sendMessage(final MessageCarrier message, int... when) {
        return sendMessage(message, new Object[0], when);
    }

    public <T extends Process<Result, Error>> T sendMessage(final MessageCarrier carrier, final Object[] messages, int... when) {
        for (int value : when) {
            addPromise(new Runnable() {
                @Override
                public void run() {
                    carrier.process = Process.this;
                    carrier.handleMessage(messages);
                }
            }, value);
        }
        return (T) this;
    }

    private void addPromise(Runnable runnable, int conditionTime) {
//        synchronized (this) {
        if (!isPromiseContains(runnable, conditionTime)) {
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(conditionTime);
            if (runnableList == null) {
                runnableList = new ConcurrentLinkedQueue();
            }
            runnableList.add(runnable);
            runnableTask.put(conditionTime, runnableList);
        }
//        }
    }

    private boolean isPromiseContains(Runnable run, int conditionTime) {
        ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(conditionTime);
        if (runnableList == null || runnableList.isEmpty()) {
            return false;
        }
        return runnableList.contains(run);
    }

    public final long getStartingTime() {
//        if (startingTime < 0) {
//            throw new ProcessManager.ProcessException("Oups, it seem than this process is not yet started.");
//        }
        return startingTime;
    }

    public final long getFinishTime() {
//        if (finishTime < 0) {
//            throw new ProcessManager.ProcessException("Oups, it seem than this process is not yet completed.");
//        }
        return finishTime;
    }

    public final long getLivingTime() {
        if (startingTime < 0) {
            return 0;
        }
        return System.currentTimeMillis() - startingTime;
    }

    private void executePromises(ConcurrentLinkedQueue<Runnable> runnableList) {
        if (!jeopardise && runnableList != null && runnableList.size() > 0) {
            for (Runnable runnable : runnableList) {
                if (!executedRunnable.contains(runnable)) {
                    runnable.run();
                    executedRunnable.add(runnable);
                }
            }
        }
    }

    protected void onFinished(int state, Result result, Error error) {

    }

    protected void onSucceed(Result result) {

    }

    protected void onError(Error error) {

    }

    protected void onFailed(Throwable e) {

    }

    protected void onAborted() {

    }

    final void notifyStarted() {
        if (!jeopardise) {
            Process.this.state = STATE_STARTED;
            this.running = true;
            post(new Runnable() {
                @Override
                public void run() {
                    if (getManager() != null) {
                        getManager().notifyProcessStarted(Process.this/*, getExecutionVariables().asArray()*/);
                    }
                    for (ProcessCallback<Result, Error> executionListener : processCallbacks) {
                        executionListener.onStart(/*Process.this*/);
                    }
                    ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(STATE_STARTED);
                    executePromises(runnableList);
                    onStateChanged(state);
                    onStarted();
                }
            });

        }
    }

    final void notifyFinished(int state) {
        if (!jeopardise) {
            this.state = state;
            this.running = false;
            if (getManager() != null) {
                getManager().notifyProcessFinished(this);
            }
            for (ProcessCallback<Result, Error> executionListener : processCallbacks) {
                executionListener.onFinished(/*this, this.result,*/ state);
            }
            executedRunnable.clear();
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(STATE_FLAG_FINISHED);
            executePromises(runnableList);
            onStateChanged(state);
            this.finishTime = System.currentTimeMillis();
            onFinished(state, result, error);
            if ((flags & FLAG_DONT_CLEAR_ON_FINISH) == FLAG_DONT_CLEAR_ON_FINISH) {
                removeCallbacks();
                runnableTask.clear();
                promiseRunnableMap.clear();
            }
            this.manager = null;
        }
    }

    protected final void notifySucceed() {
        notifySucceed(null);
    }

    protected final void notifySucceed(final Result result) {
        if (!jeopardise && running && !canceled) {
            this.state = STATE_SUCCESS;
            this.result = result;
            this.running = false;
            post(new Runnable() {
                @Override
                public void run() {
                    notifyFinished(STATE_SUCCESS);
                    for (ProcessCallback<Result, Error> executionListener : processCallbacks) {
                        executionListener.onSuccess(/*Process.this,*/ result);
                    }
                    ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(STATE_SUCCESS);
                    executePromises(runnableList);
                    onStateChanged(state);
                    onSucceed(result);
                }
            });
        }
    }

    protected final void notifyStateChanged(int state) {
        notifyStateChanged(state, false);
    }

    //TODO il serait telement cool de pouvoir fair transiter un PayLoad ici.
    protected final void notifyStateChanged(final int state, final boolean finished) {
        if (!jeopardise && running) {
            this.state = state;
            if (finished) {
                this.running = false;
            }
            post(new Runnable() {
                @Override
                public void run() {
                    if (getManager() != null) {
                        getManager().notifyProcessStateChanged(Process.this);
                    }
                    if (finished) {
                        notifyFinished(state);
                    }
                    ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(state);
                    executePromises(runnableList);
                    onStateChanged(state);
                }
            });
        }
    }

    protected void onStateChanged(int state) {

    }

    protected final void notifyError(final Error error) {
        if (!jeopardise && running && !canceled) {
            this.state = STATE_ERROR;
            this.error = error;
            this.running = false;
            post(new Runnable() {
                @Override
                public void run() {
                    notifyFinished(STATE_ERROR);
                    for (ProcessCallback<Result, Error> executionListener : processCallbacks) {
                        executionListener.onError(/*Process.this, */error);
                    }
                    ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(STATE_ERROR);
                    executePromises(runnableList);
                    onStateChanged(state);
                    onError(error);
                }
            });
        }
    }

    protected final void notifyFailed(final Throwable e) {
        if (!jeopardise && running && !canceled) {
            this.state = STATE_FAILED;
            this.exception = e;
            this.running = false;
            post(new Runnable() {
                @Override
                public void run() {
                    notifyFinished(STATE_FAILED);
                    for (ProcessCallback<Result, Error> executionListener : processCallbacks) {
                        executionListener.onFail(/*Process.this,*/ e);
                    }
                    ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(STATE_FAILED);
                    executePromises(runnableList);
                    onStateChanged(state);
                    onFailed(e);
                }
            });
        }
    }

    protected final void notifyAborted() {
        if (!jeopardise && running) {
            this.state = STATE_ABORTED;
            this.running = false;
            post(new Runnable() {
                @Override
                public void run() {
                    notifyFinished(STATE_ABORTED);
                    for (ProcessCallback<Result, Error> executionListener : processCallbacks) {
                        executionListener.onAborted(/*Process.this*/);
                    }
                    ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(STATE_ABORTED);
                    executePromises(runnableList);
                    onStateChanged(state);
                    onAborted();
                }
            });
        }
    }

    protected final void notifyDelayedAborted(int delay) {
        if (delay <= 0) {
            notifyAborted();
        } else {
            manager.postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifyAborted();
                }
            }, delay);
        }
    }

    protected final void notifyDelayedFailed(final Exception e, int delay) {
        if (delay <= 0) {
            notifyFailed(e);
        } else {
            manager.postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifyFailed(e);
                }
            }, delay);
        }
    }

    protected final void notifyDelayedError(final Error error, int delay) {
        if (delay <= 0) {
            notifyError(error);
        } else {
            manager.postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifyError(error);
                }
            }, delay);
        }
    }

    protected final void notifyDelayedSuccess(final Result result, int delay) {
        if (delay <= 0) {
            notifySucceed(result);
        } else {
            manager.postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifySucceed(result);
                }
            }, delay);
        }
    }

    public boolean hasError() {
        return error != null;
    }

    public boolean hasException() {
        return exception != null;
    }

    public boolean isFailed() {
        return exception != null;
    }

    public boolean hasSucceed() {
        return isCompleted() && !hasError() && !isFailed() && !isCanceled();
    }

    public void attach(ProcessCallback<Result, Error> callback) {
        ProcessTools.attachToProcessCycle(this, callback);
    }

    public int cancelAllCallback() {
        int callbackCount = processCallbacks.size();
        processCallbacks.clear();
        return callbackCount;
    }

    public boolean cancelCallback(ProcessCallback callback) {
        boolean removed = processCallbacks.contains(callback);
        if (removed) {
            processCallbacks.remove(callback);
        }
        return removed;
    }

    public int compromise(Runnable runnable) {
        return compromise(runnable, null);
    }

    public int compromise(Runnable runnable, Integer maxToRemove) {
        Iterator<Integer> whenIterator = runnableTask.keySet().iterator();
        int compromised = 0;
        while (whenIterator.hasNext()) {
            Integer when = whenIterator.next();
            ConcurrentLinkedQueue<Runnable> runnableList = runnableTask.get(when);
            if (runnableList != null) {
                if (runnableList.remove(runnable)) {
                    compromised++;
                }
            }
        }
        return compromised;
    }

    public boolean compromise(PromiseCallback promiseCallback) {
        Runnable runnable = promiseRunnableMap.remove(promiseCallback);
        if (runnable == null) {
            return false;
        }
        return compromise(runnable) > 0;
    }

    public void precipitatePromise(int... moments) {
        try {
            for (int moment : moments) {
                ConcurrentLinkedQueue<Runnable> executableFuture = runnableTask.get(moment);
                if (executableFuture != null && !executableFuture.isEmpty()) {
                    for (Runnable runnable : executableFuture) {
                        if (!executedRunnable.contains(runnable)) {
                            runnable.run();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public abstract static class MessageCarrier {
        List<Object> messages;
        Process process;

        void handleMessage(Object... messages) {
            Collections.addAll(this.messages, messages);
            onHandleMessage(process, messages);
        }

        public abstract void onHandleMessage(Process process, Object... messages);

        public List<Object> getMessages() {
            return messages;
        }
    }

    public class ExecutionVariables {
        public int getCount() {
            return executionVariableArray.length;
        }

        public Object[] asArray() {
            return executionVariableArray;
        }

        public List<?> asList() {
            return Arrays.asList(executionVariableArray);
        }

        public <T> T getVariable(int index) {
            if (executionVariableArray.length <= index) {
                return null;
            }
            try {
                return (T) executionVariableArray[index];
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public boolean isVariableDefined(int index) {
            return executionVariableArray.length > index;
        }

        public boolean isVariableNotNull(int index) {
            if (executionVariableArray.length > index) {
                return executionVariableArray[index] != null;
            }
            return false;
        }

        public boolean isVarInstanceOf(int index, Class<?> cLass) {
            Object var = getVariable(index);
            return var != null && cLass != null && cLass.isAssignableFrom(var.getClass());
        }

        public <T> T getVariable(int index, Class<T> cLass) throws ArrayIndexOutOfBoundsException {
            if (executionVariableArray.length <= index) {
                throw new ArrayIndexOutOfBoundsException("executionVariables length=" + executionVariableArray.length + ", requested index=" + index
                );
            }
            Object var = executionVariableArray[index];
            if (var == null) {
                return null;
            }
            if (cLass.isAssignableFrom(var.getClass())) {
                return (T) var;
            } else {
                throw new IllegalArgumentException("Item at index=" + index + " has type class=" + var.getClass() + ", requested class=" + cLass);
            }
        }

        public <T> T getVariable(int index, Class<T> cLass, T defaultValue) throws ArrayIndexOutOfBoundsException {
            if (executionVariableArray.length <= index) {
                throw new ArrayIndexOutOfBoundsException("executionVariables length=" + executionVariableArray.length + ", requested index=" + index
                );
            }
            Object var = executionVariableArray[index];
            if (var == null) {
                return defaultValue;
            }
            if (cLass.isAssignableFrom(var.getClass())) {
                return (T) var;
            } else {
                return defaultValue;
            }
        }

        public String getStringVariable(int index) throws ArrayIndexOutOfBoundsException {
            if (executionVariableArray.length <= index) {
                throw new ArrayIndexOutOfBoundsException("executionVariables length=" + executionVariableArray.length + ", requested index=" + index
                );
            }
            Object var = executionVariableArray[index];
            if (var == null) {
                return null;
            }
            return String.valueOf(var);
        }

        public int getIntVariable(int index) throws ArrayIndexOutOfBoundsException {
            if (executionVariableArray.length <= index) {
                throw new ArrayIndexOutOfBoundsException("executionVariables length=" + executionVariableArray.length + ", requested index=" + index
                );
            }
            Object var = executionVariableArray[index];
            if (var == null) {
                return 0;
            }
            return Integer.valueOf(String.valueOf(var));
        }

        public long getLongVariable(int index) throws ArrayIndexOutOfBoundsException {
            if (executionVariableArray.length <= index) {
                throw new ArrayIndexOutOfBoundsException("executionVariables length=" + executionVariableArray.length + ", requested index=" + index
                );
            }
            Object var = executionVariableArray[index];
            if (var == null) {
                return 0;
            }
            return Long.valueOf(String.valueOf(var));
        }

        public float getFloatVariable(int index) throws ArrayIndexOutOfBoundsException {
            if (executionVariableArray.length <= index) {
                throw new ArrayIndexOutOfBoundsException("executionVariables length=" + executionVariableArray.length + ", requested index=" + index
                );
            }
            Object var = executionVariableArray[index];
            if (var == null) {
                return 0;
            }
            return Float.valueOf(String.valueOf(var));
        }

        public double getDoubleVariable(int index) throws ArrayIndexOutOfBoundsException {
            if (executionVariableArray.length <= index) {
                throw new ArrayIndexOutOfBoundsException("executionVariables length=" + executionVariableArray.length + ", requested index=" + index
                );
            }
            Object var = executionVariableArray[index];
            if (var == null) {
                return 0;
            }
            return Double.valueOf(String.valueOf(var));
        }

        public int length() {
            return executionVariableArray == null ? 0 : executionVariableArray.length;
        }

        public boolean isEmpty() {
            return length() == 0;
        }

        public boolean getBooleanVariable(int index) {
            if (executionVariableArray.length <= index) {
                throw new ArrayIndexOutOfBoundsException("executionVariables length=" + executionVariableArray.length + ", requested index=" + index
                );
            }
            Object var = executionVariableArray[index];
            if (var == null) {
                return false;
            }
            return Boolean.valueOf(String.valueOf(String.valueOf(var)));
        }
    }

    public int getState() {
        return state;
    }

    protected boolean post(Runnable runnable) {
        if (getManager() == null) {
            return false;
        }
        getManager().post(runnable);
        return true;
    }

    protected boolean postDelayed(Runnable runnable, long delay) {
        if (getManager() == null) {
            return false;
        }
        getManager().postDelayed(runnable, delay);
        return true;
    }

    protected void unPost(Runnable runnable) {
        if (getManager() == null) {
            return;
        }
        getManager().unPost(runnable);
    }

    public String changeId(String newId) throws ProcessManager.ProcessException {
        String currentId = getId();
        getManager().setProcessId(newId, this);
        return currentId;
    }

    /**
     * is parameter state included into currentProcess state?
     *
     * @param state
     * @return state is included into current #Process.state
     */
    public boolean isStateAssignableTo(int state) {
        int bitAnd = state & this.state;
        return state == bitAnd;
    }
}
