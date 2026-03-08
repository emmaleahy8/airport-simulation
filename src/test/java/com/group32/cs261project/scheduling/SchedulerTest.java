package com.group32.cs261project.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.AirportSimulation;
import com.group32.cs261project.backend.events.Event;
import com.group32.cs261project.backend.events.RunwayOperationCompleteEvent;
import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.model.RunwayData;
import com.group32.cs261project.backend.model.enums.AircraftDirection;
import com.group32.cs261project.backend.model.enums.AircraftState;
import com.group32.cs261project.backend.model.enums.EmergencyStatus;
import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;
import com.group32.cs261project.backend.scheduling.Scheduler;
import com.group32.cs261project.backend.sim.SimConfig;
import com.group32.cs261project.backend.sim.SimulationContext;

class SchedulerTest {

    private static Aircraft mkAircraft(String callsign, EmergencyStatus emergency, Instant scheduledTime, AircraftDirection direction) {
        return new Aircraft(
                callsign,
                "TestAir",
                "AAA",
                "BBB",
                scheduledTime,
                emergency,
                40.0,
                AircraftState.OUTSIDE_MODEL,
                direction
        );
    }

    @Test
    void schedulesLandingOnAvailableLandingRunway() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant now = start.plusSeconds(300);
        Instant end = start.plus(Duration.ofHours(1));

        // Landing runway available
        RunwayData landingRunway = new RunwayData(3000, 90, RunwayMode.LANDING, RunwayStatus.AVAILABLE);

        Airport airport = new Airport(List.of(landingRunway));
        

        AirportSimulation airportSimulation = new AirportSimulation(
            SimConfig.builder()
                .startTime(start)
                .endTime(end)
                .randomSeed(123L)
                .build(),
            airport
        );

        // Put one aircraft in holding
        Aircraft a = mkAircraft("A1", EmergencyStatus.NONE, start.plusSeconds(100), AircraftDirection.INBOUND);
        airport.holdingPattern().enqueue(a, now);

        int occupancySeconds = 60;
        Scheduler scheduler = new Scheduler(); // adjust if your Scheduler takes config instead
        scheduler.tryScheduleOperations(now, airportSimulation.engine());

        // Holding should be drained by 1
        assertEquals(0, airport.holdingPattern().size());

        // Runway should be occupied until now + occupancy
        assertEquals(now.plusSeconds(occupancySeconds), airport.getRunway(0).occupiedUntil());


        // airportSimulation.engine() should now contain a completion event at the right time
        Event<SimulationContext> e = airportSimulation.engine().eventQueue().pop();
        assertNotNull(e);
        assertEquals(now.plusSeconds(occupancySeconds), e.time());
        assertTrue(e instanceof RunwayOperationCompleteEvent);
    }

    @Test
    void schedulesTakeoffOnAvailableTakeoffRunway() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant now = start.plusSeconds(300);
        Instant end = start.plus(Duration.ofHours(1));

        RunwayData takeoffRunway = new RunwayData(3000, 270, RunwayMode.TAKEOFF, RunwayStatus.AVAILABLE);

        Airport airport = new Airport(List.of(takeoffRunway));

        AirportSimulation airportSimulation = new AirportSimulation(
            SimConfig.builder()
                .startTime(start)
                .endTime(end)
                .randomSeed(123L)
                .build(),
            airport
        );

        Aircraft a = mkAircraft("D1", EmergencyStatus.NONE, start.plusSeconds(200), AircraftDirection.OUTBOUND);
        airport.takeOffQueue().enqueue(a, now);

        int occupancySeconds = 60;
        Scheduler scheduler = new Scheduler();
        scheduler.tryScheduleOperations(now, airportSimulation.engine());

        assertEquals(0, airport.takeOffQueue().size());
        assertEquals(now.plusSeconds(occupancySeconds), airport.getRunway(0).occupiedUntil());

        Event<SimulationContext> e = airportSimulation.engine().eventQueue().pop();
        assertNotNull(e);
        assertEquals(now.plusSeconds(occupancySeconds), e.time());
        assertTrue(e instanceof RunwayOperationCompleteEvent);
    }

    @Test
    void doesNotScheduleIfRunwayUnavailableOrQueueEmpty() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant now = start.plusSeconds(300);
        Instant end = start.plus(Duration.ofHours(1));

        RunwayData landingRunway = new RunwayData(3000, 90, RunwayMode.LANDING, RunwayStatus.INSPECTION);
        Airport airport = new Airport(List.of(landingRunway));

        AirportSimulation airportSimulation = new AirportSimulation(
            SimConfig.builder()
                .startTime(start)
                .endTime(end)
                .randomSeed(123L)
                .build(),
            airport
        );

        Scheduler scheduler = new Scheduler();

        // No aircraft in holding and runway is not available anyway
        scheduler.tryScheduleOperations(now, airportSimulation.engine());

        assertTrue(airportSimulation.engine().eventQueue().isEmpty());
        assertNull(airport.getRunway(0).occupiedUntil());
        assertEquals(0, airport.holdingPattern().size());
    }

    @Test
    void landingSelectionRespectsEmergencyPriorityFromHoldingPattern() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant now = start.plusSeconds(300);
        Instant end = start.plus(Duration.ofHours(1));

        RunwayData landingRunway = new RunwayData(3000, 90, RunwayMode.LANDING, RunwayStatus.AVAILABLE);

        Airport airport = new Airport(List.of(landingRunway));

        AirportSimulation airportSimulation = new AirportSimulation(
            SimConfig.builder()
                .startTime(start)
                .endTime(end)
                .randomSeed(123L)
                .build(),
            airport
        );

        Aircraft normal = mkAircraft("N1", EmergencyStatus.NONE, start.plusSeconds(100), AircraftDirection.INBOUND);
        Aircraft emerg  = mkAircraft("E1", EmergencyStatus.FUEL, start.plusSeconds(110), AircraftDirection.INBOUND);

        airport.holdingPattern().enqueue(normal, now);
        airport.holdingPattern().enqueue(emerg, now.plusSeconds(1));

        Scheduler scheduler = new Scheduler();
        scheduler.tryScheduleOperations(now, airportSimulation.engine());

        // After scheduling one landing, holding should have 1 aircraft left
        assertEquals(1, airport.holdingPattern().size());

        // The emergency should have been removed first; normal should remain.
        // If HoldingPattern exposes snapshot/view, use that. Otherwise, dequeue and check:
        Aircraft remaining = airport.holdingPattern().dequeueNextLandingCandidate();
        assertSame(normal, remaining);
    }
}