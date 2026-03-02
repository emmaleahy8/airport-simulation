package com.group32.cs261project.airport;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.group32.cs261project.model.Airport;
import com.group32.cs261project.model.Runway;
import com.group32.cs261project.model.enums.RunwayMode;
import com.group32.cs261project.model.enums.RunwayStatus;
import com.group32.cs261project.sim.SimConfig;
import com.group32.cs261project.sim.SimulationEngine;
import com.group32.cs261project.sim.events.Event;

/**
 * Assumes AirportSimulation is a thin wrapper around SimulationEngine<Airport>.
 *
 * This test checks that:
 *  - we can schedule events via the underlying engine
 *  - runUntil(...) processes them in time order
 *
 * If your AirportSimulation constructor / getters differ, tweak the marked lines.
 */
class AirportSimulationTest {

    private static final class RecordingEvent implements Event<Airport> {
        private final Instant time;
        private final String label;
        private final List<String> log;

        private RecordingEvent(Instant time, String label, List<String> log) {
            this.time = time;
            this.label = label;
            this.log = log;
        }

        @Override
        public Instant time() {
            return time;
        }

        @Override
        public void handle(SimulationEngine<Airport> engine) {
            log.add(label);
        }
    }

    @Test
    void runUntilProcessesScheduledEvents() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = start.plusSeconds(3600);

        SimConfig cfg = new SimConfig(start, end, 123, 15, 15, 60);

        // Airport must be constructible. Adjust to match your Airport constructor.
        // This uses a single runway as a simple default.
        Airport airport = new Airport(List.of(
                new Runway(1, 3000, 90, RunwayMode.LANDING, RunwayStatus.AVAILABLE)
        ));

        // Adjust this line to match your AirportSimulation constructor.
        // Common options:
        //   new AirportSimulation(cfg, airport)
        //   new AirportSimulation(cfg, airport, engine)
        AirportSimulation sim = new AirportSimulation(cfg, airport);

        List<String> executed = new ArrayList<>();
        SimulationEngine<Airport> engine = sim.engine(); // adjust if getter name differs

        engine.schedule(new RecordingEvent(start.plusSeconds(600), "t10", executed)); // +10 min
        engine.schedule(new RecordingEvent(start.plusSeconds(300), "t5", executed));  // +5 min

        sim.runUntil(start.plusSeconds(700)); // should process both

        assertEquals(List.of("t5", "t10"), executed);
    }

    @Test
    void runUntilDoesNotProcessEventsAfterEndTime() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = start.plusSeconds(3600);

        SimConfig cfg = new SimConfig(start, end, 123, 15, 15, 60);
        Airport airport = new Airport(List.of(
                new Runway(1, 3000, 90, RunwayMode.LANDING, RunwayStatus.AVAILABLE)
        ));

        AirportSimulation sim = new AirportSimulation(cfg, airport);

        List<String> executed = new ArrayList<>();
        SimulationEngine<Airport> engine = sim.engine();

        engine.schedule(new RecordingEvent(start.plusSeconds(300), "t5", executed));
        engine.schedule(new RecordingEvent(start.plusSeconds(900), "t15", executed));

        sim.runUntil(start.plusSeconds(600)); // up to +10 min

        assertEquals(List.of("t5"), executed);
    }
}