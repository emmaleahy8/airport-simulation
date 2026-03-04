package com.group32.cs261project.gen;

import java.util.List;

import com.group32.cs261project.airport.events.AircraftInboundArrivesEvent;
import com.group32.cs261project.airport.events.AircraftOutboundReadyEvent;
import com.group32.cs261project.sim.SimulationContext;
import com.group32.cs261project.sim.SimulationEngine;

public class FlightSeeder {

    public static void seedInbound(SimulationEngine<SimulationContext> engine, List<GeneratedFlight> inboundFlights) {
        for (GeneratedFlight flight : inboundFlights) {
            engine.schedule(new AircraftInboundArrivesEvent(flight.entryTime(), flight.aircraft()));
        }
    }

    public static void seedOutbound(SimulationEngine<SimulationContext> engine, List<GeneratedFlight> outboundFlights) {
        for (GeneratedFlight flight: outboundFlights) {
            engine.schedule(new AircraftOutboundReadyEvent(flight.entryTime(), flight.aircraft()));
        }
    }
    
}
