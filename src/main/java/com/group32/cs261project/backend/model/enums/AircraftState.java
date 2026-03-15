package com.group32.cs261project.backend.model.enums;


/**
 * Enum for which state the aircraft is in currently
 */
public enum AircraftState {
    OUTSIDE_MODEL,
    HOLDING,
    TAKEOFF_QUEUE,
    RUNWAY_ZONE,
    DIVERTED,
    CANCELLED,
    COMPLETED
}
