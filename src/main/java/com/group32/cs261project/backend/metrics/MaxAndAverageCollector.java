package com.group32.cs261project.backend.metrics;


/**
 * Generic class that collects maximum and average of numeric values over time
 */
public class MaxAndAverageCollector<T extends Number & Comparable<T>> {

    private double sum = 0; // running sum
    private T max = null; // running maximum
    private int count = 0; // number of elements considered


    /**
     * Add an entry to the data structure
     * Time complexity: O(1)
     * @param value numerical value
     */
    public void addEntry(T value) {
        sum += value.doubleValue();

        if (max == null || value.compareTo(max) > 0) {
            max = value;
        }

        count++;
    }


    /**
     * Get the average (time complexity: O(1))
     * @return average if exists, otherwise null
     */
    public Double getAverage() {
        if (isEmpty())
            return null;
        return sum / count;
    }


    /**
     * Get the maximum value (time complexity: O(1))
     * @return maximum if exists, otherwise null
     */
    public T getMax() {
        if (isEmpty())
            return null;
        return max;
    }


    /**
     * Checks if 0 elements have been considered
     * @return true if count == 0
     */
    public boolean isEmpty() {
        return count == 0;
    }
}
