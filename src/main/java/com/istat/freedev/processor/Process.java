package com.istat.freedev.processor;

import com.istat.freedev.processor.interfaces.ProcessCallback;
import com.istat.freedev.processor.utils.ProcessTools;
import com.istat.freedev.processor.utils.Toolkits;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by istat on 04/10/16.
 */

public abstract class Process<Result, Error extends Throwable> {
    public final static int FLAG_DETACHED = 1;
    public final static int FLAG_DON_NOT_CLEAR_ON_FINISH = 2;
    public final static int FLAG_NOT_CANCELABLE = 4;
    int flags;
    public final static int
            STATE_LATENT = -1,
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
    int state = STATE_LATENT;
    boolean canceled;
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
        geopardise = false;
        try {
            this.executionVariableArray = vars;
            startingTime = System.currentTimeMillis();
//            memoryRunnableTask.putAll(runnableTask);
            notifyStarted();
            onExecute(getExecutionVariables());
            dispatchState(STATE_PROCESSING, false);
        } catch (Exception e) {
            notifyStarted();
            notifyFailed(e);
        }
    }

    final void reset() {
        geopardise = false;
        executedRunnable.clear();
//        runnableTask.putAll(memoryRunnableTask);
        try {
            onExecute(getExecutionVariables());
        } catch (Exception e) {
            notifyStarted();
            notifyFailed(e);
        }
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

    public boolean isRunning() {
        return running;
    }

    public boolean isCompleted() {
        return !running && !canceled;
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
        if (running) {
            canceled = true;
            onCancel();
            notifyAborted();
        }
        this.running = false;
        this.processCallbacks.clear();
        return running;
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

    boolean geopardise = false;

    public final boolean hasBeenGeopardise() {
        return geopardise;
    }

    //TODO bien réfléchir au comportement de cette method
    public final boolean geopardise() {
        geopardise = true;
        compromise();
        boolean cancelled = cancel();
        return cancelled;

    }

    boolean hasId() {
        return !Toolkits.isEmpty(getId());
    }


    final ConcurrentLinkedQueue<Runnable> executedRunnable = new ConcurrentLinkedQueue<Runnable>();
    //    final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Runnable>> memoryRunnableTask = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Runnable>>();
    final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Runnable>> runnableTask = new ConcurrentHashMap();

    public <T extends Process> T promise(final PromiseCallback<Process> callback, int... when) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onPromise(Process.this);
                }
            }
        };
        promiseRunnableMap.put(callback, runnable);
        return promise(runnable, when);
    }

    private void removeAttachedPromise(Runnable runnable) {

    }

    public <T extends Process> T promise(Runnable runnable, int... when) {
//        if(!isRunning()){
//            throw new IllegalStateException("Oups, current Process is not running. It has to be running before adding any promise or promise");
//        }
        for (int value : when) {
            addFuture(runnable, value);
        }
        return (T) this;
    }

    public <T extends Process<Result, Error>> T then(final PromiseCallback<Result> promise) {
//        if(!isRunning()){
//            throw new IllegalStateException("Oups, current Process is not running. It has to be running before adding any promise or promise");
//        }
        addFuture(new Runnable() {
            @Override
            public void run() {
                promise.onPromise(getResult());
            }
        }, STATE_SUCCESS);
        return (T) this;
    }

    public <T extends Process> T error(final PromiseCallback<Error> promise) {
//        if(!isRunning()){
//            throw new IllegalStateException("Oups, current Process is not running. It has to be running before adding any promise or promise");
//        }
        addFuture(new Runnable() {
            @Override
            public void run() {
                promise.onPromise(getError());
            }
        }, STATE_ERROR);
        return (T) this;
    }

    public <T extends Process<Result, Error>> T failed(final PromiseCallback<Throwable> promise) {
//        if(!isRunning()){
//            throw new IllegalStateException("Oups, current Process is not running. It has to be running before adding any promise or promise");
//        }
        addFuture(new Runnable() {
            @Override
            public void run() {
                promise.onPromise(getFailCause());
            }
        }, STATE_FAILED);
        return (T) this;
    }

    public <T extends Process<Result, Error>> T catchException(final PromiseCallback<Throwable> promise) {
//        if(!isRunning()){
//            throw new IllegalStateException("Oups, current Process is not running. It has to be running before adding any promise or promise");
//        }
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
        addFuture(runnable, STATE_FAILED);
        addFuture(runnable, STATE_ERROR);
//        addFuture(runnable, STATE_ABORTED);
        return (T) this;
    }

    public <T extends Process<Result, Error>> T abortion(final PromiseCallback<Void> promise) {
//        if(!isRunning()){
//            throw new IllegalStateException("Oups, current Process is not running. It has to be running before adding any promise or promise");
//        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                promise.onPromise(null);
            }
        };
        addFuture(runnable, STATE_ABORTED);
        return (T) this;
    }

    public <T extends Process<Result, Error>> T finish(final PromiseCallback<Process> promise) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                promise.onPromise(Process.this);
            }
        };
        addFuture(runnable, STATE_FLAG_FINISHED);
        return (T) this;
    }

    public <T extends Process<Result, Error>, Y extends Throwable> T then(final Process<Result, Y> promise) {
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
                runnableList = new ConcurrentLinkedQueue();
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

    public final long getLinvingTime() {
        if (startingTime < 0) {
            return 0;
        }
        return System.currentTimeMillis() - startingTime;
    }

    private void executePromises(ConcurrentLinkedQueue<Runnable> runnableList) {
        if (!geopardise && runnableList != null && runnableList.size() > 0) {
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
        if (!geopardise) {
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
                }
            });

        }
    }

    final void notifyFinished(int state) {
        if (!geopardise) {
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
            if ((flags & FLAG_DON_NOT_CLEAR_ON_FINISH) == FLAG_DON_NOT_CLEAR_ON_FINISH) {
                removeCallbacks();
                runnableTask.clear();
                promiseRunnableMap.clear();
            }

            this.manager = null;
        }
    }

    protected final void notifySucceed(final Result result) {
        if (!geopardise) {
            this.state = STATE_SUCCESS;
            this.result = result;
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

    protected final void dispatchState(int state) {
        dispatchState(state, false);
    }

    protected final void dispatchState(final int state, final boolean finished) {
        if (!geopardise) {
            this.state = state;
            post(new Runnable() {
                @Override
                public void run() {
                    if (finished) {
                        notifyFinished(state);
                    }
                    if (getManager() != null) {
                        getManager().notifyProcessStateChanged(Process.this);
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
        if (!geopardise) {
            this.state = STATE_ERROR;
            this.error = error;
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
        if (!geopardise) {
            this.state = STATE_FAILED;
            this.exception = e;
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
        if (!geopardise) {
            this.state = STATE_ABORTED;
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
        return !hasError() && !isFailed() && !isCanceled();
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

    public boolean compromise(Runnable runnable) {
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

    public boolean compromise(PromiseCallback promiseCallback) {
        Runnable runnable = promiseRunnableMap.remove(promiseCallback);
        if (runnable == null) {
            return false;
        }
        return compromise(runnable);
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

    protected boolean postDelayed(Runnable runnable, int delay) {
        if (getManager() == null) {
            return false;
        }
        getManager().postDelayed(runnable, delay);
        return true;
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
    public boolean isStateAsignableTo(int state) {
        int bitAnd = state & this.state;
        return state == bitAnd;
    }
}
