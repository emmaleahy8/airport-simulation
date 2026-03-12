package com.group32.cs261project.backend.compare;

import java.time.Instant;
import java.util.Objects;

import com.group32.cs261project.backend.metrics.SimulationReport;
import com.group32.cs261project.backend.sim.SimConfig;

/**
 * Immutable record representing one completed simulation run.
 * Stores only lightweight summary data needed for history/comparison.
 */
public record RunCompareCompletedRun(
    String runId,
    Instant completedAt,
    SimConfig config,
    SimulationReport report
) {
    public RunCompareCompletedRun {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(completedAt, "completedAt");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(report, "report");

        if (runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
    }
}