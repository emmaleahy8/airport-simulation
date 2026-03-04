package com.group32.cs261project;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.group32.cs261project.backend.gen.FlightGenerator;
import com.group32.cs261project.backend.gen.FlightSeeder;
import com.group32.cs261project.backend.gen.FlightType;
import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.model.RunwayData;
import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;
import com.group32.cs261project.backend.scheduling.Scheduler;
import com.group32.cs261project.backend.sim.EventQueue;
import com.group32.cs261project.backend.sim.SimConfig;
import com.group32.cs261project.backend.sim.SimulationContext;
import com.group32.cs261project.backend.sim.SimulationEngine;

public class Main {
    public static void main(String[] args) {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = start.plus(Duration.ofHours(1));

        // If your SimConfig has more fields, set them accordingly.
        SimConfig config = new SimConfig(start, end, 123L, 15.0, 15.0, 60, true);

        Airport airport = new Airport(List.of(
                new RunwayData(3000, 90, RunwayMode.MIXED, RunwayStatus.AVAILABLE)
        ));

        SimulationEngine<SimulationContext> engine = new SimulationEngine<>(
            config, 
            new SimulationContext(airport, new Scheduler()), 
            new EventQueue<>());


        FlightGenerator generator = new FlightGenerator(config);

        var inbound = generator.generateFlights(FlightType.INBOUND);
        var outbound = generator.generateFlights(FlightType.OUTBOUND);

        FlightSeeder.seedInbound(engine, inbound);
        FlightSeeder.seedOutbound(engine, outbound);

        engine.runUntil(config.endTime());

        System.out.println("Simulation finished.");
        System.out.println("Holding pattern size: " + airport.holdingPattern().size());
        System.out.println("Takeoff queue size:   " + airport.takeOffQueue().size());
        System.out.println("Holding pattern: " + airport.holdingPattern().toString());
        System.out.println("Takeoff queue: " + airport.takeOffQueue().toString());
    }
}