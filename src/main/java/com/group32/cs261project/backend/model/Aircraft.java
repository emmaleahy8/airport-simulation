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
    private final EmergencyStatus emergency;
    private double fuelMinutesRemaining;
    private final AircraftDirection direction;
    private AircraftState state;
    private Instant holdingEntryTime;
    private Instant takeOffQueueEntryTime;

    /**
     * Constructor
     * @param callsign
     * @param operator
     * @param origin
     * @param destination
     * @param scheduledTime
     * @param emergency
     * @param fuelMinutesRemaining
     * @param state
     * @param holdingEntryTime
     * @param takeOffQueueEntryTime
     */
    public Aircraft(String callsign, String operator, String origin, String destination, 
                    Instant scheduledTime, EmergencyStatus emergency, double fuelMinutesRemaining,
                    AircraftState state, AircraftDirection direction
    ) {
        this.callsign = Objects.requireNonNull(callsign);
        this.operator = Objects.requireNonNull(operator);
        this.origin = Objects.requireNonNull(origin);
        this.destination = Objects.requireNonNull(destination);
        this.scheduledTime = Objects.requireNonNull(scheduledTime);
        this.emergency = Objects.requireNonNull(emergency);
        this.fuelMinutesRemaining = fuelMinutesRemaining;
        this.state = Objects.requireNonNull(state);
        this.direction = Objects.requireNonNull(direction);
        this.holdingEntryTime = null;
        this.takeOffQueueEntryTime = null;

    }

    @Override
    public String toString() {
        return String.format("Aircraft(%s, %s)", this.callsign, this.state.toString());
    }

    // getters
    public String callsign() {
        return this.callsign;
    }

    public String operator() {
        return this.operator;
    }

    public String origin() {
        return this.origin;
    }

    public String destination() {
        return this.destination;
    }

    public Instant scheduledTime() {
        return this.scheduledTime;
    }

    public EmergencyStatus emergencyStatus() {
        return this.emergency;
    }
    
    public AircraftDirection direction() {
        return this.direction;
    }

    public boolean isEmergency() {
        return this.emergency != EmergencyStatus.NONE;
    }

    public AircraftState state() {
        return this.state;
    }

    public Instant holdingPatternEntryTime() {
        return this.holdingEntryTime;
    }

    public Instant takeOffQueueEntryTime() {
        return this.takeOffQueueEntryTime;
    }

    // extra function
    public double delayMinutes(Instant actualTime) {
        return Duration.between(actualTime, this.scheduledTime).toMinutes();
    }

    // state transition
    public void markHolding(Instant entryTime) {
        this.state = AircraftState.HOLDING;
        this.holdingEntryTime = entryTime;
    }

    public void markTakeoffQueue(Instant entryTime) {
        this.state = AircraftState.TAKEOFF_QUEUE;
        this.takeOffQueueEntryTime = entryTime;
    }

    public void markRunwayZone() {
        this.state = AircraftState.RUNWAY_ZONE;
    }

    public void markCompleted() {
        this.state = AircraftState.COMPLETED;
    }

    public void markDiverted() {
        this.state = AircraftState.DIVERTED;
    }

    public void markCancelled() {
        this.state = AircraftState.CANCELLED;
    }

    // fuel logic (for later)
    public void consumeFuelMinutes(double minutes) {
        if (this.fuelMinutesRemaining > minutes)
            this.fuelMinutesRemaining -= minutes;
    }

    public boolean isLowFuel(double thresholdMinutes) {
        return this.fuelMinutesRemaining <= thresholdMinutes;
    }
    
}
