package com.group32.cs261project.backend.scheduling;

import com.group32.cs261project.backend.model.Runway;
import com.group32.cs261project.backend.model.enums.AircraftDirection;

/**
 * Interface for a mixed mode scheduling policy
 * This determines what operation should be performed next on a mixed mode runway
 */
public interface MixedModePolicy {

    /**
     * choose next runway operation on a mixed mode runway
     * @param hasArrivals whether holding pattern is non-empty
     * @param hasDepartures whether takeoff queue is non-empty
     * @param runway runway object
     * @return one of INBOUND or OUTBOUND
     */
    AircraftDirection chooseNextOp(boolean hasArrivals, boolean hasDepartures, Runway runway);
    
}
