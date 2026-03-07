package com.group32.cs261project.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.AirportSimulation;
import com.group32.cs261project.backend.events.AircraftInboundArrivesEvent;
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
        RunwayData runway = new RunwayData(
                3000,
                90,
                RunwayMode.MIXED,
                RunwayStatus.INSPECTION
        );

        Airport airport = new Airport(List.of(runway));

        // -------------------------
        // 3. Create simulation airportSimulation.engine()
        // -------------------------
        AirportSimulation airportSimulation = new AirportSimulation(config, airport);

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
                AircraftState.OUTSIDE_MODEL,
                AircraftDirection.INBOUND
        );

        Aircraft outboundAircraft = new Aircraft(
                "OUT456",
                "TestAir",
                "MAN",
                "LHR",
                start.plusSeconds(600),
                EmergencyStatus.NONE,
                120.0,
                AircraftState.OUTSIDE_MODEL,
                AircraftDirection.OUTBOUND);

        // -------------------------
        // 5. Schedule events
        // -------------------------
        airportSimulation.engine().schedule(new AircraftInboundArrivesEvent(
                inboundAircraft.scheduledTime(),
                inboundAircraft
        ));

        airportSimulation.engine().schedule(new AircraftOutboundReadyEvent(
                outboundAircraft.scheduledTime(),
                outboundAircraft
        ));

        // -------------------------
        // 6. Run simulation
        // -------------------------
        airportSimulation.engine().runUntil(end);

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

        // airportSimulation.engine() time should have advanced
        assertTrue(airportSimulation.engine().now().isAfter(start));
    }
}