package com.group32.cs261project.backend.scheduling;

import com.group32.cs261project.backend.model.Runway;

public interface MixedModePolicy {

    OperationType chooseNextOp(boolean hasArrivals, boolean hasDepartures, Runway runway);
    
}
