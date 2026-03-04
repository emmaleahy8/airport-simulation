package com.group32.cs261project.backend.scheduling;

import java.time.Instant;

import com.group32.cs261project.backend.events.RunwayOperationCompleteEvent;
import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.model.Runway;
import com.group32.cs261project.backend.sim.SimulationContext;
import com.group32.cs261project.backend.sim.SimulationEngine;

public class Scheduler {

    private final MixedModePolicy mixedModePolicy;


    /**
     * Constructor
     */
    public Scheduler() {
        this.mixedModePolicy = new AlternatingMixedModePolicy();
    }


    /**
     * Method to schedule a landing operation
     * @param completionTime time the operation completes
     * @param runway available runway the operation is scheduled on
     * @param engine simulation engine object
     */
    private void scheduleLandingOperation(
        Instant completionTime, Runway runway, SimulationEngine<SimulationContext> engine
    ) {

        // dequeue from holding pattern
        Aircraft aircraft = engine.context().airport().holdingPattern().dequeueNextLandingCandidate();

        // update metrics
        engine.context().metrics().onHoldingDequeue(completionTime, engine.context().airport().holdingPattern().size());

        // occupy runway
        runway.occupyUntil(completionTime);

        // schedule new runway operation completion event for when it completes
        engine.schedule(new RunwayOperationCompleteEvent(completionTime, aircraft, runway, OperationType.LANDING));
    }


    /**
     * Method to schedule a takeoff operation
     * @param completionTime time the operation completes
     * @param runway available runway the operation is scheduled on
     * @param engine simulation engine object
     */
    private void scheduleTakeoffOperation(
        Instant completionTime, Runway runway, SimulationEngine<SimulationContext> engine
    ) {

        // dequeue from takeoff queue
        Aircraft aircraft = engine.context().airport().takeOffQueue().dequeue();

        // update metrics
        engine.context().metrics().onTakeoffDequeue(completionTime, engine.context().airport().takeOffQueue().size());

        // occupy runway
        runway.occupyUntil(completionTime);

        // schedule new runway operation completion event for when it completes
        engine.schedule(new RunwayOperationCompleteEvent(completionTime, aircraft, runway, OperationType.TAKEOFF));

    }


    /**
     * Main method to schedule operations on the simulation engine
     * @param now the current time
     * @param engine simulation engine object
     */
    public void tryScheduleOperations(Instant now, SimulationEngine<SimulationContext> engine) {

        Airport airport = engine.context().airport();

        // add on runway occupancy seconds to the current time
        Instant completionTime = now.plusSeconds(engine.config().runwayOccupancySeconds());

        // iterate through all runways
        for (Runway runway : airport.runways()) {

            boolean hasArrivals = !airport.holdingPattern().isEmpty();
            boolean hasDepartures = !airport.takeOffQueue().isEmpty();

            switch (runway.mode()) {

                // runway is landing only
                case LANDING -> {
                    if (hasArrivals && runway.isAvailable(now))
                        this.scheduleLandingOperation(completionTime, runway, engine);
                }

                // runway is takeoff only
                case TAKEOFF -> {
                    if (hasDepartures && runway.isAvailable(now))
                        this.scheduleTakeoffOperation(completionTime, runway, engine);
                }

                // runway is mixed
                case MIXED -> {
                    if ((hasArrivals || hasDepartures) && runway.isAvailable(now)) {
                        switch (this.mixedModePolicy.chooseNextOp(hasArrivals, hasDepartures, runway)) {
                            case LANDING -> this.scheduleLandingOperation(completionTime, runway, engine);
                            case TAKEOFF -> this.scheduleTakeoffOperation(completionTime, runway, engine);
                        }
                    }
                }
            }
        }
    }
}
