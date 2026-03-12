package com.group32.cs261project.backend.compare;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.group32.cs261project.backend.metrics.SimulationReport;
import com.group32.cs261project.backend.sim.SimConfig;

/**
 * Service for comparing two completed simulation runs.
 * Produces user-facing config differences and numeric metric deltas.
 *
 * Metric delta convention:
 *   delta = rightRun value - leftRun value
 */
public class RunCompareService {

    /**
     * Compares two completed runs and returns a comparison result.
     */
    public RunCompareResult compare(RunCompareCompletedRun leftRun, RunCompareCompletedRun rightRun) {
        Objects.requireNonNull(leftRun, "leftRun");
        Objects.requireNonNull(rightRun, "rightRun");

        Map<String, String> configDifferences =
                buildConfigDifferences(leftRun.config(), rightRun.config());

        Map<String, Double> metricDeltas =
                buildMetricDeltas(leftRun.report(), rightRun.report());

        return new RunCompareResult(leftRun, rightRun, configDifferences, metricDeltas);
    }

    private Map<String, String> buildConfigDifferences(SimConfig left, SimConfig right) {
        Map<String, String> diffs = new LinkedHashMap<>();

        if (!left.startTime().equals(right.startTime())) {
            diffs.put("Start time", left.startTime() + " -> " + right.startTime());
        }

        if (!left.endTime().equals(right.endTime())) {
            diffs.put("End time", left.endTime() + " -> " + right.endTime());
        }

        long leftDurationMinutes = Duration.between(left.startTime(), left.endTime()).toMinutes();
        long rightDurationMinutes = Duration.between(right.startTime(), right.endTime()).toMinutes();
        if (leftDurationMinutes != rightDurationMinutes) {
            diffs.put("Simulation duration minutes", leftDurationMinutes + " -> " + rightDurationMinutes);
        }

        if (left.randomSeed() != right.randomSeed()) {
            diffs.put("Random seed", left.randomSeed() + " -> " + right.randomSeed());
        }

        if (Double.compare(left.inboundRatePerHour(), right.inboundRatePerHour()) != 0) {
            diffs.put("Inbound rate per hour",
                    left.inboundRatePerHour() + " -> " + right.inboundRatePerHour());
        }

        if (Double.compare(left.outboundRatePerHour(), right.outboundRatePerHour()) != 0) {
            diffs.put("Outbound rate per hour",
                    left.outboundRatePerHour() + " -> " + right.outboundRatePerHour());
        }

        if (left.runwayOccupancySeconds() != right.runwayOccupancySeconds()) {
            diffs.put("Runway occupancy seconds",
                    left.runwayOccupancySeconds() + " -> " + right.runwayOccupancySeconds());
        }

        if (left.maxTakeoffWaitMinutes() != right.maxTakeoffWaitMinutes()) {
            diffs.put("Max takeoff wait minutes",
                    left.maxTakeoffWaitMinutes() + " -> " + right.maxTakeoffWaitMinutes());
        }

        if (left.lowFuelMinutes() != right.lowFuelMinutes()) {
            diffs.put("Low fuel minutes", left.lowFuelMinutes() + " -> " + right.lowFuelMinutes());
        }

        if (left.fuelDiversionMinutes() != right.fuelDiversionMinutes()) {
            diffs.put("Fuel diversion minutes",
                    left.fuelDiversionMinutes() + " -> " + right.fuelDiversionMinutes());
        }

        if (Double.compare(left.mechanicalEmergencyProbability(), right.mechanicalEmergencyProbability()) != 0) {
            diffs.put("Mechanical emergency probability",
                    left.mechanicalEmergencyProbability() + " -> " + right.mechanicalEmergencyProbability());
        }

        if (Double.compare(left.passengerHealthEmergencyProbability(),
                right.passengerHealthEmergencyProbability()) != 0) {
            diffs.put("Passenger health emergency probability",
                    left.passengerHealthEmergencyProbability() + " -> "
                            + right.passengerHealthEmergencyProbability());
        }

        if (left.verbose() != right.verbose()) {
            diffs.put("Verbose", left.verbose() + " -> " + right.verbose());
        }

        return diffs;
    }

    private Map<String, Double> buildMetricDeltas(SimulationReport left, SimulationReport right) {
        Map<String, Double> deltas = new LinkedHashMap<>();

        deltas.put("Max holding size",
                delta(left.maxHoldingSize(), right.maxHoldingSize()));
        deltas.put("Avg holding time minutes",
                delta(left.avgHoldingTimeMinutes(), right.avgHoldingTimeMinutes()));

        deltas.put("Max takeoff queue size",
                delta(left.maxTakeoffQueueSize(), right.maxTakeoffQueueSize()));
        deltas.put("Avg takeoff wait minutes",
                delta(left.avgTakeoffWaitMinutes(), right.avgTakeoffWaitMinutes()));

        deltas.put("Max arrival delay minutes",
                delta(left.maxArrivalDelayMinutes(), right.maxArrivalDelayMinutes()));
        deltas.put("Avg arrival delay minutes",
                delta(left.avgArrivalDelayMinutes(), right.avgArrivalDelayMinutes()));

        deltas.put("Max departure delay minutes",
                delta(left.maxDepartureDelayMinutes(), right.maxDepartureDelayMinutes()));
        deltas.put("Avg departure delay minutes",
                delta(left.avgDepartureDelayMinutes(), right.avgDepartureDelayMinutes()));

        deltas.put("Number diverted",
                delta(left.numDiverted(), right.numDiverted()));
        deltas.put("Number of cancellations",
                delta(left.numCancellations(), right.numCancellations()));

        return deltas;
    }

    /**
     * Returns right - left.
     * Null values are treated as 0.0 so comparisons remain robust for empty metrics.
     */
    private double delta(Number left, Number right) {
        double leftValue = (left == null) ? 0.0 : left.doubleValue();
        double rightValue = (right == null) ? 0.0 : right.doubleValue();
        return rightValue - leftValue;
    }
}