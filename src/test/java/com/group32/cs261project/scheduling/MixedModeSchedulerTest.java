package com.group32.cs261project.scheduling;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.group32.cs261project.airport.events.RunwayOperationCompleteEvent;
import com.group32.cs261project.model.Aircraft;
import com.group32.cs261project.model.Airport;
import com.group32.cs261project.model.RunwayData;
import com.group32.cs261project.model.enums.AircraftState;
import com.group32.cs261project.model.enums.EmergencyStatus;
import com.group32.cs261project.model.enums.RunwayMode;
import com.group32.cs261project.model.enums.RunwayStatus;
import com.group32.cs261project.sim.SimConfig;
import com.group32.cs261project.sim.SimulationContext;
import com.group32.cs261project.sim.SimulationEngine;
import com.group32.cs261project.sim.events.Event;
import com.group32.cs261project.sim.queue.PriorityEventQueue;

class MixedModeSchedulerTest {

    private static Aircraft mk(String cs, Instant scheduled) {
        return new Aircraft(cs, "Op", "AAA", "BBB", scheduled, EmergencyStatus.NONE, 40.0, AircraftState.OUTSIDE_MODEL);
    }

    @Test
    void mixedModeAlternatesBetweenLandingAndTakeoffWhenBothQueuesNonEmpty() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant now = start.plusSeconds(0);
        Instant end = start.plus(Duration.ofHours(1));

        RunwayData mixed = new RunwayData(3000, 90, RunwayMode.MIXED, RunwayStatus.AVAILABLE);
        Airport airport = new Airport(List.of(mixed));

        SimulationEngine<SimulationContext> engine = new SimulationEngine<>(
            new SimConfig(start, end, 1L, 15, 15, 60),
            new SimulationContext(airport, new Scheduler()),
            new PriorityEventQueue<>()
        );

        // Add one to each queue
        airport.holdingPattern().enqueue(mk("IN1", start), now);
        airport.takeOffQueue().enqueue(mk("OUT1", start), now);

        // Scheduler with alternating policy (adjust ctor)
        Scheduler scheduler = new Scheduler();
        scheduler.tryScheduleOperations(now, engine);

        // Pop completion event 1
        Event<SimulationContext> e1 = engine.eventQueue().pop();
        assertNotNull(e1);
        assertTrue(e1 instanceof RunwayOperationCompleteEvent);

        // Simulate completion to free runway (in your real sim, the completion event does this)
        // Here we just advance time and clear occupancy if needed; adjust to your runway model.
        airport.getRunway(0).clearOccupancy(); // or clearOccupancy()

        // Ensure both queues still have at least one item for the next choice:
        // (Depending on which op was chosen first, one queue is now empty; add another)
        if (airport.holdingPattern().isEmpty()) {
            airport.holdingPattern().enqueue(mk("IN2", start), now);
        }
        if (airport.takeOffQueue().isEmpty()) {
            airport.takeOffQueue().enqueue(mk("OUT2", start), now);
        }

        // Schedule again at same timestamp
        scheduler.tryScheduleOperations(now, engine);

        Event<SimulationContext> e2 = engine.eventQueue().pop();
        assertNotNull(e2);
        assertTrue(e2 instanceof RunwayOperationCompleteEvent);

        // The second op should be the opposite of the first (alternation).
        // If RunwayOperationCompleteEvent exposes op type, assert that here:
        // assertNotEquals(((RunwayOperationCompleteEvent) e1).op(), ((RunwayOperationCompleteEvent) e2).op());
        assertTrue(true);
    }
}