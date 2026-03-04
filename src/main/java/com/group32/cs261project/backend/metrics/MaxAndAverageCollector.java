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

    public Double getAverage() {
        if (isEmpty())
            return null;
        return sum / count;
    }

    public T getMax() {
        if (isEmpty())
            return null;
        return max;
    }

    public boolean isEmpty() {
        return count == 0;
    }
}
