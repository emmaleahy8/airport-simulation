package com.group32.cs261project.backend.queues;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.group32.cs261project.backend.model.Aircraft;

public class HoldingPattern {

    private final Queue<Aircraft> emergencyQueue;
    private final Queue<Aircraft> normalQueue;

    /**
     * Constructor
     */
    public HoldingPattern() {
        this.emergencyQueue = new LinkedList<>(); // landing always prefers this queue
        this.normalQueue = new LinkedList<>();
    }

    /**
     * Enqueue an aircraft to the holding pattern given its time
     */
    public void enqueue(Aircraft aircraft, Instant time) {
        
        aircraft.markHolding(time);

        if (aircraft.isEmergency()) {
            this.emergencyQueue.add(aircraft);
        } else {
            this.normalQueue.add(aircraft);
        }
    }

    /**
     * Gets number of aircraft in the holding pattern
     * @return Number of aircraft in holding pattern
     */
    public int size() {
        return this.emergencyQueue.size() + this.normalQueue.size();
    }

    /**
     * Checks if the holding pattern is empty
     * @return True if the holding pattern is empty
     */
    public boolean isEmpty() {
        return this.emergencyQueue.isEmpty() && this.normalQueue.isEmpty();
    }

    /**
     * Gets the next landing candidate without removing it
     * @return aircraft to land next
     */
    public Aircraft peekNextLandingCandidate() {
        if (!this.emergencyQueue.isEmpty()) {
            return this.emergencyQueue.peek();
        } else {
            return this.normalQueue.peek();
        }
    }

    /**
     * Removes the next landing candidate and returns it
     * @return aircraft to land next
     */
    public Aircraft dequeueNextLandingCandidate() {
        if (!this.emergencyQueue.isEmpty()) {
            return this.emergencyQueue.poll();
        } else {
            return this.normalQueue.poll();
        }
    }

    /**
     * Returns the holding pattern as a list of planes in the right order
     * @return list of aircraft objects
     */
    public List<Aircraft> toList() {
        List<Aircraft> planes = new ArrayList<>();
        planes.addAll(this.emergencyQueue);
        planes.addAll(this.normalQueue);
        return planes;
    }

    /**
     * Get string representation
     */
    @Override
    public String toString() {
        return String.format("(%s, %s)", this.emergencyQueue.toString(), this.normalQueue.toString());
    }


    /**
     * consumes fuel for all waiting aircraft
     * checks fuel
     * escalates emergency
     * moves low fuel aircarft to the emergency queue
     * 
     */
    public void updateFuelStates(double thresholdMinutes, double minutesPassed) {

        for (Aircraft aircraft : normalQueue) {
    
            aircraft.consumeFuelMinutes(minutesPassed);
            aircraft.updateFuelEmergency(thresholdMinutes);
    
            if (aircraft.isEmergency()) {
                emergencyQueue.add(aircraft);
            }
        }
    
        normalQueue.removeIf(Aircraft::isEmergency);
    
        for (Aircraft aircraft : emergencyQueue) {
            aircraft.consumeFuelMinutes(minutesPassed);
        }
    }

}
