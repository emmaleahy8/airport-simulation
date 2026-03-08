package com.group32.cs261project.backend.events;

import java.time.Instant;

import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.sim.SimulationContext;
import com.group32.cs261project.backend.sim.SimulationEngine;

public class MechanicalFailureEmergencyEvent implements Event<SimulationContext> {
    
    private final Aircraft aircraft;
    private final Instant time;

    public MechanicalFailureEmergencyEvent(Aircraft aircraft, Instant time) {
        this.aircraft = aircraft;
        this.time = time;
    }

    @Override
    public Instant time() {
        return time;
    }

    @Override
    public void handle(SimulationEngine<SimulationContext> engine) {

        if (!this.aircraft.isCompleted()) { // aircraft not completed yet

            if (engine.config().isVerbose())
                System.out.format("%s: %s promoted to emergency due to mechanical failure\n", time.toString(), aircraft.toString());

            engine.context().airport().holdingPattern().promoteToEmergency(this.aircraft, this.time);
        }
    }
    
}
