package com.group32.cs261project.backend.sim;

import java.time.Instant;
import java.util.Objects;


/**
 * Configuration class for the simulation
 */
public record SimConfig(
    Instant startTime,
    Instant endTime,
    long randomSeed,
    double inboundRatePerHour,
    double outboundRatePerHour,
    int runwayOccupancySeconds,
    long maxTakeoffWaitMinutes,
    int runwayCount,
    int lowFuelMinutes,
    int fuelDiversionMinutes,
    double mechanicalEmergencyProbability,
    double passengerHealthEmergencyProbability,
    boolean verbose
) {
    public SimConfig {

        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(endTime, "endTime");

        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime must be equal or after startTime");
        }

    }

    /**
     * Makes a new builder to specify optional parameters
     * @return sim config builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        // defaults
        private Instant startTime = Instant.parse("2026-01-01T00:00:00Z");
        private Instant endTime = Instant.parse("2026-01-01T02:00:00Z");
        private long randomSeed = 42L;
        private double inboundRatePerHour = 15.0;
        private double outboundRatePerHour = 15.0;
        private int runwayOccupancySeconds = 60;
        private long maxTakeoffWaitMinutes = 30;
        private int runwayCount = 2;
        private int lowFuelMinutes = 20;
        private int fuelDiversionMinutes = 10;
        private double mechanicalEmergencyProbability = 0.10;
        private double passengerHealthEmergencyProbability = 0.10;
        private boolean verbose = false;

        private Builder() {}

        public Builder startTime(Instant v) { this.startTime = Objects.requireNonNull(v); return this; }
        public Builder endTime(Instant v) { this.endTime = Objects.requireNonNull(v); return this; }
        public Builder randomSeed(long v) { this.randomSeed = v; return this; }
        public Builder inboundRatePerHour(double v) { this.inboundRatePerHour = v; return this; }
        public Builder outboundRatePerHour(double v) { this.outboundRatePerHour = v; return this; }
        public Builder runwayOccupancySeconds(int v) { this.runwayOccupancySeconds = v; return this; }
        public Builder maxTakeoffWaitMinutes(long v) { this.maxTakeoffWaitMinutes = v; return this; }
        public Builder runwayCount(int v) { this.runwayCount = v; return this; }
        public Builder lowFuelMinutes(int v) { this.lowFuelMinutes = v; return this; }
        public Builder fuelDiversionMinutes(int v) { this.fuelDiversionMinutes = v; return this; }
        public Builder mechanicalEmergencyProbability(double v) { this.mechanicalEmergencyProbability = v; return this; }
        public Builder passengerHealthEmergencyProbability(double v) { this.passengerHealthEmergencyProbability = v; return this; }
        public Builder verbose(boolean v) { this.verbose = v; return this; }

        /**
         * turn the builder object back into a simconfig object
         * @return simconfig
         */
        public SimConfig build() {
            return new SimConfig(
                this.startTime,
                this.endTime,
                this.randomSeed,
                this.inboundRatePerHour,
                this.outboundRatePerHour,
                this.runwayOccupancySeconds,
                this.maxTakeoffWaitMinutes,
                this.runwayCount,
                this.lowFuelMinutes,
                this.fuelDiversionMinutes,
                this.mechanicalEmergencyProbability,
                this.passengerHealthEmergencyProbability,
                this.verbose
            );

        }
    
    }

}
