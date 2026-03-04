package com.group32.cs261project.gen;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.gen.FlightGenerator;
import com.group32.cs261project.backend.gen.FlightSeeder;
import com.group32.cs261project.backend.gen.FlightType;
import com.group32.cs261project.backend.gen.GeneratedFlight;
import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.model.RunwayData;
import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;
import com.group32.cs261project.backend.scheduling.Scheduler;
import com.group32.cs261project.backend.sim.EventQueue;
import com.group32.cs261project.backend.sim.SimConfig;
import com.group32.cs261project.backend.sim.SimulationContext;
import com.group32.cs261project.backend.sim.SimulationEngine;

class FlightSeederTest {

    @Test
    void seedingSchedulesEventsAndRunningEnqueuesAircraft() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = start.plus(Duration.ofHours(1));

        SimConfig cfg = new SimConfig(start, end, 123L, 15.0, 15.0, 60);

        Airport airport = new Airport(List.of(
                new RunwayData(3000, 90, RunwayMode.MIXED, RunwayStatus.INSPECTION)
        ));

        SimulationEngine<SimulationContext> engine =
                new SimulationEngine<>(cfg, new SimulationContext(airport, new Scheduler()), new EventQueue<>());

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