package com.group32.cs261project.backend.sim;

import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.scheduling.Scheduler;

public class SimulationContext {

    private final Airport airport;
    private final Scheduler scheduler;
    
    public SimulationContext(Airport airport, Scheduler scheduler) {
        this.airport = airport;
        this.scheduler = scheduler;
    }

    public Airport airport() {
        return this.airport;
    }

    public Scheduler scheduler() {
        return this.scheduler;
    }
    
}
