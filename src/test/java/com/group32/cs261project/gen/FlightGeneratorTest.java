package com.group32.cs261project.gen;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.gen.FlightGenerator;
import com.group32.cs261project.backend.gen.FlightType;
import com.group32.cs261project.backend.gen.GeneratedFlight;
import com.group32.cs261project.backend.sim.SimConfig;

class FlightGeneratorTest {

    @Test
    void scheduledTimesHaveUniformSpacingFromFlowRate() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = start.plus(Duration.ofHours(1));

        SimConfig cfg = SimConfig.builder()
            .startTime(start)
            .endTime(end)
            .randomSeed(123L)
            .build();

        FlightGenerator gen = new FlightGenerator(cfg, new Random(cfg.randomSeed()));
        List<GeneratedFlight> inbound = gen.generateFlights(FlightType.INBOUND);

        // For 15 per hour → spacing = 4 minutes
        Duration spacing = Duration.ofMinutes(4);

        // We test the first few scheduled times.
        List<Instant> scheduled = inbound.stream()
                .limit(5)
                .map(GeneratedFlight::scheduledTime)
                .collect(Collectors.toList());

        assertTrue(scheduled.size() >= 5, "Need at least 5 flights to test spacing");

        assertEquals(start, scheduled.get(0));
        assertEquals(start.plus(spacing), scheduled.get(1));
        assertEquals(start.plus(spacing.multipliedBy(2)), scheduled.get(2));
        assertEquals(start.plus(spacing.multipliedBy(3)), scheduled.get(3));
        assertEquals(start.plus(spacing.multipliedBy(4)), scheduled.get(4));
    }

    @Test
    void generationIsReproducibleWithSameSeed() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = start.plus(Duration.ofHours(1));

        SimConfig cfg1 = SimConfig.builder()
            .startTime(start)
            .endTime(end)
            .randomSeed(42L)
            .build();

        SimConfig cfg2 = SimConfig.builder()
            .startTime(start)
            .endTime(end)
            .randomSeed(42L)
            .build();

        FlightGenerator g1 = new FlightGenerator(cfg1, new Random(cfg1.randomSeed()));
        FlightGenerator g2 = new FlightGenerator(cfg2, new Random(cfg2.randomSeed()));

        List<GeneratedFlight> a = g1.generateFlights(FlightType.INBOUND);
        List<GeneratedFlight> b = g2.generateFlights(FlightType.INBOUND);

        assertEquals(a.size(), b.size());

        // Compare first N entry times (t'k). Exact match should hold with same seed.
        int n = Math.min(20, a.size());
        for (int i = 0; i < n; i++) {
            assertEquals(a.get(i).entryTime(), b.get(i).entryTime(), "Mismatch at i=" + i);
        }
    }

    @Test
    void entryTimesAreCloseToScheduledTimesButNotAlwaysEqual() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = start.plus(Duration.ofHours(1));

        SimConfig cfg = SimConfig.builder()
            .startTime(start)
            .endTime(end)
            .randomSeed(999L)
            .build();

        FlightGenerator gen = new FlightGenerator(cfg, new Random(cfg.randomSeed()));
        List<GeneratedFlight> flights = gen.generateFlights(FlightType.INBOUND);

        assertFalse(flights.isEmpty());

        // Noise is N(0, 5^2) minutes. We just sanity-check:
        // - entry times exist
        // - not all equal to scheduled times (should be very unlikely)
        boolean anyDifferent = false;

        int n = Math.min(30, flights.size());
        for (int i = 0; i < n; i++) {
            Instant scheduled = flights.get(i).scheduledTime();
            Instant entry = flights.get(i).entryTime();
            assertNotNull(entry);

            if (!entry.equals(scheduled)) anyDifferent = true;

            // optional: basic bound sanity (not strict statistical test)
            // assertTrue(Duration.between(scheduled, entry).abs().toMinutes() < 60);
        }

        assertTrue(anyDifferent, "Expected at least one entry time to differ from scheduled time");
    }
}