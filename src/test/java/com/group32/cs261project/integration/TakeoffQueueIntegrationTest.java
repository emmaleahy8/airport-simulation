package com.group32.cs261project.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.group32.cs261project.airport.events.AircraftOutboundReadyEvent;
import com.group32.cs261project.model.Aircraft;
import com.group32.cs261project.model.Airport;
import com.group32.cs261project.model.Runway;
import com.group32.cs261project.model.enums.AircraftState;
import com.group32.cs261project.model.enums.EmergencyStatus;
import com.group32.cs261project.model.enums.RunwayMode;
import com.group32.cs261project.model.enums.RunwayStatus;
import com.group32.cs261project.sim.SimConfig;
import com.group32.cs261project.sim.SimulationEngine;
import com.group32.cs261project.sim.queue.PriorityEventQueue;

public class TakeoffQueueIntegrationTest {

    @Test
    void outboundAircraftMaintainFIFOOrderWhenReadyAtSameTime() {

        // -------------------------
        // 1. Simulation setup
        // -------------------------
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant end = start.plusSeconds(3600);

        SimConfig config = new SimConfig(
                start,
                end,
                42L,
                0.0,
                0.0
        );

        Runway runway = new Runway(
                0,
                3000,
                90,
                RunwayMode.MIXED,
                RunwayStatus.AVAILABLE
        );

        Airport airport = new Airport(List.of(runway));

        SimulationEngine<Airport> engine =
                new SimulationEngine<>(
                        config,
                        airport,
                        new PriorityEventQueue<>()
                );

        // -------------------------
        // 2. Create two outbound aircraft
        // SAME timestamp to test true FIFO
        // -------------------------
        Instant readyTime = start.plusSeconds(600);

        Aircraft firstAircraft = new Aircraft(
                "OUT1",
                "TestAir",
                "MAN",
                "LHR",
                readyTime,
                EmergencyStatus.NONE,
                0.0,
                AircraftState.OUTSIDE_MODEL
        );

        Aircraft secondAircraft = new Aircraft(
                "OUT2",
                "TestAir",
                "MAN",
                "LHR",
                readyTime,
                EmergencyStatus.NONE,
                0.0,
                AircraftState.OUTSIDE_MODEL
        );

        // -------------------------
        // 3. Schedule outbound ready events (order matters)
        // -------------------------
        engine.schedule(new AircraftOutboundReadyEvent(readyTime, firstAircraft));
        engine.schedule(new AircraftOutboundReadyEvent(readyTime, secondAircraft));

        // -------------------------
        // 4. Run simulation
        // -------------------------
        engine.runUntil(end);

        // -------------------------
        // 5. Assertions
        // -------------------------

        // Both aircraft should now be in takeoff queue
        assertEquals(2, airport.takeOffQueue().size());

        // FIFO: first scheduled should be at front
        assertEquals(
                firstAircraft,
                airport.takeOffQueue().peek(),
                "FIFO ordering failed: first outbound aircraft should be first in queue"
        );

        // Dequeue first
        Aircraft dequeuedFirst = airport.takeOffQueue().dequeue();
        assertEquals(firstAircraft, dequeuedFirst);

        // Next should be second
        Aircraft dequeuedSecond = airport.takeOffQueue().dequeue();
        assertEquals(secondAircraft, dequeuedSecond);

        // Both states should be TAKEOFF_QUEUE
        assertEquals(AircraftState.TAKEOFF_QUEUE, firstAircraft.state());
        assertEquals(AircraftState.TAKEOFF_QUEUE, secondAircraft.state());

        // Entry times should be set correctly
        assertEquals(readyTime, firstAircraft.takeOffQueueEntryTime());
        assertEquals(readyTime, secondAircraft.takeOffQueueEntryTime());
    }
}