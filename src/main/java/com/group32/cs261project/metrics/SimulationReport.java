package com.group32.cs261project.metrics;

public record SimulationReport(
    
    int maxHoldingSize, // maximum size of holding pattern (2b)
    double avgHoldingTimeMinutes, // avg time in holding pattern (2b)

    int maxTakeoffQueueSize, // maximum size of takeoff queue (1b)
    double avgTakeoffWaitMinutes, // avg time in takeoff queue (1b)
    
    double maxArrivalDelayMinutes, // maximum abs(scheduled arrival time - actual arrival time) (1c)
    double avgArrivalDelayMinutes, // avg abs(scheuled arrival time - actual arrival time) (1c)

    double maxDepartureDelayMinutes, // maximum abs(scheduled departure time - actual departure time) (1c)
    double avgDepartureDelayMinutes, // avg abs(scheduled departure time - actual departure time) (1c)

    int numDiverted, // number of planes diverted (5)
    int numCancellations // number of planes cancelled based on configurable max wait time (4b)
) {}
