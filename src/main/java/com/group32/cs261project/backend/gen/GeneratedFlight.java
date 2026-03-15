package com.group32.cs261project.backend.gen;

import java.time.Instant;

import com.group32.cs261project.backend.model.Aircraft;

/**
 * Class to store data of flight generated
 */
public record GeneratedFlight(
    Aircraft aircraft,
    Instant scheduledTime,
    Instant entryTime
) {}