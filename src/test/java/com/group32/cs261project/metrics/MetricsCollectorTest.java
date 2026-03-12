package com.group32.cs261project.metrics;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.metrics.MetricsCollector;
import com.group32.cs261project.backend.metrics.SimulationReport;
import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.enums.AircraftDirection;
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

    private static final int TEST_NUM_RUNWAYS = 3;

    private static Aircraft mkAircraft(Instant scheduled, Instant holdingEntry, Instant takeoffEntry, AircraftDirection direction) {
        Aircraft aircraft = new Aircraft(
                "CS1",
                "OP",
                "AAA",
                "BBB",
                scheduled,
                EmergencyStatus.NONE,
                100.0,
                AircraftState.OUTSIDE_MODEL,
                direction
        );

        if (holdingEntry != null) {
            aircraft.markHolding(holdingEntry);
        }
        if (takeoffEntry != null) {
            aircraft.markTakeoffQueue(takeoffEntry);
        }

        return aircraft;
    }

    @Test
    void buildReportAggregatesQueueSizesAndCounts() {
        MetricsCollector mc = new MetricsCollector();
        Instant t = Instant.parse("2026-01-01T00:00:00Z");

        mc.onHoldingEnqueue(t, 1);
        mc.onHoldingEnqueue(t, 3);
        mc.onHoldingDequeue(t, 2);

        mc.onTakeoffEnqueue(t, 1);
        mc.onTakeoffEnqueue(t, 5);
        mc.onTakeoffDequeue(t, 4);

        mc.onCancelled(null, t);
        mc.onCancelled(null, t);
        mc.onDiverted(null, t);

        Instant scheduled = t;
        Instant holdingEntry = t.plusSeconds(60);
        Instant landingActual = holdingEntry.plusSeconds(10 * 60);

        Instant takeoffEntry = t.plusSeconds(120);
        Instant takeoffActual = takeoffEntry.plusSeconds(7 * 60);

        Aircraft a = mkAircraft(scheduled, holdingEntry, takeoffEntry, AircraftDirection.INBOUND);

        mc.onLandingCompleted(a, landingActual);
        mc.onTakeoffCompleted(a, takeoffActual);

        SimulationReport r = mc.buildReport(TEST_NUM_RUNWAYS);

        assertEquals(TEST_NUM_RUNWAYS, r.numRunways());
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
        Instant holdingEntry = t0.plusSeconds(5 * 60);
        Instant landingActual = t0.plusSeconds(17 * 60);

        Aircraft a = mkAircraft(scheduled, holdingEntry, null, AircraftDirection.INBOUND);

        mc.onHoldingEnqueue(holdingEntry, 1);
        mc.onHoldingDequeue(landingActual, 0);
        mc.onLandingCompleted(a, landingActual);

        SimulationReport r = mc.buildReport(TEST_NUM_RUNWAYS);

        assertEquals(TEST_NUM_RUNWAYS, r.numRunways());
        assertEquals(1, r.maxHoldingSize());
        assertEquals(12.0, r.avgHoldingTimeMinutes(), 1e-9);
        assertEquals(17.0, r.avgArrivalDelayMinutes(), 1e-9);
        assertEquals(17.0, r.maxArrivalDelayMinutes(), 1e-9);
    }

    @Test
    void takeoffCompletionComputesPositiveWaitTimeAndDepartureDelay() {
        MetricsCollector mc = new MetricsCollector();
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        Instant scheduled = t0.plusSeconds(10 * 60);
        Instant takeoffEntry = t0.plusSeconds(12 * 60);
        Instant takeoffActual = t0.plusSeconds(25 * 60);

        Aircraft a = mkAircraft(scheduled, null, takeoffEntry, AircraftDirection.OUTBOUND);

        mc.onTakeoffEnqueue(takeoffEntry, 1);
        mc.onTakeoffDequeue(takeoffActual, 0);
        mc.onTakeoffCompleted(a, takeoffActual);

        SimulationReport r = mc.buildReport(TEST_NUM_RUNWAYS);

        assertEquals(TEST_NUM_RUNWAYS, r.numRunways());
        assertEquals(1, r.maxTakeoffQueueSize());
        assertEquals(13.0, r.avgTakeoffWaitMinutes(), 1e-9);
        assertEquals(15.0, r.avgDepartureDelayMinutes(), 1e-9);
        assertEquals(15.0, r.maxDepartureDelayMinutes(), 1e-9);
    }

    @Test
    void maxAndAverageReflectMultipleSamples() {
        MetricsCollector mc = new MetricsCollector();
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        Aircraft a1 = mkAircraft(t0, t0, null, AircraftDirection.INBOUND);
        mc.onHoldingEnqueue(t0, 1);
        mc.onLandingCompleted(a1, t0.plusSeconds(10 * 60));

        Aircraft a2 = mkAircraft(t0, t0.plusSeconds(5 * 60), null, AircraftDirection.INBOUND);
        mc.onHoldingEnqueue(t0.plusSeconds(5 * 60), 2);
        mc.onLandingCompleted(a2, t0.plusSeconds(25 * 60));

        Aircraft d1 = mkAircraft(t0, null, t0, AircraftDirection.OUTBOUND);
        mc.onTakeoffEnqueue(t0, 1);
        mc.onTakeoffCompleted(d1, t0.plusSeconds(5 * 60));

        SimulationReport r = mc.buildReport(TEST_NUM_RUNWAYS);

        assertEquals(TEST_NUM_RUNWAYS, r.numRunways());
        assertEquals(15.0, r.avgHoldingTimeMinutes(), 1e-9);
        assertEquals(17.5, r.avgArrivalDelayMinutes(), 1e-9);
        assertEquals(25.0, r.maxArrivalDelayMinutes(), 1e-9);
    }
}