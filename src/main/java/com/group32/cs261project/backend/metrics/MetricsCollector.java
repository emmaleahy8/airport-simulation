package com.group32.cs261project.backend.metrics;

import java.time.Duration;
import java.time.Instant;

import com.group32.cs261project.backend.model.Aircraft;


/**
 * Class that collects metrics for the simulation
 */
public class MetricsCollector {

    private final MaxAndAverageCollector<Integer> holdingSizeCollector;
    private final MaxAndAverageCollector<Long> holdingTimeMinutesCollector;

    private final MaxAndAverageCollector<Integer> takeoffQueueSizeCollector;
    private final MaxAndAverageCollector<Long> takeoffTimeWaitMinutesCollector;

    private final MaxAndAverageCollector<Long> arrivalDelayMinutesCollector;
    private final MaxAndAverageCollector<Long> departureDelayMinutesCollector;

    private int numDiverted;
    private int numCancellations;


    /**
     * Constructor
     */
    public MetricsCollector() {
        this.holdingSizeCollector = new MaxAndAverageCollector<>();
        this.holdingTimeMinutesCollector = new MaxAndAverageCollector<>();
        this.takeoffQueueSizeCollector = new MaxAndAverageCollector<>();
        this.takeoffTimeWaitMinutesCollector = new MaxAndAverageCollector<>();
        this.arrivalDelayMinutesCollector = new MaxAndAverageCollector<>();
        this.departureDelayMinutesCollector = new MaxAndAverageCollector<>();
        this.numDiverted = 0;
        this.numCancellations = 0;
    }


    /**
     * Method called when aircraft is enqueued to holding pattern and updates metrics
     * @param time time aircraft is enqueued
     * @param size new size of holding pattern
     */
    public void onHoldingEnqueue(Instant time, int size) {
        this.holdingSizeCollector.addEntry(size);
    }


    /**
     * Method called when aircraft is dequeued from holding pattern and updates metrics
     * @param time time aircraft is dequeued
     * @param size new size of holding pattern
     */
    public void onHoldingDequeue(Instant time, int size) {
        this.holdingSizeCollector.addEntry(size);
    }


    /**
     * Method called when aircraft is enqueued to takeoff queue and updates metrics
     * @param time time aircraft is enqueued
     * @param size new size of takeoff queue
     */
    public void onTakeoffEnqueue(Instant time, int size) {
        this.takeoffQueueSizeCollector.addEntry(size);
    }


    /**
     * Method called when aircraft is dequeued from takeoff queue and updates metrics
     * @param time time aircraft is dequeued
     * @param size new size of takeoff queue
     */
    public void onTakeoffDequeue(Instant time, int size) {
        this.takeoffQueueSizeCollector.addEntry(size);
    }


    /**
     * Method called when aircraft completes landing on a runway, and updates metrics
     * @param aircraft aircraft object
     * @param actualTime time aircraft completes landing (not equal to time it enters runway)
     */
    public void onLandingCompleted(Aircraft aircraft, Instant actualTime) {
        this.holdingTimeMinutesCollector.addEntry(
            Duration.between(aircraft.holdingPatternEntryTime(), actualTime).toMinutes()
        );
        this.arrivalDelayMinutesCollector.addEntry(
            Duration.between(aircraft.scheduledTime(), actualTime).toMinutes()
        );
    }


    /**
     * Method called when aircraft completes takeoff on a runway, and updates metrics
     * @param aircraft aircraft object
     * @param actualTime time aircraft completes takeoff (not equal to time it enters runway)
     */
    public void onTakeoffCompleted(Aircraft aircraft, Instant actualTime) {
        this.takeoffTimeWaitMinutesCollector.addEntry(
            Duration.between(aircraft.takeOffQueueEntryTime(), actualTime).toMinutes()
        );
        this.departureDelayMinutesCollector.addEntry(
            Duration.between(aircraft.scheduledTime(), actualTime).toMinutes()
        );
    }


    /**
     * Method called when aircraft is cancelled and updates metrics
     * @param aircraft aircraft being cancelled
     * @param time time aircraft is cancelled
     */
    public void onCancelled(Aircraft aircraft, Instant time) {
        this.numCancellations++;
    }


    /**
     * Method called when aircraft is diverted and updates metrics
     * @param aircraft aircraft being diverted
     * @param time time aircraft is diverted
     */
    public void onDiverted(Aircraft aircraft, Instant time) {
        this.numDiverted++;
    }


    /**
     * Generates a simulation report for the simulation after it is complete
     * @param numRunways number of runways in the simulation
     * @return a simulation report containing all the data
     */
    public SimulationReport buildReport(int numRunways) {
        return new SimulationReport(
            numRunways,
            holdingSizeCollector.getMax(),
            holdingTimeMinutesCollector.getAverage(),
            takeoffQueueSizeCollector.getMax(),
            takeoffTimeWaitMinutesCollector.getAverage(),
            arrivalDelayMinutesCollector.getMax(),
            arrivalDelayMinutesCollector.getAverage(),
            departureDelayMinutesCollector.getMax(),
            departureDelayMinutesCollector.getAverage(),
            numDiverted,
            numCancellations
        );
    }
}