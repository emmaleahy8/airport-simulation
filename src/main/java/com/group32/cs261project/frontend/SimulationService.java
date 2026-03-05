package com.group32.cs261project.frontend;

import java.util.function.Consumer;

public interface SimulationService {
    // Start a simulation based on config. Snapshots are delivered via onSnapshot
    void start(SimulationData config, Consumer<SimulationData> onSnapshot);

    // Apply updated config while running 
    void updateConfig(SimulationData newConfig);

    // Stop simulation and return a final report snapshot 
    SimulationData stop();

    boolean isRunning();
}