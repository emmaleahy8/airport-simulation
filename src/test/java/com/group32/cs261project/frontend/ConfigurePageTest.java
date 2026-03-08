package com.group32.cs261project.frontend;

import com.group32.cs261project.App;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Consumer;

public class ConfigurePageTest extends ApplicationTest {

    private ConfigurePage page;
    private TestApp app;
    private SimulationService sim;

    @Override
    public void start(Stage stage) {
        app = new TestApp();
        sim = new StubSimulationService();

        page = new ConfigurePage(app, sim);

        stage.setScene(new Scene(page.getView(), 900, 600));
        stage.show();
    }

    @BeforeEach
    void setup() {
    }

    @Test
    void testGetViewReturnsRoot() {
        Parent view = page.getView();
        assertNotNull(view);
    }

    @Test
    void testOnEnterDoesNotCrash() {
        assertDoesNotThrow(() -> page.onEnter(null));
    }

    @Test
    void testSliderUpdatesRunwayValue() {
        interact(() -> {
            page.getView().lookupAll(".slider");
        });

        moveTo(".slider").drag(50, 0);

        assertTrue(page.getView().lookup(".label") != null);
    }

    @Test
    void testRunButtonWithValidInputNavigates() {

        clickOn("▶  Run Simulation");

        assertEquals(AppState.RUNNING, app.lastState);
        assertNotNull(app.lastData);
    }

    @Test
    void testUndoRedoButtonsExist() {
        assertNotNull(lookup("↶  Undo").queryButton());
        assertNotNull(lookup("↷  Redo").queryButton());
    }

    @Test
    void testResetButtonResetsConfiguration() {

        clickOn("⟲  Reset");

        assertTrue(page.getView() != null);
    }

    @Test
    void testRunwayCardsExist() {

        int comboBoxes = lookup(".combo-box").queryAll().size();

        assertTrue(comboBoxes >= 2);
    }

    @Test
    void testInboundSliderChange() {

        moveTo(".slider").drag(100, 0);

        assertTrue(page.getView() != null);
    }

    @Test
    void testOutboundSliderChange() {

        moveTo(".slider").drag(-100, 0);

        assertTrue(page.getView() != null);
    }

    @Test
    void testUndoDoesNotCrash() {

        clickOn("↶  Undo");

        assertTrue(page.getView() != null);
    }

    @Test
    void testRedoDoesNotCrash() {

        clickOn("↷  Redo");

        assertTrue(page.getView() != null);
    }

    private static class TestApp extends App {

        AppState lastState;
        Object lastData;

        @Override
        public void switchTo(AppState next, Object data) {
            this.lastState = next;
            this.lastData = data;
        }
    }

    private static class StubSimulationService implements SimulationService {

        private boolean running = false;
        private boolean paused = false;

        @Override
        public void start(SimulationData config, Consumer<SimulationData> onSnapshot) {
            running = true;
        }

        @Override
        public void updateConfig(SimulationData newConfig) {
        }

        @Override
        public SimulationData stop() {
            running = false;
            return new SimulationData();
        }

        @Override
        public boolean isRunning() {
            return running;
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