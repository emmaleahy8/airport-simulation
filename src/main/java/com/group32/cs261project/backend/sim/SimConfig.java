package com.group32.cs261project.backend.sim;

import java.time.Instant;
import java.util.Objects;

/**
 * Configuration class for the simulation
 */
public final class SimConfig {

    private final Instant startTime;
    private final Instant endTime;
    private final long randomSeed;
    private final double inboundRatePerHour;
    private final double outboundRatePerHour;
    private final int runwayOccupancySeconds;
    private int lowFuelMinutes = 20;
    private int fuelDiversionMinutes = 10; 
    private boolean verbose = false;

    public SimConfig(Instant startTime, Instant endTime, long randomSeed, double inboundRatePerHour, 
                     double outboundRatePerHour, int runwayOccupancySeconds 
    ) {
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.endTime = Objects.requireNonNull(endTime, "endTime");
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime must be >= startTime");
        }
        this.randomSeed = randomSeed;
        this.inboundRatePerHour = inboundRatePerHour;
        this.outboundRatePerHour = outboundRatePerHour;
        this.runwayOccupancySeconds = runwayOccupancySeconds;
    }

    public SimConfig(Instant startTime, Instant endTime, long randomSeed, double inboundRatePerHour, 
                     double outboundRatePerHour, int runwayOccupancySeconds, int lowFuelMinutes
    ) {
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.endTime = Objects.requireNonNull(endTime, "endTime");
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime must be >= startTime");
        }
        this.randomSeed = randomSeed;
        this.inboundRatePerHour = inboundRatePerHour;
        this.outboundRatePerHour = outboundRatePerHour;
        this.runwayOccupancySeconds = runwayOccupancySeconds;
        this.lowFuelMinutes = lowFuelMinutes;
    }

    public SimConfig(Instant startTime, Instant endTime, long randomSeed, 
        double inboundRatePerHour, double outboundRatePerHour, int runwayOccupancySeconds, 
        int lowFuelMinutes, int fuelDiversionMinutes
    ) {
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.endTime = Objects.requireNonNull(endTime, "endTime");
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime must be >= startTime");
        }
        this.randomSeed = randomSeed;
        this.inboundRatePerHour = inboundRatePerHour;
        this.outboundRatePerHour = outboundRatePerHour;
        this.runwayOccupancySeconds = runwayOccupancySeconds;
        this.lowFuelMinutes = lowFuelMinutes;
        this.fuelDiversionMinutes = fuelDiversionMinutes;
    }

    public SimConfig(Instant startTime, Instant endTime, long randomSeed, 
        double inboundRatePerHour, double outboundRatePerHour, int runwayOccupancySeconds, 
        int lowFuelMinutes, int fuelDiversionMinutes, boolean verbose
    ) {
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.endTime = Objects.requireNonNull(endTime, "endTime");
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime must be >= startTime");
        }
        this.randomSeed = randomSeed;
        this.inboundRatePerHour = inboundRatePerHour;
        this.outboundRatePerHour = outboundRatePerHour;
        this.runwayOccupancySeconds = runwayOccupancySeconds;
        this.lowFuelMinutes = lowFuelMinutes;
        this.fuelDiversionMinutes = fuelDiversionMinutes;
        this.verbose = verbose;
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public Instant startTime() {
        return this.startTime;
    }

    public Instant endTime() {
        return this.endTime;
    }

    public long randomSeed() {
        return this.randomSeed;
    }

    public double inboundRatePerHour() {
        return this.inboundRatePerHour;
    }

    public double outboundRatePerHour() {
        return this.outboundRatePerHour;
    }

    public int runwayOccupancySeconds() {
        return this.runwayOccupancySeconds;
    }

    public int lowFuelMinutes() {
        return this.lowFuelMinutes;
    }

    public int fuelDiversionMinutes() {
        return this.fuelDiversionMinutes;
    }
}