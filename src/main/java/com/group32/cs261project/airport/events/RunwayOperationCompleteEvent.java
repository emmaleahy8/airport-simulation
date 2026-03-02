package com.group32.cs261project.airport.events;

import java.time.Instant;

import com.group32.cs261project.model.Airport;
import com.group32.cs261project.sim.SimulationEngine;
import com.group32.cs261project.sim.events.Event;

public class RunwayOperationCompleteEvent implements Event<Airport> {

    @Override
    public Instant time() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'time'");
    }

    @Override
    public void handle(SimulationEngine<Airport> engine) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handle'");
    }
    
}
