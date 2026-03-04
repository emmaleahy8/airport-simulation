package com.group32.cs261project.backend.metrics;

public class MaxAndAverageCollector<T extends Number & Comparable<T>> {

    private double sum = 0;
    private T max = null;
    private int count = 0;

    public void addEntry(T value) {
        sum += value.doubleValue();

        if (max == null || value.compareTo(max) > 0) {
            max = value;
        }

        count++;
    }

    public double getAverage() {
        if (count == 0) {
            throw new IllegalStateException("No entries");
        }
        return sum / count;
    }

    public T getMax() {
        if (max == null) {
            throw new IllegalStateException("No entries");
        }
        return max;
    }
}
