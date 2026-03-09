package com.group32.cs261project.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.AirportSimulation;
import com.group32.cs261project.backend.events.AircraftOutboundReadyEvent;
import com.group32.cs261project.backend.metrics.SimulationReport;
import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.model.RunwayData;
import com.group32.cs261project.backend.model.enums.AircraftDirection;
import com.group32.cs261project.backend.model.enums.AircraftState;
import com.group32.cs261project.backend.model.enums.EmergencyStatus;
import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;
import com.group32.cs261project.backend.sim.SimConfig;

/**
 * Integration tests for the end-of-run SimulationReport.
 *
 * These tests verify that key metrics are populated and consistent
 * after a complete run that includes both a normal outbound departure
 * and a cancellation, in line with the output requirements OMC-01
 * to OMC-06 (queue sizes, waits, delays, cancellations).
 */
public class MetricsReportIT {

    @Test
    void reportIncludesCancellationCountAndQueueMetrics() {
        // Scenario: one outbound aircraft departs successfully, another
        // is prevented from taking off (runway closed) and must be
        // cancelled after exceeding the maximum wait time. We then
        // assert that the final SimulationReport contains a positive
        // take-off queue max size and a cancellation count of 1.

        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant end = start.plusSeconds(7200);

        // Runway occupancy long enough that first aircraft holds the runway past
        // the second's 30 min cancellation (second ready at 120s, cancelled at 1920s).

        SimConfig config = SimConfig.builder()
            .startTime(start)
            .endTime(end)
            .randomSeed(99L)
            .inboundRatePerHour(0.0)
            .outboundRatePerHour(0.0)
            .runwayOccupancySeconds(3600)
            .build();

        // Two runways:
        // - Runway 0: AVAILABLE take-off runway so first aircraft can depart (and hold it).
        // - Runway 1: INSPECTION, keeps second aircraft from ever taking off.
        RunwayData runway0 = new RunwayData(
                3000,
                90,
                RunwayMode.TAKEOFF,
                RunwayStatus.AVAILABLE
        );
        RunwayData runway1 = new RunwayData(
                3000,
                90,
                RunwayMode.TAKEOFF,
                RunwayStatus.INSPECTION
        );

        Airport airport = new Airport(List.of(runway0, runway1));
        AirportSimulation sim = new AirportSimulation(config, airport);

        Instant readyTime1 = start.plusSeconds(60);
        Instant readyTime2 = start.plusSeconds(120);

        // First outbound aircraft should be able to depart (AVAILABLE runway).
        Aircraft outboundOk = new Aircraft(
                "OUT-METRICS-OK",
                "TestAir",
                "MAN",
                "LHR",
                readyTime1,
                EmergencyStatus.NONE,
                60.0,
                AircraftState.OUTSIDE_MODEL,
                AircraftDirection.OUTBOUND
        );

        // Second outbound aircraft will still get enqueued (and a cancellation
        // deadline scheduled), but because there is no additional available
        // capacity for it in this simple scenario, it will ultimately be
        // cancelled by the deadline.
        Aircraft outboundCancelled = new Aircraft(
                "OUT-METRICS-CXL",
                "TestAir",
                "MAN",
                "LHR",
                readyTime2,
                EmergencyStatus.NONE,
                60.0,
                AircraftState.OUTSIDE_MODEL,
                AircraftDirection.OUTBOUND
        );

        sim.engine().schedule(new AircraftOutboundReadyEvent(readyTime1, outboundOk));
        sim.engine().schedule(new AircraftOutboundReadyEvent(readyTime2, outboundCancelled));

        sim.runUntil(end);

        SimulationReport report = sim.buildReport();

        // At least one outbound should have waited in the take-off queue,
        // so the maximum queue size should be >= 1.
        assertNotNull(report.maxTakeoffQueueSize());
        assertTrue(
                report.maxTakeoffQueueSize() >= 1,
                "Max take-off queue size should be at least 1 in this scenario"
        );

        // Average take-off wait should be non-negative.
        assertNotNull(report.avgTakeoffWaitMinutes());
        assertTrue(
                report.avgTakeoffWaitMinutes() >= 0.0,
                "Average take-off wait should be non-negative"
        );

        // We expect exactly one cancellation in this mixed scenario.
        assertEquals(
                1,
                report.numCancellations(),
                "Report should record exactly 1 cancelled outbound flight"
        );
    }
}

