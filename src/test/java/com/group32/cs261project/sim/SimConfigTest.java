package com.group32.cs261project.sim;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class SimConfigTest {

    private Instant start() {
        return Instant.parse("2026-01-01T10:00:00Z");
    }

    private Instant end() {
        return Instant.parse("2026-01-01T12:00:00Z");
    }

    // ---------------------------------
    // Constructor + Getters
    // ---------------------------------

    @Test
    void constructorStoresAllValuesCorrectly() {
        SimConfig config = new SimConfig(
                start(),
                end(),
                42L,
                20.0,
                15.0,
                60
        );

        assertEquals(start(), config.startTime());
        assertEquals(end(), config.endTime());
        assertEquals(42L, config.randomSeed());
        assertEquals(20.0, config.inboundRatePerHour());
        assertEquals(15.0, config.outboundRatePerHour());
    }

    // ---------------------------------
    // Null Validation
    // ---------------------------------

    @Test
    void nullStartTimeThrowsException() {
        assertThrows(NullPointerException.class, () ->
                new SimConfig(
                        null,
                        end(),
                        1L,
                        10.0,
                        10.0,
                        60
                )
        );
    }

    @Test
    void nullEndTimeThrowsException() {
        assertThrows(NullPointerException.class, () ->
                new SimConfig(
                        start(),
                        null,
                        1L,
                        10.0,
                        10.0,
                        60
                )
        );
    }

    // ---------------------------------
    // Time Order Validation
    // ---------------------------------

    @Test
    void endTimeBeforeStartTimeThrowsException() {
        Instant start = start();
        Instant endBefore = start.minusSeconds(60);

        assertThrows(IllegalArgumentException.class, () ->
                new SimConfig(
                        start,
                        endBefore,
                        1L,
                        10.0,
                        10.0,
                        60
                )
        );
    }

    @Test
    void endTimeEqualToStartTimeIsAllowed() {
        Instant same = start();

        SimConfig config = new SimConfig(
                same,
                same,
                1L,
                10.0,
                10.0,
                60
        );

        assertEquals(same, config.startTime());
        assertEquals(same, config.endTime());
    }
}