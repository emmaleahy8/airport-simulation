package com.group32.cs261project.backend.model;

import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;

public record RunwayData(
    int lengthM,
    int bearingDeg,
    RunwayMode mode,
    RunwayStatus status
) {}
