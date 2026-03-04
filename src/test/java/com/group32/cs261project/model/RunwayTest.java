package com.group32.cs261project.model;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.model.Runway;
import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;

class RunwayTest {

    @Test
    void landingAvailabilityDependsOnModeStatusAndOccupancy() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t1 = t0.plusSeconds(60);

        Runway r = new Runway(9, 3000, 90, RunwayMode.LANDING, RunwayStatus.AVAILABLE);

        assertTrue(r.isAvailableForLanding(t0));
        assertFalse(r.isAvailableForTakeOff(t0));

        r.occupyUntil(t1);
        assertFalse(r.isAvailableForLanding(t0)); // occupied
        assertTrue(r.isAvailableForLanding(t1));  // free at occupiedUntil
    }

    @Test
    void takeoffAvailabilityDependsOnModeStatusAndOccupancy() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        Runway r = new Runway(27, 2800, 270, RunwayMode.TAKEOFF, RunwayStatus.AVAILABLE);

        assertTrue(r.isAvailableForTakeOff(t0));
        assertFalse(r.isAvailableForLanding(t0));
    }

    @Test
    void mixedModeAllowsBothOpsWhenAvailable() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        Runway r = new Runway(18, 2500, 180, RunwayMode.MIXED, RunwayStatus.AVAILABLE);

        assertTrue(r.isAvailableForLanding(t0));
        assertTrue(r.isAvailableForTakeOff(t0));
    }

    @Test
    void nonAvailableStatusBlocksAllOps() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        Runway r = new Runway(1, 2000, 10, RunwayMode.MIXED, RunwayStatus.INSPECTION);

        assertFalse(r.isAvailableForLanding(t0));
        assertFalse(r.isAvailableForTakeOff(t0));
    }
}