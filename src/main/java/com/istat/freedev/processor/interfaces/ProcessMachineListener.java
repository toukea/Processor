package com.istat.freedev.processor.interfaces;

import com.istat.freedev.processor.Process;
import com.istat.freedev.processor.ProcessManager;

/**
 * Created by istat on 14/10/16.
 */

public interface ProcessMachineListener {
     void onProcessStarted(ProcessManager pm, Process process, String id);

     void onProcessCompleted(ProcessManager pm, Process process, String id);

}
