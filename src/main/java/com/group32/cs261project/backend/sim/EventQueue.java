// src/main/java/com/group32/cs261project/sim/queue/PriorityEventQueue.java
package com.group32.cs261project.backend.sim;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;

import com.group32.cs261project.backend.events.Event;

/**
 * Implementation of the event queue
 */
public final class EventQueue<C> {

    private static final class QueuedEvent<C> {
        final Event<C> event;
        final Instant time;
        final long sequenceNumber;

        QueuedEvent(Event<C> event, long sequenceNumber) {
            this.event = event;
            this.time = event.time();
            this.sequenceNumber = sequenceNumber;
        }
    }

    private final Queue<QueuedEvent<C>> pq;
    
    /**
     * Constructor
     */
    public EventQueue() {
        this.pq = new PriorityQueue<>(Comparator.<QueuedEvent<C>, Instant>comparing(q -> q.time)
            .thenComparingLong(q -> q.sequenceNumber)
        );
    }

    /**
     * Push event to queue
     */
    public void push(Event<C> event, long sequenceNumber) {
        Objects.requireNonNull(event, "event");
        pq.add(new QueuedEvent<>(event, sequenceNumber));
    }

    /**
     * Pop event from queue with earliest time
     */
    public Event<C> pop() {
        QueuedEvent<C> q = pq.poll();
        return q == null ? null : q.event;
    }

    /**
     * Get event with earliest time
     */
    public Event<C> peek() {
        QueuedEvent<C> q = pq.peek();
        return q == null ? null : q.event;
    }

    /**
     * Get time of earliest event
     */
    public Instant peekTime() {
        QueuedEvent<C> q = pq.peek();
        return q == null ? null : q.time;
    }

    /**
     * Checks if the queue is empty
     */
    public boolean isEmpty() {
        return pq.isEmpty();
    }

    /**
     * Gets the number of elements in the queue
     */
    public int size() {
        return pq.size();
    }
}
