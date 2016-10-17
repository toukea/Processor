package com.istat.freedev.processor.interfaces;

import com.istat.freedev.processor.Process;

/**
 * Created by istat on 14/10/16.
 */

public interface ProcessListener {
    public void onProcessStarted(Process process, String id);

    public void onProcessCompleted(Process process, String id);

}
