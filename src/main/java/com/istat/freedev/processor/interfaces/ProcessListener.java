package com.istat.freedev.processor.interfaces;

import com.istat.freedev.processor.Process;

/**
 * Created by istat on 14/10/16.
 */

public interface ProcessListener {
    void onProcessStarted(Process process, String id);

    void onProcessCompleted(Process process, String id);

}
