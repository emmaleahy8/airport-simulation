# CS261 Airport Traffic Simulation 

## Overview

This project is an airport operations simulation developed for the University of Warwick CS261 Software Engineering module.
The system models aircraft arriving and departing from an airport, simulating runway usage, holding patterns, take-off queues, delays, cancellations, and diversions.

The simulation includes:
- A JavaFX graphical interface showing aircraft movement
- Configurable simulation parameters
- A real-time animated radar-style visualisation
- Performance metrics such as queue lengths, delays, and cancellations
- A run comparison system for analysing multiple simulation runs

The aim is to explore how different airport configurations affect congestion and efficiency.

<img width="1277" height="744" alt="image" src="https://github.com/user-attachments/assets/54a38eb7-833b-4a0b-a916-7a5d98c68a8d" />

## Features
### Simulation configuration
Users can configure:
- Number of runways
- Inbound aircraft rate
- Outbound aircraft rate
- Emergency probabilities
- Maximum take-off queue wait time
- Simulation duration
- Runway operating mode and operational status

### Live visualisation
The interface shows:
- Aircraft entering the airspace
- Holding patterns for inbound aircraft shown as a radar
- Take-off queues
- Runway usage
- Aircraft departures and arrivals

The runway operating modes and operational status, as well as the speed of the simulation can be adjusted during runtime.
The visualisation automatically disables at speeds above 2× to maintain clarity.

### Simulation metrics
After each run the system reports:
- Maximum take-off queue length
- Average take-off queue length
- Maximum holding pattern size
- Average holding pattern size
- Maximum inbound delay
- Maximum outbound delay
- Number of cancellations
- Number of diversions

These metrics allow performance comparison between different configurations.

### Run comparison
Completed simulation runs are stored and can be compared to identify how configuration changes affect outcomes.

The comparison view shows side-by-side lists and bar charts of key metrics.

# How to Run
## Requirements
- Java 17+
- Maven

## Run with Maven
Run the project with: ```mvn javafx:run```

# Testing
Run the automated tests using the command: ```mvn verify```

# Technologies Used
- Java
- JavaFX
- Maven
- TestFX (UI testing)

# Authors
- Isabella Borgese - Project Manager and Frontend
- Tom Draper - Frontend
- Emma Leahy - Frontend Lead
- Nok Hang Lo - Backend Lead
- Ayden Mehmood - Backend
- Turgut Sahin - Testing

# References
CravenA (2020) Ho Chi Minh City, Vietnam – 04 Aug 2020: Vietnam Airlines fly over urban airport runway. Available at: https://www.shutterstock.com/image-photo/ho-chi-minh-city-vietnam-04-1790036027. (Accessed: 1 March 2026).
