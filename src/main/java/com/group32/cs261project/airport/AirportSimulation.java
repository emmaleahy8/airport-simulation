package com.group32.cs261project.airport;

import java.time.Instant;

import com.group32.cs261project.model.Airport;
import com.group32.cs261project.scheduling.Scheduler;
import com.group32.cs261project.sim.SimConfig;
import com.group32.cs261project.sim.SimulationContext;
import com.group32.cs261project.sim.SimulationEngine;
import com.group32.cs261project.sim.queue.PriorityEventQueue;

/**
 * Main class for the backend airport simulation
 */
public class AirportSimulation {

    private final SimConfig config;
    private final Airport airport;
    private final SimulationEngine<SimulationContext> engine;

    /**
     * Constructor
     * @param config Configuration for the airport simulation
     * @param airport airport object
     */
    public AirportSimulation(SimConfig config, Airport airport) {
        this.config = config;
        this.airport = airport;
        this.engine = new SimulationEngine<>(config, new SimulationContext(airport, new Scheduler()), new PriorityEventQueue<>());
    }

    /**
     * Getter for airport
     * @return airport object
     */
    public Airport airport() {
        return this.airport;
    }

    /**
     * Getter for engine
     * @return simulation engine object
     */
    public SimulationEngine<SimulationContext> engine() {
        return this.engine;
    }

    /**
     * Getter for simulation configuration
     * @return config object
     */
    public SimConfig config() {
        return this.config;
    }

    /**
     * Run until config end time
     */
    public void run() {
        this.engine.runUntil(this.config.endTime());
    }

    /**
     * Run until a specific end time
     * @param endTime specific end time
     */
    public void runUntil(Instant endTime) {
        this.engine.runUntil(endTime);
    }

    /**
     * Step the simulation one step
     * @return true if can step, false if completed
     */
    public boolean step() {
        return this.engine.step();
    }
    
}
