package com.group32.cs261project.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.group32.cs261project.airport.events.AircraftInboundArrivesEvent;
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

public class EmergencyPriorityIT {

    @Test
    void emergencyAircraftIsPrioritizedInHoldingPattern() {

        // -------------------------
        // 1. Simulation config
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

        // -------------------------
        // 2. Airport + runway
        // -------------------------
        Runway runway = new Runway(
                0,
                3000,
                90,
                RunwayMode.MIXED,
                RunwayStatus.INSPECTION
        );

        Airport airport = new Airport(List.of(runway));

        // -------------------------
        // 3. Engine
        // -------------------------
        SimulationEngine<SimulationContext> engine =
                new SimulationEngine<>(
                        config,
                        new SimulationContext(airport ,new Scheduler()),
                        new PriorityEventQueue<>()
                );

        // -------------------------
        // 4. Create aircraft
        // -------------------------
        Aircraft normalAircraft = new Aircraft(
                "NORMAL1",
                "TestAir",
                "LHR",
                "MAN",
                start.plusSeconds(300),
                EmergencyStatus.NONE,
                120.0,
                AircraftState.OUTSIDE_MODEL
        );

        Aircraft emergencyAircraft = new Aircraft(
                "EMERG1",
                "TestAir",
                "LHR",
                "MAN",
                start.plusSeconds(301),
                EmergencyStatus.FUEL,
                10.0,
                AircraftState.OUTSIDE_MODEL
        );

        // -------------------------
        // 5. Schedule inbound events
        // -------------------------
        engine.schedule(new AircraftInboundArrivesEvent(
                normalAircraft.scheduledTime(),
                normalAircraft
        ));

        engine.schedule(new AircraftInboundArrivesEvent(
                emergencyAircraft.scheduledTime(),
                emergencyAircraft
        ));

        // -------------------------
        // 6. Run simulation
        // -------------------------
        engine.runUntil(end);

        // -------------------------
        // 7. Assertions
        // -------------------------

        // Both should be in holding
        assertEquals(2, airport.holdingPattern().size());

        // Emergency aircraft should be next landing candidate
        assertEquals(
                emergencyAircraft,
                airport.holdingPattern().peekNextLandingCandidate(),
                "Emergency aircraft must be prioritized"
        );

        // Ensure both states updated
        assertEquals(AircraftState.HOLDING, normalAircraft.state());
        assertEquals(AircraftState.HOLDING, emergencyAircraft.state());
    }
}