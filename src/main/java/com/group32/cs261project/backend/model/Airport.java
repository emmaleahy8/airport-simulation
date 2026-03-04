package com.group32.cs261project.backend.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;
import com.group32.cs261project.backend.queues.HoldingPattern;
import com.group32.cs261project.backend.queues.TakeOffQueue;

/**
 * Class for Airport
 */
public class Airport {

    private final List<Runway> runways;
    private final HoldingPattern holdingPattern;
    private final TakeOffQueue takeOffQueue;

    /**
     * Constructor
     * @param runways A list of runway objects
     */
    public Airport(List<RunwayData> runways) {

        this.runways = IntStream.range(0, runways.size())
            .mapToObj(i -> new Runway(
                i, 
                runways.get(i).lengthM(),
                runways.get(i).bearingDeg(),
                runways.get(i).mode(),
                runways.get(i).status()
            )).toList();

        this.holdingPattern = new HoldingPattern();
        this.takeOffQueue = new TakeOffQueue();
    }

    /**
     * Getter for list of runways
     * @return list of runways
     */
    public List<Runway> runways() {
        return this.runways;
    }

    /**
     * Getter for holding pattern
     * @return holding pattern object
     */
    public HoldingPattern holdingPattern() {
        return this.holdingPattern;
    }

    /**
     * Getter for take off queue
     * @return takeoff queue object
     */
    public TakeOffQueue takeOffQueue() {
        return this.takeOffQueue;
    }

    /**
     * Gets runway of given runway number in the airport
     * @param runwayNumber runway number
     * @return corresponding runway at the airport
     */
    public Runway getRunway(int runwayNumber) {
        return this.runways.get(runwayNumber);
    }

    /**
     * Set status of runway with given runway number
     * @param runwayNumber runway number
     * @param status one of LANDING, TAKEOFF, MIXED 
     */
    public void setRunwayStatus(int runwayNumber, RunwayStatus status) {
        this.getRunway(runwayNumber).setStatus(status);
    }

    /**
     * Set mode of runway with given runway number
     * @param runwayNumber runway number
     * @param mode one of AVAILABLE, INSPECTION, SNOW_CLEARANCE, EQUIPMENT_FAILURE
     */
    public void setRunwayMode(int runwayNumber, RunwayMode mode) {
        this.getRunway(runwayNumber).setMode(mode);
    }

    /**
     * Finds an available landing runway for landing at the given time
     * @param time datetime to check
     * @return an available runway or null if none available
     */
    public Optional<Runway> findAvailableLandingRunway(Instant time) {
        for (Runway runway : this.runways) {
            if (runway.isAvailableForLanding(time)) {
                return Optional.of(runway);
            }
        }
        return Optional.empty();
    }

    /**
     * Finds an available runway for takeoff at the given time
     * @param time datetime to check
     * @return an available runway or null if none available
     */
    public Optional<Runway> findAvailableTakeoffRunway(Instant time) {
        for (Runway runway : this.runways) {
            if (runway.isAvailableForTakeOff(time)) {
                return Optional.of(runway);
            }
        }
        return Optional.empty();
    }
}
