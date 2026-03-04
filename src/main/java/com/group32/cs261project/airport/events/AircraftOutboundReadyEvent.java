package com.group32.cs261project.airport.events;

import java.time.Instant;

import com.group32.cs261project.model.Aircraft;
import com.group32.cs261project.sim.SimulationContext;
import com.group32.cs261project.sim.SimulationEngine;
import com.group32.cs261project.sim.events.Event;

public class AircraftOutboundReadyEvent implements Event<SimulationContext> {

    private final Instant time;
    private final Aircraft aircraft;

    public AircraftOutboundReadyEvent(Instant time, Aircraft aircraft) {
        this.time = time;
        this.aircraft = aircraft;
    }

    @Override
    public Instant time() {
        return time;
    }

    @Override
    public void handle(SimulationEngine<SimulationContext> engine) {
        if (engine.config().isVerbose())
            System.out.format("%s: %s enqueued to takeoff queue\n", time.toString(), aircraft.toString());
        engine.context().airport().takeOffQueue().enqueue(aircraft, time);
        engine.context().scheduler().tryScheduleOperations(time, engine);
    }
}
