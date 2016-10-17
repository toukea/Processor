package com.istat.freedev.processor.interfaces;

import com.istat.freedev.processor.Process;

/**
 * Created by istat on 14/10/16.
 */

public interface ProcessExecutionListener {
    public void onSucceed(Process process);

    public void onError(Process process);

    public void onFail(Process process, Exception e);

    public void onAborted(Process process);


}