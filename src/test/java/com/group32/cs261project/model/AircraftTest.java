package com.group32.cs261project.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.group32.cs261project.model.enums.AircraftState;
import com.group32.cs261project.model.enums.EmergencyStatus;

class AircraftTest {

    private Aircraft makeAircraft(EmergencyStatus emergency, double fuel) {
        return new Aircraft(
                "BA123",
                "BAW",
                "JFK",
                "LHR",
                Instant.parse("2026-01-01T10:00:00Z"),
                emergency,
                fuel,
                AircraftState.OUTSIDE_MODEL
        );
    }

    // ---------------------------
    // Constructor + Getters
    // ---------------------------

    @Test
    void constructorStoresValuesCorrectly() {
        Instant scheduled = Instant.parse("2026-01-01T10:00:00Z");

        Aircraft aircraft = new Aircraft(
                "BA123",
                "BAW",
                "JFK",
                "LHR",
                scheduled,
                EmergencyStatus.NONE,
                120.0,
                AircraftState.OUTSIDE_MODEL
        );

        assertEquals("BA123", aircraft.callsign());
        assertEquals("BAW", aircraft.operator());
        assertEquals("JFK", aircraft.origin());
        assertEquals("LHR", aircraft.destination());
        assertEquals(scheduled, aircraft.scheduledTime());
        assertEquals(EmergencyStatus.NONE, aircraft.emergencyStatus());
        assertEquals(AircraftState.OUTSIDE_MODEL, aircraft.state());
        assertNull(aircraft.holdingPatternEntryTime());
        assertNull(aircraft.takeOffQueueEntryTime());
    }

    // ---------------------------
    // Emergency Logic
    // ---------------------------

    @Test
    void isEmergencyReflectsEmergencyStatus() {
        Aircraft normal = makeAircraft(EmergencyStatus.NONE, 100);
        Aircraft emergency = makeAircraft(EmergencyStatus.FUEL, 100);

        assertFalse(normal.isEmergency());
        assertTrue(emergency.isEmergency());
    }

    // ---------------------------
    // Delay Calculation
    // ---------------------------

    @Test
    void delayMinutesCalculatesCorrectDifference() {
        Instant scheduled = Instant.parse("2026-01-01T10:00:00Z");
        Aircraft aircraft = makeAircraft(EmergencyStatus.NONE, 100);

        Instant actualEarly = scheduled.minusSeconds(600); // 10 minutes early
        Instant actualLate = scheduled.plusSeconds(300);   // 5 minutes late

        assertEquals(10, aircraft.delayMinutes(actualEarly));
        assertEquals(-5, aircraft.delayMinutes(actualLate));
        assertEquals(0, aircraft.delayMinutes(scheduled));
    }

    // ---------------------------
    // State Transitions
    // ---------------------------

    @Test
    void markHoldingUpdatesStateAndEntryTime() {
        Aircraft aircraft = makeAircraft(EmergencyStatus.NONE, 100);
        Instant entry = Instant.parse("2026-01-01T09:55:00Z");

        aircraft.markHolding(entry);

        assertEquals(AircraftState.HOLDING, aircraft.state());
        assertEquals(entry, aircraft.holdingPatternEntryTime());
    }

    @Test
    void markTakeoffQueueUpdatesStateAndEntryTime() {
        Aircraft aircraft = makeAircraft(EmergencyStatus.NONE, 100);
        Instant entry = Instant.parse("2026-01-01T09:50:00Z");

        aircraft.markTakeoffQueue(entry);

        assertEquals(AircraftState.TAKEOFF_QUEUE, aircraft.state());
        assertEquals(entry, aircraft.takeOffQueueEntryTime());
    }

    @Test
    void markRunwayZoneUpdatesState() {
        Aircraft aircraft = makeAircraft(EmergencyStatus.NONE, 100);

        aircraft.markRunwayZone();

        assertEquals(AircraftState.RUNWAY_ZONE, aircraft.state());
    }

    @Test
    void markCompletedUpdatesState() {
        Aircraft aircraft = makeAircraft(EmergencyStatus.NONE, 100);

        aircraft.markCompleted();

        assertEquals(AircraftState.COMPLETED, aircraft.state());
    }

    @Test
    void markDivertedUpdatesState() {
        Aircraft aircraft = makeAircraft(EmergencyStatus.NONE, 100);

        aircraft.markDiverted();

        assertEquals(AircraftState.DIVERTED, aircraft.state());
    }

    @Test
    void markCancelledUpdatesState() {
        Aircraft aircraft = makeAircraft(EmergencyStatus.NONE, 100);

        aircraft.markCancelled();

        assertEquals(AircraftState.CANCELLED, aircraft.state());
    }

    // ---------------------------
    // Fuel Logic
    // ---------------------------

    @Test
    void consumeFuelReducesFuelButNotBelowZeroCondition() {
        Aircraft aircraft = makeAircraft(EmergencyStatus.NONE, 50);

        aircraft.consumeFuelMinutes(10);
        assertFalse(aircraft.isLowFuel(30)); // 40 remaining

        aircraft.consumeFuelMinutes(35);
        assertTrue(aircraft.isLowFuel(10));  // 5 remaining

        // Consuming more than remaining should NOT reduce further
        aircraft.consumeFuelMinutes(10);
        assertTrue(aircraft.isLowFuel(10));  // still <= threshold
    }

    @Test
    void isLowFuelRespectsThreshold() {
        Aircraft aircraft = makeAircraft(EmergencyStatus.NONE, 15);

        assertFalse(aircraft.isLowFuel(10));
        assertTrue(aircraft.isLowFuel(15));
        assertTrue(aircraft.isLowFuel(20));
    }
}