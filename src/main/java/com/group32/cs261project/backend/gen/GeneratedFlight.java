package com.group32.cs261project.backend.gen;

import java.time.Instant;

import com.group32.cs261project.backend.model.Aircraft;

/**
 * Class to store data of flight generated
 */
public class GeneratedFlight {

    private final Aircraft aircraft;
    private final Instant scheduledTime;
    private final Instant entryTime;

    public GeneratedFlight(Aircraft aircraft, Instant scheduledTime, Instant entryTime) {
        this.aircraft = aircraft;
        this.scheduledTime = scheduledTime;
        this.entryTime = entryTime;
    }

    public Aircraft aircraft() {
        return this.aircraft;
    }

    public Instant scheduledTime() {
        return this.scheduledTime;
    }

    public Instant entryTime() {
        return this.entryTime;
    }
}
