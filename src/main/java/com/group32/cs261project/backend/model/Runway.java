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
    private final int lengthM; // basically unused
    private final int bearingDeg; // basically unused
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

    /* getters */
    public int runwayNumber() { return this.runwayNumber; }
    public int lengthM() { return this.lengthM; }
    public int bearingDeg() { return this.bearingDeg; }
    public RunwayMode mode() { return this.mode; }
    public RunwayStatus status() { return this.status; }
    public Instant occupiedUntil() { return this.occupiedUntil; }

    /* setters */
    public void setMode(RunwayMode newMode) { this.mode = newMode; }
    public void setStatus(RunwayStatus newStatus) { this.status = newStatus; }
    public void clearOccupancy() { this.occupiedUntil = null; }

    public void occupyUntil(Instant time) {
        if (!this.isOccupied(time))
            this.occupiedUntil = time;
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

    @Override
    public String toString() {
        return String.format("Runway(%s)",this.getRunwayNumberString());
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
     * Checks if runway is occupied at a given time
     * @param time time to check
     * @return true if occupied
     */
    public boolean isOccupied(Instant time) {
        return (this.occupiedUntil != null && this.occupiedUntil.isAfter(time));
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
