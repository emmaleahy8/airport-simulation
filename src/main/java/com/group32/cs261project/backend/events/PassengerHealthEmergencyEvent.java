package com.group32.cs261project.backend.events;

import java.time.Instant;

import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.sim.SimulationContext;
import com.group32.cs261project.backend.sim.SimulationEngine;


/**
 * Event for promoting aircraft to emergency due to passenger health emergency
 * Created in the future with a given probability for each aircraft
 * If aircraft isn't in holding pattern anymore, this is just ignored
 */
public class PassengerHealthEmergencyEvent implements Event<SimulationContext> {

    private final Aircraft aircraft;
    private final Instant time;

    public PassengerHealthEmergencyEvent(Aircraft aircraft, Instant time) {
        this.aircraft = aircraft;
        this.time = time;
    }

    @Override
    public Instant time() {
        return this.time;
    }

    @Override
    public void handle(SimulationEngine<SimulationContext> engine) {

        if (!this.aircraft.isCompleted()) { // aircraft not completed yet

            if (engine.config().verbose())
                System.out.format("%s: %s promoted to emergency due to passenger health emergency\n", time.toString(), aircraft.toString());

            engine.context().airport().holdingPattern().promoteToEmergency(this.aircraft, this.time);
        }

    }
    
}
