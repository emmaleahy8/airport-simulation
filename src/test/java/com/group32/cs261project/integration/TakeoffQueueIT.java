package com.group32.cs261project.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.AirportSimulation;
import com.group32.cs261project.backend.events.AircraftOutboundReadyEvent;
import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.model.RunwayData;
import com.group32.cs261project.backend.model.enums.AircraftDirection;
import com.group32.cs261project.backend.model.enums.AircraftState;
import com.group32.cs261project.backend.model.enums.EmergencyStatus;
import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;
import com.group32.cs261project.backend.sim.SimConfig;

public class TakeoffQueueIT {

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

        AirportSimulation airportSimulation = new AirportSimulation(config, airport);

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
                AircraftState.OUTSIDE_MODEL,
                AircraftDirection.OUTBOUND
                
        );

        Aircraft secondAircraft = new Aircraft(
                "OUT2",
                "TestAir",
                "MAN",
                "LHR",
                readyTime,
                EmergencyStatus.NONE,
                0.0,
                AircraftState.OUTSIDE_MODEL,
                AircraftDirection.OUTBOUND
        );

        // -------------------------
        // 3. Schedule outbound ready events (order matters)
        // -------------------------
        airportSimulation.engine().schedule(new AircraftOutboundReadyEvent(readyTime, firstAircraft));
        airportSimulation.engine().schedule(new AircraftOutboundReadyEvent(readyTime, secondAircraft));

        // -------------------------
        // 4. Run simulation
        // -------------------------
        airportSimulation.engine().runUntil(end);

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