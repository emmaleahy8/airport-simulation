package com.group32.cs261project.model;

import org.junit.jupiter.api.Test;

import com.group32.cs261project.backend.model.Airport;
import com.group32.cs261project.backend.model.RunwayData;
import com.group32.cs261project.backend.model.enums.RunwayMode;
import com.group32.cs261project.backend.model.enums.RunwayStatus;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Unit test: runway "display number" should be exactly 2 digits
 * 
 * Assumes your Runway has:
 * - an int runwayNumber (e.g. 1..10)
 * - a method like getRunwayNumberString()
 */
public class RunwayNumberFormatTest {

    @Test
    void runwayNumberStringIsTwoDigits() {

        List<RunwayData> runways = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            runways.add(new RunwayData(3000, 90, RunwayMode.LANDING, RunwayStatus.AVAILABLE));
        }

        Airport airport = new Airport(runways);
        
        assertEquals("00", airport.getRunway(0).getRunwayNumberString());
        assertEquals("01", airport.getRunway(1).getRunwayNumberString());
        assertEquals("02", airport.getRunway(2).getRunwayNumberString());
        assertEquals("03", airport.getRunway(3).getRunwayNumberString());
        assertEquals("04", airport.getRunway(4).getRunwayNumberString());
        assertEquals("05", airport.getRunway(5).getRunwayNumberString());
        assertEquals("06", airport.getRunway(6).getRunwayNumberString());
        assertEquals("07", airport.getRunway(7).getRunwayNumberString());
        assertEquals("08", airport.getRunway(8).getRunwayNumberString());
        assertEquals("09", airport.getRunway(9).getRunwayNumberString());
        assertEquals("10", airport.getRunway(10).getRunwayNumberString());

        for (int i = 0; i < 10; i++) {
            assertTrue(airport.getRunway(i).getRunwayNumberString().matches("\\d{2}"));
        }

    }
    
}
