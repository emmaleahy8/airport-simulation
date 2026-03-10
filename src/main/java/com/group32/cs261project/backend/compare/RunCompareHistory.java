package com.group32.cs261project.backend.compare;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Stores a bounded history of completed simulation runs.
 * Keeps only the most recent 20 runs and discards older ones.
 */
public class RunCompareHistory {

    private static final int MAX_RUNS = 20;

    private final List<RunCompareCompletedRun> runs = new ArrayList<>();

    /**
     * Adds a completed run to the history.
     * If capacity is exceeded, the oldest run is removed.
     */
    public void addRun(RunCompareCompletedRun run) {
        runs.add(Objects.requireNonNull(run, "run"));

        while (runs.size() > MAX_RUNS) {
            runs.remove(0);
        }
    }

    /**
     * Returns an unmodifiable view of the stored runs.
     */
    public List<RunCompareCompletedRun> getAllRuns() {
        return Collections.unmodifiableList(runs);
    }

    /**
     * Returns the most recent run, or null if none exist.
     */
    public RunCompareCompletedRun getLatestRun() {
        if (runs.isEmpty()) {
            return null;
        }
        return runs.get(runs.size() - 1);
    }

    /**
     * Returns the run before the most recent one,
     * or null if fewer than two runs exist.
     */
    public RunCompareCompletedRun getPreviousRun() {
        if (runs.size() < 2) {
            return null;
        }
        return runs.get(runs.size() - 2);
    }

    /**
     * Returns the number of stored runs.
     */
    public int size() {
        return runs.size();
    }

    /**
     * Returns true if the history contains at least the specified number of runs.
     */
    public boolean hasAtLeast(int count) {
        return runs.size() >= count;
    }

    /**
     * Returns the fixed maximum number of runs retained.
     */
    public int maxRuns() {
        return MAX_RUNS;
    }
}