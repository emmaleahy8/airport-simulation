package com.group32.cs261project.backend.queues;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.group32.cs261project.backend.model.Aircraft;

/**
 * Class for take off queue
 */
public class TakeOffQueue {

    private final Queue<Aircraft> queue; // fifo queue

    /**
     * Constructor
     */
    public TakeOffQueue() {
        this.queue = new LinkedList<>();
    }

    /**
     * Adds an aircraft to the takeoff queue
     * @param aircraft aircraft object
     * @param time time added
     */
    public void enqueue(Aircraft aircraft, Instant time) {
        aircraft.markTakeoffQueue(time);
        this.queue.add(aircraft);
    }

    /**
     * Gets aircraft at front of takeoff queue without removing it
     * @return aircraft at front of queue
     */
    public Aircraft peek() {
        return this.queue.peek();
    }

    /**
     * Gets aircraft at front of takeoff queue and returns it
     * @return aircraft at front of queue
     */
    public Aircraft dequeue() {
        return this.queue.poll();
    }

    /**
     * Gets number of aircraft in the queue
     * @return number of aircraft in takeoff queue
     */
    public int size() {
        return this.queue.size();
    }

    /**
     * Checks if takeoff queue is empty
     * @return True if no aircraft is in the queue
     */
    public boolean isEmpty() {
        return this.queue.isEmpty();
    }

    /**
     * Returns the takeoff queue as a list of aircraft in the right order
     * @return list of aircraft objects
     */
    public List<Aircraft> toList() {
        List<Aircraft> planes = new ArrayList<>();
        planes.addAll(this.queue);
        return planes;
    }

    /**
     * Get string representation
     */
    @Override
    public String toString() {
        return this.queue.toString();
    }

}
