package com.group32.cs261project.backend.gen;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import com.group32.cs261project.backend.events.MechanicalFailureEmergencyEvent;
import com.group32.cs261project.backend.events.PassengerHealthEmergencyEvent;
import com.group32.cs261project.backend.model.Aircraft;
import com.group32.cs261project.backend.sim.SimConfig;
import com.group32.cs261project.backend.sim.SimulationContext;
import com.group32.cs261project.backend.sim.SimulationEngine;


/**
 * Class to generate passenger health or mechanical failure emegencies for an aircraft
 * based on the probabilities given in the simulation config
 */
public class EmergencyGenerator {

    /**
     * randomly generate an emergency type for an aircraft
     * @param config simulation config
     * @return one of NONE, MECHANICAL_FAILURE, PASSENGER_HEALTH
     */
    public static RandomEmergencyType generateEmergencyType(SimConfig config, Random rng) {

        double u = rng.nextDouble();

        double pMechanical = config.mechanicalEmergencyProbability();
        double pHealth = config.passengerHealthEmergencyProbability();

        if (u < pMechanical) {
           return RandomEmergencyType.MECHANICAL_FAILURE; 

        } else if (u < pMechanical + pHealth) {
            return RandomEmergencyType.PASSENGER_HEALTH;

        } else {
            return RandomEmergencyType.NONE;
        }
        
    }

    
    /**
     * get time to emergency given the emergency type generated
     * @param config simulation config
     * @param emergencyType one of NONE, MECHANICAL_FAILURE, PASSENGER_HEALTH
     * @return time in minutes until the emergency
     */
    public static double timeToEmergency(SimConfig config, RandomEmergencyType emergencyType) {

        double lambda = switch(emergencyType) {
            case MECHANICAL_FAILURE -> config.mechanicalEmergencyProbability();
            case PASSENGER_HEALTH -> config.passengerHealthEmergencyProbability();
            case NONE -> throw new UnsupportedOperationException("do not pass NONE to timeToEmergency");
        };


        Random random = new Random(config.randomSeed());
        double u = random.nextDouble();

        return -Math.log(u) / lambda;
    }


    /**
     * Scheudle random emergencies for when an aircraft enters holding pattern
     * @param engine simulation engine
     * @param aircraft aircraft object
     * @param time current time
     */
    public static void scheduleEmergency(SimulationEngine<SimulationContext> engine, Aircraft aircraft, Instant time) {
        
        RandomEmergencyType emergencyType = EmergencyGenerator.generateEmergencyType(engine.config(), engine.rng());
        if (emergencyType != RandomEmergencyType.NONE) {

            double emergencyMinutes = EmergencyGenerator.timeToEmergency(engine.config(), emergencyType);
            Instant emergencyTime = time.plus(Duration.ofSeconds((long) (emergencyMinutes * 60.0)));

            switch (emergencyType) {

                case MECHANICAL_FAILURE -> {
                    // System.out.printf("**** mechanical failure emergency scheduled for %s at %s\n", aircraft, emergencyTime);
                    engine.schedule(new MechanicalFailureEmergencyEvent(aircraft, emergencyTime));
                }

                case PASSENGER_HEALTH -> {
                    // System.out.printf("**** passenger health emergency scheduled for %s at %s\n", aircraft, emergencyTime);
                    engine.schedule(new PassengerHealthEmergencyEvent(aircraft, emergencyTime));
                }

                default -> {}
            }

        }
    }

}
