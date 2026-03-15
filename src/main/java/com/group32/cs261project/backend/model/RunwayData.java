package com.group32.cs261project.backend.model;

import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;

/**
 * Record for runway data needed to construct a new runway
 * This constructor is preferred as we don't need to specify runway numbers
 * Runway numbers are assigned by the airport
 */
public record RunwayData(
    int lengthM,
    int bearingDeg,
    RunwayMode mode,
    RunwayStatus status
) {}
