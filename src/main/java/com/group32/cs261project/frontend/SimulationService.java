package com.group32.cs261project.frontend;

import java.util.function.Consumer;

import com.group32.cs261project.backend.compare.RunCompareHistory;

/**
 * Interface defining the frontend-facing simulation control service.
 *
 * Implementations of this service manage the lifecycle of the airport
 * simulation, including starting, stopping, pausing, and updating the
 * simulation configuration. The service also provides snapshot updates
 * for the UI and maintains a history of completed runs for comparison.
 */
public interface SimulationService {

    /**
     * Start a simulation based on config. Snapshots are delivered via onSnapshot
     * 
     * @param config UI-facing config
     * @param onSnapshot function that takes a snapshot and returns nothing
     */
    void start(SimulationData config, Consumer<SimulationData> onSnapshot);

    /**
     * Apply updated config while running 
     * 
     * @param newConfig new UI-facing config
     */
    void updateConfig(SimulationData newConfig);

    /**
     * Stop simulation and return a final report snapshot 
     * 
     * @return final report snapshot
     */
    SimulationData stop();

    /**
     * gets whether the simulation service is running
     * 
     * @return true if the simulation is running
     */
    boolean isRunning();

    /**
     * Pause the simulation
     */
    void pause();

    /**
     * Resume the simulation
     */
    void resume();

    /**
     * Returns whether the simulation is currently paused
     * 
     * @return true if the simulation is paused
     */
    boolean isPaused();

    /**
     * Returns the stored run comparison history.
     * The history contains the most recent completed runs.
     * 
     * @return the run comparison history
     */
    RunCompareHistory getRunCompareHistory();

    /**
     * Sets the playback speed of the simulation.
     *
     * @param speed the playback speed multiplier
     */
    void setPlaybackSpeed(double speed);
}