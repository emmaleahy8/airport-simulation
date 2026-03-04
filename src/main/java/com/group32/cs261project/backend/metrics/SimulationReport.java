package com.group32.cs261project.backend.metrics;

public record SimulationReport(
    
    Integer maxHoldingSize, // maximum size of holding pattern (2b)
    Double avgHoldingTimeMinutes, // avg time in holding pattern (2b)

    Integer maxTakeoffQueueSize, // maximum size of takeoff queue (1b)
    Double avgTakeoffWaitMinutes, // avg time in takeoff queue (1b)
    
    Long maxArrivalDelayMinutes, // maximum abs(scheduled arrival time - actual arrival time) (1c)
    Double avgArrivalDelayMinutes, // avg abs(scheuled arrival time - actual arrival time) (1c)

    Long maxDepartureDelayMinutes, // maximum abs(scheduled departure time - actual departure time) (1c)
    Double avgDepartureDelayMinutes, // avg abs(scheduled departure time - actual departure time) (1c)

    Integer numDiverted, // number of planes diverted (5)
    Integer numCancellations // number of planes cancelled based on configurable max wait time (4b)
) {}
