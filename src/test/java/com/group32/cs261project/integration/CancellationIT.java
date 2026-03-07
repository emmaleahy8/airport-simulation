package com.group32.cs261project.integration;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
 * Integration tests for outbound cancellations based on maximum wait time.
 *
 * These tests exercise the full backend pipeline to verify that outbound
 * aircraft which wait longer than the configured maximum take-off queue
 * time are cancelled and counted in the final report, as required by FC-11 /
 * OMC-05, and that aircraft which depart before the deadline are not
 * cancelled.
 */
public class CancellationIT {

    @Test
    void outboundAircraftCancelledAfterExceedingMaxWait() {
        // Scenario: single take-off runway kept unavailable the whole run.
        // An outbound aircraft joins the take-off queue and cannot depart,
        // so when the CancellationDeadlineEvent fires after the configured
        // maxTakeoffWaitMinutes, it must be removed from the queue, marked
        // CANCELLED, and counted in the report.

        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        // Run long enough to go past the 30-minute default cancellation window.
        Instant end = start.plusSeconds(7200); // 2 hours

        SimConfig config = new SimConfig(
                start,
                end,
                42L,
                0.0,
                0.0,
                300 // runway occupancy (doesn't matter because runway is closed)
        );

        // One runway that is permanently unavailable (INSPECTION),
        // so no take-off operations can actually run.
        RunwayData runwayData = new RunwayData(
                3000,
                90,
                RunwayMode.TAKEOFF,
                RunwayStatus.INSPECTION
        );

        Airport airport = new Airport(List.of(runwayData));
        AirportSimulation sim = new AirportSimulation(config, airport);

        // Single outbound aircraft that becomes ready shortly after start.
        Instant readyTime = start.plusSeconds(60);
        Aircraft outbound = new Aircraft(
                "OUT-CXL",
                "TestAir",
                "MAN",
                "LHR",
                readyTime,
                EmergencyStatus.NONE,
                60.0,
                AircraftState.OUTSIDE_MODEL,
                AircraftDirection.OUTBOUND
        );

        // Schedule the outbound-ready event.
        sim.engine().schedule(new AircraftOutboundReadyEvent(readyTime, outbound));

        // Run the simulation until well after the cancellation deadline.
        sim.runUntil(end);

        // The aircraft should have been cancelled by the deadline.
        assertEquals(
                AircraftState.CANCELLED,
                outbound.state(),
                "Outbound aircraft should be cancelled after exceeding max wait time"
        );

        // It must no longer be in the take-off queue.
        assertTrue(
                airport.takeOffQueue().isEmpty(),
                "Take-off queue should not retain cancelled aircraft"
        );

        // The final report must count exactly one cancellation.
        SimulationReport report = sim.buildReport();
        assertEquals(
                1,
                report.numCancellations(),
                "Cancellation count in report should be 1 for this scenario"
        );
    }

    @Test
    void noCancellationIfAircraftTakesOffBeforeDeadline() {
        // Scenario: single available take-off runway.
        // An outbound aircraft joins the queue and is able to depart
        // promptly, so when the CancellationDeadlineEvent is reached
        // it should see that the aircraft is no longer in the queue
        // and NOT mark it as cancelled.

        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant end = start.plusSeconds(7200);

        SimConfig config = new SimConfig(
                start,
                end,
                43L,
                0.0,
                0.0,
                60 // 1-minute runway occupancy to keep things simple
        );

        // One available take-off runway; aircraft can use it immediately.
        RunwayData runwayData = new RunwayData(
                3000,
                90,
                RunwayMode.TAKEOFF,
                RunwayStatus.AVAILABLE
        );

        Airport airport = new Airport(List.of(runwayData));
        AirportSimulation sim = new AirportSimulation(config, airport);

        Instant readyTime = start.plusSeconds(60);
        Aircraft outbound = new Aircraft(
                "OUT-OK",
                "TestAir",
                "MAN",
                "LHR",
                readyTime,
                EmergencyStatus.NONE,
                60.0,
                AircraftState.OUTSIDE_MODEL,
                AircraftDirection.OUTBOUND
        );

        // The outbound-ready event will enqueue the aircraft and immediately
        // allow the scheduler to start a take-off operation because the runway
        // is available.
        sim.engine().schedule(new AircraftOutboundReadyEvent(readyTime, outbound));

        // Run the simulation until the end.
        sim.runUntil(end);

        // Aircraft should have completed normally, not be cancelled.
        assertEquals(
                AircraftState.COMPLETED,
                outbound.state(),
                "Outbound aircraft that departs before the deadline must not be cancelled"
        );

        // No cancellations should be reported.
        SimulationReport report = sim.buildReport();
        assertEquals(
                0,
                report.numCancellations(),
                "Cancellation count should be 0 when all outbound flights depart before the deadline"
        );
    }
}

