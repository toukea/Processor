package com.istat.freedev.processor.interfaces;

import com.istat.freedev.processor.Process;

/**
 * Created by istat on 14/10/16.
 */

public interface ProcessCallback<Result, Error extends Throwable> {
    /**
     * called when the process started.
     *
     * @param process
     */
    public void onStart(Process process);

    /**
     * called when process is completed
     *
     * @param process
     * @param success state if process succeed.
     */
    public void onCompleted(Process process, boolean success);

    /**
     * called when the process succeed
     *
     * @param process
     * @param result  the process Result.
     */
    public void onSuccess(Process process, Result result);

    /**
     * The process is started but some error happen durring.
     *
     * @param process
     * @param error   the error rencontered by the process
     */
    public void onError(Process process, Error error);

    /**
     * The process is started. but never running.s
     *
     * @param process
     * @param e the exception that cause the process failed
     */
    public void onFail(Process process, Exception e);

    /**
     * called when the process has been aborted.
     *
     * @param process
     */
    public void onAborted(Process process);


}