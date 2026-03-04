package com.group32.cs261project.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.group32.cs261project.model.enums.RunwayMode;
import com.group32.cs261project.model.enums.RunwayStatus;

class AirportTest {

    @Test
    void findsLandingRunwayWhenAvailable() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        Runway takeoffOnly = new Runway(9, 3000, 90, RunwayMode.TAKEOFF, RunwayStatus.AVAILABLE);
        Runway landingOnly = new Runway(27, 3000, 270, RunwayMode.LANDING, RunwayStatus.AVAILABLE);

        Airport airport = new Airport(List.of(takeoffOnly, landingOnly));

        Optional<Runway> r = airport.findAvailableLandingRunway(t0);
        assertTrue(r.isPresent());
        assertEquals(27, r.get().runwayNumber());
    }

    @Test
    void returnsEmptyWhenNoLandingRunwayAvailable() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        Runway landingClosed = new Runway(27, 3000, 270, RunwayMode.LANDING, RunwayStatus.SNOW_CLEARANCE);
        Airport airport = new Airport(List.of(landingClosed));

        assertTrue(airport.findAvailableLandingRunway(t0).isEmpty());
    }

    @Test
    void findsTakeoffRunwayWhenAvailable() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        Runway takeoffOnly = new Runway(9, 3000, 90, RunwayMode.TAKEOFF, RunwayStatus.AVAILABLE);
        Runway landingOnly = new Runway(27, 3000, 270, RunwayMode.LANDING, RunwayStatus.AVAILABLE);

        Airport airport = new Airport(List.of(takeoffOnly, landingOnly));

        Optional<Runway> r = airport.findAvailableTakeoffRunway(t0);
        assertTrue(r.isPresent());
        assertEquals(9, r.get().runwayNumber());
    }

    @Test
    void occupiedRunwayIsNotReturnedUntilFree() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t1 = t0.plusSeconds(120);

        Runway landing = new Runway(27, 3000, 270, RunwayMode.LANDING, RunwayStatus.AVAILABLE);
        landing.occupyUntil(t1);

        Airport airport = new Airport(List.of(landing));

        assertTrue(airport.findAvailableLandingRunway(t0).isEmpty());
        assertTrue(airport.findAvailableLandingRunway(t1).isPresent());
    }
    @Test
    void getRunwayReturnsCorrectRunwayAndAllowsUpdates() {
        // Create two runways
        Runway r0 = new Runway(9, 3000, 90, RunwayMode.TAKEOFF, RunwayStatus.AVAILABLE);
        Runway r1 = new Runway(27, 3000, 270, RunwayMode.LANDING, RunwayStatus.AVAILABLE);

        Airport airport = new Airport(List.of(r0, r1));

        // Test getRunway returns the correct runway
        Runway runway0 = airport.getRunway(9);
        Runway runway1 = airport.getRunway(27);
        assertEquals(9, runway0.runwayNumber());
        assertEquals(27, runway1.runwayNumber());

        // Test setRunwayStatus updates the runway status
        airport.setRunwayStatus(0, RunwayStatus.INSPECTION);
        assertEquals(RunwayStatus.INSPECTION, airport.getRunway(0).status());

        // Test setRunwayMode updates the runway mode
        airport.setRunwayMode(1, RunwayMode.MIXED);
        assertEquals(RunwayMode.MIXED, airport.getRunway(1).mode());
    }
}