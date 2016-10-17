package com.istat.freedev.processor;

import android.content.Context;

/**
 * Created by istat on 04/10/16.
 */

public class Processor {
    final static ProcessManager mProcessManager = new ProcessManager();

    public final static ProcessManager getDefaultProcessManager() {
        return mProcessManager;
    }

    public static Processor getDefault() {
        return null;
    }

    public final static Processor from(String processorTag) {
        return null;
    }

    public Process execute(Process process, Object... vars) {
        return null;
    }

    public Process execute(Process process, String PID, Object... vars) {
        return null;
    }

    public int cancel() {
        return 0;
    }


}