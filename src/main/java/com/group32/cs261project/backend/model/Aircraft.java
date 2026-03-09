package com.group32.cs261project.backend.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import com.group32.cs261project.backend.model.enums.AircraftDirection;
import com.group32.cs261project.backend.model.enums.AircraftState;
import com.group32.cs261project.backend.model.enums.EmergencyStatus;

/**
 * Aircraft class, contains details about aircraft
 */
public class Aircraft {
    
    private final String callsign;
    private final String operator;
    private final String origin;
    private final String destination;
    private final Instant scheduledTime;
    private EmergencyStatus emergency; // made this non-final - needs to be mutable
    private final double initialFuelMinutesRemaining;
    private final AircraftDirection direction;
    private AircraftState state;
    private Instant holdingEntryTime;
    private Instant takeOffQueueEntryTime;

    public Aircraft(String callsign, String operator, String origin, String destination, 
                    Instant scheduledTime, EmergencyStatus emergency, double initialFuelMinutesRemaining,
                    AircraftState state, AircraftDirection direction
    ) {
        this.callsign = Objects.requireNonNull(callsign);
        this.operator = Objects.requireNonNull(operator);
        this.origin = Objects.requireNonNull(origin);
        this.destination = Objects.requireNonNull(destination);
        this.scheduledTime = Objects.requireNonNull(scheduledTime);
        this.emergency = Objects.requireNonNull(emergency);
        this.initialFuelMinutesRemaining = initialFuelMinutesRemaining;
        this.state = Objects.requireNonNull(state);
        this.direction = Objects.requireNonNull(direction);
        this.holdingEntryTime = null;
        this.takeOffQueueEntryTime = null;

    }

    @Override
    public String toString() {
        return String.format("Aircraft(%s, %s, %f)", this.callsign, this.state.toString(), this.initialFuelMinutesRemaining);
    }

    /* getters */
    public String callsign() { return this.callsign; }
    public String operator() { return this.operator; }
    public String origin() { return this.origin; }
    public String destination() { return this.destination; }
    public Instant scheduledTime() { return this.scheduledTime; }
    public EmergencyStatus emergencyStatus() { return this.emergency; }
    public AircraftDirection direction() { return this.direction; }
    public AircraftState state() { return this.state; }
    public Instant holdingPatternEntryTime() { return this.holdingEntryTime; }
    public Instant takeOffQueueEntryTime() { return this.takeOffQueueEntryTime; }
    public double initialFuelMinutesRemaining() { return this.initialFuelMinutesRemaining; }

    
    /* emergency setters and helper methods */
    public boolean isEmergency() { return this.emergency != EmergencyStatus.NONE; }
    public void setEmergencyStatus(EmergencyStatus status) { this.emergency = Objects.requireNonNull(status); }

    
    /* state transitions */
    public void markHolding(Instant t) { this.state = AircraftState.HOLDING; this.holdingEntryTime = t; }
    public void markTakeoffQueue(Instant t) { this.state = AircraftState.TAKEOFF_QUEUE; this.takeOffQueueEntryTime = t; }
    public void markRunwayZone() { this.state = AircraftState.RUNWAY_ZONE; }
    public void markCompleted() { this.state = AircraftState.COMPLETED; }
    public void markDiverted() { this.state = AircraftState.DIVERTED; }
    public void markCancelled() { this.state = AircraftState.CANCELLED; }

    
    // check whether an aircraft has been handled by the airport already
    public boolean isCompleted() {
        return this.state != AircraftState.HOLDING && this.state != AircraftState.TAKEOFF_QUEUE;
    }

    // extra function
    public double delayMinutes(Instant actualTime) {
        return Duration.between(actualTime, this.scheduledTime).toMinutes();
    }

    // helper methods if emergencies escalate dynamically - may not be needed with setEmergency status methods
    // used for when an emergency is triggered
    public void declareMechanicalEmergency() {
        this.emergency = EmergencyStatus.MECHANICAL;
    }

    public void declarePassengerHealthEmergency() {
        this.emergency = EmergencyStatus.PASSENGER_HEALTH;
    }
    
}
