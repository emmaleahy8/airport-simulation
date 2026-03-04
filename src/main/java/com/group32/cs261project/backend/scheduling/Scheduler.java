package com.group32.cs261project.backend.scheduling;

import java.time.Instant;

import com.group32.cs261project.backend.events.RunwayOperationCompleteEvent;
import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.model.Runway;
import com.group32.cs261project.backend.sim.SimulationContext;
import com.group32.cs261project.backend.sim.SimulationEngine;

public class Scheduler {

    private final MixedModePolicy mixedModePolicy;

    public Scheduler() {
        this.mixedModePolicy = new AlternatingMixedModePolicy();
    }

    private void scheduleLandingOperation(
        Instant completionTime, Runway runway, SimulationEngine<SimulationContext> engine
    ) {
        engine.context().airport().holdingPattern().dequeueNextLandingCandidate();
        runway.occupyUntil(completionTime);
        engine.schedule(new RunwayOperationCompleteEvent(completionTime, runway));
    }

    private void scheduleTakeoffOperation(
        Instant completionTime, Runway runway, SimulationEngine<SimulationContext> engine
    ) {
        engine.context().airport().takeOffQueue().dequeue();
        runway.occupyUntil(completionTime);
        engine.schedule(new RunwayOperationCompleteEvent(completionTime, runway));

    }

    public void tryScheduleOperations(Instant now, SimulationEngine<SimulationContext> engine) {

        Airport airport = engine.context().airport();
        Instant completionTime = now.plusSeconds(engine.config().runwayOccupancySeconds());

        for (Runway runway : airport.runways()) {

            boolean hasArrivals = !airport.holdingPattern().isEmpty();
            boolean hasDepartures = !airport.takeOffQueue().isEmpty();

            switch (runway.mode()) {
                case LANDING -> {
                    if (hasArrivals && runway.isAvailable(now))
                        this.scheduleLandingOperation(completionTime, runway, engine);
                }
                case TAKEOFF -> {
                    if (hasDepartures && runway.isAvailable(now))
                        this.scheduleTakeoffOperation(completionTime, runway, engine);
                }
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
