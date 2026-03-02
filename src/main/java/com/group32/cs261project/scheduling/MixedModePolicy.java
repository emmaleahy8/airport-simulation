package com.group32.cs261project.scheduling;

import com.group32.cs261project.model.Runway;

public interface MixedModePolicy {

    OperationType chooseNextOp(boolean hasArrivals, boolean hasDepartures, Runway runway);
    
}
