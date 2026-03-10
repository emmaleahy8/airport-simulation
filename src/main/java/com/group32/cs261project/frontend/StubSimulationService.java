package com.group32.cs261project.frontend;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.*;

import com.group32.cs261project.backend.compare.RunCompareCompletedRun;
import com.group32.cs261project.backend.compare.RunCompareHistory;
import com.group32.cs261project.backend.metrics.SimulationReport;
import com.group32.cs261project.backend.sim.SimConfig;
/* Stub implementation of simulation backend.
*  It implements the SimulationService interface methods: start, updateConfig, stop, and isRunning.
*  Will be replaced by SimulationService once backend is complete.
*/
public class StubSimulationService implements SimulationService {

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(); // Single background thread that runs scheduled tasks
    private volatile boolean running = false; // Flag to indicate if the simulation is running
    private volatile boolean paused = false;

    private final Object lock = new Object(); // Lock for synchronizing access to shared simulation state
    private SimulationData current;          // Current simulation state
    private Consumer<SimulationData> sink;   // Callback to deliver snapshots to the UI

    // Stats accumulation
    private long ticks = 0;
    private long sumHolding = 0;
    private long sumTakeoff = 0;

    private Random rng;

    // compare-runs feature state
    private final RunCompareHistory runCompareHistory = new RunCompareHistory();
    private SimConfig currentRunConfig = null;

    // Plane Storage and queues
    private final Map<String, Plane> planes = new HashMap<>();

    // FIFO within each queue, but emergencies take priority
    private final Deque<String> holdingEmergency = new ArrayDeque<>();
    private final Deque<String> holdingNormal = new ArrayDeque<>();

    private final Deque<String> outboundQueue = new ArrayDeque<>();

    private int planeSeq = 1;

    // Tune durations in "ticks" - 1 minute per tick
    private static final int LANDING_TICKS = 2;   // 2 minutes
    private static final int TAKEOFF_TICKS = 2;   // 2 minutes
    private static final int DIVERT_TICKS = 3;    // shows diverted for a bit before removal
    private static final int TAXI_TICKS = 2;      // 2 minutes

    private static final double P_EMERGENCY = 0.10;
    private static final double P_DIVERT_GIVEN_EMERGENCY = 0.10;

     // ----- Internal plane model (not exposed to UI directly) -----
    private static class Plane {
        final String code;
        final SimulationData.PlaneDirection direction;

        SimulationData.PlaneStatus status;
        boolean emergency;

        // Diversion roll happens once while holding
        boolean diversionPending;

        int remainingTicks; // for LANDING/TAKING_OFF/DIVERTED/TAXI_TO_RUNWAY

        Plane(String code, SimulationData.PlaneDirection direction) {
            this.code = code;
            this.direction = direction;
        }

        int assignedRunwayIndex = -1; // Initially set runway index to -1
    }

    // Start a simulation based on config
    @Override
    public void start(SimulationData config, Consumer<SimulationData> onSnapshot) {
        // Lock to ensure start is thread-safe
        synchronized (lock) {
            if (running) return; // If already running, ignore start request

            // Copy config so caller can't mutate internal state after starting
            current = config.copy();
            current.ensureRunwayListSize();

            sink = onSnapshot; // Store the callback for later tick updates
            rng = new Random(current.stableConfigHash()); // Seed RNG with config hash for deterministic behavior
            currentRunConfig = mapConfigFromCurrent(current);
            
            running = true; // Mark simulation as running

            // Reset accumulators for a new run
            ticks = 0;
            sumHolding = 0;
            sumTakeoff = 0;

            // Clear all planes and queues
            planes.clear();
            holdingEmergency.clear();
            holdingNormal.clear();
            outboundQueue.clear();
            planeSeq = 1;
        }

        // Tick every 200ms, advance 1 simulated minute per tick
        exec.scheduleAtFixedRate(this::tick, 0, 200, TimeUnit.MILLISECONDS);
    }

    // Update config while running (not implemented yet)
    @Override
    public void updateConfig(SimulationData newConfig) {
        synchronized (lock) {
            if (!running) return;
            // Keep sim time + live state, but update config/runways/rates
            int simMinute = current.simMinute;
            int holding = current.holdingQueue;
            int takeoff = current.takeoffQueue;

            current = newConfig.copy();
            current.ensureRunwayListSize();

            current.simMinute = simMinute;
            current.holdingQueue = holding;
            current.takeoffQueue = takeoff;
            currentRunConfig = mapConfigFromCurrent(current);
        }
    }

    // ----- Simulation logic -----
    private void tick() {
        SimulationData snap;
        synchronized (lock) {
            if (!running || paused) return;

            current.ensureRunwayListSize();
            current.simMinute += 1;

            // Capacity depends on runways available + mode
            int landingCapacity = 0;
            int takeoffCapacity = 0;

            // Only available runways contribute to capacity
            for (SimulationData.RunwayConfig r : current.runways) {
                if (r.status != SimulationData.RunwayStatus.AVAILABLE) continue;

                // Capacity rules per runway per tick
                switch (r.mode) {
                    case LANDING -> landingCapacity += 2;
                    case TAKE_OFF -> takeoffCapacity += 2;
                    case MIXED -> { landingCapacity += 1; takeoffCapacity += 1; }
                }
            }

            // Advance planes already in progress (landing/takeoff/divert/taxi)
            // Decrement timers, transition statuses, and remove when done
            List<String> toRemove = new ArrayList<>();

            for (Plane p : planes.values()) {

                // Only tick planes that are in a timed phase
                boolean timed =
                        p.status == SimulationData.PlaneStatus.LANDING
                    || p.status == SimulationData.PlaneStatus.TAXI_TO_RUNWAY
                    || p.status == SimulationData.PlaneStatus.TAKING_OFF
                    || p.status == SimulationData.PlaneStatus.DIVERTED;

                if (!timed) continue;

                p.remainingTicks--;

                if (p.remainingTicks > 0) continue;

                // Phase finished: decide next state
                switch (p.status) {

                    case LANDING -> {
                        p.status = SimulationData.PlaneStatus.COMPLETED;
                        toRemove.add(p.code);
                    }

                    case TAXI_TO_RUNWAY -> {
                        // Taxi finished -> start takeoff roll
                        p.status = SimulationData.PlaneStatus.TAKING_OFF;
                        p.remainingTicks = TAKEOFF_TICKS; // how long the takeoff roll takes
                    }

                    case TAKING_OFF -> {
                        // Takeoff finished -> plane leaves sim
                        p.status = SimulationData.PlaneStatus.COMPLETED;
                        toRemove.add(p.code);
                    }

                    case DIVERTED -> {
                        p.status = SimulationData.PlaneStatus.COMPLETED;
                        toRemove.add(p.code);
                    }

                    default -> { /* nothing */ }
                }
            }

            for (String code : toRemove) {
                planes.remove(code);
            }

            // Generate new arrivals based on rates (using a simple poisson approximation)
            int inboundArrivals = poissonApprox(current.inboundRatePerHour / 60.0);
            int outboundReady = poissonApprox(current.outboundRatePerHour / 60.0);

            for (int i = 0; i < inboundArrivals; i++) spawnInbound();
            for (int i = 0; i < outboundReady; i++) spawnOutbound();

            // Diversion Logic 
            // Roll once for emergency planes while holding - we do this after spawning so new emergency planes can be diverted too
            divertSomeEmergencyFromHoldingOnce();

            // Pull planes from queues to start Landing/Taking off
            // Respect emergency first then FIFO
            for (int i = 0; i < landingCapacity; i++) {
                Plane next = pollNextInboundForLanding();
                if (next == null) break;
                next.status = SimulationData.PlaneStatus.LANDING;
                next.remainingTicks = LANDING_TICKS;
            }

            for (int i = 0; i < takeoffCapacity; i++) {
                Plane next = pollNextOutboundForTakeoff();
                if (next == null) break;

                int runwayIndex = pickTakeoffRunwayIndex();
                if (runwayIndex == -1) break; // no takeoff runway available
                next.assignedRunwayIndex = runwayIndex;
                next.status = SimulationData.PlaneStatus.TAXI_TO_RUNWAY;
                next.remainingTicks = TAXI_TICKS;

            }

            // ----- Update existing summary stats based on queue sizes -----

            // Queue evolution
            int holdingCount = holdingEmergency.size() + holdingNormal.size();
            int takeoffCount = outboundQueue.size();

            current.holdingQueue = holdingCount;
            current.takeoffQueue = takeoffCount;
            
            // Delay proxies
            int inboundDelay = Math.min(120, current.holdingQueue);
            int outboundDelay = Math.min(120, current.takeoffQueue);

            current.maxInboundDelayMin = Math.max(current.maxInboundDelayMin, inboundDelay);
            current.maxOutboundDelayMin = Math.max(current.maxOutboundDelayMin, outboundDelay);

            // Accumulate averages
            ticks++;
            sumHolding += holdingCount;
            sumTakeoff += takeoffCount;

            current.maxHoldingQueue = Math.max(current.maxHoldingQueue, holdingCount);
            current.maxTakeoffQueue = Math.max(current.maxTakeoffQueue, takeoffCount);

            current.avgHoldingQueue = sumHolding / (double) ticks;
            current.avgTakeoffQueue = sumTakeoff / (double) ticks;

            current.avgInboundDelayMin = current.avgHoldingQueue;
            current.avgOutboundDelayMin = current.avgTakeoffQueue;

            // Counts for UI convenience 
            current.holdingEmergencyCount = holdingEmergency.size();
            current.holdingNormalCount = holdingNormal.size();
            current.outboundQueueCount = outboundQueue.size(); 
            
            // Export FIFO outbound queue order for the UI
            current.outboundQueueIds.clear();
            current.outboundQueueIds.addAll(outboundQueue);
            
            // Build planes list for snapshot
            current.planes.clear();
            for (Plane p : planes.values()) {
                SimulationData.PlaneDTO dto = new SimulationData.PlaneDTO();
                dto.code = p.code;
                dto.id = p.code;
                dto.direction = p.direction;
                dto.status = p.status;
                dto.emergency = p.emergency;
                dto.etaTicks = p.remainingTicks;
                dto.assignedRunwayIndex = p.assignedRunwayIndex;
                current.planes.add(dto);
            }

            // End automatically after 12 simulated hours (720 minutes) unless user stops
            if (current.simMinute >= 720) {
                running = false;
            }

            snap = current.copy();
        }

        if (sink != null) sink.accept(snap);
    }

    // Spawn inbound planes
    private void spawnInbound(){
        String code = genPlaneCode("IN");
        Plane p = new Plane(code, SimulationData.PlaneDirection.INBOUND);
        p.emergency = rng.nextDouble() < P_EMERGENCY;
        p.status = SimulationData.PlaneStatus.HOLDING;

        // Diversion is decided from holding, but only emergencies are eligible
        p.diversionPending = p.emergency;

        planes.put(code, p);

        if (p.emergency) holdingEmergency.addLast(code);
        else holdingNormal.addLast(code);
    }

    // Spawn outbound planes
    private void spawnOutbound() {
        String code = genPlaneCode("OUT");
        Plane p = new Plane(code, SimulationData.PlaneDirection.OUTBOUND);
        p.emergency = false;
        p.diversionPending = false;
        p.status = SimulationData.PlaneStatus.OUTBOUND_QUEUE; // or a separate OUTBOUND_QUEUE status if you add it

        planes.put(code, p);
        outboundQueue.addLast(code);
    }

    private void divertSomeEmergencyFromHoldingOnce() {
        // Inspect emergency holding queue and divert some planes.
        // Because removing from the middle of ArrayDeque is awkward, simplest stub approach:
        // rebuild the deque with diverted planes removed.

        if (holdingEmergency.isEmpty()) return;

        Deque<String> rebuilt = new ArrayDeque<>();
        while (!holdingEmergency.isEmpty()) {
            String code = holdingEmergency.pollFirst();
            Plane p = planes.get(code);
            if (p == null) continue; // already removed

            if (p.status == SimulationData.PlaneStatus.HOLDING && p.emergency && p.diversionPending) {
                // Roll once
                boolean divert = rng.nextDouble() < P_DIVERT_GIVEN_EMERGENCY;
                p.diversionPending = false;

                if (divert) {
                    p.status = SimulationData.PlaneStatus.DIVERTED;
                    p.remainingTicks = DIVERT_TICKS;
                    current.divertedCount += 1; // keep metric
                    continue; // do NOT re-add to holding queue
                }
            }

            // keep waiting
            rebuilt.addLast(code);
        }

        holdingEmergency.addAll(rebuilt);
    }

    private Plane pollNextInboundForLanding() {
        String code = !holdingEmergency.isEmpty() ? holdingEmergency.pollFirst() : !holdingNormal.isEmpty() ? holdingNormal.pollFirst() : null;
        if (code == null) return null;

        Plane p = planes.get(code);
        if (p == null) return null;

        return p;
    }

    private Plane pollNextOutboundForTakeoff() {
        String code = outboundQueue.pollFirst();
        if (code == null) return null;

        Plane p = planes.get(code);
        if (p == null) return null;

        return p;
    }

    // Generate plane code
    private String genPlaneCode(String prefix) {
        // deterministic-ish but unique
        int n = planeSeq++;
        // add “airline” look
        String airline = switch (rng.nextInt(5)) {
            case 0 -> "BAW";
            case 1 -> "EZY";
            case 2 -> "VIR";
            case 3 -> "RYR";
            default -> "TUI";
        };
        return prefix + "-" + airline + String.format("%03d", n);
    }

    private int poissonApprox(double lambda) {
        // "Poisson-like” for small lambda
        double x = rng.nextDouble();
        if (lambda <= 0) return 0;
        if (lambda < 0.05) return x < lambda ? 1 : 0;
        if (lambda < 0.15) return x < lambda ? 1 : (x < lambda * 1.2 ? 0 : 0);
        // Otherwise jitter around lambda
        int base = (int) Math.floor(lambda);
        double frac = lambda - base;
        return base + (rng.nextDouble() < frac ? 1 : 0) + (rng.nextDouble() < 0.08 ? 1 : 0);
    }

    // compare-runs helpers
    private SimConfig mapConfigFromCurrent(SimulationData data) {
        Instant start = Instant.now();
        Instant end = start.plus(Duration.ofHours(12));

        return SimConfig.builder()
            .startTime(start)
            .endTime(end)
            .randomSeed(data.stableConfigHash())
            .inboundRatePerHour(data.inboundRatePerHour)
            .outboundRatePerHour(data.outboundRatePerHour)
            .runwayOccupancySeconds(60)
            .maxTakeoffWaitMinutes(30)
            .lowFuelMinutes(20)
            .fuelDiversionMinutes(10)
            .mechanicalEmergencyProbability(data.mechanicalEmergencyProbability)
            .passengerHealthEmergencyProbability(data.passengerHealthEmergencyProbability)
            .verbose(false)
            .build();
    }

    private SimulationReport buildReportFromCurrent(SimulationData data) {
        return new SimulationReport(
            data.runwayCount,
            data.maxHoldingQueue,
            data.avgHoldingQueue,
            data.maxTakeoffQueue,
            data.avgTakeoffQueue,
            (long) data.maxInboundDelayMin,
            data.avgInboundDelayMin,
            (long) data.maxOutboundDelayMin,
            data.avgOutboundDelayMin,
            data.divertedCount,
            data.cancelledCount
        );
    }

    // Stop the simulation safely and return a final report snapshot
    @Override
    public SimulationData stop() {
        synchronized (lock) {
            running = false;

            if (current != null && currentRunConfig != null) {
                SimulationReport report = buildReportFromCurrent(current);

                RunCompareCompletedRun completedRun = new RunCompareCompletedRun(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    currentRunConfig,
                    report
                );

                runCompareHistory.addRun(completedRun);
            }

            currentRunConfig = null;

            // Final snapshot already has report fields; just return copy
            return current == null ? new SimulationData() : current.copy();
        }
    }

    // Check if the simulation is currently running
    @Override
    public boolean isRunning() {
        return running;
    }

        @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void resume() {
        paused = false;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public RunCompareHistory getRunCompareHistory() {
        return runCompareHistory;
    }

    private int pickTakeoffRunwayIndex() {
        List<Integer> candidates = new ArrayList<>();
        for (int idx = 0; idx < current.runways.size(); idx++) {
            var r = current.runways.get(idx);
            if (r.status != SimulationData.RunwayStatus.AVAILABLE) continue;
            if (r.mode == SimulationData.RunwayMode.LANDING) continue;
            candidates.add(idx);
        }
        if (candidates.isEmpty()) return -1;
        return candidates.get(rng.nextInt(candidates.size()));
    }
}