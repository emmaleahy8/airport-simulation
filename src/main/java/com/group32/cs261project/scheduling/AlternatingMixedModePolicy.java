package com.group32.cs261project.scheduling;

import java.util.HashMap;
import java.util.Map;

import com.group32.cs261project.model.Runway;

public class AlternatingMixedModePolicy implements MixedModePolicy {

    private final Map<Integer, OperationType> lastOpByRunway ;

    public AlternatingMixedModePolicy() {
        this.lastOpByRunway = new HashMap<>();
    }

    @Override
    public OperationType chooseNextOp(boolean hasArrivals, boolean hasDepartures, Runway runway) {

        if (hasArrivals && !hasDepartures) {
            this.lastOpByRunway.put(runway.runwayNumber(), OperationType.LANDING);
            return OperationType.LANDING;

        } else if (!hasArrivals && hasDepartures) {
            this.lastOpByRunway.put(runway.runwayNumber(), OperationType.TAKEOFF);
            return OperationType.TAKEOFF;

        } else if (!this.lastOpByRunway.containsKey(runway.runwayNumber())) {
            this.lastOpByRunway.put(runway.runwayNumber(), OperationType.LANDING);
            return OperationType.LANDING;
        } else if (this.lastOpByRunway.get(runway.runwayNumber()) == OperationType.LANDING) {
            this.lastOpByRunway.put(runway.runwayNumber(), OperationType.TAKEOFF);
            return OperationType.TAKEOFF;
        } else {
            this.lastOpByRunway.put(runway.runwayNumber(), OperationType.LANDING);
            return OperationType.LANDING;
        }
    }
    
}
