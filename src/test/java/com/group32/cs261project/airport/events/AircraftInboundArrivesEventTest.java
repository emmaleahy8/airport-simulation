package com.group32.cs261project.airport.events;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

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

class AircraftInboundArrivesEventTest {

    @Test
    void inboundAircraftIsPlacedIntoHoldingPattern() {

        Instant time = Instant.parse("2026-01-01T00:10:00Z");

        SimConfig cfg = new SimConfig(
                time.minusSeconds(600),
                time.plusSeconds(3600),
                42L,
                15.0,
                15.0,
                60
        );

        Airport airport = new Airport(List.of(
                new Runway(9, 3000, 90, RunwayMode.LANDING, RunwayStatus.INSPECTION)
        ));

        SimulationEngine<SimulationContext> engine =
                new SimulationEngine<>(cfg, new SimulationContext(airport, new Scheduler()), new PriorityEventQueue<>());

        Aircraft aircraft = new Aircraft(
                "BA123",
                "British Airways",
                "JFK",
                "LHR",
                time,
                EmergencyStatus.NONE,
                120,
                AircraftState.OUTSIDE_MODEL
        );

        AircraftInboundArrivesEvent event =
                new AircraftInboundArrivesEvent(time, aircraft);

        event.handle(engine);

        assertFalse(airport.holdingPattern().isEmpty());
        assertEquals(1, airport.holdingPattern().size());
        assertEquals(aircraft,
                airport.holdingPattern().peekNextLandingCandidate());
        assertEquals(AircraftState.HOLDING, aircraft.state());
        assertEquals(time, aircraft.holdingPatternEntryTime());
    }
}