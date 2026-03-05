package com.group32.cs261project.frontend;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import com.group32.cs261project.backend.AirportSimulation;

public class BackendSimulationService implements SimulationService {

    private volatile boolean running = false;
    private ScheduledExecutorService executor = null;

    private Consumer<SimulationData> onSnapshot;
    private SimulationData initialConfig;
    private AirportSimulation backendSim;
    private Instant startTime, endTime;

    @Override
    public void start(SimulationData config, Consumer<SimulationData> onSnapshot) {

        if (this.running)
            return;

        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.onSnapshot = onSnapshot;
        this.initialConfig = config;



    }

    @Override
    public void updateConfig(SimulationData newConfig) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateConfig'");
    }

    @Override
    public SimulationData stop() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'stop'");
    }

    @Override
    public boolean isRunning() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isRunning'");
    }
    
}
