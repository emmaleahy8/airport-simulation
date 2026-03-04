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

import com.group32.cs261project.backend.events.Event;
import com.group32.cs261project.backend.events.RunwayOperationCompleteEvent;
import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.model.RunwayData;
import com.group32.cs261project.backend.model.enums.AircraftState;
import com.group32.cs261project.backend.model.enums.EmergencyStatus;
import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;
import com.group32.cs261project.backend.scheduling.Scheduler;
import com.group32.cs261project.backend.sim.PriorityEventQueue;
import com.group32.cs261project.backend.sim.SimConfig;
import com.group32.cs261project.backend.sim.SimulationContext;
import com.group32.cs261project.backend.sim.SimulationEngine;

class SchedulerTest {

    private static Aircraft mkAircraft(String callsign, EmergencyStatus emergency, Instant scheduledTime) {
        return new Aircraft(
                callsign,
                "TestAir",
                "AAA",
                "BBB",
                scheduledTime,
                emergency,
                40.0,
                AircraftState.OUTSIDE_MODEL
        );
    }

    // private static SimulationEngine<SimulationContext> mkEngine(Instant start, Instant end, long seed) {
    //     // Adjust constructor to your SimConfig signature.
    //     // If your SimConfig includes flows, pass them; otherwise keep the 3-arg version.
    //     SimConfig cfg = new SimConfig(start, end, seed, 15, 15, 60);
    //     Airport dummyAirport = new Airport(List.of(
    //             new Runway(1, 3000, 90, RunwayMode.MIXED, RunwayStatus.AVAILABLE)
    //     ));
    //     return new SimulationEngine<>(cfg, new SimulationContext(dummyAirport, new Scheduler()), new PriorityEventQueue<>());
    // }

    @Test
    void schedulesLandingOnAvailableLandingRunway() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant now = start.plusSeconds(300);
        Instant end = start.plus(Duration.ofHours(1));

        // Landing runway available
        RunwayData landingRunway = new RunwayData(3000, 90, RunwayMode.LANDING, RunwayStatus.AVAILABLE);

        Airport airport = new Airport(List.of(landingRunway));
        

        SimulationEngine<SimulationContext> engine = new SimulationEngine<>(
            new SimConfig( start, end, 123L, 15, 15, 60), 
            new SimulationContext(airport, new Scheduler()), 
            new PriorityEventQueue<>()
        );

        // Put one aircraft in holding
        Aircraft a = mkAircraft("A1", EmergencyStatus.NONE, start.plusSeconds(100));
        airport.holdingPattern().enqueue(a, now);

        int occupancySeconds = 60;
        Scheduler scheduler = new Scheduler(); // adjust if your Scheduler takes config instead
        scheduler.tryScheduleOperations(now, engine);

        // Holding should be drained by 1
        assertEquals(0, airport.holdingPattern().size());

        // Runway should be occupied until now + occupancy
        assertEquals(now.plusSeconds(occupancySeconds), airport.getRunway(0).occupiedUntil());


        // Engine should now contain a completion event at the right time
        Event<SimulationContext> e = engine.eventQueue().pop();
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
        SimulationEngine<SimulationContext> engine = new SimulationEngine<>(
            new SimConfig( start, end, 123L, 15, 15, 60), 
            new SimulationContext(airport, new Scheduler()), 
            new PriorityEventQueue<>()
        );

        Aircraft a = mkAircraft("D1", EmergencyStatus.NONE, start.plusSeconds(200));
        airport.takeOffQueue().enqueue(a, now);

        int occupancySeconds = 60;
        Scheduler scheduler = new Scheduler();
        scheduler.tryScheduleOperations(now, engine);

        assertEquals(0, airport.takeOffQueue().size());
        assertEquals(now.plusSeconds(occupancySeconds), airport.getRunway(0).occupiedUntil());

        Event<SimulationContext> e = engine.eventQueue().pop();
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

        int occupancySeconds = 60;
        SimulationEngine<SimulationContext> engine = new SimulationEngine<>(
            new SimConfig( start, end, 123L, 15, 15, occupancySeconds), 
            new SimulationContext(airport, new Scheduler()), 
            new PriorityEventQueue<>()
        );

        Scheduler scheduler = new Scheduler();

        // No aircraft in holding and runway is not available anyway
        scheduler.tryScheduleOperations(now, engine);

        assertTrue(engine.eventQueue().isEmpty());
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

        SimulationEngine<SimulationContext> engine = new SimulationEngine<>(
            new SimConfig( start, end, 123L, 15, 15, 60), 
            new SimulationContext(airport, new Scheduler()), 
            new PriorityEventQueue<>()
        );

        Aircraft normal = mkAircraft("N1", EmergencyStatus.NONE, start.plusSeconds(100));
        Aircraft emerg  = mkAircraft("E1", EmergencyStatus.FUEL, start.plusSeconds(110));

        airport.holdingPattern().enqueue(normal, now);
        airport.holdingPattern().enqueue(emerg, now.plusSeconds(1));

        Scheduler scheduler = new Scheduler();
        scheduler.tryScheduleOperations(now, engine);

        // After scheduling one landing, holding should have 1 aircraft left
        assertEquals(1, airport.holdingPattern().size());

        // The emergency should have been removed first; normal should remain.
        // If HoldingPattern exposes snapshot/view, use that. Otherwise, dequeue and check:
        Aircraft remaining = airport.holdingPattern().dequeueNextLandingCandidate();
        assertSame(normal, remaining);
    }
}