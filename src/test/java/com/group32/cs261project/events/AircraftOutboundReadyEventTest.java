package com.group32.cs261project.events;

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

class AircraftOutboundReadyEventTest {

    @Test
    void eventTimeIsReturnedCorrectly() {

        Instant time = Instant.parse("2026-01-01T00:20:00Z");

        Aircraft aircraft = new Aircraft(
                "BA456",
                "British Airways",
                "LHR",
                "JFK",
                time,
                EmergencyStatus.NONE,
                200,
                AircraftState.OUTSIDE_MODEL,
                AircraftDirection.INBOUND
        );

        AircraftOutboundReadyEvent event =
                new AircraftOutboundReadyEvent(time, aircraft);

        assertEquals(time, event.time());
    }

    @Test
    void outboundAircraftIsPlacedIntoTakeOffQueue() {

        Instant time = Instant.parse("2026-01-01T00:20:00Z");

        SimConfig cfg = SimConfig.builder()
                .startTime(time.minusSeconds(600))
                .endTime(time.plusSeconds(3600))
                .randomSeed(42L)
                .build();

        Airport airport = new Airport(List.of(
                new RunwayData(3000, 90, RunwayMode.TAKEOFF, RunwayStatus.INSPECTION)
        ));

        AirportSimulation airportSimulation = new AirportSimulation(cfg, airport);

        Aircraft aircraft = new Aircraft(
                "BA456",
                "British Airways",
                "LHR",
                "JFK",
                time,
                EmergencyStatus.NONE,
                200,
                AircraftState.OUTSIDE_MODEL,
                AircraftDirection.OUTBOUND
        );

        AircraftOutboundReadyEvent event =
                new AircraftOutboundReadyEvent(time, aircraft);

        event.handle(airportSimulation.engine());

        assertFalse(airport.takeOffQueue().isEmpty());
        assertEquals(1, airport.takeOffQueue().size());
        assertEquals(aircraft, airport.takeOffQueue().peek());
        assertEquals(AircraftState.TAKEOFF_QUEUE, aircraft.state());
        assertEquals(time, aircraft.takeOffQueueEntryTime());
    }
}