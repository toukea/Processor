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
        Runnable finish = new Runnable() {
            @Override
            public void run() {
                listener.onFinished(/*process, */process.getResult(), process.getState());
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
        process.promise(start, Process.STATE_STARTING);
        process.promise(finish, Process.STATE_FINISHED);
        process.promise(success, Process.STATE_SUCCESS);
        process.promise(error, Process.STATE_ERROR);
        process.promise(fail, Process.STATE_FAILED);
        process.promise(aborted, Process.STATE_ABORTED);
    }
}
