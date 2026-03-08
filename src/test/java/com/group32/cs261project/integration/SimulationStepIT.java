package com.group32.cs261project.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.events.Event;
import com.group32.cs261project.backend.sim.EventQueue;
import com.group32.cs261project.backend.sim.SimConfig;
import com.group32.cs261project.backend.sim.SimulationEngine;

public class SimulationStepIT {

    /**
     * Simple test context that records handled events.
     */
    static class TestContext {
        List<String> handledEvents = new ArrayList<>();
    }

    /**
     * Simple test event that records when it was handled.
     */
    static class TestEvent implements Event<TestContext> {

        private final Instant time;
        private final String name;

        TestEvent(Instant time, String name) {
            this.time = time;
            this.name = name;
        }

        @Override
        public Instant time() {
            return time;
        }

        @Override
        public void handle(SimulationEngine<TestContext> engine) {
            engine.context().handledEvents.add(name);
        }
    }

    @Test
    void stepProcessesEventsOneByOneInCorrectOrder() {

        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant t1 = start.plusSeconds(60);
        Instant t2 = start.plusSeconds(120);

        SimConfig config = SimConfig.builder()
            .startTime(start)
            .endTime(start.plusSeconds(3600))
            .randomSeed(123L)
            .inboundRatePerHour(0.0)
            .outboundRatePerHour(0.0)
            .build();

        TestContext context = new TestContext();

        SimulationEngine<TestContext> engine =
                new SimulationEngine<>(
                        config,
                        context,
                        new EventQueue<>()
                );

        // Schedule two events
        engine.schedule(new TestEvent(t1, "E1"));
        engine.schedule(new TestEvent(t2, "E2"));

        // Initially, now should equal start time
        assertEquals(start, engine.now());

        // -------------------------
        // Step 1
        // -------------------------
        boolean stepped1 = engine.step();
        assertTrue(stepped1);

        // Engine time should advance to first event
        assertEquals(t1, engine.now());

        // Only first event should be handled
        assertEquals(1, context.handledEvents.size());
        assertEquals("E1", context.handledEvents.get(0));

        // -------------------------
        // Step 2
        // -------------------------
        boolean stepped2 = engine.step();
        assertTrue(stepped2);

        assertEquals(t2, engine.now());
        assertEquals(2, context.handledEvents.size());
        assertEquals("E2", context.handledEvents.get(1));

        // -------------------------
        // Step 3 (no more events)
        // -------------------------
        boolean stepped3 = engine.step();
        assertFalse(stepped3);

        // Time should remain unchanged
        assertEquals(t2, engine.now());
    }

    @Test
    void scheduleInPastThrowsException() {

        Instant start = Instant.parse("2025-01-01T00:00:00Z");


        SimConfig config = SimConfig.builder()
            .startTime(start)
            .endTime(start.plusSeconds(3600))
            .randomSeed(123L)
            .inboundRatePerHour(0.0)
            .outboundRatePerHour(0.0)
            .build();

        TestContext context = new TestContext();

        SimulationEngine<TestContext> engine =
                new SimulationEngine<>(
                        config,
                        context,
                        new EventQueue<>()
                );

        Instant pastTime = start.minusSeconds(10);

        TestEvent pastEvent = new TestEvent(pastTime, "PAST");

        assertThrows(IllegalArgumentException.class, () -> {
            engine.schedule(pastEvent);
        });
    }
}