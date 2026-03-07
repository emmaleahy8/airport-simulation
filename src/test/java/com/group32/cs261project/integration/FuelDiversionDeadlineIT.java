package com.group32.cs261project.integration;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.AirportSimulation;
import com.group32.cs261project.backend.events.FuelDeadlineDiversionEvent;
import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.model.RunwayData;
import com.group32.cs261project.backend.model.enums.AircraftDirection;
import com.group32.cs261project.backend.model.enums.AircraftState;
import com.group32.cs261project.backend.model.enums.EmergencyStatus;
import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;
import com.group32.cs261project.backend.sim.SimConfig;

class FuelDiversionDeadlineIT {

    @Test
    void diversionDeadlineDivertsWhenStillHoldingAndNoLandingSlot() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t1 = t0.plus(Duration.ofHours(1));

        // Build minimal backend context
        SimConfig config = new SimConfig(t0, t1, 123L, 15, 15, 60);
        Airport airport = new Airport(List.of(
                new RunwayData(3000, 90, RunwayMode.LANDING, RunwayStatus.INSPECTION)
        ));

        AirportSimulation sim = new AirportSimulation(config, airport);

        // Put aircraft into holding as normal (non-emergency)
        Aircraft a = new Aircraft(
                "CS1", "Op", "AAA", "BBB",
                t0,                    // scheduled time
                EmergencyStatus.NONE,
                30.0,                  // initial fuel minutes (or however you store it)
                AircraftState.HOLDING, AircraftDirection.INBOUND
        );



        
        airport.holdingPattern().enqueue(a, t0);

        // Diversion threshold is 10, so deadline is at t0 + (12 - 10) = +2 minutes
        Instant deadline = t0.plusSeconds(2 * 60);
        sim.engine().schedule(new FuelDeadlineDiversionEvent(a, deadline));

        sim.runUntil(t1);

        assertEquals(AircraftState.DIVERTED, a.state());
        assertFalse(airport.holdingPattern().toList().contains(a), "diverted aircraft should be removed from holding");
    }
}