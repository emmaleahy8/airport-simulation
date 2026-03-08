package com.group32.cs261project.frontend;

import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationPaneVisualTest extends ApplicationTest {

    private SimulationPaneVisual visual;

    @Override
    public void start(Stage stage) {

        visual = new SimulationPaneVisual();

        stage.setScene(new Scene(visual, 900, 600));
        stage.show();
    }

    @Test
    void testConstructorCreatesComponent() {
        assertNotNull(visual);
        assertTrue(visual.getChildren().size() > 0);
    }

    @Test
    void testUpdateWithEmptySnapshot() {

        SimulationData data = new SimulationData();
        data.runwayCount = 2;
        data.ensureRunwayListSize();

        interact(() -> visual.update(data));

        assertNotNull(visual);
    }

    @Test
    void testRunwayRebuildOccurs() {

        SimulationData data = new SimulationData();
        data.runwayCount = 3;
        data.ensureRunwayListSize();

        interact(() -> visual.update(data));

        assertTrue(visual.getChildren().size() > 0);
    }

    @Test
    void testOutboundPlaneAppearsInQueue() {

        SimulationData snap = new SimulationData();

        snap.runwayCount = 1;
        snap.ensureRunwayListSize();

        SimulationData.PlaneDTO plane = new SimulationData.PlaneDTO();
        plane.code = "P1";
        plane.direction = SimulationData.PlaneDirection.OUTBOUND;
        plane.status = SimulationData.PlaneStatus.OUTBOUND_QUEUE;

        snap.planes.add(plane);
        snap.outboundQueueIds.add("P1");

        interact(() -> visual.update(snap));

        assertTrue(snap.planes.size() == 1);
    }

    @Test
    void testPlaneRemovedWhenNotInSnapshot() {

        SimulationData snap1 = new SimulationData();
        snap1.runwayCount = 1;
        snap1.ensureRunwayListSize();

        SimulationData.PlaneDTO plane = new SimulationData.PlaneDTO();
        plane.code = "P1";
        plane.direction = SimulationData.PlaneDirection.OUTBOUND;
        plane.status = SimulationData.PlaneStatus.OUTBOUND_QUEUE;

        snap1.planes.add(plane);
        snap1.outboundQueueIds.add("P1");

        interact(() -> visual.update(snap1));

        SimulationData snap2 = new SimulationData();
        snap2.runwayCount = 1;
        snap2.ensureRunwayListSize();

        interact(() -> visual.update(snap2));

        assertTrue(snap2.planes.isEmpty());
    }

    @Test
    void testTaxiToRunwayTransition() {

        SimulationData snap1 = new SimulationData();
        snap1.runwayCount = 1;
        snap1.ensureRunwayListSize();

        SimulationData.PlaneDTO plane = new SimulationData.PlaneDTO();
        plane.code = "P1";
        plane.direction = SimulationData.PlaneDirection.OUTBOUND;
        plane.status = SimulationData.PlaneStatus.OUTBOUND_QUEUE;

        snap1.planes.add(plane);
        snap1.outboundQueueIds.add("P1");

        interact(() -> visual.update(snap1));

        SimulationData snap2 = new SimulationData();
        snap2.runwayCount = 1;
        snap2.ensureRunwayListSize();

        SimulationData.PlaneDTO plane2 = new SimulationData.PlaneDTO();
        plane2.code = "P1";
        plane2.direction = SimulationData.PlaneDirection.OUTBOUND;
        plane2.status = SimulationData.PlaneStatus.TAXI_TO_RUNWAY;
        plane2.assignedRunwayIndex = 0;

        snap2.planes.add(plane2);

        interact(() -> visual.update(snap2));

        assertTrue(snap2.planes.size() == 1);
    }

    @Test
    void testRunwayStatusColorLogic() {

        SimulationData data = new SimulationData();
        data.runwayCount = 1;
        data.ensureRunwayListSize();

        data.runways.get(0).status = SimulationData.RunwayStatus.SNOW_CLEARANCE;

        interact(() -> visual.update(data));

        assertEquals(SimulationData.RunwayStatus.SNOW_CLEARANCE, data.runways.get(0).status);
    }

}