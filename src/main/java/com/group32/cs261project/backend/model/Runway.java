package com.group32.cs261project.backend.model;

import java.time.Instant;
import java.util.Objects;

import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;

/**
 * Runway class
 */
public class Runway {

    private final int runwayNumber;
    private final int lengthM;
    private final int bearingDeg;
    private RunwayMode mode;
    private RunwayStatus status;
    private Instant occupiedUntil;

    /**
     * Constructor
     * @param runwayNumber
     * @param lengthM
     * @param bearingDeg
     * @param mode
     * @param status
     */
    public Runway(int runwayNumber, int lengthM, int bearingDeg, RunwayMode mode, RunwayStatus status) {
        this.runwayNumber = Objects.requireNonNull(runwayNumber);
        this.lengthM = Objects.requireNonNull(lengthM);
        this.bearingDeg = Objects.requireNonNull(bearingDeg);
        this.mode = Objects.requireNonNull(mode);
        this.status = Objects.requireNonNull(status);
        this.occupiedUntil = null;
    }

    /**
     * Get string representation
     */
    @Override
    public String toString() {
        return String.format("Runway(%s)", String.valueOf(this.runwayNumber));
    }

    /**
     * Getter for runway number
     * @return the runway number
     */
    public int runwayNumber() {
        return this.runwayNumber;
    }

    /**
     * Gets the two digit representation of the runway number
     * @return 2 digit runway number string
     */
    public String getRunwayNumberString() {
        if (this.runwayNumber < 10) {
            return String.format("0%d", this.runwayNumber);
        } else {
            return String.valueOf(this.runwayNumber);
        }

    }

    /**
     * Getter for runway length
     * @return runway length in metres
     */
    public int lengthM() {
        return this.lengthM;
    }

    /**
     * Getter for bearing of runway
     * @return bearing in degrees
     */
    public int bearingDeg() {
        return this.bearingDeg;
    }

    /**
     * Getter for runway mode
     * @return one of MIXED, LANDING, TAKEOFF
     */
    public RunwayMode mode() {
        return this.mode;
    }

    /**
     * Getter for runway status
     * @return one of AVAILABLE, INSPECTION, SNOW_CLEARANCE, EQUIPMENT_FAILURE
     */
    public RunwayStatus status() {
        return this.status;
    }

    /**
     * Setter for runway mode
     * @param newMode one of LANDING, TAKEOFF, MIXED
     */
    public void setMode(RunwayMode newMode) {
        this.mode = newMode;
    }

    /**
     * Setter for runway status
     * @param newStatus one of AVAILABLE, INSPECTION, SNOW_CLEARANCE, EQUIPMENT_FAILURE
     */
    public void setStatus(RunwayStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * Checks if the runway is available in general
     * @param time time to check
     * @return true if status is AVAILABLE and is not occupied
     */
    public boolean isAvailable(Instant time) {
        return this.status == RunwayStatus.AVAILABLE && !this.isOccupied(time);
    }

    /**
     * Checks if the runway is available for landing at the given time
     * @param time time to check
     * @return true if available
     */
    public boolean isAvailableForLanding(Instant time) {
        if (this.status != RunwayStatus.AVAILABLE)
            return false;
        if (this.mode != RunwayMode.LANDING && this.mode != RunwayMode.MIXED)
            return false;
        return !this.isOccupied(time);
    }

    /**
     * Checks if the runway is available for takeoff at the given time
     * @param time time to check
     * @return true if available
     */
    public boolean isAvailableForTakeOff(Instant time) {
        if (this.status != RunwayStatus.AVAILABLE)
            return false;
        if (this.mode != RunwayMode.TAKEOFF && this.mode != RunwayMode.MIXED)
            return false;
        return !this.isOccupied(time);
    }

    /**
     * Getter for time the runway is occupied until
     * @return a time if the runway is occupied, null otherwise
     */
    public Instant occupiedUntil() {
        return this.occupiedUntil;
    }

    /**
     * set runway to be occupied until a given time
     * @param time time to set
     */
    public void occupyUntil(Instant time) {
        if (!this.isOccupied(time))
            this.occupiedUntil = time;
    }
    
    /**
     * Checks if runway is occupied at a given time
     * @param time time to check
     * @return true if occupied
     */
    public boolean isOccupied(Instant time) {
        return (this.occupiedUntil != null && this.occupiedUntil.isAfter(time));
    }
    
    /**
     * Marks runway is not occupied
     */
    public void clearOccupancy() {
        this.occupiedUntil = null;
    }

    /**
     * Gets gets next available time runway is available
     * @param time current time
     * @return current time if available, otherwise next available time
     */
    public Instant nextAvailableTime(Instant time) {
        return this.isOccupied(time) ? this.occupiedUntil : time;
    }
}
