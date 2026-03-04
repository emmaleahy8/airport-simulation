package com.group32.cs261project.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.group32.cs261project.airport.events.AircraftInboundArrivesEvent;
import com.group32.cs261project.airport.events.AircraftOutboundReadyEvent;
import com.group32.cs261project.model.Aircraft;
import com.group32.cs261project.model.Airport;
import com.group32.cs261project.model.Runway;
import com.group32.cs261project.model.enums.AircraftState;
import com.group32.cs261project.model.enums.EmergencyStatus;
import com.group32.cs261project.model.enums.RunwayMode;
import com.group32.cs261project.model.enums.RunwayStatus;
import com.group32.cs261project.scheduling.Scheduler;
import com.group32.cs261project.sim.SimConfig;
import com.group32.cs261project.sim.SimulationContext;
import com.group32.cs261project.sim.SimulationEngine;
import com.group32.cs261project.sim.queue.PriorityEventQueue;

public class SimulationEndToEndIT {

    @Test
    void inboundAndOutboundEventsAreProcessedAndQueuedCorrectly() {

        // -------------------------
        // 1. Simulation configuration
        // -------------------------
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant end = start.plusSeconds(3600);

        SimConfig config = new SimConfig(
                start,
                end,
                42L,
                5.0,
                5.0,
                60
        );

        // -------------------------
        // 2. Create runway + airport
        // -------------------------
        Runway runway = new Runway(
                0,
                3000,
                90,
                RunwayMode.MIXED,
                RunwayStatus.AVAILABLE
        );

        Airport airport = new Airport(List.of(runway));

        // -------------------------
        // 3. Create simulation engine
        // -------------------------
        SimulationEngine<SimulationContext> engine = new SimulationEngine<>(
                config,
                new SimulationContext(airport, new Scheduler()),
                new PriorityEventQueue<>()
        );

        // -------------------------
        // 4. Create aircraft
        // -------------------------
        Aircraft inboundAircraft = new Aircraft(
                "IN123",
                "TestAir",
                "LHR",
                "MAN",
                start.plusSeconds(300),
                EmergencyStatus.NONE,
                120.0,
                AircraftState.OUTSIDE_MODEL
        );

        Aircraft outboundAircraft = new Aircraft(
                "OUT456",
                "TestAir",
                "MAN",
                "LHR",
                start.plusSeconds(600),
                EmergencyStatus.NONE,
                120.0,
                AircraftState.OUTSIDE_MODEL
        );

        // -------------------------
        // 5. Schedule events
        // -------------------------
        engine.schedule(new AircraftInboundArrivesEvent(
                inboundAircraft.scheduledTime(),
                inboundAircraft
        ));

        engine.schedule(new AircraftOutboundReadyEvent(
                outboundAircraft.scheduledTime(),
                outboundAircraft
        ));

        // -------------------------
        // 6. Run simulation
        // -------------------------
        engine.runUntil(end);

        // -------------------------
        // 7. Assertions
        // -------------------------

        // Inbound aircraft should be in holding pattern
        assertFalse(
                airport.holdingPattern().isEmpty(),
                "Holding pattern should contain inbound aircraft"
        );

        // Outbound aircraft should be in takeoff queue
        assertFalse(
                airport.takeOffQueue().isEmpty(),
                "Takeoff queue should contain outbound aircraft"
        );

        // Engine time should have advanced
        assertTrue(engine.now().isAfter(start));
    }
}