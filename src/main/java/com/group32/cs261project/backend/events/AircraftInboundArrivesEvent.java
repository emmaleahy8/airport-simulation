package com.group32.cs261project.backend.events;

import java.time.Duration;
import java.time.Instant;

import com.group32.cs261project.backend.gen.EmergencyGenerator;
import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.sim.SimulationContext;
import com.group32.cs261project.backend.sim.SimulationEngine;

public class AircraftInboundArrivesEvent implements Event<SimulationContext> {

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
    public void handle(SimulationEngine<SimulationContext> engine) {

        if (engine.config().verbose())
            System.out.format("%s: %s enqueued to holding pattern\n", time.toString(), aircraft.toString());


        // enqueue to holding pattern
        engine.context().airport().holdingPattern().enqueue(aircraft, time);


        // schedule fuel emergency event
        Instant fuelEmergencyTime = time.plus(
            Duration.ofSeconds((long) Math.max(60.0 * (this.aircraft.fuelMinutesRemaining() - engine.config().lowFuelMinutes()), 0.0))
        );
        engine.schedule(new FuelEmergencyEvent(this.aircraft, fuelEmergencyTime));


        // schedule fuel deadline event
        Instant fuelDiversionTime = time.plus(
            Duration.ofSeconds((long) Math.max(60.0 * (this.aircraft.fuelMinutesRemaining() - engine.config().fuelDiversionMinutes()), 0.0))
        );
        engine.schedule(new FuelDeadlineDiversionEvent(aircraft, fuelDiversionTime));

        // schedule random emergencies
        EmergencyGenerator.scheduleEmergency(engine, this.aircraft, this.time);

        // try to schedule runway operations immediately
        engine.context().scheduler().tryScheduleOperations(time, engine);

        // update metrics by calling onHoldingEnqueue with updated size
        engine.context().metrics().onHoldingEnqueue(time, engine.context().airport().holdingPattern().size());
    }
    
}
