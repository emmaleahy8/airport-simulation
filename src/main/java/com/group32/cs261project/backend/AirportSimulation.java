package com.group32.cs261project.backend;

import java.time.Instant;

import com.group32.cs261project.backend.metrics.MetricsCollector;
import com.group32.cs261project.backend.metrics.SimulationReport;
import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.scheduling.Scheduler;
import com.group32.cs261project.backend.sim.EventQueue;
import com.group32.cs261project.backend.sim.SimConfig;
import com.group32.cs261project.backend.sim.SimulationContext;
import com.group32.cs261project.backend.sim.SimulationEngine;

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
        this.engine = new SimulationEngine<>(config, new SimulationContext(airport, new Scheduler(), new MetricsCollector()), new EventQueue<>());
    }

    /* getters */
    public Airport airport() { return this.airport; }
    public SimulationEngine<SimulationContext> engine() { return this.engine; }
    public SimConfig config() { return this.config; }

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

    /**
     * Build simulation report
     * @return simulation report object
     */
    public SimulationReport buildReport() {
        return this.engine.context().metrics().buildReport(this.airport.runways().size());
    }

}
