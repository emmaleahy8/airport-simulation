package com.group32.cs261project.backend.sim;

import com.group32.cs261project.backend.metrics.MetricsCollector;
import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.scheduling.Scheduler;

public record SimulationContext(
    Airport airport,
    Scheduler scheduler,
    MetricsCollector metrics
) {}
