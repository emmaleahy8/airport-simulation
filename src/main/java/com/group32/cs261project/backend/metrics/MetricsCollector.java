package com.group32.cs261project.backend.metrics;

import java.time.Duration;
import java.time.Instant;

import com.group32.cs261project.backend.model.Aircraft;

public class MetricsCollector {

    private final MaxAndAverageCollector<Integer> holdingSizeCollector;
    private final MaxAndAverageCollector<Long> holdingTimeMinutesCollector;

    private final MaxAndAverageCollector<Integer> takeoffQueueSizeCollector;
    private final MaxAndAverageCollector<Long> takeoffTimeWaitMinutesCollector;

    private final MaxAndAverageCollector<Long> arrivalDelayMinutesCollector;
    private final MaxAndAverageCollector<Long> departureDelayMinutesCollector;

    private int numDiverted;
    private int numCancellations;

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
        this.holdingTimeMinutesCollector.addEntry(
            Duration.between(aircraft.holdingPatternEntryTime(), actualTime).toMinutes()
        );
        this.arrivalDelayMinutesCollector.addEntry(
            Duration.between(aircraft.scheduledTime(), actualTime).toMinutes()
        );
    }

    public void onTakeoffCompleted(Aircraft aircraft, Instant actualTime) {
        this.takeoffTimeWaitMinutesCollector.addEntry(
            Duration.between(aircraft.takeOffQueueEntryTime(), actualTime).toMinutes()
        );
        this.departureDelayMinutesCollector.addEntry(
            Duration.between(aircraft.scheduledTime(), actualTime).toMinutes()
        );
    }

    public void onCancelled(Aircraft aircraft, Instant time) {
        this.numCancellations++;
    }

    public void onDiverted(Aircraft aircraft, Instant time) {
        this.numDiverted++;
    }

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