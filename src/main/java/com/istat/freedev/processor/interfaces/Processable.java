package com.istat.freedev.processor.interfaces;

import com.istat.freedev.processor.Process;
import com.istat.freedev.processor.ProcessManager;

/**
 * Created by istat on 23/09/17.
 */

public interface Processable<Result, Error extends Throwable> {
    ProcessManager getManager();

    void addProcessCallback(ProcessCallback<Result, Error> executionListener);

    Process.ExecutionVariables getExecutionVariables();


    boolean isCanceled();

    Error getError();

    Throwable getFailCause();

    Result getResult();

    String getId();

    void pause();

    void resume();

    void restart();

    int RESTART_MODE_GEOPARDISE = 0, RESTART_MODE_ABORT = 1;
    int TIME_MILLISEC_WAIT_FOR_RESTART = 100;

    void restart(int mode);

    void stop();

    boolean cancel();

    boolean geopardise();

    boolean hasError();

    boolean isFailed();

    boolean isSuccess();

    int getState();
}
