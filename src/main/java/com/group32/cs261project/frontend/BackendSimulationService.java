package com.group32.cs261project.frontend;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.group32.cs261project.backend.AirportSimulation;
import com.group32.cs261project.backend.gen.FlightGenerator;
import com.group32.cs261project.backend.gen.FlightSeeder;
import com.group32.cs261project.backend.gen.FlightType;
import com.group32.cs261project.backend.gen.GeneratedFlight;
import com.group32.cs261project.backend.metrics.SimulationReport;
import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.model.Runway;
import com.group32.cs261project.backend.model.RunwayData;
import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;
import com.group32.cs261project.backend.sim.SimConfig;




public class BackendSimulationService implements SimulationService {

    // Private variables
    private volatile boolean running = false;
    private ScheduledExecutorService executor = null;
    private final int tickMilliSeconds = 200; 
    private final int numEventsPerTick = 10;

    private Consumer<SimulationData> onSnapshot = null;
    private SimulationData initialConfig = null;
    private AirportSimulation backendSim = null;
    private Instant startTime = null, endTime = null;
    private SimulationReport lastReport = null;


    /**
     * Helper function to convert UI runway mode to backend runway mode
     * @param uiMode frontend runway mode
     * @return backend runway mode
     */
    private static RunwayMode convertMode(SimulationData.RunwayMode uiMode) {
        return switch (uiMode) {
            case LANDING -> RunwayMode.LANDING;
            case MIXED -> RunwayMode.MIXED;
            case TAKE_OFF -> RunwayMode.TAKEOFF;
        };
    }

    private static SimulationData.RunwayMode convertMode(RunwayMode mode) {
        return switch (mode) {
            case LANDING -> SimulationData.RunwayMode.LANDING;
            case MIXED -> SimulationData.RunwayMode.MIXED;
            case TAKEOFF -> SimulationData.RunwayMode.TAKE_OFF;
        };
    }


    /**
     * Helper function to convert UI runway status to backend runway status
     * @param uiStatus frontend runway status
     * @return backend runway status
     */
    private static RunwayStatus convertStatus(SimulationData.RunwayStatus uiStatus) {
        return switch (uiStatus) {
            case AVAILABLE -> RunwayStatus.AVAILABLE;
            case EQUIPMENT_FAILURE -> RunwayStatus.EQUIPMENT_FAILURE;
            case RUNWAY_INSPECTION -> RunwayStatus.INSPECTION;
            case SNOW_CLEARANCE -> RunwayStatus.SNOW_CLEARANCE;
        };
    }

    private static SimulationData.RunwayStatus convertStatus(RunwayStatus status) {
        return switch (status) {
            case AVAILABLE -> SimulationData.RunwayStatus.AVAILABLE;
            case EQUIPMENT_FAILURE -> SimulationData.RunwayStatus.EQUIPMENT_FAILURE;
            case INSPECTION -> SimulationData.RunwayStatus.RUNWAY_INSPECTION;
            case SNOW_CLEARANCE -> SimulationData.RunwayStatus.SNOW_CLEARANCE;
        };
    
    }

    
    /**
     * get backend config from frontend snapshot (helper method)
     * @param ui frontend snapshot
     * @return backend simulation config
     */
    private static SimConfig mapConfig(SimulationData ui) {
        return new SimConfig(
            Instant.now(),
            null, // there is no end time???
            new Random().nextLong(),
            ui.inboundRatePerHour,
            ui.outboundRatePerHour,
            60 // no runway occupancy seconds?
        );
    }


    /**
     * get backend airport form frontend snapshot (helper method)
     * @param ui frontend snapshot
     * @return airport object
     */
    private static Airport mapAirport(SimulationData ui) {
        return new Airport(ui.runways.stream().map(runway -> new RunwayData(
            3000, 
            90,
            convertMode(runway.mode),
            convertStatus(runway.status)
        )).toList());
    }


    /**
     * Take a snapshot from the backend simulation
     * @param sim instance of backend simulation
     * @return frontend snapshot
     */
    private static SimulationData snapshotFrom(AirportSimulation sim) {

        SimulationData snapshot = new SimulationData();

        // pass current sim time
        snapshot.simMinute = (int) Duration.between(sim.config().startTime(), Instant.now()).toMinutes();

        // pass queue sizes
        snapshot.holdingQueue = sim.airport().holdingPattern().size();
        snapshot.takeoffQueue = sim.airport().takeOffQueue().size();

        // pass config values
        snapshot.runwayCount = sim.airport().runways().size();
        snapshot.inboundRatePerHour = (int) sim.config().inboundRatePerHour();
        snapshot.outboundRatePerHour = (int) sim.config().outboundRatePerHour();

        // pass runways
        snapshot.runways.clear();
        for (Runway runway : sim.airport().runways()) {
            SimulationData.RunwayConfig uiRunway = new SimulationData.RunwayConfig(runway.runwayNumber());
            uiRunway.mode = convertMode(runway.mode());
            uiRunway.status = convertStatus(runway.status());
            snapshot.runways.add(uiRunway);
        }

        // pass planes
        snapshot.planes.clear(); // pass empty list for now
        
        return snapshot;
    }


    @Override
    public void start(SimulationData uiConfig, Consumer<SimulationData> onSnapshot) {

        if (this.running)
            return;

        this.onSnapshot = onSnapshot;
        this.initialConfig = uiConfig;

        SimConfig cfg = mapConfig(uiConfig);
        Airport airport = mapAirport(uiConfig);

        this.backendSim = new AirportSimulation(cfg, airport);

        FlightGenerator gen = new FlightGenerator(cfg);
        List<GeneratedFlight> in = gen.generateFlights(FlightType.INBOUND);
        List<GeneratedFlight> out = gen.generateFlights(FlightType.OUTBOUND);
        FlightSeeder.seedInbound(this.backendSim.engine(), in);
        FlightSeeder.seedOutbound(this.backendSim.engine(), out);

        SimulationData snap0 = snapshotFrom(this.backendSim);
        onSnapshot.accept(snap0);

        this.running = true;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::tick, 0, this.tickMilliSeconds, TimeUnit.MILLISECONDS);
    }


    /**
     * Tick the simulation by a set amount
     */
    private void tick() {
        if (!this.running)
            return;

        for (int i = 0; i < this.numEventsPerTick; i++) {
            this.backendSim.step();
        }

        SimulationData snap = snapshotFrom(this.backendSim);
        this.onSnapshot.accept(snap);
    }


    @Override
    public void updateConfig(SimulationData newConfig) {



    }



    @Override
    public SimulationData stop() {
        this.running = false;
        this.executor.shutdownNow(); this.executor = null;
        this.lastReport = this.backendSim.buildReport();
        SimulationData snap = snapshotFrom(this.backendSim);
        return snap;
    }


    @Override
    public boolean isRunning() {
        return this.running;
    }
    
}
