package com.group32.cs261project.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.group32.cs261project.airport.events.AircraftInboundArrivesEvent;
import com.group32.cs261project.model.Aircraft;
import com.group32.cs261project.model.Airport;
import com.group32.cs261project.model.RunwayData;
import com.group32.cs261project.model.enums.AircraftState;
import com.group32.cs261project.model.enums.EmergencyStatus;
import com.group32.cs261project.model.enums.RunwayMode;
import com.group32.cs261project.model.enums.RunwayStatus;
import com.group32.cs261project.scheduling.Scheduler;
import com.group32.cs261project.sim.SimConfig;
import com.group32.cs261project.sim.SimulationContext;
import com.group32.cs261project.sim.SimulationEngine;
import com.group32.cs261project.sim.queue.PriorityEventQueue;

public class FIFOQueueIT {

    @Test
    void normalAircraftMaintainFIFOOrderWhenArrivingAtSameTime() {

        // -------------------------
        // 1. Setup simulation
        // -------------------------
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant end = start.plusSeconds(3600);

        SimConfig config = new SimConfig(
                start,
                end,
                42L,
                0.0,
                0.0,
                60
        );

        RunwayData runway = new RunwayData(
                3000,
                90,
                RunwayMode.MIXED,
                RunwayStatus.INSPECTION
        );

        Airport airport = new Airport(List.of(runway));

        SimulationEngine<SimulationContext> engine = new SimulationEngine<>(
                config,
                new SimulationContext(airport, new Scheduler()),
                new PriorityEventQueue<>()
        );

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
                AircraftState.OUTSIDE_MODEL
        );

        Aircraft secondAircraft = new Aircraft(
                "SECOND",
                "TestAir",
                "LHR",
                "MAN",
                arrivalTime,
                EmergencyStatus.NONE,
                120.0,
                AircraftState.OUTSIDE_MODEL
        );

        // -------------------------
        // 3. Schedule events (order matters)
        // -------------------------
        engine.schedule(new AircraftInboundArrivesEvent(arrivalTime, firstAircraft));
        engine.schedule(new AircraftInboundArrivesEvent(arrivalTime, secondAircraft));

        // -------------------------
        // 4. Run simulation
        // -------------------------
        engine.runUntil(end);

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