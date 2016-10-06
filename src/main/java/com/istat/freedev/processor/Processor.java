package com.istat.freedev.processor;

/**
 * Created by istat on 04/10/16.
 */

public class Processor {
    final static ProcessManager mProcessManager = new ProcessManager();

    public final static ProcessManager getProcessManager() {
        return mProcessManager;
    }
}