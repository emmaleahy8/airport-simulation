package com.group32.cs261project.backend.metrics;


/**
 * Simulation report class containing data to be displayed on the UI
 * data fields map to requirements in the specification
 */
public record SimulationReport(

    Integer numRunways,

    Integer maxHoldingSize, // maximum size of holding pattern (2b)
    Double avgHoldingTimeMinutes, // avg time in holding pattern (2b)

    Integer maxTakeoffQueueSize, // maximum size of takeoff queue (1b)
    Double avgTakeoffWaitMinutes, // avg time in takeoff queue (1b)

    Long maxArrivalDelayMinutes, // maximum actual arrival time - scheduled arrival time (1c)
    Double avgArrivalDelayMinutes, // avg actual arrival time - scheduled arrival time (1c)

    Long maxDepartureDelayMinutes, // maximum actual departure time - scheduled departure time (1c)
    Double avgDepartureDelayMinutes, // avg actual departure time - scheduled departure time (1c)

    Integer numDiverted, // number of planes diverted (5)
    Integer numCancellations // number of planes cancelled based on configurable max wait time (4b)
) {}