package com.group32.cs261project.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.AirportSimulation;
import com.group32.cs261project.backend.events.AircraftInboundArrivesEvent;
import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.model.RunwayData;
import com.group32.cs261project.backend.model.enums.AircraftDirection;
import com.group32.cs261project.backend.model.enums.AircraftState;
import com.group32.cs261project.backend.model.enums.EmergencyStatus;
import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;
import com.group32.cs261project.backend.sim.SimConfig;

public class FIFOQueueIT {

    @Test
    void normalAircraftMaintainFIFOOrderWhenArrivingAtSameTime() {

        // -------------------------
        // 1. Setup simulation
        // -------------------------
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant end = start.plusSeconds(3600);

        SimConfig config = SimConfig.builder()
                .startTime(start)
                .endTime(end)
                .randomSeed(42L)
                .inboundRatePerHour(0.0)
                .outboundRatePerHour(0.0)
                .build();

        RunwayData runway = new RunwayData(
                3000,
                90,
                RunwayMode.MIXED,
                RunwayStatus.INSPECTION
        );

        Airport airport = new Airport(List.of(runway));

        AirportSimulation airportSimulation = new AirportSimulation(config, airport);

        // -------------------------
        // 2. Create two normal aircraft
        // SAME timestamp to test true FIFO
        // -------------------------
        Instant arrivalTime = start.plusSeconds(300);

        Aircraft firstAircraft = new Aircraft(
                "FIRST",
                "TestAir",
                "LHR",
                "MAN",
                arrivalTime,
                EmergencyStatus.NONE,
                120.0,
                AircraftState.OUTSIDE_MODEL,
                AircraftDirection.INBOUND
        );

        Aircraft secondAircraft = new Aircraft(
                "SECOND",
                "TestAir",
                "LHR",
                "MAN",
                arrivalTime,
                EmergencyStatus.NONE,
                120.0,
                AircraftState.OUTSIDE_MODEL,
                AircraftDirection.INBOUND
        );

        // -------------------------
        // 3. Schedule events (order matters)
        // -------------------------
        airportSimulation.engine().schedule(new AircraftInboundArrivesEvent(arrivalTime, firstAircraft));
        airportSimulation.engine().schedule(new AircraftInboundArrivesEvent(arrivalTime, secondAircraft));

        // -------------------------
        // 4. Run simulation
        // -------------------------
        airportSimulation.engine().runUntil(end);

        // -------------------------
        // 5. Assertions
        // -------------------------

        // Both should be in holding
        assertEquals(2, airport.holdingPattern().size());

        // First scheduled aircraft should be first landing candidate
        assertEquals(
                firstAircraft,
                airport.holdingPattern().peekNextLandingCandidate(),
                "FIFO ordering failed: first aircraft should be first in queue"
        );

        // Dequeue first
        Aircraft dequeuedFirst =
                airport.holdingPattern().dequeueNextLandingCandidate();

        assertEquals(firstAircraft, dequeuedFirst);

        // Now second should be next
        Aircraft dequeuedSecond =
                airport.holdingPattern().dequeueNextLandingCandidate();

        assertEquals(secondAircraft, dequeuedSecond);

        // Both states should be HOLDING
        assertEquals(AircraftState.HOLDING, firstAircraft.state());
        assertEquals(AircraftState.HOLDING, secondAircraft.state());
    }
}