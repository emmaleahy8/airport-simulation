package com.group32.cs261project.sim;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.sim.SimConfig;

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
        SimConfig config = SimConfig.builder()
            .startTime(start())
            .endTime(end())
            .randomSeed(42L)
            .inboundRatePerHour(20.0)
            .outboundRatePerHour(15.0)
            .build();

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
            SimConfig.builder()
                .startTime(null)
                .build()
        );
    }

    @Test
    void nullEndTimeThrowsException() {
        assertThrows(NullPointerException.class, () ->
            SimConfig.builder()
                .endTime(null)
                .build()
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
            SimConfig.builder()
                .startTime(start)
                .endTime(endBefore)
                .build()
        );
    }

    @Test
    void endTimeEqualToStartTimeIsAllowed() {
        Instant same = start();

        SimConfig config = SimConfig.builder()
            .startTime(same)
            .endTime(same)
            .build();

        assertEquals(same, config.startTime());
        assertEquals(same, config.endTime());
    }
}