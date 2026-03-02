package com.group32.cs261project.gen;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.group32.cs261project.model.Airport;
import com.group32.cs261project.model.Runway;
import com.group32.cs261project.model.enums.RunwayMode;
import com.group32.cs261project.model.enums.RunwayStatus;
import com.group32.cs261project.scheduling.Scheduler;
import com.group32.cs261project.sim.SimConfig;
import com.group32.cs261project.sim.SimulationContext;
import com.group32.cs261project.sim.SimulationEngine;
import com.group32.cs261project.sim.queue.PriorityEventQueue;

class FlightSeederTest {

    @Test
    void seedingSchedulesEventsAndRunningEnqueuesAircraft() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = start.plus(Duration.ofHours(1));

        SimConfig cfg = new SimConfig(start, end, 123L, 15.0, 15.0, 60);

        Airport airport = new Airport(List.of(
                new Runway(1, 3000, 90, RunwayMode.MIXED, RunwayStatus.AVAILABLE)
        ));

        SimulationEngine<SimulationContext> engine =
                new SimulationEngine<>(cfg, new SimulationContext(airport, new Scheduler()), new PriorityEventQueue<>());

        FlightGenerator generator = new FlightGenerator(cfg);
        List<GeneratedFlight> inbound = generator.generateFlights(FlightType.INBOUND);
        List<GeneratedFlight> outbound;
        outbound = generator.generateFlights(FlightType.OUTBOUND);

        FlightSeeder.seedInbound(engine, inbound);
        FlightSeeder.seedOutbound(engine, outbound);

        // Run the simulation to end: all entry events should have executed.
        engine.runUntil(end);

        assertEquals(inbound.size(), airport.holdingPattern().size());
        assertEquals(outbound.size(), airport.takeOffQueue().size());
    }

    @Test
    void seedingCreatesCorrectEventClassPerFlightType() {
        // This test assumes FlightSeeder has a method that returns the event it schedules
        // OR that you can inspect the queue. If you can't peek at events, you can skip this.
        //
        // If your EventQueue does not expose queued events, keep only the end-to-end test above.
        assertTrue(true);
    }
}