package com.group32.cs261project.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.metrics.MaxAndAverageCollector;

class MaxAndAverageCollectorTest {

    @Test
    void getAverageAndMaxWork() {
        MaxAndAverageCollector<Integer> c = new MaxAndAverageCollector<>();
        c.addEntry(2);
        c.addEntry(10);
        c.addEntry(4);

        assertEquals(10, c.getMax());
        assertEquals((2 + 10 + 4) / 3.0, c.getAverage(), 1e-9);
    }

    @Test
    void suitableValueIfNoEntries() {
        MaxAndAverageCollector<Integer> c = new MaxAndAverageCollector<>();
        assertEquals(null, c.getMax());
        assertEquals(null, c.getAverage());
    }
}