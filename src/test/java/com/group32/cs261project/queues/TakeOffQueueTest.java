package com.group32.cs261project.queues;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.enums.AircraftState;
import com.group32.cs261project.backend.model.enums.EmergencyStatus;
import com.group32.cs261project.backend.queues.TakeOffQueue;

class TakeOffQueueTest {

    private static Aircraft mkAircraft(String callsign) {
        return new Aircraft(
                callsign,
                "TestAir",
                "AAA",
                "BBB",
                Instant.parse("2026-01-01T00:00:00Z"),
                EmergencyStatus.NONE,
                30.0,
                AircraftState.OUTSIDE_MODEL
        );
    }

    @Test
    void enqueueAndDequeueAreFifo() {
        TakeOffQueue q = new TakeOffQueue();
        Instant t = Instant.parse("2026-01-01T00:00:00Z");

        Aircraft a = mkAircraft("A1");
        Aircraft b = mkAircraft("B2");
        Aircraft c = mkAircraft("C3");

        q.enqueue(a, t);
        q.enqueue(b, t.plusSeconds(60));
        q.enqueue(c, t.plusSeconds(120));

        assertEquals(3, q.size());
        assertSame(a, q.peek());

        assertSame(a, q.dequeue());
        assertSame(b, q.dequeue());
        assertSame(c, q.dequeue());

        assertTrue(q.isEmpty());
        assertNull(q.peek());
        assertNull(q.dequeue());
    }

    @Test
    void enqueueSetsAircraftStateAndEntryTime() {
        TakeOffQueue q = new TakeOffQueue();
        Instant t = Instant.parse("2026-01-01T00:00:00Z");

        Aircraft a = mkAircraft("A1");
        q.enqueue(a, t);

        // Adjust these getters to match your Aircraft API (or check fields if public).
        assertEquals(AircraftState.TAKEOFF_QUEUE, a.state());
        assertEquals(t, a.takeOffQueueEntryTime());
    }
}