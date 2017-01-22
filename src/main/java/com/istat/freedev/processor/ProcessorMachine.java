package com.istat.freedev.processor;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by istat on 13/01/17.
 */

public class ProcessorMachine {
    public final static String DEFAULT_PROCESSOR_TAG = "com.istat.freedev.processor.DEFAULT";
    final static ConcurrentHashMap<String, Processor> processorQueue = new ConcurrentHashMap<String, Processor>() {
        {
            put(DEFAULT_PROCESSOR_TAG, new Processor());
        }
    };

    public int getProcessorCount() {
        return processorQueue.size();
    }
}
