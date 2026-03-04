package com.group32.cs261project.queues;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.model.Aircraft;
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
                AircraftState.OUTSIDE_MODEL
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
}