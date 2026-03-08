package com.group32.cs261project.frontend;

import com.group32.cs261project.App;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Consumer;

public class RunningPageIT extends ApplicationTest {

    private RunningPage page;
    private TestApp app;
    private TestSimulationService sim;

    @Override
    public void start(Stage stage) {

        app = new TestApp();
        sim = new TestSimulationService();

        page = new RunningPage(app, sim);

        stage.setScene(new Scene(page.getView(), 1000, 700));
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
    void testOnEnterStartsSimulation() {

        SimulationData data = buildData();

        interact(() -> page.onEnter(data));

        assertTrue(sim.started);
    }

    @Test
    void testMetricsLabelsUpdateFromSnapshot() {

        SimulationData config = buildData();

        interact(() -> page.onEnter(config));

        SimulationData snap = buildData();
        snap.simMinute = 5;
        snap.holdingQueue = 3;
        snap.takeoffQueue = 2;

        sim.pushSnapshot(snap);

        sleep(100);

        assertTrue(lookup("5").tryQuery().isPresent());
        assertTrue(lookup("3").tryQuery().isPresent());
        assertTrue(lookup("2").tryQuery().isPresent());
    }

    @Test
    void testPauseButtonTogglesPause() {

        SimulationData data = buildData();
        interact(() -> page.onEnter(data));

        clickOn("Pause");
        assertTrue(sim.paused);

        clickOn("Resume");
        assertFalse(sim.paused);
    }

    @Test
    void testEndSimulationNavigatesToResults() {

        SimulationData data = buildData();
        interact(() -> page.onEnter(data));

        clickOn("End Simulation");

        assertEquals(AppState.RESULTS, app.lastState);
    }

    @Test
    void testRestartNavigatesToConfigure() {

        SimulationData data = buildData();
        interact(() -> page.onEnter(data));

        clickOn("Restart");

        assertEquals(AppState.CONFIGURE, app.lastState);
    }

    @Test
    void testRunwayTableBuilds() {

        SimulationData data = buildData();
        data.runwayCount = 3;

        interact(() -> page.onEnter(data));

        assertTrue(lookup("Runway 1:").tryQuery().isPresent());
        assertTrue(lookup("Runway 2:").tryQuery().isPresent());
        assertTrue(lookup("Runway 3:").tryQuery().isPresent());
    }

    @Test
    void testPrettyFormattingAppears() {

        SimulationData data = buildData();

        data.runways.get(0).mode = SimulationData.RunwayMode.MIXED;
        data.runways.get(0).status = SimulationData.RunwayStatus.AVAILABLE;

        interact(() -> page.onEnter(data));

        assertTrue(lookup("Mixed").tryQuery().isPresent());
    }

    @Test
    void testOnExitStopsSpinner() {

        assertDoesNotThrow(() -> page.onExit());
    }

    private SimulationData buildData() {

        SimulationData d = new SimulationData();

        d.runwayCount = 2;
        d.ensureRunwayListSize();

        d.runways.get(0).mode = SimulationData.RunwayMode.MIXED;
        d.runways.get(0).status = SimulationData.RunwayStatus.AVAILABLE;

        d.runways.get(1).mode = SimulationData.RunwayMode.LANDING;
        d.runways.get(1).status = SimulationData.RunwayStatus.SNOW_CLEARANCE;

        return d;
    }

    private static class TestApp extends App {

        AppState lastState;

        @Override
        public void switchTo(AppState next, Object data) {
            lastState = next;
        }
    }

    private static class TestSimulationService implements SimulationService {

        boolean started = false;
        boolean paused = false;

        Consumer<SimulationData> snapshotConsumer;

        @Override
        public void start(SimulationData config, Consumer<SimulationData> onSnapshot) {
            started = true;
            snapshotConsumer = onSnapshot;
        }

        void pushSnapshot(SimulationData snap) {
            if (snapshotConsumer != null) {
                snapshotConsumer.accept(snap);
            }
        }

        @Override
        public void updateConfig(SimulationData newConfig) { }

        @Override
        public SimulationData stop() {
            return new SimulationData();
        }

        @Override
        public boolean isRunning() {
            return started;
        }

        @Override
        public void pause() {
            paused = true;
        }

        @Override
        public void resume() {
            paused = false;
        }

        @Override
        public boolean isPaused() {
            return paused;
        }
    }
}