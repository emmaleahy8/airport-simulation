package com.group32.cs261project.metrics;

import java.time.Instant;

import com.group32.cs261project.model.Aircraft;

public class MetricsCollector {


    private int maxHoldingSize; // maximum size of holding pattern (2b)
    private double avgHoldingTimeMinutes; // avg time in holding pattern (2b)

    private int maxTakeoffQueueSize; // maximum size of takeoff queue (1b)
    private double avgTakeoffWaitMinutes; // avg time in takeoff queue (1b)
    
    private double maxArrivalDelayMinutes; // maximum abs(scheduled arrival time - actual arrival time) (1c)
    private double avgArrivalDelayMinutes; // avg abs(scheuled arrival time - actual arrival time) (1c)

    private double maxDepartureDelayMinutes; // maximum abs(scheduled departure time - actual departure time) (1c)
    private double avgDepartureDelayMinutes; // avg abs(scheduled departure time - actual departure time) (1c)

    private int numDiverted; // number of planes diverted (5)
    private int numCancellations; // number of planes cancelled based on configurable max wait time (4b)

    public void onHoldingEnqueue(Instant time, int size) {

    }

    public void onHoldingDequeue(Instant time, int size) {

    }

    public void onTakeoffEnqueue(Instant time, int size) {

    }

    public void onTakeoffDequeue(Instant time, int size) {

    }

    public void onLandingCompleted(Aircraft aircraft, Instant actualTime) {

    }

    public void onTakeoffCompleted(Aircraft aircraft, Instant actualTime) {

    }

    public void onCancelled(Aircraft aircraft, Instant time) {

    }

    public void onDiverted(Aircraft aircraft, Instant time) {

    }

    public SimulationReport buildReport() {
        return null;
    }

    
}
