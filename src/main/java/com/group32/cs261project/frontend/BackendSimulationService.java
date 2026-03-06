package com.group32.cs261project.frontend;

import java.time.Instant;
import java.util.List;
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
import com.group32.cs261project.backend.sim.SimConfig;




public class BackendSimulationService implements SimulationService {

    private volatile boolean running = false;
    private ScheduledExecutorService executor = null;

    private Consumer<SimulationData> onSnapshot = null;
    private SimulationData initialConfig = null;
    private AirportSimulation backendSim = null;
    private Instant startTime = null, endTime = null;
    private SimulationReport lastReport = null;


    /**
     * helper methods
     */
    static class SimulationMapper {
    
        private static SimConfig mapConfig(SimulationData ui) {
            return null;
        }

        private static Airport mapAirport(SimulationData ui) {
            return null;
        }

        private static SimulationData snapshotFrom(AirportSimulation sim) {
            return null;
        }

    }


    @Override
    public void start(SimulationData uiConfig, Consumer<SimulationData> onSnapshot) {

        if (this.running)
            return;

        this.onSnapshot = onSnapshot;
        this.initialConfig = uiConfig;

        SimConfig cfg = SimulationMapper.mapConfig(uiConfig);
        Airport airport = SimulationMapper.mapAirport(uiConfig);

        this.backendSim = new AirportSimulation(cfg, airport);

        FlightGenerator gen = new FlightGenerator(cfg);
        List<GeneratedFlight> in = gen.generateFlights(FlightType.INBOUND);
        List<GeneratedFlight> out = gen.generateFlights(FlightType.OUTBOUND);
        FlightSeeder.seedInbound(this.backendSim.engine(), in);
        FlightSeeder.seedOutbound(this.backendSim.engine(), out);

        SimulationData snap0 = SimulationMapper.snapshotFrom(this.backendSim);
        onSnapshot.accept(snap0);

        this.running = true;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::tick, 0, 200, TimeUnit.MILLISECONDS);
    }


    private void tick() {
        // TODO implement logic to tick backend

    }


    @Override
    public void updateConfig(SimulationData newConfig) {



    }



    @Override
    public SimulationData stop() {
        this.running = false;
        this.executor.shutdownNow(); this.executor = null;
        this.lastReport = this.backendSim.buildReport();
        SimulationData snap = SimulationMapper.snapshotFrom(this.backendSim);
        return snap;
    }


    @Override
    public boolean isRunning() {
        return this.running;
    }
    
}
