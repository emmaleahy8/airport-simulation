package com.group32.cs261project.frontend;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SimulationData {

    // ----- Config inputs -----
    public int runwayCount = 2;             // 1-10
    public int inboundRatePerHour = 10;     // 0-30
    public int outboundRatePerHour = 10;    // 0-30
    public int maxWaitMinutes = 30;         // default 30
    
    public double mechanicalEmergencyProbability = 0.0; // 0-100 (%)
    public double passengerHealthEmergencyProbability = 0.0; // 0-100 (%)
    

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
    public record PlaneLanded(String planeId, int runwayIndex) implements SimEvent {}
    public record PlaneTookOff(String planeId, int runwayIndex) implements SimEvent {}
    public record PlaneEmergency(String planeId) implements SimEvent {}
    public record PlaneDiverted(String planeId, String reason) implements SimEvent {}

    // Enums - allowed values for runway config
    public enum RunwayMode { LANDING, TAKE_OFF, MIXED;

        // ----- Print Enums as Normal Text -----
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
    public enum RunwayStatus { AVAILABLE, RUNWAY_INSPECTION, SNOW_CLEARANCE, EQUIPMENT_FAILURE;

        // ----- Print Enums as Normal Text -----
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

     // Plane enums + DTO for UI visuals
    public enum PlaneDirection { INBOUND, OUTBOUND }
    public enum PlaneStatus {
        HOLDING,            // inbound holding
        OUTBOUND_QUEUE,     // waiting in takeoff queue
        TAXI_TO_RUNWAY,     // leaving queue, driving to runway
        LANDING,
        TAKING_OFF,
        DIVERTED,
        COMPLETED
    }
    public static class PlaneDTO {
        public String id;   // identifier for UI mapping
        public String code; // display label
        public PlaneDirection direction;
        public PlaneStatus status;
        public boolean emergency;
        public int etaTicks; // remaining ticks in landing/takeoff/divert/taxi (0 otherwise)
        public int assignedRunwayIndex; // -1 if none
    }
    // --- Runway Config Inner Class -----
    public static class RunwayConfig {
        public final int id; // Runway identifier
        public RunwayMode mode = RunwayMode.MIXED; // Default to MIXED
        public RunwayStatus status = RunwayStatus.AVAILABLE; // Default to AVAILABLE

        public RunwayConfig(int id) { this.id = id; }

        // Debug-friendly string representation
        @Override public String toString() {
            return "Runway " + id + " [" + status + ", " + mode + "]";
        }
    }

    // Ensures the runway list length always matches runway count
    public void ensureRunwayListSize() {
        while (runways.size() < runwayCount) runways.add(new RunwayConfig(runways.size() + 1)); // If the list is too small: add new RunwayConfig objects 
        while (runways.size() > runwayCount) runways.remove(runways.size() - 1); // If list is too big: remove entries from end 
    }

    // Creates a deep copy of this SimulationData (for safely passing snapshots to the UI)
    public SimulationData copy() {
        SimulationData d = new SimulationData();  // Create a separate SimulationData object 
        d.runwayCount = runwayCount;
        d.inboundRatePerHour = inboundRatePerHour;
        d.outboundRatePerHour = outboundRatePerHour;
        d.maxWaitMinutes = maxWaitMinutes;
        d.passengerHealthEmergencyProbability = passengerHealthEmergencyProbability;
        d.mechanicalEmergencyProbability = mechanicalEmergencyProbability;

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

    // Generate hash based on config fields
    public int stableConfigHash() {
        ensureRunwayListSize();
        int h = Objects.hash(runwayCount, inboundRatePerHour, outboundRatePerHour, maxWaitMinutes);
        for (RunwayConfig r : runways) {
            h = 31 * h + Objects.hash(r.id, r.mode, r.status);
        }
        return h;
    }
}