package com.group32.cs261project.events;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.AirportSimulation;
import com.group32.cs261project.backend.events.AircraftInboundArrivesEvent;
import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.model.RunwayData;
import com.group32.cs261project.backend.model.enums.AircraftState;
import com.group32.cs261project.backend.model.enums.EmergencyStatus;
import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;
import com.group32.cs261project.backend.sim.SimConfig;

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
                new RunwayData(3000, 90, RunwayMode.LANDING, RunwayStatus.INSPECTION)
        ));

        AirportSimulation airportSimulation = new AirportSimulation(cfg, airport);

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

        event.handle(airportSimulation.engine());

        assertFalse(airport.holdingPattern().isEmpty());
        assertEquals(1, airport.holdingPattern().size());
        assertEquals(aircraft,
                airport.holdingPattern().peekNextLandingCandidate());
        assertEquals(AircraftState.HOLDING, aircraft.state());
        assertEquals(time, aircraft.holdingPatternEntryTime());
    }
}