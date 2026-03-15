package com.group32.cs261project.backend.gen;

import java.util.List;

import com.group32.cs261project.backend.events.AircraftInboundArrivesEvent;
import com.group32.cs261project.backend.events.AircraftOutboundReadyEvent;
import com.group32.cs261project.backend.sim.SimulationContext;
import com.group32.cs261project.backend.sim.SimulationEngine;


/**
 * Class containing static methods to create events for the simulation kernel given 
 * the list of generated inbound and outbound flights
 */
public class FlightSeeder {

    /**
     * Schedule aircraft inbound events given list of inbound flights
     * @param engine simulation engine
     * @param inboundFlights list of inbound generated flights
     */
    public static void seedInbound(SimulationEngine<SimulationContext> engine, List<GeneratedFlight> inboundFlights) {
        for (GeneratedFlight flight : inboundFlights) {
            engine.schedule(new AircraftInboundArrivesEvent(flight.entryTime(), flight.aircraft()));
        }
    }

    /**
     * Schedule aircraft outbound events given list of outbound flights
     * @param engine simulation engine
     * @param outboundFlights list of outbound generated flights
     */
    public static void seedOutbound(SimulationEngine<SimulationContext> engine, List<GeneratedFlight> outboundFlights) {
        for (GeneratedFlight flight: outboundFlights) {
            engine.schedule(new AircraftOutboundReadyEvent(flight.entryTime(), flight.aircraft()));
        }
    }
    
}
