package com.group32.cs261project.frontend;

import com.group32.cs261project.App;

import javafx.scene.control.Labeled;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Consumer;

public class ResultsPageIT extends ApplicationTest {

    private ResultsPage page;
    private TestApp app;
    private SimulationService sim;

    @Override
    public void start(Stage stage) {
        app = new TestApp();
        sim = new StubSimulationService();

        page = new ResultsPage(app, sim);

        stage.setScene(new Scene(page.getView(), 900, 600));
        stage.show();
    }

    @AfterEach
    void cleanup() throws Exception {
        FxToolkit.cleanupStages();
        FxToolkit.hideStage();
    }

    @Test
    void testGetViewReturnsRoot() {
        Parent view = page.getView();
        assertNotNull(view);
    }

    @Test
    void testOnEnterPopulatesLabels() {

        SimulationData data = buildSimulationData();

        interact(() -> page.onEnter(data));

        assertTrue(lookup("10.00 aircraft/hour").tryQuery().isPresent());
        assertTrue(lookup("2").tryQuery().isPresent());
        assertTrue(lookup("30.00 minutes").tryQuery().isPresent());
    }

    @Test
    void testRunwayModesDisplayed() {

        SimulationData data = buildSimulationData();

        interact(() -> page.onEnter(data));

        assertTrue(
            lookup((Node n) ->
                (n instanceof Labeled l) && l.getText() != null && l.getText().contains("Runway 1")
            ).tryQuery().isPresent()
        );
    }

    @Test
    void testQueueStatisticsDisplayed() {

        SimulationData data = buildSimulationData();

        interact(() -> page.onEnter(data));

        assertTrue(lookup("5").tryQuery().isPresent());
    }

    @Test
    void testBackButtonNavigatesToConfigure() {

        clickOn("Back to Configure");

        assertEquals(AppState.CONFIGURE, app.lastState);
    }

    @Test
    void testCardsExist() {

        assertTrue(lookup("General Information").tryQuery().isPresent());
        assertTrue(lookup("Take-off Queue").tryQuery().isPresent());
        assertTrue(lookup("Holding Pattern").tryQuery().isPresent());
        assertTrue(lookup("Delays").tryQuery().isPresent());
        assertTrue(lookup("Cancellations and Diversions").tryQuery().isPresent());
    }

    private SimulationData buildSimulationData() {

        SimulationData d = new SimulationData();

        d.runwayCount = 2;
        d.inboundRatePerHour = 10;
        d.outboundRatePerHour = 12;
        d.maxWaitMinutes = 30;

        d.ensureRunwayListSize();

        d.runways.get(0).mode = SimulationData.RunwayMode.MIXED;
        d.runways.get(0).status = SimulationData.RunwayStatus.AVAILABLE;

        d.runways.get(1).mode = SimulationData.RunwayMode.LANDING;
        d.runways.get(1).status = SimulationData.RunwayStatus.SNOW_CLEARANCE;

        d.maxTakeoffQueue = 5;
        d.avgTakeoffQueue = 3.5;

        d.maxHoldingQueue = 4;
        d.avgHoldingQueue = 2.2;

        d.maxInboundDelayMin = 7;
        d.avgInboundDelayMin = 4.5;

        d.cancelledCount = 1;
        d.divertedCount = 2;

        return d;
    }

    private static class TestApp extends App {

        AppState lastState;

        @Override
        public void switchTo(AppState next, Object data) {
            this.lastState = next;
        }
    }

    private static class StubSimulationService implements SimulationService {

        @Override
        public void start(SimulationData config, Consumer<SimulationData> onSnapshot) { }

        @Override
        public void updateConfig(SimulationData newConfig) { }

        @Override
        public SimulationData stop() {
            return new SimulationData();
        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public void pause() { }

        @Override
        public void resume() { }

        @Override
        public boolean isPaused() {
            return false;
        }
    }
}