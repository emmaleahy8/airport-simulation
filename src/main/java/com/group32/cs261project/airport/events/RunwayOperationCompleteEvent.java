package com.group32.cs261project.airport.events;

import java.time.Instant;

import com.group32.cs261project.model.Airport;
import com.group32.cs261project.model.Runway;
import com.group32.cs261project.sim.SimulationEngine;
import com.group32.cs261project.sim.events.Event;

public class RunwayOperationCompleteEvent implements Event<Airport> {

    private final Instant time;
    private final Runway runway;

    public RunwayOperationCompleteEvent(Instant time, Runway runway) {
        this.time = time;
        this.runway = runway;
    }

    @Override
    public Instant time() {
        return this.time;
    }

    @Override
    public void handle(SimulationEngine<Airport> engine) {
        this.runway.clearOccupancy();
    }
    
}
