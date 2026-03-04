package com.group32.cs261project.metrics;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.metrics.MetricsCollector;
import com.group32.cs261project.backend.metrics.SimulationReport;
import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.enums.AircraftState;
import com.group32.cs261project.backend.model.enums.EmergencyStatus;

/**
 * Tests MetricsCollector end-to-end: queue sizes, completion metrics, diverted/cancelled counts, report building.
 *
 * NOTE: These tests assume:
 *  - Aircraft has methods: scheduledTime(), holdingPatternEntryTime(), takeOffQueueEntryTime()
 *  - Those entry times are non-null for the relevant completion method
 */
class MetricsCollectorTest {

    private static Aircraft mkAircraft(Instant scheduled, Instant holdingEntry, Instant takeoffEntry) {
        // Adapt this if your Aircraft constructor differs.
        // Only the three time getters used by MetricsCollector must be correct.
        Aircraft aircraft = new Aircraft(
                "CS1",
                "OP",
                "AAA",
                "BBB",
                scheduled,
                EmergencyStatus.NONE,
                100.0,
                AircraftState.OUTSIDE_MODEL
        );
        aircraft.markHolding(holdingEntry);
        aircraft.markTakeoffQueue(takeoffEntry);
        return aircraft;
    }

    @Test
    void buildReportAggregatesQueueSizesAndCounts() {
        MetricsCollector mc = new MetricsCollector();
        Instant t = Instant.parse("2026-01-01T00:00:00Z");

        // Holding sizes sampled over time
        mc.onHoldingEnqueue(t, 1);
        mc.onHoldingEnqueue(t, 3);
        mc.onHoldingDequeue(t, 2);

        // Takeoff sizes sampled over time
        mc.onTakeoffEnqueue(t, 1);
        mc.onTakeoffEnqueue(t, 5);
        mc.onTakeoffDequeue(t, 4);

        // Cancel/divert counters
        mc.onCancelled(null, t);
        mc.onCancelled(null, t);
        mc.onDiverted(null, t);

        // To allow buildReport without "No entries" for time/delay collectors,
        // we need at least one landing and one takeoff completion.
        Instant scheduled = t;
        Instant holdingEntry = t.plusSeconds(60);
        Instant landingActual = holdingEntry.plusSeconds(10 * 60); // +10 min hold

        Instant takeoffEntry = t.plusSeconds(120);
        Instant takeoffActual = takeoffEntry.plusSeconds(7 * 60); // +7 min wait

        Aircraft a = mkAircraft(scheduled, holdingEntry, takeoffEntry);

        mc.onLandingCompleted(a, landingActual);
        mc.onTakeoffCompleted(a, takeoffActual);

        SimulationReport r = mc.buildReport();

        assertEquals(3, r.maxHoldingSize());
        assertEquals(5, r.maxTakeoffQueueSize());

        assertEquals(1, r.numDiverted());
        assertEquals(2, r.numCancellations());
    }

    @Test
    void landingCompletionComputesPositiveHoldingTimeAndArrivalDelay() {
        MetricsCollector mc = new MetricsCollector();
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        Instant scheduled = t0;
        Instant holdingEntry = t0.plusSeconds(5 * 60);     // enters holding at +5 min
        Instant landingActual = t0.plusSeconds(17 * 60);   // lands at +17 min

        // Holding time should be 12 minutes; arrival delay should be 17 minutes (if scheduled at t0)
        Aircraft a = mkAircraft(scheduled, holdingEntry, null);

        mc.onHoldingEnqueue(holdingEntry, 1);
        mc.onHoldingDequeue(landingActual, 0);
        mc.onLandingCompleted(a, landingActual);

        SimulationReport r = mc.buildReport();

        assertEquals(1, r.maxHoldingSize());

        // These should be positive if Duration.between(entry, actual) is used.
        assertEquals(12.0, r.avgHoldingTimeMinutes(), 1e-9);
        assertEquals(17.0, r.avgArrivalDelayMinutes(), 1e-9);

        // With one sample, max == avg
        // assertEquals(12.0, r.maxHoldingTimeMinutes(), 1e-9);
        assertEquals(17.0, r.maxArrivalDelayMinutes(), 1e-9);
    }

    @Test
    void takeoffCompletionComputesPositiveWaitTimeAndDepartureDelay() {
        MetricsCollector mc = new MetricsCollector();
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        Instant scheduled = t0.plusSeconds(10 * 60);       // scheduled at +10 min
        Instant takeoffEntry = t0.plusSeconds(12 * 60);    // enters queue at +12 min
        Instant takeoffActual = t0.plusSeconds(25 * 60);   // takes off at +25 min

        // Wait time should be 13 minutes; departure delay should be 15 minutes (25 - 10)
        Aircraft a = mkAircraft(scheduled, null, takeoffEntry);

        mc.onTakeoffEnqueue(takeoffEntry, 1);
        mc.onTakeoffDequeue(takeoffActual, 0);
        mc.onTakeoffCompleted(a, takeoffActual);

        SimulationReport r = mc.buildReport();

        assertEquals(1, r.maxTakeoffQueueSize());

        assertEquals(13.0, r.avgTakeoffWaitMinutes(), 1e-9);
        assertEquals(15.0, r.avgDepartureDelayMinutes(), 1e-9);

        // assertEquals(13.0, r.maxTakeoffWaitMinutes(), 1e-9);
        assertEquals(15.0, r.maxDepartureDelayMinutes(), 1e-9);
    }

    @Test
    void maxAndAverageReflectMultipleSamples() {
        MetricsCollector mc = new MetricsCollector();
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        // Two landings with different holding times and delays
        // Landing #1: entry +0m, land +10m => hold 10, delay (scheduled t0) 10
        Aircraft a1 = mkAircraft(t0, t0, null);
        mc.onHoldingEnqueue(t0, 1);
        mc.onLandingCompleted(a1, t0.plusSeconds(10 * 60));

        // Landing #2: entry +5m, land +25m => hold 20, delay 25
        Aircraft a2 = mkAircraft(t0, t0.plusSeconds(5 * 60), null);
        mc.onHoldingEnqueue(t0.plusSeconds(5 * 60), 2);
        mc.onLandingCompleted(a2, t0.plusSeconds(25 * 60));

        // One takeoff so buildReport doesn't throw for takeoff collectors:
        Aircraft d1 = mkAircraft(t0, null, t0);
        mc.onTakeoffEnqueue(t0, 1);
        mc.onTakeoffCompleted(d1, t0.plusSeconds(5 * 60));

        SimulationReport r = mc.buildReport();

        // Holding time avg = (10 + 20)/2 = 15; max = 20
        assertEquals(15.0, r.avgHoldingTimeMinutes(), 1e-9);
        // assertEquals(20.0, r.maxHoldingTimeMinutes(), 1e-9);

        // Arrival delay avg = (10 + 25)/2 = 17.5; max = 25
        assertEquals(17.5, r.avgArrivalDelayMinutes(), 1e-9);
        assertEquals(25.0, r.maxArrivalDelayMinutes(), 1e-9);
    }
}