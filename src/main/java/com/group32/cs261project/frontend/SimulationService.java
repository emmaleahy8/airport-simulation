package com.group32.cs261project.frontend;

import java.util.function.Consumer;

import com.group32.cs261project.backend.compare.RunCompareHistory;

public interface SimulationService {

    /**
     * Start a simulation based on config. Snapshots are delivered via onSnapshot
     * @param config UI-facing config
     * @param onSnapshot function that takes a snapshot and returns nothing
     */
    void start(SimulationData config, Consumer<SimulationData> onSnapshot);

    /**
     * Apply updated config while running 
     * @param newConfig new UI-facing config
     */
    void updateConfig(SimulationData newConfig);

    /**
     * Stop simulation and return a final report snapshot 
     * @return final report snapshot
     */
    SimulationData stop();

    /**
     * gets whether the simulation service is running
     * @return boolean
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
     */
    boolean isPaused();

    /**
     * Returns the stored run comparison history.
     * The history contains the most recent completed runs.
     */
    RunCompareHistory getRunCompareHistory();

    /*
     * Changes the speed of the simulation
     */
    void setPlaybackSpeed(double speed);
}