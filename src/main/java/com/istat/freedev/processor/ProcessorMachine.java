package com.istat.freedev.processor;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by istat on 13/01/17.
 */

public class ProcessorMachine {
    public final static String DEFAULT_PROCESSOR_TAG = "com.istat.freedev.processor.DEFAULT";
    final static ConcurrentHashMap<String, ProcessorMachine> machineRepository = new ConcurrentHashMap<String, ProcessorMachine>() {
        {
            put(DEFAULT_PROCESSOR_TAG, new ProcessorMachine(DEFAULT_PROCESSOR_TAG));
        }
    };
    ConcurrentHashMap<String, Processor> processorCluster = new ConcurrentHashMap<String, Processor>() {
        {
            put(DEFAULT_PROCESSOR_TAG, Processor.get(Processor.DEFAULT_PROCESSOR_TAG) != null ?
                    Processor.get(Processor.DEFAULT_PROCESSOR_TAG) :
                    Processor.boot(Processor.DEFAULT_PROCESSOR_TAG));
        }
    };
    String name;

    ProcessorMachine(String name) {
        this.name = name;
    }

    public final static int getMachineCount() {
        return machineRepository.size();
    }

    public int getProcessorCount() {
        return processorCluster.size();
    }

    public Processor getProcessor(String nameSpace) {
        return null;
    }

    public final static ProcessorMachine find(String name) {
        if (machineRepository.contains(name)) {
            return machineRepository.get(name);
        }
        ProcessorMachine processorMachine = new ProcessorMachine(name);
        machineRepository.put(name, processorMachine);
        return processorMachine;
    }

    public void shutDown() {
    }

    public void release() {
    }

    public void reset() {

    }

    public void shutDownProcessor(String nameSpace) {

    }

    public void releaseProcessor(String nameSpace) {

    }

    public final static int shutDownAll() {
        Iterator<String> iterator = machineRepository.keySet().iterator();
        int count = 0;
        ProcessorMachine machine;
        while (iterator.hasNext()) {
            String name = iterator.next();
            machine = machineRepository.get(name);
            if (machine != null) {
                machine.shutDown();
                count++;
            }
        }
        machineRepository.clear();
        machineRepository.put(DEFAULT_PROCESSOR_TAG, new ProcessorMachine(DEFAULT_PROCESSOR_TAG));
        return count;
    }


    public final static int releaseAll() {
        Iterator<String> iterator = machineRepository.keySet().iterator();
        int count = 0;
        ProcessorMachine machine;
        while (iterator.hasNext()) {
            String name = iterator.next();
            machine = machineRepository.get(name);
            if (machine != null) {
                machine.release();
                count++;
            }
        }
        return count;
    }


}
