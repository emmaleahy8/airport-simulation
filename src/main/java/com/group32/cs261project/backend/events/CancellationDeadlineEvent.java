package com.group32.cs261project.backend.events;

import java.time.Duration;
import java.time.Instant;

import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.enums.AircraftState;
import com.group32.cs261project.backend.sim.SimulationContext;
import com.group32.cs261project.backend.sim.SimulationEngine;

/**
 * Event that enforces the maximum wait time for outbound aircraft
 * in the take-off queue. If, when this event fires, the aircraft
 * is still waiting in the take-off queue and has exceeded the
 * configured maximum wait time, it is cancelled and counted in
 * the metrics, as required by FC-11 / OMC-05.
 */
public class CancellationDeadlineEvent implements Event<SimulationContext> {

    private final Instant time;
    private final Aircraft aircraft;

    public CancellationDeadlineEvent(Instant time, Aircraft aircraft) {
        this.time = time;
        this.aircraft = aircraft;
    }

    @Override
    public Instant time() {
        return this.time;
    }

    @Override
    public void handle(SimulationEngine<SimulationContext> engine) {

        // If the aircraft is no longer in the take-off queue, do nothing.
        if (this.aircraft.state() != AircraftState.TAKEOFF_QUEUE) {
            return;
        }

        // Compute how long the aircraft has been waiting in the take-off queue.
        if (this.aircraft.takeOffQueueEntryTime() == null) {
            return;
        }

        long waitMinutes = Duration.between(this.aircraft.takeOffQueueEntryTime(), this.time).toMinutes();
        long maxWait = engine.config().maxTakeoffWaitMinutes();

        // Cancel when wait has reached or exceeded the threshold (event fires at exactly entry + maxWait).
        if (waitMinutes < maxWait) {
            return;
        }

        // Remove from queue if still present.
        boolean removed = engine.context().airport().takeOffQueue().remove(this.aircraft);
        if (!removed) {
            // It has likely just been dequeued for take-off; treat as no-op.
            return;
        }

        // Mark as cancelled and record in metrics.
        this.aircraft.markCancelled();
        engine.context().metrics().onCancelled(this.aircraft, this.time);
    }
}

