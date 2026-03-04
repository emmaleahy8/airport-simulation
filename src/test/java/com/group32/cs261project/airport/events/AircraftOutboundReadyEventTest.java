package com.group32.cs261project.airport.events;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.group32.cs261project.model.Aircraft;
import com.group32.cs261project.model.Airport;
import com.group32.cs261project.model.RunwayData;
import com.group32.cs261project.model.enums.AircraftState;
import com.group32.cs261project.model.enums.EmergencyStatus;
import com.group32.cs261project.model.enums.RunwayMode;
import com.group32.cs261project.model.enums.RunwayStatus;
import com.group32.cs261project.scheduling.Scheduler;
import com.group32.cs261project.sim.SimConfig;
import com.group32.cs261project.sim.SimulationContext;
import com.group32.cs261project.sim.SimulationEngine;
import com.group32.cs261project.sim.queue.PriorityEventQueue;

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
                AircraftState.OUTSIDE_MODEL
        );

        AircraftOutboundReadyEvent event =
                new AircraftOutboundReadyEvent(time, aircraft);

        assertEquals(time, event.time());
    }

    @Test
    void outboundAircraftIsPlacedIntoTakeOffQueue() {

        Instant time = Instant.parse("2026-01-01T00:20:00Z");

        SimConfig cfg = new SimConfig(
                time.minusSeconds(600),
                time.plusSeconds(3600),
                42L,
                15.0,
                15.0,
                60
        );

        Airport airport = new Airport(List.of(
                new RunwayData(3000, 90, RunwayMode.TAKEOFF, RunwayStatus.INSPECTION)
        ));

        SimulationEngine<SimulationContext> engine =
                new SimulationEngine<>(cfg, new SimulationContext(airport, new Scheduler()), new PriorityEventQueue<>());

        Aircraft aircraft = new Aircraft(
                "BA456",
                "British Airways",
                "LHR",
                "JFK",
                time,
                EmergencyStatus.NONE,
                200,
                AircraftState.OUTSIDE_MODEL
        );

        AircraftOutboundReadyEvent event =
                new AircraftOutboundReadyEvent(time, aircraft);

        event.handle(engine);

        assertFalse(airport.takeOffQueue().isEmpty());
        assertEquals(1, airport.takeOffQueue().size());
        assertEquals(aircraft, airport.takeOffQueue().peek());
        assertEquals(AircraftState.TAKEOFF_QUEUE, aircraft.state());
        assertEquals(time, aircraft.takeOffQueueEntryTime());
    }
}