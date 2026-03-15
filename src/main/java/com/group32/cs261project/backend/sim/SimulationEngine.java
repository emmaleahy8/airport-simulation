// src/main/java/com/group32/cs261project/sim/SimulationEngine.java
package com.group32.cs261project.backend.sim;

import java.time.Instant;
import java.util.Objects;
import java.util.Random;

import com.group32.cs261project.backend.events.Event;

/**
 * Simulation engine kernel class that handles events
 * Generic which separates engine implementation from rest of code
 */
public final class SimulationEngine<C> {

    private final SimConfig config;
    private final C context;
    private final EventQueue<C> eventQueue;
    private final Random rng;

    private Instant now;
    private long nextSequence = 0L;

    /**
     * Constructor
     * @param config
     * @param context
     * @param eventQueue
     */
    public SimulationEngine(SimConfig config, C context, EventQueue<C> eventQueue) {
        this.config = Objects.requireNonNull(config, "config");
        this.context = Objects.requireNonNull(context, "context");
        this.eventQueue = Objects.requireNonNull(eventQueue, "eventQueue");
        this.rng = new Random(config.randomSeed());
        this.now = config.startTime();
    }

    /*  getters */
    public SimConfig config() { return config; }
    public C context() { return context; }
    public EventQueue<C> eventQueue() { return eventQueue; }
    public Random rng() { return rng; }
    public Instant now() { return now; }

    /**
     * Schedules an event to the simulation kernel
     * @param event
     */
    public void schedule(Event<C> event) {
        Objects.requireNonNull(event, "event");
        if (event.time().isBefore(now)) {
            throw new IllegalArgumentException("Cannot schedule in the past: event=" + event.time() + ", now=" + now);
        }
        eventQueue.push(event, nextSequence++); // increment sequence number
    }

    /**
     * Runs the simulation until it finishes
     * @param endTime
     */
    public void runUntil(Instant endTime) {
        Objects.requireNonNull(endTime, "endTime");
        while (!eventQueue.isEmpty()) {
            Event<C> next = eventQueue.peek();
            if (next == null) break;
            if (next.time().isAfter(endTime)) break;
            processNextEvent();
        }
    }

    /**
     * Steps the simulation forward by one step
     * @return true if can step, false if completed
     */
    public boolean step() {
        if (eventQueue.isEmpty()) return false;
        processNextEvent();
        return true;
    }

    /**
     * Method to process the next event
     * Pops from the queue and handles that event
     */
    public void processNextEvent() {
        Event<C> next = eventQueue.pop();
        if (next == null) return;
        now = next.time();
        next.handle(this);
    }

}
