// src/test/java/com/group32/cs261project/sim/SimulationEngineTest.java
package com.group32.cs261project.sim;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.events.Event;
import com.group32.cs261project.backend.sim.PriorityEventQueue;
import com.group32.cs261project.backend.sim.SimConfig;
import com.group32.cs261project.backend.sim.SimulationEngine;

class SimulationEngineTest {

    private static final class RecordingEvent<C> implements Event<C> {
        private final Instant time;
        private final String label;
        private final List<String> log;
        private final Runnable sideEffect;

        RecordingEvent(Instant time, String label, List<String> log) {
            this(time, label, log, () -> {});
        }

        RecordingEvent(Instant time, String label, List<String> log, Runnable sideEffect) {
            this.time = time;
            this.label = label;
            this.log = log;
            this.sideEffect = sideEffect;
        }

        @Override public Instant time() { return time; }

        @Override
        public void handle(SimulationEngine<C> engine) {
            log.add(label);
            sideEffect.run();
        }
    }

    @Test
    void eventsExecuteInChronologicalOrder() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        SimConfig cfg = new SimConfig(start, start.plusSeconds(3600), 123, 15, 15, 60);
        List<String> log = new ArrayList<>();

        SimulationEngine<Object> engine = new SimulationEngine<>(cfg, new Object(), new PriorityEventQueue<>());

        engine.schedule(new RecordingEvent<>(start.plusSeconds(600), "t10", log)); // +10 min
        engine.schedule(new RecordingEvent<>(start.plusSeconds(300), "t5", log));  // +5 min
        engine.schedule(new RecordingEvent<>(start.plusSeconds(1200), "t20", log)); // +20 min

        engine.runUntil(start.plusSeconds(2000));

        assertEquals(List.of("t5", "t10", "t20"), log);
    }

    @Test
    void deterministicTieBreakUsesInsertionOrder() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        SimConfig cfg = new SimConfig(start, start.plusSeconds(3600), 123, 15, 15, 60);
        List<String> log = new ArrayList<>();

        SimulationEngine<Object> engine = new SimulationEngine<>(cfg, new Object(), new PriorityEventQueue<>());

        Instant t = start.plusSeconds(300);
        engine.schedule(new RecordingEvent<>(t, "A", log));
        engine.schedule(new RecordingEvent<>(t, "B", log));
        engine.runUntil(t);

        assertEquals(List.of("A", "B"), log);
    }

    @Test
    void runUntilDoesNotProcessEventsAfterEndTime() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        SimConfig cfg = new SimConfig(start, start.plusSeconds(3600), 123, 15, 15, 60);
        List<String> log = new ArrayList<>();

        SimulationEngine<Object> engine = new SimulationEngine<>(cfg, new Object(), new PriorityEventQueue<>());

        engine.schedule(new RecordingEvent<>(start.plusSeconds(300), "t5", log));
        engine.schedule(new RecordingEvent<>(start.plusSeconds(600), "t10", log));
        engine.schedule(new RecordingEvent<>(start.plusSeconds(900), "t15", log));

        engine.runUntil(start.plusSeconds(600));

        assertEquals(List.of("t5", "t10"), log);
        assertFalse(engine.eventQueue().isEmpty());
        assertEquals(start.plusSeconds(900), engine.eventQueue().peekTime());
    }

    @Test
    void eventsCanScheduleMoreEvents() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        SimConfig cfg = new SimConfig(start, start.plusSeconds(3600), 123, 15, 15, 60);
        List<String> log = new ArrayList<>();

        SimulationEngine<Object> engine = new SimulationEngine<>(cfg, new Object(), new PriorityEventQueue<>());

        Instant t5 = start.plusSeconds(300);
        Instant t7 = start.plusSeconds(420);

        engine.schedule(new RecordingEvent<>(t5, "A", log, () ->
                engine.schedule(new RecordingEvent<>(t7, "B", log))
        ));

        engine.runUntil(start.plusSeconds(1000));
        assertEquals(List.of("A", "B"), log);
    }

    @Test
    void schedulingInThePastThrows() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        SimConfig cfg = new SimConfig(start, start.plusSeconds(3600), 123, 15, 15, 60);
        List<String> log = new ArrayList<>();

        SimulationEngine<Object> engine = new SimulationEngine<>(cfg, new Object(), new PriorityEventQueue<>());

        Instant t10 = start.plusSeconds(600);
        engine.schedule(new RecordingEvent<>(t10, "advance", log));
        engine.runUntil(t10);
        assertEquals(t10, engine.now());

        Instant t9 = start.plusSeconds(540);
        assertThrows(IllegalArgumentException.class, () ->
                engine.schedule(new RecordingEvent<>(t9, "past", log))
        );
    }
}
