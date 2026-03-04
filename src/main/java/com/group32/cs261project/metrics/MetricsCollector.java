package com.group32.cs261project.metrics;

import java.time.Instant;

import com.group32.cs261project.backend.model.Aircraft;

class MaxAndAverageCollector<T extends Number & Comparable<T>> {

    private double sum = 0;
    private T max = null;
    private int count = 0;

    public void addEntry(T value) {
        sum += value.doubleValue();

        if (max == null || value.compareTo(max) > 0) {
            max = value;
        }

        count++;
    }

    public double getAverage() {
        if (count == 0) {
            throw new IllegalStateException("No entries");
        }
        return sum / count;
    }

    public T getMax() {
        if (max == null) {
            throw new IllegalStateException("No entries");
        }
        return max;
    }
}


public class MetricsCollector {


    private final MaxAndAverageCollector<Integer> holdingSizeCollector; // maximum size of holding pattern (2b)
    private final MaxAndAverageCollector<Double> holdingTimeMinutesCollector; // avg time in holding pattern (2b)

    private final MaxAndAverageCollector<Integer> takeoffQueueSizeCollector; // maximum size of takeoff queue (1b)
    private final MaxAndAverageCollector<Double> takeoffTimeWaitMinutesCollector; // avg time in takeoff queue (1b)

    private final MaxAndAverageCollector<Double> arrivalDelayMinutesCollector; // maximum & average abs(scheduled arrival time - actual arrival time) (1c)

    private final MaxAndAverageCollector<Double> departureDelayMinutesCollector; // maximum & average abs(scheduled departure time - actual departure time) (1c)

    private int numDiverted; // number of planes diverted (5)
    private int numCancellations; // number of planes cancelled based on configurable max wait time (4b)


    /**
     * Constructor initialises all values
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

    public void onHoldingEnqueue(Instant time, int size) {
        this.holdingSizeCollector.addEntry(size);
    }

    public void onHoldingDequeue(Instant time, int size) {
        this.holdingSizeCollector.addEntry(size);
    }

    public void onTakeoffEnqueue(Instant time, int size) {
        this.takeoffQueueSizeCollector.addEntry(size);
    }

    public void onTakeoffDequeue(Instant time, int size) {
        this.takeoffQueueSizeCollector.addEntry(size);
    }

    public void onLandingCompleted(Aircraft aircraft, Instant actualTime) {

    }

    public void onTakeoffCompleted(Aircraft aircraft, Instant actualTime) {

    }

    public void onCancelled(Aircraft aircraft, Instant time) {
        this.numCancellations++;
    }

    public void onDiverted(Aircraft aircraft, Instant time) {
        this.numDiverted++;
    }

    public SimulationReport buildReport() {
        return null;
    }

    
}
