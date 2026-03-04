package com.group32.cs261project.backend.events;

import java.time.Instant;

import com.group32.cs261project.backend.sim.SimulationEngine;

/**
 * Interface for events to implement
 */
public interface Event<C> {
    Instant time();
    void handle(SimulationEngine<C> engine);
}
