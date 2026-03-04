package com.group32.cs261project.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.model.Runway;
import com.group32.cs261project.backend.model.RunwayData;
import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;

class AirportTest {

    @Test
    void findsLandingRunwayWhenAvailable() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        RunwayData takeoffOnly = new RunwayData(3000, 90, RunwayMode.TAKEOFF, RunwayStatus.AVAILABLE);
        RunwayData landingOnly = new RunwayData(3000, 270, RunwayMode.LANDING, RunwayStatus.AVAILABLE);

        Airport airport = new Airport(List.of(takeoffOnly, landingOnly));

        Optional<Runway> r = airport.findAvailableLandingRunway(t0);
        assertTrue(r.isPresent());
        assertEquals(1, r.get().runwayNumber());
    }

    @Test
    void returnsEmptyWhenNoLandingRunwayAvailable() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        RunwayData landingClosed = new RunwayData(3000, 270, RunwayMode.LANDING, RunwayStatus.SNOW_CLEARANCE);
        Airport airport = new Airport(List.of(landingClosed));

        assertTrue(airport.findAvailableLandingRunway(t0).isEmpty());
    }

    @Test
    void findsTakeoffRunwayWhenAvailable() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        RunwayData takeoffOnly = new RunwayData(93000, 90, RunwayMode.TAKEOFF, RunwayStatus.AVAILABLE);
        RunwayData landingOnly = new RunwayData(3000, 270, RunwayMode.LANDING, RunwayStatus.AVAILABLE);

        Airport airport = new Airport(List.of(takeoffOnly, landingOnly));

        Optional<Runway> r = airport.findAvailableTakeoffRunway(t0);
        assertTrue(r.isPresent());
        assertEquals(0, r.get().runwayNumber());
    }

    @Test
    void occupiedRunwayIsNotReturnedUntilFree() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t1 = t0.plusSeconds(120);

        RunwayData landing = new RunwayData(3000, 270, RunwayMode.LANDING, RunwayStatus.AVAILABLE);
        Airport airport = new Airport(List.of(landing));

        airport.getRunway(0).occupyUntil(t1);

        assertTrue(airport.findAvailableLandingRunway(t0).isEmpty());
        assertTrue(airport.findAvailableLandingRunway(t1).isPresent());
    }
    @Test
    void getRunwayReturnsCorrectRunwayAndAllowsUpdates() {
        // Create two runways
        RunwayData r0 = new RunwayData(3000, 90, RunwayMode.TAKEOFF, RunwayStatus.AVAILABLE);
        RunwayData r1 = new RunwayData(3000, 270, RunwayMode.LANDING, RunwayStatus.AVAILABLE);

        Airport airport = new Airport(List.of(r0, r1));

        // Test getRunway returns the correct runway
        Runway runway0 = airport.getRunway(0);
        Runway runway1 = airport.getRunway(1);
        assertEquals(0, runway0.runwayNumber());
        assertEquals(1, runway1.runwayNumber());

        // Test setRunwayStatus updates the runway status
        airport.setRunwayStatus(0, RunwayStatus.INSPECTION);
        assertEquals(RunwayStatus.INSPECTION, airport.getRunway(0).status());

        // Test setRunwayMode updates the runway mode
        airport.setRunwayMode(1, RunwayMode.MIXED);
        assertEquals(RunwayMode.MIXED, airport.getRunway(1).mode());
    }
}