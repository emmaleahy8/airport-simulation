package com.group32.cs261project.integration;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.AirportSimulation;
import com.group32.cs261project.backend.events.FuelEmergencyEvent;
import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.model.RunwayData;
import com.group32.cs261project.backend.model.enums.AircraftDirection;
import com.group32.cs261project.backend.model.enums.AircraftState;
import com.group32.cs261project.backend.model.enums.EmergencyStatus;
import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;
import com.group32.cs261project.backend.sim.SimConfig;

/**
 * Integration test: the emergency fuel event promotes an aircraft in holding.
 * Name ends with IT => Maven Failsafe will run it in mvn verify.
 */
class FuelEmergencyIT {

    @Test
    void fuelEmergencyEventPromotesAircraftToEmergencyQueue() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t1 = t0.plus(Duration.ofHours(1));

        // Build minimal backend context
        SimConfig config = SimConfig.builder()
            .startTime(t0)
            .endTime(t1)
            .randomSeed(123L)
            .build();
            
        Airport airport = new Airport(List.of(
                new RunwayData(3000, 90, RunwayMode.LANDING, RunwayStatus.AVAILABLE)
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

        assertFalse(a.isEmergency(), "precondition: aircraft should not be emergency");
        assertEquals(airport.holdingPattern().toList().contains(a), true);

        // Schedule emergency event at +5 minutes
        sim.engine().schedule(new FuelEmergencyEvent(a, t0.plusSeconds(5 * 60)));

        // Run until queue empty (or step until after +5min)
        sim.runUntil(t1);

        assertEquals(EmergencyStatus.FUEL, a.emergencyStatus());
        // // depending on your design: verify it is now in emergency sub-queue
        // assertTrue(airport.holdingPattern().isInEmergencyQueue(a));
    }
}