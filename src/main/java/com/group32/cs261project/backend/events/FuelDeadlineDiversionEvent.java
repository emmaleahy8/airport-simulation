package com.group32.cs261project.backend.events;

import java.time.Instant;

import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.sim.SimulationContext;
import com.group32.cs261project.backend.sim.SimulationEngine;

public class FuelDeadlineDiversionEvent implements Event<SimulationContext> {

    private final Aircraft aircraft;
    private final Instant time;

    public FuelDeadlineDiversionEvent(Aircraft aircraft, Instant time) {
        this.aircraft = aircraft;
        this.time = time;
    }

    @Override
    public Instant time() {
        return this.time;
    }

    @Override
    public void handle(SimulationEngine<SimulationContext> engine) {

        if (engine.config().isVerbose())
            System.out.format("%s: %s diverted due to low fuel\n", time.toString(), aircraft.toString());

        if (!this.aircraft.isCompleted()) { // aircraft not completed yet

            // remove aircraft from holding pattern        
            engine.context().airport().holdingPattern().remove(this.aircraft);

            // update metrics for diversions
            engine.context().metrics().onDiverted(this.aircraft, this.time);
        }
    }

}
