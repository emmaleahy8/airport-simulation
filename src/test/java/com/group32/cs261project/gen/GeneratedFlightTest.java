package com.group32.cs261project.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.gen.GeneratedFlight;
import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.model.enums.AircraftState;
import com.group32.cs261project.backend.model.enums.EmergencyStatus;

class GeneratedFlightTest {

    @Test
    void constructorStoresValuesCorrectly() {

        Instant scheduledTime = Instant.parse("2026-01-01T10:00:00Z");
        Instant entryTime = Instant.parse("2026-01-01T09:55:00Z");

        Aircraft aircraft = new Aircraft(
                "BA123",
                "British Airways",
                "JFK",
                "LHR",
                scheduledTime,
                EmergencyStatus.NONE,
                120,
                AircraftState.OUTSIDE_MODEL
        );

        GeneratedFlight flight =
                new GeneratedFlight(aircraft, scheduledTime, entryTime);

        assertEquals(aircraft, flight.aircraft());
        assertEquals(scheduledTime, flight.scheduledTime());
        assertEquals(entryTime, flight.entryTime());
    }
}