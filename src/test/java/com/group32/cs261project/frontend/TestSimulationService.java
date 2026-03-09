package com.group32.cs261project.frontend;

import java.util.function.Consumer;

public class TestSimulationService implements SimulationService {

    private boolean paused = false;

    @Override
    public void start(SimulationData config, Consumer<SimulationData> onSnapshot) {}

    @Override
    public void updateConfig(SimulationData newConfig) {}

    @Override
    public SimulationData stop() {
        return new SimulationData();
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void resume() {
        paused = false;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }
}