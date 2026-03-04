package com.group32.cs261project.model;

import com.group32.cs261project.model.enums.RunwayMode;
import com.group32.cs261project.model.enums.RunwayStatus;

public record RunwayData(
    int lengthM,
    int bearingDeg,
    RunwayMode mode,
    RunwayStatus status
) {}
