package com.group32.cs261project.backend.scheduling;

import com.group32.cs261project.backend.model.Runway;
import com.group32.cs261project.backend.model.enums.AircraftDirection;

public interface MixedModePolicy {

    AircraftDirection chooseNextOp(boolean hasArrivals, boolean hasDepartures, Runway runway);
    
}
