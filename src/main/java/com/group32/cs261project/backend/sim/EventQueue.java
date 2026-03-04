package com.group32.cs261project.backend.sim;

import java.time.Instant;

import com.group32.cs261project.backend.events.Event;

/**
 * Interface for the event queue
 */
public interface EventQueue<C> {
    void push(Event<C> event, long sequenceNumber);
    Event<C> pop();
    Event<C> peek();
    Instant peekTime();
    boolean isEmpty();
    int size();
}
