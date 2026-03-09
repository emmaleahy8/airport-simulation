package com.group32.cs261project.backend.gen;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.enums.AircraftDirection;
import com.group32.cs261project.backend.model.enums.AircraftState;
import com.group32.cs261project.backend.model.enums.EmergencyStatus;
import com.group32.cs261project.backend.sim.SimConfig;

public class FlightGenerator {

    private final Random rng;
    private final double sigmaMinutes = 5.0;
    private final Instant startTime;
    private final Instant endTime;
    private final double inboundRatePerHour;
    private final double outboundRatePerHour;

    private int callsignCounter = 1;
    private static final List<String> OPERATORS = List.of(
            "BAW", "EZY", "RYR", "VIR", "KLM", "AFR", "DLH"
    );
    private static final List<String> AIRPORTS = List.of(
            "LHR", "LGW", "MAN", "EDI", "DUB", "AMS", "CDG", "FRA"
    );
    private static final String HOME_AIRPORT = "LHR";


    
    /**
     * Constructor
     * @param engine instance of simulation engine
     */
    public FlightGenerator(SimConfig config, Random rng) {
        this.rng = rng;
        this.startTime = config.startTime();
        this.endTime = config.endTime();
        this.inboundRatePerHour = config.inboundRatePerHour();
        this.outboundRatePerHour = config.outboundRatePerHour();
    }

    private String randomOperator() {
        return OPERATORS.get(this.rng.nextInt(OPERATORS.size()));
    }

    private String randomOtherAirport() {
        // Ensure it’s not HOME_AIRPORT
        String code;
        do {
            code = AIRPORTS.get(rng.nextInt(AIRPORTS.size()));
        } while (code.equals(HOME_AIRPORT));
        return code;
    }

    private String nextCallsign(String operator) {
        // e.g. "BAW0001", "EZY0002" ...
        return String.format("%s%04d", operator, callsignCounter++);
    }

    private double randomFuelMinutes() {
        // Uniform 20–60 minutes (inclusive-ish; close enough for sim)
        return 20.0 + rng.nextDouble() * 40.0;
    }

    private Aircraft makeInboundAircraft(Instant scheduledTime) {
        String operator = randomOperator();
        String callsign = nextCallsign(operator);

        String origin = randomOtherAirport();
        String destination = HOME_AIRPORT;

        return new Aircraft(
                callsign,
                operator,
                origin,
                destination,
                scheduledTime,
                EmergencyStatus.NONE,
                randomFuelMinutes(),
                AircraftState.OUTSIDE_MODEL,
                AircraftDirection.INBOUND
        );
    }

    private Aircraft makeOutboundAircraft(Instant scheduledTime) {
        String operator = randomOperator();
        String callsign = nextCallsign(operator);

        String origin = HOME_AIRPORT;
        String destination = randomOtherAirport();

        return new Aircraft(
                callsign,
                operator,
                origin,
                destination,
                scheduledTime,
                EmergencyStatus.NONE,
                randomFuelMinutes(),
                AircraftState.OUTSIDE_MODEL,
                AircraftDirection.OUTBOUND
        );
    }
    
    /**
     * Helper method to get entry time from scheduled time
     * @param scheduledTime scheduled time
     * @return entry time + N(0, 5^2) random variable
     */
    private Instant entryTime(Instant scheduledTime) {
        double delayMinutes = rng.nextGaussian() * this.sigmaMinutes;
        long millis = Math.round(delayMinutes * 60_000.0);
        Instant entryTime = scheduledTime.plusMillis(millis);
        if (entryTime.isBefore(startTime)) {
            return startTime;
        } else if (entryTime.isAfter(endTime)) {
            return endTime;
        } else {
            return entryTime;
        }
    }

    /**
     * Helper method to get inter flight duration from rate per hour
     * @param ratePerHour rate per hour (in or out)
     * @return duration object
     */
    private Duration interFlightDuration(double ratePerHour) {
        double spacingMinutes = 60.0 / ratePerHour;
        long spacingMillis = Math.round(spacingMinutes * 60_000.0);
        return Duration.ofMillis(spacingMillis);
    }

    /**
     * Generate flights based on type
     * @param type one of INBOUND, OUTBOUND
     * @return list of generated flight objects
     */
    public List<GeneratedFlight> generateFlights(FlightType type) {

        List<GeneratedFlight> flights = new ArrayList<>();

        Duration spacing = interFlightDuration( switch (type) {
            case INBOUND -> this.inboundRatePerHour;
            case OUTBOUND -> this.outboundRatePerHour;
        });

        Instant t = this.startTime;
        
        while (!t.isAfter(endTime)) {
            Aircraft aircraft = switch (type) {
                case INBOUND -> this.makeInboundAircraft(t);
                case OUTBOUND -> this.makeOutboundAircraft(t);
            };
            flights.add(new GeneratedFlight(aircraft, t, this.entryTime(t)));
            t = t.plus(spacing);
        }

        return flights;
    }
    
}
