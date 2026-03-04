package com.group32.cs261project.backend.events;

import java.time.Instant;

import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.Runway;
import com.group32.cs261project.backend.scheduling.OperationType;
import com.group32.cs261project.backend.sim.SimulationContext;
import com.group32.cs261project.backend.sim.SimulationEngine;

public class RunwayOperationCompleteEvent implements Event<SimulationContext> {

    private final Instant time;
    private final Aircraft aircraft;
    private final Runway runway;
    private final OperationType operationType;

    public RunwayOperationCompleteEvent(Instant time, Aircraft aircraft, Runway runway, OperationType operationType) {
        this.time = time;
        this.aircraft = aircraft;
        this.runway = runway;
        this.operationType = operationType;
    }

    @Override
    public Instant time() {
        return this.time;
    }

    @Override
    public void handle(SimulationEngine<SimulationContext> engine) {

        if (engine.config().isVerbose())
            System.out.format("%s: Operation on %s complete\n", time.toString(), runway.toString());

        // clear the runway
        this.runway.clearOccupancy();

        // try to schedule next runway operation now the runway is clear
        engine.context().scheduler().tryScheduleOperations(this.time, engine);

        // update metrics based on the operation type
        switch (this.operationType) {
            case LANDING -> engine.context().metrics().onLandingCompleted(this.aircraft, this.time);
            case TAKEOFF -> engine.context().metrics().onTakeoffCompleted(this.aircraft, this.time);
        }
    }
    
}
