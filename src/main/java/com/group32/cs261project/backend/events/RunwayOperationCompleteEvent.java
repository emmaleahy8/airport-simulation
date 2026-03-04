package com.group32.cs261project.backend.events;

import java.time.Instant;

import com.group32.cs261project.backend.model.Runway;
import com.group32.cs261project.backend.sim.SimulationContext;
import com.group32.cs261project.backend.sim.SimulationEngine;

public class RunwayOperationCompleteEvent implements Event<SimulationContext> {

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
    public void handle(SimulationEngine<SimulationContext> engine) {
        if (engine.config().isVerbose())
            System.out.format("%s: Operation on %s complete\n", time.toString(), runway.toString());
        this.runway.clearOccupancy();
        engine.context().scheduler().tryScheduleOperations(this.time, engine);
    }
    
}
