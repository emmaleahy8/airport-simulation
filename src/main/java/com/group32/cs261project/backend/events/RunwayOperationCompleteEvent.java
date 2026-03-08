package com.group32.cs261project.backend.events;

import java.time.Instant;

import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.Runway;
import com.group32.cs261project.backend.sim.SimulationContext;
import com.group32.cs261project.backend.sim.SimulationEngine;

public class RunwayOperationCompleteEvent implements Event<SimulationContext> {

    private final Instant time;
    private final Aircraft aircraft;
    private final Runway runway;

    public RunwayOperationCompleteEvent(Instant time, Aircraft aircraft, Runway runway) {
        this.time = time;
        this.aircraft = aircraft;
        this.runway = runway;
    }

    @Override
    public Instant time() {
        return this.time;
    }

    @Override
    public void handle(SimulationEngine<SimulationContext> engine) {

        if (engine.config().verbose())
            System.out.format("%s: Operation on %s complete\n", time.toString(), runway.toString());

        // clear the runway
        this.runway.clearOccupancy();
        engine.context().airport().clearRunway(runway);

        // mark aircraft as having completed the operation (landed or departed)
        this.aircraft.markCompleted();

        // try to schedule next runway operation now the runway is clear
        engine.context().scheduler().tryScheduleOperations(this.time, engine);

        // update metrics based on the operation type
        switch (this.aircraft.direction()) {
            case INBOUND -> engine.context().metrics().onLandingCompleted(this.aircraft, this.time);
            case OUTBOUND -> engine.context().metrics().onTakeoffCompleted(this.aircraft, this.time);
        }
    }
    
}
