package com.group32.cs261project.backend.compare;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable comparison result for two completed simulation runs.
 * Holds both runs, user-facing config differences, and numeric metric deltas.
 */
public record RunCompareResult(
    RunCompareCompletedRun leftRun,
    RunCompareCompletedRun rightRun,
    Map<String, String> configDifferences,
    Map<String, Double> metricDeltas
) {
    public RunCompareResult {
        Objects.requireNonNull(leftRun, "leftRun");
        Objects.requireNonNull(rightRun, "rightRun");
        Objects.requireNonNull(configDifferences, "configDifferences");
        Objects.requireNonNull(metricDeltas, "metricDeltas");

        configDifferences = Map.copyOf(configDifferences);
        metricDeltas = Map.copyOf(metricDeltas);
    }
}