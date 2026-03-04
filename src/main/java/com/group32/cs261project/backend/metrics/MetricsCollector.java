package com.group32.cs261project.backend.metrics;

import java.time.Duration;
import java.time.Instant;

import com.group32.cs261project.backend.model.Aircraft;


public class MetricsCollector {


    private final MaxAndAverageCollector<Integer> holdingSizeCollector; // maximum size of holding pattern (2b)
    private final MaxAndAverageCollector<Long> holdingTimeMinutesCollector; // avg time in holding pattern (2b)

    private final MaxAndAverageCollector<Integer> takeoffQueueSizeCollector; // maximum size of takeoff queue (1b)
    private final MaxAndAverageCollector<Long> takeoffTimeWaitMinutesCollector; // avg time in takeoff queue (1b)

    private final MaxAndAverageCollector<Long> arrivalDelayMinutesCollector; // maximum & average abs(scheduled arrival time - actual arrival time) (1c)

    private final MaxAndAverageCollector<Long> departureDelayMinutesCollector; // maximum & average abs(scheduled departure time - actual departure time) (1c)

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

    public SimulationReport buildReport() {
        return new SimulationReport(
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
