package com.group32.cs261project.frontend;

import java.util.function.Consumer;

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

    void pause();
    void resume();
    boolean isPaused();
}