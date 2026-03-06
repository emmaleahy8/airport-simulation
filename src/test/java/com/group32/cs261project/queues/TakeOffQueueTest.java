package com.group32.cs261project.queues;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

@Test
    void planesReturnsEmptyListInitially() {
        TakeOffQueue q = new TakeOffQueue();
        List<Aircraft> planes = q.toList();

        assertNotNull(planes);
        assertTrue(planes.isEmpty());
    }

    @Test
    void planesContainsEnqueuedAircraftInOrder() {
        TakeOffQueue q = new TakeOffQueue();
        Instant t = Instant.parse("2026-01-01T00:00:00Z");

        Aircraft a1 = mkAircraft("D1");
        Aircraft a2 = mkAircraft("D2");

        q.enqueue(a1, t);
        q.enqueue(a2, t.plusSeconds(1));

        List<Aircraft> planes = q.toList();

        assertEquals(2, planes.size());
        assertSame(a1, planes.get(0));
        assertSame(a2, planes.get(1));
    }

    @Test
    void planesReflectsDequeue() {
        TakeOffQueue q = new TakeOffQueue();
        Instant t = Instant.parse("2026-01-01T00:00:00Z");

        Aircraft a1 = mkAircraft("D1");
        Aircraft a2 = mkAircraft("D2");

        q.enqueue(a1, t);
        q.enqueue(a2, t.plusSeconds(1));

        Aircraft removed = q.dequeue();
        assertSame(a1, removed); // FIFO expected

        List<Aircraft> planes = q.toList();
        assertEquals(1, planes.size());
        assertSame(a2, planes.get(0));
    }

}