package com.group32.cs261project.scheduling;

import java.time.Instant;

import com.group32.cs261project.airport.events.RunwayOperationCompleteEvent;
import com.group32.cs261project.model.Airport;
import com.group32.cs261project.model.Runway;
import com.group32.cs261project.sim.SimulationContext;
import com.group32.cs261project.sim.SimulationEngine;

public class Scheduler {

    public void tryScheduleOperations(Instant now, SimulationEngine<SimulationContext> engine) {

        Airport airport = engine.context().airport();
        Instant completionTime = now.plusSeconds(engine.config().runwayOccupancySeconds());

        for (Runway runway : airport.runways()) {

            switch (runway.mode()) {

                case LANDING -> {
                    if (runway.isAvailableForLanding(now)) {
                        airport.holdingPattern().dequeueNextLandingCandidate();
                        runway.occupyUntil(completionTime);
                        engine.schedule(new RunwayOperationCompleteEvent(completionTime, runway));
                    }
                }

                case TAKEOFF, MIXED -> {
                    if (runway.isAvailableForTakeOff(now)) {
                        airport.takeOffQueue().dequeue();
                        runway.occupyUntil(completionTime);
                        engine.schedule(new RunwayOperationCompleteEvent(completionTime, runway));

                    }
                }
            }
        }
    }
}
