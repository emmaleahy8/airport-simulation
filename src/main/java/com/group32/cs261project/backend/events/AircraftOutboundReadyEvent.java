package com.group32.cs261project.backend.events;

import java.time.Instant;

import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.sim.SimulationContext;
import com.group32.cs261project.backend.sim.SimulationEngine;

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

        // enqueue to takeoff queue
        engine.context().airport().takeOffQueue().enqueue(aircraft, time);

        // try to schedule runway operations immediately
        engine.context().scheduler().tryScheduleOperations(time, engine);

        // update metrics by calling onTakeoffEnqueue method with new size
        engine.context().metrics().onTakeoffEnqueue(time, engine.context().airport().takeOffQueue().size());
    }
}
