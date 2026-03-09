package com.group32.cs261project.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.model.Runway;
import com.group32.cs261project.backend.model.RunwayData;
import com.group32.cs261project.backend.model.enums.AircraftDirection;
import com.group32.cs261project.backend.model.enums.AircraftState;
import com.group32.cs261project.backend.model.enums.EmergencyStatus;
import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;

class AirportTest {

    private static Aircraft mkAircraft(String callsign, EmergencyStatus emergency, AircraftDirection direction) {
        return new Aircraft(
                callsign,
                "TestAir",
                "AAA",
                "BBB",
                Instant.parse("2026-01-01T00:00:00Z"),
                emergency,
                30.0,
                AircraftState.OUTSIDE_MODEL,
                direction
        );
    }

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

    @Test
    void assignAircraftToRunwayAddsAircraftToRunwayAircraftList() {
        RunwayData r1 = new RunwayData(3000, 90, RunwayMode.LANDING, RunwayStatus.AVAILABLE);
        Airport airport = new Airport(List.of(r1));

        Aircraft a1 = mkAircraft("A1", EmergencyStatus.NONE, AircraftDirection.INBOUND);

        // Precondition
        assertEquals(airport.runwayAircraft().get(0), null);

        airport.assignAircraftToRunway(airport.getRunway(0), a1);

        List<Aircraft> onRunways = airport.runwayAircraft();
        assertEquals(1, onRunways.size());
        assertTrue(onRunways.contains(a1));
    }

    @Test
    void clearRunwayRemovesAircraftFromThatRunway() {
        RunwayData r1 = new RunwayData(3000, 90, RunwayMode.LANDING, RunwayStatus.AVAILABLE);
        Airport airport = new Airport(List.of(r1));

        Aircraft a1 = mkAircraft("A1", EmergencyStatus.NONE, AircraftDirection.INBOUND);

        airport.assignAircraftToRunway(airport.getRunway(0), a1);
        assertEquals(1, airport.runwayAircraft().size());

        airport.clearRunway(airport.getRunway(0));

        assertEquals(airport.runwayAircraft().get(0), null);
    }

    @Test
    void assigningTwoRunwaysTracksBothAircraftAndClearingOneLeavesTheOther() {
        RunwayData r1 = new RunwayData(3000, 90, RunwayMode.LANDING, RunwayStatus.AVAILABLE);
        RunwayData r2 = new RunwayData(3000, 270, RunwayMode.TAKEOFF, RunwayStatus.AVAILABLE);
        Airport airport = new Airport(List.of(r1, r2));

        Aircraft a1 = mkAircraft("A1", EmergencyStatus.NONE, AircraftDirection.INBOUND);
        Aircraft a2 = mkAircraft("D1", EmergencyStatus.NONE, AircraftDirection.OUTBOUND);

        airport.assignAircraftToRunway(airport.getRunway(0), a1);
        airport.assignAircraftToRunway(airport.getRunway(1), a2);

        List<Aircraft> onRunways = airport.runwayAircraft();
        assertEquals(2, onRunways.size());
        assertTrue(onRunways.containsAll(List.of(a1, a2)));

        airport.clearRunway(airport.getRunway(0));

        List<Aircraft> afterClear = airport.runwayAircraft();
        assertEquals(airport.runwayAircraft().get(0), null);
        assertTrue(afterClear.contains(a2));
        assertFalse(afterClear.contains(a1));
    }

    @Test
    void reassigningSameRunwayReplacesAircraft() {
        RunwayData r1 = new RunwayData(3000, 90, RunwayMode.LANDING, RunwayStatus.AVAILABLE);
        Airport airport = new Airport(List.of(r1));

        Aircraft a1 = mkAircraft("A1", EmergencyStatus.NONE, AircraftDirection.INBOUND);
        Aircraft a2 = mkAircraft("A2", EmergencyStatus.NONE, AircraftDirection.INBOUND);

        airport.assignAircraftToRunway(airport.getRunway(0), a1);
        assertTrue(airport.runwayAircraft().contains(a1));

        // Assign different aircraft to same runway
        airport.assignAircraftToRunway(airport.getRunway(0), a2);

        List<Aircraft> onRunways = airport.runwayAircraft();
        assertEquals(1, onRunways.size());
        assertTrue(onRunways.contains(a2));
        assertFalse(onRunways.contains(a1));
    }
}