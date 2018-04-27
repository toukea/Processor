package com.istat.freedev.processor.utils;

import com.istat.freedev.processor.Process;
import com.istat.freedev.processor.interfaces.ProcessCallback;

/**
 * Created by istat on 18/11/16.
 */

public class ProcessTools {
    public static <T, Y extends Throwable> void attachToProcessCycle(final Process<T, Y> process, final ProcessCallback<T, Y> listener) {
        Runnable start = new Runnable() {
            @Override
            public void run() {
                listener.onStart(/*process*/);
            }
        };
        Runnable complete = new Runnable() {
            @Override
            public void run() {
                listener.onCompleted(/*process, */process.getResult(), process.hasSucceed());
            }
        };
        Runnable success = new Runnable() {
            @Override
            public void run() {
                listener.onSuccess(/*process,*/ process.getResult());
            }
        };
        Runnable error = new Runnable() {
            @Override
            public void run() {
                listener.onError(/*process, */process.getError());
            }
        };
        Runnable fail = new Runnable() {
            @Override
            public void run() {
                listener.onFail(/*process,*/ process.getFailCause());
            }
        };
        Runnable aborted = new Runnable() {
            @Override
            public void run() {
                listener.onAborted(/*process*/);
            }
        };
        process.promise(start, Process.PROMISE_WHEN_STARTED);
        process.promise(complete, Process.PROMISE_WHEN_ANYWAY);
        process.promise(success, Process.PROMISE_WHEN_SUCCESS);
        process.promise(error, Process.PROMISE_WHEN_ERROR);
        process.promise(fail, Process.PROMISE_WHEN_FAIL);
        process.promise(aborted, Process.PROMISE_WHEN_ABORTED);
    }
}
