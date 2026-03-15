package com.group32.cs261project.frontend;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Data transfer object representing simulation configuration, live state,
 * and completed run results.
 *
 * This class is used throughout the frontend to store:
 * • initial simulation input parameters
 * • live snapshot data while the simulation is running
 * • end-of-run result metrics
 * • runway and aircraft UI state
 *
 * It also provides helper methods for maintaining runway configuration
 * consistency, creating safe deep copies, and generating a stable hash
 * for configuration comparison.
 */
public class SimulationData {

    // ----- Config inputs -----
    public int runwayCount = 2;             // 1-10
    public int inboundRatePerHour = 10;     // 0-30
    public int outboundRatePerHour = 10;    // 0-30
    public int maxWaitMinutes = 30;         // default 30
    
    public double mechanicalEmergencyProbability = 0.0; // 0-100 (%)
    public double passengerHealthEmergencyProbability = 0.0; // 0-100 (%)
    
    public int simulationHours = 6;     // 1–24 hours

    public final List<RunwayConfig> runways = new ArrayList<>();

    // ----- Live snapshot fields (while running) -----
    public int simMinute = 0;     // simulated minutes since start
    public int holdingQueue = 0;
    public int takeoffQueue = 0;

    // ----- Live plane snapshot for UI -----
    public final List<PlaneDTO> planes = new ArrayList<>();

    // Queue order (FIFO) for visuals
    public final List<String> outboundQueueIds = new ArrayList<>();

    // Breakdown counts (UI convenience)
    public int holdingEmergencyCount = 0;
    public int holdingNormalCount = 0;
    public int outboundQueueCount = 0;

    // ----- Results/report fields (filled at end) -----
    public int maxHoldingQueue = 0;
    public int maxTakeoffQueue = 0;
    public double avgHoldingQueue = 0;
    public double avgTakeoffQueue = 0;

    public int cancelledCount = 0;
    public int divertedCount = 0;

    // Delay metrics
    public double avgInboundDelayMin = 0;
    public double avgOutboundDelayMin = 0;
    public int maxInboundDelayMin = 0;
    public int maxOutboundDelayMin = 0;

    // Event List
    public List<SimEvent> events = new ArrayList<>();
    public sealed interface SimEvent permits PlaneArrivedHolding, PlaneArrivedTakeoff, PlaneLanded, PlaneTookOff, PlaneEmergency, PlaneDiverted {}
    public record PlaneArrivedHolding(String planeId, boolean emergency) implements SimEvent {}
    public record PlaneArrivedTakeoff(String planeId) implements SimEvent {}
    public record PlaneLanded(String planeId, int runwayIndex, boolean emergency,  double runwayOccupancySeconds) implements SimEvent {}
    public record PlaneTookOff(String planeId, int runwayIndex,  double runwayOccupancySeconds) implements SimEvent {}
    public record PlaneEmergency(String planeId) implements SimEvent {}
    public record PlaneDiverted(String planeId, String reason) implements SimEvent {}

    /**
     * Enumeration of the operating modes available for a runway.
     */
    public enum RunwayMode { LANDING, TAKE_OFF, MIXED;

        /**
         * Returns a human-readable version of the runway mode.
         *
         * @return the runway mode formatted for display
         */
        @Override 
        public String toString() {
            String lower = name().toLowerCase().replace('_', ' '); // Change underscores to spaces eg. RUNWAY_INSPECTION -> Runway Inspection
            String[] parts = lower.split(" ");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                if (p.isBlank()) continue;
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" "); // Capitalize first letter of each word
            }
            return sb.toString().trim(); // Remove trailing space
        }
    }

    /**
     * Enumeration of the operational statuses available for a runway.
     */
    public enum RunwayStatus { AVAILABLE, RUNWAY_INSPECTION, SNOW_CLEARANCE, EQUIPMENT_FAILURE;

        /**
         * Returns a human-readable version of the runway status.
         *
         * @return the runway status formatted for display
         */
        @Override 
        public String toString() {
            String lower = name().toLowerCase().replace('_', ' '); // Change underscores to spaces eg. RUNWAY_INSPECTION -> Runway Inspection
            String[] parts = lower.split(" ");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                if (p.isBlank()) continue;
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" "); // Capitalize first letter of each word
            }
            return sb.toString().trim(); // Remove trailing space
        }
     }

    /**
     * Enumeration representing whether an aircraft is inbound or outbound.
     */
    public enum PlaneDirection { INBOUND, OUTBOUND }

    /**
     * Enumeration representing the current visual state of an aircraft
     * within the frontend simulation view.
     */
    public enum PlaneStatus {
        HOLDING,            // inbound holding
        OUTBOUND_QUEUE,     // waiting in takeoff queue
        TAXI_TO_RUNWAY,     // leaving queue, driving to runway
        LANDING,
        TAKING_OFF,
        DIVERTED,
        COMPLETED
    }

    /**
     * Data transfer object representing an aircraft in the frontend view.
     *
     * This stores the information needed by the UI to render aircraft
     * position, status, direction, and emergency state.
     */
    public static class PlaneDTO {
        public String id;   // identifier for UI mapping
        public String code; // display label
        public PlaneDirection direction;
        public PlaneStatus status;
        public boolean emergency;
        public int etaTicks; // remaining ticks in landing/takeoff/divert/taxi (0 otherwise)
        public int assignedRunwayIndex; // -1 if none
    }
    /**
     * Data object representing the configuration of a single runway.
     *
     * Each runway has a unique identifier together with an operating mode
     * and operational status.
     */
    public static class RunwayConfig {
        public final int id; // Runway identifier
        public RunwayMode mode = RunwayMode.MIXED; // Default to MIXED
        public RunwayStatus status = RunwayStatus.AVAILABLE; // Default to AVAILABLE

        /**
         * Creates a runway configuration for the given runway identifier.
         *
         * @param id the runway identifier
         */
        public RunwayConfig(int id) { this.id = id; }

        /**
         * Returns a debug-friendly string representation of this runway configuration.
         *
         * @return a string describing the runway identifier, status, and mode
         */
        @Override public String toString() {
            return "Runway " + id + " [" + status + ", " + mode + "]";
        }
    }

    /**
     * Ensures that the runway configuration list matches the current runway count.
     *
     * New runway configurations are added if the list is too short, and
     * excess runway configurations are removed if the list is too long.
     */
    public void ensureRunwayListSize() {
        while (runways.size() < runwayCount) runways.add(new RunwayConfig(runways.size() + 1)); // If the list is too small: add new RunwayConfig objects 
        while (runways.size() > runwayCount) runways.remove(runways.size() - 1); // If list is too big: remove entries from end 
    }

    /**
     * Creates a deep copy of this simulation data object.
     *
     * This is used to safely pass simulation snapshots between components
     * without allowing accidental mutation of shared state.
     *
     * @return a deep copy of this simulation data
     */
    public SimulationData copy() {
        SimulationData d = new SimulationData();  // Create a separate SimulationData object 
        d.runwayCount = runwayCount;
        d.inboundRatePerHour = inboundRatePerHour;
        d.outboundRatePerHour = outboundRatePerHour;
        d.maxWaitMinutes = maxWaitMinutes;
        d.passengerHealthEmergencyProbability = passengerHealthEmergencyProbability;
        d.mechanicalEmergencyProbability = mechanicalEmergencyProbability;
        d.simulationHours = simulationHours;

        d.simMinute = simMinute;
        d.holdingQueue = holdingQueue;
        d.takeoffQueue = takeoffQueue;

        d.maxHoldingQueue = maxHoldingQueue;
        d.maxTakeoffQueue = maxTakeoffQueue;
        d.avgHoldingQueue = avgHoldingQueue;
        d.avgTakeoffQueue = avgTakeoffQueue;

        d.cancelledCount = cancelledCount;
        d.divertedCount = divertedCount;

        d.avgInboundDelayMin = avgInboundDelayMin;
        d.avgOutboundDelayMin = avgOutboundDelayMin;
        d.maxInboundDelayMin = maxInboundDelayMin;
        d.maxOutboundDelayMin = maxOutboundDelayMin;

        // Deep copy runway configs 
        // (important to create new RunwayConfig objects so that the UI can safely modify them without affecting the simulation's internal state)
        d.runways.clear();
        for (RunwayConfig r : runways) {
            RunwayConfig rr = new RunwayConfig(r.id);
            rr.mode = r.mode;
            rr.status = r.status;
            d.runways.add(rr);
        }

        // Copy queue breakdown counts
        d.holdingEmergencyCount = holdingEmergencyCount;
        d.holdingNormalCount = holdingNormalCount;
        d.outboundQueueCount = outboundQueueCount;

        // Deep copy planes list
        d.planes.clear();
        for (PlaneDTO p : planes) {
            PlaneDTO pp = new PlaneDTO();
            pp.code = p.code;
            pp.id = p.code;
            pp.direction = p.direction;
            pp.status = p.status;
            pp.emergency = p.emergency;
            pp.etaTicks = p.etaTicks;
            pp.assignedRunwayIndex = p.assignedRunwayIndex;
            d.planes.add(pp);
        }

        // Copy events 
        d.events = new ArrayList<>(events);

        d.outboundQueueIds.clear();
        d.outboundQueueIds.addAll(outboundQueueIds);

        return d;
    }

    /**
     * Generates a stable hash value based on the simulation configuration.
     *
     * The hash is derived from the main input parameters and runway
     * configurations, allowing configurations to be compared efficiently.
     *
     * @return a stable hash representing the current configuration
     */
    public int stableConfigHash() {
        ensureRunwayListSize();
        int h = Objects.hash(runwayCount, inboundRatePerHour, outboundRatePerHour, maxWaitMinutes, simulationHours);
        for (RunwayConfig r : runways) {
            h = 31 * h + Objects.hash(r.id, r.mode, r.status);
        }
        return h;
    }
}