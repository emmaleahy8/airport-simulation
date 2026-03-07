package com.group32.cs261project.backend.scheduling;

import java.util.HashMap;
import java.util.Map;

import com.group32.cs261project.backend.model.Runway;
import com.group32.cs261project.backend.model.enums.AircraftDirection;

public class AlternatingMixedModePolicy implements MixedModePolicy {

    private final Map<Integer, AircraftDirection> lastOpByRunway ;

    public AlternatingMixedModePolicy() {
        this.lastOpByRunway = new HashMap<>();
    }

    @Override
    public AircraftDirection chooseNextOp(boolean hasArrivals, boolean hasDepartures, Runway runway) {

        if (hasArrivals && !hasDepartures) {
            this.lastOpByRunway.put(runway.runwayNumber(), AircraftDirection.INBOUND);
            return AircraftDirection.INBOUND;

        } else if (!hasArrivals && hasDepartures) {
            this.lastOpByRunway.put(runway.runwayNumber(), AircraftDirection.OUTBOUND);
            return AircraftDirection.OUTBOUND;

        } else if (!this.lastOpByRunway.containsKey(runway.runwayNumber())) {
            this.lastOpByRunway.put(runway.runwayNumber(), AircraftDirection.INBOUND);
            return AircraftDirection.INBOUND;
        } else if (this.lastOpByRunway.get(runway.runwayNumber()) == AircraftDirection.INBOUND) {
            this.lastOpByRunway.put(runway.runwayNumber(), AircraftDirection.OUTBOUND);
            return AircraftDirection.OUTBOUND;
        } else {
            this.lastOpByRunway.put(runway.runwayNumber(), AircraftDirection.INBOUND);
            return AircraftDirection.INBOUND;
        }
    }
    
}
