package com.istat.freedev.processor.interfaces;

import com.istat.freedev.processor.Process;

/**
 * Created by istat on 14/10/16.
 */

public interface ProcessExecutionCallback<Result, Error extends Process.ProcessError> {
    public void onStart(Process process);

    public void onCompleted(Process process, boolean success);

    public void onSuccess(Process process, Result result);

    public void onError(Process process, Error error);

    public void onFail(Process process, Exception e);

    public void onAborted(Process process);


}