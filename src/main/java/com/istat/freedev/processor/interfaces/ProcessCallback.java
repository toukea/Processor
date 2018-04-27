package com.istat.freedev.processor.interfaces;

/**
 * Created by istat on 14/10/16.
 */

public interface ProcessCallback<Result, Error extends Throwable> {
    /**
     * called when the process started.
     */
    void onStart(/*Process<Result, Error> process*/);

    /**
     * called when process is completed
     *
     * @param finishState state if process succeed.
     */
    void onFinished(/*Process<Result, Error> process,*/ Result result, int finishState);

    /**
     * called when the process succeed
     *
     * @param result the process Result.
     */
    void onSuccess(/*Process<Result, Error> process,*/ Result result);

    /**
     * The process is started but some error happen durring.
     *
     * @param error the error rencontered by the process
     */
    void onError(/*Process<Result, Error> process, */Error error);

    /**
     * The process is started. but never running.s
     *
     * @param e the exception that cause the process failed
     */
    void onFail(/*Process<Result, Error> process,*/ Throwable e);

    /**
     * called when the process has been aborted.
     */
    void onAborted(/*Process<Result, Error> process*/);


}