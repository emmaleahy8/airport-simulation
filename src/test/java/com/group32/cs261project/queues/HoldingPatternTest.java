package com.group32.cs261project.queues;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.enums.AircraftDirection;
import com.group32.cs261project.backend.model.enums.AircraftState;
import com.group32.cs261project.backend.model.enums.EmergencyStatus;
import com.group32.cs261project.backend.queues.HoldingPattern;

class HoldingPatternTest {

    private static Aircraft mkAircraft(String callsign, EmergencyStatus emergency) {
        return new Aircraft(
                callsign,
                "TestAir",
                "AAA",
                "BBB",
                Instant.parse("2026-01-01T00:00:00Z"),
                emergency,
                30.0,
                AircraftState.OUTSIDE_MODEL,
                AircraftDirection.INBOUND 
        );
    }

    @Test
    void fifoWhenNoEmergencies() {
        HoldingPattern hp = new HoldingPattern();
        Instant t = Instant.parse("2026-01-01T00:00:00Z");

        Aircraft a = mkAircraft("A1", EmergencyStatus.NONE);
        Aircraft b = mkAircraft("B2", EmergencyStatus.NONE);

        hp.enqueue(a, t);
        hp.enqueue(b, t.plusSeconds(60));

        assertEquals(2, hp.size());
        assertSame(a, hp.peekNextLandingCandidate());
        assertSame(a, hp.dequeueNextLandingCandidate());
        assertSame(b, hp.dequeueNextLandingCandidate());
        assertTrue(hp.isEmpty());
    }

    @Test
    void emergencyAircraftIsPrioritisedOverFifo() {
        HoldingPattern hp = new HoldingPattern();
        Instant t = Instant.parse("2026-01-01T00:00:00Z");

        Aircraft normal1 = mkAircraft("N1", EmergencyStatus.NONE);
        Aircraft emergency = mkAircraft("E1", EmergencyStatus.FUEL);
        Aircraft normal2 = mkAircraft("N2", EmergencyStatus.NONE);

        hp.enqueue(normal1, t);
        hp.enqueue(emergency, t.plusSeconds(60));
        hp.enqueue(normal2, t.plusSeconds(120));

        // emergency first
        assertSame(emergency, hp.peekNextLandingCandidate());
        assertSame(emergency, hp.dequeueNextLandingCandidate());

        // then FIFO among remaining normals
        assertSame(normal1, hp.dequeueNextLandingCandidate());
        assertSame(normal2, hp.dequeueNextLandingCandidate());
        assertTrue(hp.isEmpty());
    }

    @Test
    void enqueueSetsAircraftStateAndEntryTime() {
        HoldingPattern hp = new HoldingPattern();
        Instant t = Instant.parse("2026-01-01T00:00:00Z");

        Aircraft a = mkAircraft("A1", EmergencyStatus.NONE);
        hp.enqueue(a, t);

        assertEquals(AircraftState.HOLDING, a.state());
        assertEquals(t, a.holdingPatternEntryTime());
    }

    @Test
    void planesReturnsEmptyListInitially() {
        HoldingPattern hp = new HoldingPattern();
        List<Aircraft> planes = hp.toList();

        assertNotNull(planes);
        assertTrue(planes.isEmpty());
    }

    @Test
    void planesContainsEnqueuedAircraftInOrder() {
        HoldingPattern hp = new HoldingPattern();
        Instant t = Instant.parse("2026-01-01T00:00:00Z");

        Aircraft a1 = mkAircraft("A1", EmergencyStatus.NONE);
        Aircraft a2 = mkAircraft("A2", EmergencyStatus.NONE);

        hp.enqueue(a1, t);
        hp.enqueue(a2, t.plusSeconds(1));

        List<Aircraft> planes = hp.toList();

        assertEquals(2, planes.size());
        assertSame(a1, planes.get(0));
        assertSame(a2, planes.get(1));
    }

    @Test
    void planesReflectsDequeue() {
        HoldingPattern hp = new HoldingPattern();
        Instant t = Instant.parse("2026-01-01T00:00:00Z");

        Aircraft a1 = mkAircraft("A1", EmergencyStatus.NONE);
        Aircraft a2 = mkAircraft("A2", EmergencyStatus.NONE);

        hp.enqueue(a1, t);
        hp.enqueue(a2, t.plusSeconds(1));

        Aircraft removed = hp.dequeueNextLandingCandidate();
        assertNotNull(removed);

        List<Aircraft> planes = hp.toList();
        assertEquals(1, planes.size());
        assertFalse(planes.contains(removed));
    }
}