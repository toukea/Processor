package com.istat.freedev.processor.interfaces;

import com.istat.freedev.processor.Process;

/**
 * Created by istat on 14/10/16.
 */

public interface ProcessListener {
    default void onProcessEnqueued(Process process, String id){}

    default void onProcessStarted(Process process, String id) {}

    default void onProcessStateChanged(Process process, String id, int state) {}

    default void onProcessFinished(Process process, String id){}

}
