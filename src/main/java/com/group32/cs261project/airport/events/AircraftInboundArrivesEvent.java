package com.group32.cs261project.airport.events;

import java.time.Instant;

import com.group32.cs261project.model.Aircraft;
import com.group32.cs261project.model.Airport;
import com.group32.cs261project.sim.SimulationEngine;
import com.group32.cs261project.sim.events.Event;

public class AircraftInboundArrivesEvent implements Event<Airport> {

    private final Instant time;
    private final Aircraft aircraft;

    public AircraftInboundArrivesEvent(Instant time, Aircraft aircraft) {
        this.time = time;
        this.aircraft = aircraft;
    }

    @Override
    public Instant time() {
        return time;
    }

    @Override
    public void handle(SimulationEngine<Airport> engine) {
        System.out.format("%s: %s enqueued to holding pattern\n", time.toString(), aircraft.toString());
        engine.context().holdingPattern().enqueue(aircraft, time);
    }
    
}
