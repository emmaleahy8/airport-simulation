package com.group32.cs261project.sim;

import com.group32.cs261project.model.Airport;
import com.group32.cs261project.scheduling.Scheduler;

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
