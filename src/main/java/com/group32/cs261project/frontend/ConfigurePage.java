package com.group32.cs261project.frontend;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Line;
import javafx.beans.binding.DoubleBinding;

import java.util.ArrayList;
import java.util.List;

import com.group32.cs261project.App;
import com.group32.cs261project.frontend.HelpOverlay;
import com.group32.cs261project.frontend.HelpText;

/**
 * Configuration page for the airport simulation system.
 *
 * This page allows the user to configure all simulation inputs before
 * starting a run, including:
 * • runway count and runway settings
 * • inbound and outbound aircraft generation rates
 * • emergency probabilities
 * • simulation duration
 * • maximum takeoff queue time
 *
 * It also supports undo/redo functionality for configuration changes
 * and displays configurable runway configuration cards for each runway.
 */
public class ConfigurePage implements Page {

    private final App app;
    private final SimulationService sim;

    private final BorderPane root = new BorderPane();

    // Sliders
    private final Slider runwaySlider = new Slider(1, 10, 2);
    private final Slider inboundSlider = new Slider(1, 45, 44);
    private final Slider outboundSlider = new Slider(1, 45, 44);
    private final Slider passengerHealthEmergencySlider = new Slider(0, 100, 0);
    private final Slider mechanicalEmergencySlider = new Slider(0, 100, 0);
    private final Slider simulationTimeSlider = new Slider(1, 24, 6);
    private final Slider maxTakeoffQueueTimeSlider = new Slider(10, 60, 30);

    // Labels showing current slider values
    private final Label runwayValue = new Label();
    private final Label inboundValue = new Label();
    private final Label outboundValue = new Label();
    private final Label passengerHealthEmergencyValue = new Label();
    private final Label mechanicalEmergencyValue = new Label();
    private final Label simulationTimeValue = new Label();
    private final Label maxTakeoffQueueTimeValue = new Label();

    private final Label errorLabel = new Label();

    // Runway grid and cards - each card represents one runway and has controls for its config
    private final GridPane runwayGrid = new GridPane();
    private final List<RunwayCard> runwayCards = new ArrayList<>();

    // Holds the current config values for the simulation. When user clicks "Run", this is validated and passed to the RunningPage.
    private SimulationData data = new SimulationData();

    // ----- Undo / Redo Buttons -----
    private final List<SimulationData> undoStack = new ArrayList<>();
    private final List<SimulationData> redoStack = new ArrayList<>();
    private boolean applyingSnapshot = false;

    // References to enable and disable buttons
    private Button undoBtn;
    private Button redoBtn;

    // ----- Styling Constants -----
    // Page background
    private static final String PAGE_BG = "-fx-background-color: rgb(14, 26, 40);";    
    private static final String PANEL =
        "-fx-background-color: rgba(8, 18, 28, 0.96);" + 
        "-fx-border-color: #1f3b52;" +
        "-fx-border-width: 1.5;" +
        "-fx-border-radius: 14;" +
        "-fx-background-radius: 14;" +
        "-fx-padding: 12;" +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 14, 0.2, 0, 3);";
    
    // "Wall"
    private static final String SCREEN_WALL =
        "-fx-background-color: rgba(5, 12, 20, 0.97);" +
        "-fx-border-color: #24445b;" +
        "-fx-border-width: 1.5;" +
        "-fx-border-radius: 16;" +
        "-fx-background-radius: 16;" +
        "-fx-padding: 18;" +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.40), 18, 0.22, 0, 5);";
    
    // Runway cards (active)
    private static final String CARD =
        "-fx-background-color: linear-gradient(to bottom, #071018 0%, #081722 55%, #06111a 100%);" +
        "-fx-border-color: #1dd6ff;" +
        "-fx-border-width: 1.6;" +
        "-fx-border-radius: 12;" +
        "-fx-background-radius: 14;" +
        "-fx-padding: 14;" +
        "-fx-effect: dropshadow(gaussian, rgba(29,214,255,0.22), 14, 0.25, 0, 0);";
    
    // Runway cards (disabled)
    private static final String CARD_DISABLED =
        "-fx-background-color:  linear-gradient(to bottom, #1a232b 0%, #182028 100%);" +
        "-fx-border-color: #46535e;" +
        "-fx-border-width: 1.2;" +
        "-fx-border-radius: 12;" +
        "-fx-background-radius: 12;" +
        "-fx-padding: 14;" +
        "-fx-opacity: 0.72;";
    
    // Text styles
    private static final String TITLE_TEXT =
        "-fx-text-fill: #eaf6ff;";

    private static final String SUBTITLE_TEXT =
        "-fx-text-fill: #cccccc;";

    private static final String LABEL_TEXT =
        "-fx-text-fill: #dcebf5;";

    private static final String SMALL_TEXT =
        "-fx-text-fill: #b8cad6;";

    private static final String MONITOR_TITLE =
        "-fx-text-fill: #63e6ff;";

    private static final String MONITOR_LABEL =
        "-fx-text-fill: #d7f8ff;";

    // Button styles
    private static final String BTN_PRIMARY =
        "-fx-background-color: linear-gradient(to bottom, #03418be6, #1a6eff);" +
        "-fx-text-fill: white;" +
        "-fx-font-weight: 700;" +
        "-fx-padding: 10 14;" +
        "-fx-background-radius: 8;" +
        "-fx-border-radius: 8;" +
        "-fx-border-color: #2a4658;";

    private static final String BTN_SOFT =
        "-fx-background-color: linear-gradient(to bottom, #1b4fff, #163fc7);" +
        "-fx-text-fill: white;" +
        "-fx-font-weight: 700;" +
        "-fx-padding: 8 12;" +
        "-fx-background-radius: 8;";
    
    private static final String COMBO_STYLE =
        "-fx-background-color: #0d1d29;" +
        "-fx-control-inner-background: #0d1d29;" +
        "-fx-background-insets: 0;" +
        "-fx-background-radius: 6;" +
        "-fx-border-color: #2d6678;" +
        "-fx-border-radius: 6;" +
        "-fx-text-fill: white;" +
        "-fx-prompt-text-fill: white;" +
        "-fx-mark-color: white;";

        /**
     * Creates a new configuration page.
     *
     * This constructor initialises the page layout, creates all major UI
     * sections, configures slider behaviour, loads the default simulation
     * data into the controls, and prepares the runway card grid.
     *
     * @param app the main application controller used for page navigation
     * @param sim the simulation service used to run the simulation
     */
    public ConfigurePage(App app, SimulationService sim) {
        this.app = app;
        this.sim = sim;

        // Apply page background style and padding
        root.setStyle(PAGE_BG);
        root.setPadding(new Insets(16));

        // ----- Header (title + subtitle) -----
        VBox header = new VBox(4);
        Label title = new Label("✈ Airport Traffic Simulation System");
        title.setFont(Font.font(null, FontWeight.BOLD, 30));
        title.setStyle(TITLE_TEXT);
        Label subtitle = new Label("Model aircraft throughput and analyse runway configurations.");
        subtitle.setStyle(SUBTITLE_TEXT);

        header.getChildren().addAll(title, subtitle);
        header.setPadding(new Insets(0, 0, 12, 0));
        root.setTop(header);

        // ----- Main Layout: left sidebar + right content -----
        VBox pageBody = new VBox(5);
        pageBody.setPadding(new Insets(6, 0, 0, 0));
        pageBody.setFillWidth(true);
        pageBody.setMinWidth(1200);
        pageBody.setMinHeight(820);

        StackPane atcView = buildAtcView();
        atcView.setMaxWidth(Double.MAX_VALUE);

        HBox bottomDashboard = new HBox(18);
        bottomDashboard.setAlignment(Pos.BOTTOM_LEFT);
        bottomDashboard.setFillHeight(true);
        bottomDashboard.setMinHeight(420);

        VBox leftSidebar = buildLeftSidebar();
        VBox rightContent = buildRightContent();

        HBox.setHgrow(rightContent, Priority.ALWAYS); // Let right side expand to fill remaining width
        VBox.setVgrow(bottomDashboard, Priority.ALWAYS);
        bottomDashboard.getChildren().addAll(leftSidebar, rightContent);

        pageBody.getChildren().addAll(atcView, bottomDashboard);

        ScrollPane pageScroll = new ScrollPane(pageBody);
        pageScroll.setFitToWidth(true);
        pageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        pageScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        pageScroll.setPannable(true);
        pageScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        root.setCenter(pageScroll);

        // Initial Setup
        initSliders();
        loadIntoControls(data);
        buildRunwayCards();
        refreshRunwayGrid();

        runwaySlider.setOnMousePressed(e -> pushUndoSnapshot());
        inboundSlider.setOnMousePressed(e -> pushUndoSnapshot());
        outboundSlider.setOnMousePressed(e -> pushUndoSnapshot());
        passengerHealthEmergencySlider.setOnMousePressed(e -> pushUndoSnapshot());
        mechanicalEmergencySlider.setOnMousePressed(e -> pushUndoSnapshot());
        simulationTimeSlider.setOnMousePressed(e -> pushUndoSnapshot());
        maxTakeoffQueueTimeSlider.setOnMousePressed(e -> pushUndoSnapshot());
    }

    // ==================================
    // ----- Page Interface Methods -----
    // ==================================

    /**
     * Returns the root view for this page wrapped in the help overlay.
     *
     * @return the page view
     */
    @Override
    public Parent getView() {
        return HelpOverlay.wrap(root, "Configure Page", HelpText.configurePage());
    }

    /**
     * Called when the page becomes active.
     *
     * This implementation preserves the current configuration state and
     * does not reset the controls.
     *
     * @param ignored optional navigation data, not used on this page
     */
    @Override
    public void onEnter(Object ignored) {
        // keep last state
    }

    /**
     * Builds the left-hand sidebar containing the main simulation controls.
     *
     * This includes panels for:
     * • simulation control
     * • aircraft generation
     * • emergency generation
     *
     * @return the fully constructed left sidebar
     */
    private VBox buildLeftSidebar() {
        VBox left = new VBox(8); 
        left.setPrefWidth(330);
        left.setAlignment(Pos.BOTTOM_LEFT);
        VBox.setVgrow(left, Priority.ALWAYS);

        // ----- Simulation Control panel -----
        VBox simPanel = new VBox(8); 
        simPanel.setStyle(PANEL); 

        Label simTitle = new Label("Simulation Control");
        simTitle.setFont(Font.font(null, FontWeight.BOLD, 20));
        simTitle.setStyle(MONITOR_LABEL);

        // Number of runways
        runwayValue.setFont(Font.font(null, FontWeight.BOLD, 12));

        VBox runwayRow = new VBox(6); 
        Label runwayLabel = new Label("Number of Runways: ");
        HBox runwayLine = new HBox(6, runwayLabel, runwayValue); 
        runwayLine.setAlignment(Pos.CENTER_LEFT);
        runwayRow.getChildren().addAll(runwayLine, runwaySlider);

        // Style runway labels
        runwayLabel.setStyle(LABEL_TEXT);
        runwayValue.setStyle(MONITOR_TITLE);

        // ----- Simulation Duration -----
        simulationTimeValue.setFont(Font.font(null, FontWeight.BOLD, 12));

        VBox durationRow = new VBox(6);
        Label durationLabel = new Label("Simulation Duration:");

        HBox durationLine = new HBox(6, durationLabel, simulationTimeValue, new Label("hours"));
        durationLine.setAlignment(Pos.CENTER_LEFT);

        durationRow.getChildren().addAll(durationLine, simulationTimeSlider);

        // styling
        durationLabel.setStyle(LABEL_TEXT);
        ((Label) durationLine.getChildren().get(2)).setStyle(SMALL_TEXT);
        simulationTimeValue.setStyle(MONITOR_TITLE);

        // ----- Max takeoff queue time -----
        maxTakeoffQueueTimeValue.setFont(Font.font(null, FontWeight.BOLD, 12));

        VBox maxTakeoffQueueTimeRow = new VBox(6);
        Label maxTakeoffQueueTimeLabel = new Label("Max. time in takeoff queue:");

        HBox maxTakeoffQueueTimeLine = new HBox(
            6,
            maxTakeoffQueueTimeLabel,
            maxTakeoffQueueTimeValue,
            new Label("mins")
        );
        maxTakeoffQueueTimeLine.setAlignment(Pos.CENTER_LEFT);

        maxTakeoffQueueTimeRow.getChildren().addAll(maxTakeoffQueueTimeLine, maxTakeoffQueueTimeSlider);

        // styling
        maxTakeoffQueueTimeLabel.setStyle(LABEL_TEXT);
        ((Label) maxTakeoffQueueTimeLine.getChildren().get(2)).setStyle(SMALL_TEXT);
        maxTakeoffQueueTimeValue.setStyle(MONITOR_TITLE);

        // Run button - on click, confirm inputs are valid, then switch to RunningPage and pass the current config data
        Button runBtn = new Button("▶  Run Simulation");
        runBtn.setMaxWidth(Double.MAX_VALUE);
        runBtn.setStyle(BTN_PRIMARY);
        runBtn.setOnAction(e -> onRun());

        // Undo/Redo/Reset buttons
        HBox undoRedoReset = new HBox(10);
        undoBtn = new Button("↶  Undo");
        redoBtn = new Button("↷  Redo");
        Button resetBtn = new Button("⟲  Reset");

        undoBtn.setStyle(BTN_SOFT);
        redoBtn.setStyle(BTN_SOFT);
        resetBtn.setStyle(BTN_SOFT);

        undoBtn.setOnAction(e -> undo());
        redoBtn.setOnAction(e -> redo());
        
        updateUndoRedoButtons();

        // Reset button clears all inputs and resets to default config
        resetBtn.setOnAction(e -> {
            // Allow reset to be undoable by taking a snapshot
            pushUndoSnapshot();
            applySnapshot(new SimulationData());
        });

        undoBtn.setMaxWidth(Double.MAX_VALUE);
        redoBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(undoBtn, Priority.ALWAYS);
        HBox.setHgrow(redoBtn, Priority.ALWAYS);
        HBox.setHgrow(resetBtn, Priority.ALWAYS);

        undoRedoReset.getChildren().addAll(undoBtn, redoBtn, resetBtn);

        simPanel.getChildren().addAll(simTitle, runwayRow, durationRow, maxTakeoffQueueTimeRow, runBtn, undoRedoReset);

        // ----- Aircraft Generation panel -----
        VBox aircraftPanel = new VBox(8); 
        aircraftPanel.setStyle(PANEL);

        Label aircraftTitle = new Label("Aircraft Generation");
        aircraftTitle.setFont(Font.font(null, FontWeight.BOLD, 20));
        aircraftTitle.setStyle(MONITOR_LABEL);

        // Inbound flow rate slider + label
        inboundValue.setFont(Font.font(null, FontWeight.BOLD, 12));

        VBox inboundBox = new VBox(5);
        HBox inboundLine = new HBox(5, new Label("Arrival Rate:"), inboundValue, new Label("/hour"));
        inboundLine.setAlignment(Pos.CENTER_LEFT);
        inboundBox.getChildren().addAll(inboundLine, inboundSlider);

        // Outbound flow rate slider + label
        outboundValue.setFont(Font.font(null, FontWeight.BOLD, 12));

        VBox outboundBox = new VBox(5);
        HBox outboundLine = new HBox(5, new Label("Departure Rate:"), outboundValue, new Label("/hour"));
        outboundLine.setAlignment(Pos.CENTER_LEFT);
        outboundBox.getChildren().addAll(outboundLine, outboundSlider);

        // Inbound/Outbound Flow Rate Styling
        ((Label) inboundLine.getChildren().get(0)).setStyle(LABEL_TEXT);
        ((Label) inboundLine.getChildren().get(2)).setStyle(SMALL_TEXT);
        inboundValue.setStyle(MONITOR_TITLE);

        ((Label) outboundLine.getChildren().get(0)).setStyle(LABEL_TEXT);
        ((Label) outboundLine.getChildren().get(2)).setStyle(SMALL_TEXT);
        outboundValue.setStyle(MONITOR_TITLE);

        aircraftPanel.getChildren().addAll(aircraftTitle, inboundBox, outboundBox);

        // ----- Emergency Generation panel -----
        VBox emergencyPanel = new VBox(8); 
        emergencyPanel.setStyle(PANEL);

        Label emergencyTitle = new Label("Emergency Generation");
        emergencyTitle.setFont(Font.font(null, FontWeight.BOLD, 20));
        emergencyTitle.setStyle(MONITOR_LABEL);

        // Passenger Emergency slider + label
        passengerHealthEmergencyValue.setFont(Font.font(null, FontWeight.BOLD, 12));

        VBox passengerHealthEmergencyBox = new VBox(5);
        HBox passengerHealthEmergencyLine = new HBox(5, new Label("Passenger Health Emergencies:"), passengerHealthEmergencyValue, new Label("%"));
        passengerHealthEmergencyLine.setAlignment(Pos.CENTER_LEFT);
        passengerHealthEmergencyBox.getChildren().addAll(passengerHealthEmergencyLine, passengerHealthEmergencySlider);

        // Mechanical Emergency slider + label
        mechanicalEmergencyValue.setFont(Font.font(null, FontWeight.BOLD, 12));

        VBox mechanicalEmergencyBox = new VBox(5);
        HBox mechanicalEmergencyLine = new HBox(5, new Label("Mechanical Emergencies:"), mechanicalEmergencyValue, new Label("%"));
        mechanicalEmergencyLine.setAlignment(Pos.CENTER_LEFT);
        mechanicalEmergencyBox.getChildren().addAll(mechanicalEmergencyLine, mechanicalEmergencySlider);

        // Emergency Generation Styling
        ((Label) passengerHealthEmergencyLine.getChildren().get(0)).setStyle(LABEL_TEXT);
        ((Label) passengerHealthEmergencyLine.getChildren().get(2)).setStyle(SMALL_TEXT);
        passengerHealthEmergencyValue.setStyle(MONITOR_TITLE);

        ((Label) mechanicalEmergencyLine.getChildren().get(0)).setStyle(LABEL_TEXT);
        ((Label) mechanicalEmergencyLine.getChildren().get(2)).setStyle(SMALL_TEXT);
        mechanicalEmergencyValue.setStyle(MONITOR_TITLE);

        emergencyPanel.getChildren().addAll(emergencyTitle, passengerHealthEmergencyBox, mechanicalEmergencyBox);

        left.getChildren().addAll(simPanel, aircraftPanel, emergencyPanel);
        
        return left;
    }

    /**
     * Builds the right-hand content area of the page.
     *
     * This section contains the runway configuration monitor wall and
     * the grid of runway cards used to configure each runway.
     *
     * @return the fully constructed right-side content area
     */
    private VBox buildRightContent() {
        VBox right = new VBox(16);
        right.setAlignment(Pos.BOTTOM_LEFT);
        right.setFillWidth(true);
        VBox.setVgrow(right, Priority.ALWAYS);

        // ----- "Monitor wall" -----
        VBox monitorWall = new VBox(12);
        monitorWall.setFillWidth(true);
        monitorWall.setAlignment(Pos.TOP_LEFT);
        monitorWall.setStyle(SCREEN_WALL);
        VBox.setVgrow(monitorWall, Priority.ALWAYS);
        monitorWall.setPrefHeight(587);
        monitorWall.setMaxHeight(587);

        Label runwayTitle = new Label("Runway Configuration");
        runwayTitle.setFont(Font.font(null, FontWeight.BOLD, 22));
        runwayTitle.setStyle(MONITOR_LABEL);

        runwayGrid.setHgap(18);
        runwayGrid.setVgap(0);
        runwayGrid.setPadding(new Insets(8));
        VBox.setVgrow(runwayGrid, Priority.ALWAYS);

        if (runwayGrid.getColumnConstraints().isEmpty()) {
            for (int c = 0; c < 5; c++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setHgrow(Priority.ALWAYS);
                cc.setFillWidth(true);
                cc.setPercentWidth(20);
                runwayGrid.getColumnConstraints().add(cc);
            }
        }
        if (runwayGrid.getRowConstraints().isEmpty()) {
            for (int r = 0; r < 5; r++) {
                RowConstraints rc = new RowConstraints();

                // spacer rows: 0, 2, 4
                if (r % 2 == 0) {
                    rc.setVgrow(Priority.ALWAYS);
                } else {
                    rc.setVgrow(Priority.NEVER);
                }

                runwayGrid.getRowConstraints().add(rc);
            }
        }

        monitorWall.getChildren().addAll(runwayTitle, runwayGrid);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        right.getChildren().addAll(spacer, monitorWall);

        return right;
    }
    
    /**
     * Initialises all sliders used on the page.
     *
     * This includes setting tick behaviour, snap settings, and registering
     * listeners so that slider movements update the underlying simulation
     * configuration data and visible value labels.
     */
    private void initSliders() {
        runwaySlider.setMajorTickUnit(1);
        runwaySlider.setMinorTickCount(0);
        runwaySlider.setSnapToTicks(true); // Snap to integers
        runwaySlider.setShowTickLabels(false);
        runwaySlider.setShowTickMarks(false);

        inboundSlider.setMajorTickUnit(5);
        inboundSlider.setShowTickLabels(false);
        inboundSlider.setShowTickMarks(false);

        outboundSlider.setMajorTickUnit(5);
        outboundSlider.setShowTickLabels(false);
        outboundSlider.setShowTickMarks(false);

        simulationTimeSlider.setMajorTickUnit(1);
        simulationTimeSlider.setMinorTickCount(0);
        simulationTimeSlider.setSnapToTicks(true);
        simulationTimeSlider.setShowTickLabels(false);
        simulationTimeSlider.setShowTickMarks(false);

        maxTakeoffQueueTimeSlider.setMajorTickUnit(10);
        maxTakeoffQueueTimeSlider.setMinorTickCount(0);
        maxTakeoffQueueTimeSlider.setSnapToTicks(true);
        maxTakeoffQueueTimeSlider.setShowTickLabels(false);
        maxTakeoffQueueTimeSlider.setShowTickMarks(false);

        // ----- Listeners -----
        runwaySlider.valueProperty().addListener((obs, o, n) -> {
            if (applyingSnapshot) return;
                data.runwayCount = (int) Math.round(n.doubleValue());
                runwayValue.setText(String.valueOf(data.runwayCount));
                data.ensureRunwayListSize();
                refreshRunwayGrid();
        });

        inboundSlider.valueProperty().addListener((obs, o, n) -> {
            if (applyingSnapshot) return;
            data.inboundRatePerHour = (int) Math.round(n.doubleValue());
            inboundValue.setText(String.valueOf(data.inboundRatePerHour));
        });

        outboundSlider.valueProperty().addListener((obs, o, n) -> {
            if (applyingSnapshot) return;
            data.outboundRatePerHour = (int) Math.round(n.doubleValue());
            outboundValue.setText(String.valueOf(data.outboundRatePerHour));
        });

        passengerHealthEmergencySlider.valueProperty().addListener((obs, o, n) -> {
            if (applyingSnapshot) return;
            data.passengerHealthEmergencyProbability = (int) Math.round(n.doubleValue());
            passengerHealthEmergencyValue.setText(String.valueOf(data.passengerHealthEmergencyProbability));
        });

        mechanicalEmergencySlider.valueProperty().addListener((obs, o, n) -> {
            if (applyingSnapshot) return;
            data.mechanicalEmergencyProbability = (int) Math.round(n.doubleValue());
            mechanicalEmergencyValue.setText(String.valueOf(data.mechanicalEmergencyProbability));
        });

        simulationTimeSlider.valueProperty().addListener((obs, o, n) -> {
            if (applyingSnapshot) return;
            data.simulationHours = (int) Math.round(n.doubleValue());
            simulationTimeValue.setText(String.valueOf(data.simulationHours));
        });

        maxTakeoffQueueTimeSlider.valueProperty().addListener((obs, o, n) -> {
            if (applyingSnapshot) return;
            data.maxWaitMinutes = (int) Math.round(n.doubleValue());
            maxTakeoffQueueTimeValue.setText(String.valueOf(data.maxWaitMinutes));
        });
    }

    /**
     * Creates the runway card components used in the runway configuration grid.
     *
     * Cards are created for all ten possible runway positions so that inactive
     * runways can still be shown visually in a disabled state.
     */
    private void buildRunwayCards() {
        runwayCards.clear(); // Clear old UI cards
        data.ensureRunwayListSize();

        for (int i = 1; i <= 10; i++) {
            // Ensure we have runway cards for 1 to runwayCount only,
            // but cards 1-10 always exist visually.
            SimulationData.RunwayConfig cfg = (i <= data.runways.size())
                    ? data.runways.get(i - 1)
                    : new SimulationData.RunwayConfig(i);

            runwayCards.add(new RunwayCard(cfg));
        }
    }

    /**
     * Refreshes the runway card grid to reflect the current runway count
     * and runway configuration data.
     *
     * Active runway cards are bound to the current simulation data and enabled,
     * while inactive runway cards remain visible but disabled.
    */
    private void refreshRunwayGrid() {
        data.ensureRunwayListSize();

        runwayGrid.getChildren().clear();

        // For each card 1-10, get the matching UI object and add to the grid
        for (int i = 1; i <= 10; i++) {
            RunwayCard card = runwayCards.get(i - 1);

            // Enable active runways - bind cards to the real data object and enable inputs
            if (i <= data.runwayCount) {
                card.bindTo(data.runways.get(i - 1));
                card.setEnabled(true);
            } else {
                // Disabled cards still show but are greyed out
                card.setEnabled(false);
            }

            // Compute grid position
            int idx = i - 1;
            int visualRow = idx / 5;          // 0 or 1
            int row = visualRow * 2 + 1;      // puts rows at 1 and 3
            int col = idx % 5;

            runwayGrid.add(card.root, col, row);
        }
    }

    /**
     * Loads simulation configuration data into the page controls.
     *
     * This updates the slider positions and numeric labels so that the UI
     * reflects the values stored in the provided {@link SimulationData}.
     *
     * @param d the simulation configuration to display in the controls
     */
    private void loadIntoControls(SimulationData d) {
        d.ensureRunwayListSize();

        // Set UI slider positions from stored data
        runwaySlider.setValue(d.runwayCount);
        inboundSlider.setValue(d.inboundRatePerHour);
        outboundSlider.setValue(d.outboundRatePerHour);
        passengerHealthEmergencySlider.setValue(d.passengerHealthEmergencyProbability);
        mechanicalEmergencySlider.setValue(d.mechanicalEmergencyProbability);
        simulationTimeSlider.setValue(d.simulationHours);
        maxTakeoffQueueTimeSlider.setValue(d.maxWaitMinutes);


        // Set numeric labels accordingly
        runwayValue.setText(String.valueOf(d.runwayCount));
        inboundValue.setText(String.valueOf(d.inboundRatePerHour));
        outboundValue.setText(String.valueOf(d.outboundRatePerHour));
        passengerHealthEmergencyValue.setText(String.valueOf(d.passengerHealthEmergencyProbability));
        mechanicalEmergencyValue.setText(String.valueOf(d.mechanicalEmergencyProbability));
        simulationTimeValue.setText(String.valueOf(d.simulationHours));
        maxTakeoffQueueTimeValue.setText(String.valueOf(d.maxWaitMinutes));
    }

    /**
     * Handles the Run Simulation action.
     *
     * This method reads the current control values, validates the input,
     * writes the validated values back into the page's simulation data,
     * and switches to the running page if all inputs are valid.
     *
     * If validation fails, an error message is displayed and navigation
     * does not occur.
     */
    private void onRun() {
        // Read values from sliders
        int runways = (int) Math.round(runwaySlider.getValue());
        int in = (int) Math.round(inboundSlider.getValue());
        int out = (int) Math.round(outboundSlider.getValue());
        int hours = (int) Math.round(simulationTimeSlider.getValue());
        int maxWait = (int) Math.round(maxTakeoffQueueTimeSlider.getValue());

        // Validate runway count and flow rates - if invalid, show error message and don't proceed
        if (runways < 1 || runways > 10) {
            errorLabel.setText("Runway count must be between 1 and 10.");
            return;
        }
        if (in < 0 || in > 45 || out < 0 || out > 45) {
            errorLabel.setText("Flow rates must be between 0 and 30 per hour.");
            return;
        }
        if (maxWait < 10 || maxWait > 60) {
            errorLabel.setText("Max. time in takeoff queue must be between 10 and 60 minutes.");
            return;
        }

        errorLabel.setText(""); 

        // Write validated values back into data
        data.runwayCount = runways;
        data.inboundRatePerHour = in;
        data.outboundRatePerHour = out;
        data.simulationHours = hours;
        data.maxWaitMinutes = maxWait;
        data.ensureRunwayListSize();

        app.switchTo(AppState.RUNNING, data.copy()); // Navigate to Running Page and pass a copy of the current config data
    }

    /**
     * UI component representing a single runway configuration card.
     *
     * Each card displays the runway identifier together with controls for:
     * • operational status
     * • operating mode
     *
     * A card can be bound to a specific {@link SimulationData.RunwayConfig}
     * object and can also be enabled or disabled depending on whether the
     * runway is currently active.
     */
    private class RunwayCard {
        private final VBox root = new VBox(10); // Set runway card size and spacing

        private final Label title = new Label();
        private final ComboBox<SimulationData.RunwayStatus> statusBox = new ComboBox<>();
        private final ComboBox<SimulationData.RunwayMode> modeBox = new ComboBox<>();

        private SimulationData.RunwayConfig bound;

        /**
         * Creates a runway card bound to the given runway configuration.
         *
         * The card initialises its labels, combo boxes, styling, and listeners
         * so that UI changes are written back into the bound runway data.
         *
         * @param cfg the initial runway configuration for this card
         */
        RunwayCard(SimulationData.RunwayConfig cfg) {
            root.setStyle(CARD);
            root.setMinWidth(180);
            root.setPrefWidth(220);
            root.setMinHeight(170);

            title.setFont(Font.font(null, FontWeight.BOLD, 16));
            title.setStyle(MONITOR_TITLE);

            Label statusLabel = new Label("Operational Status");
            statusLabel.setStyle(MONITOR_LABEL);

            statusBox.getItems().setAll(SimulationData.RunwayStatus.values()); // Populate dropdown with enum values
            statusBox.setStyle(COMBO_STYLE);
            statusBox.setMaxWidth(Double.MAX_VALUE);

            Label modeLabel = new Label("Operating Mode");
            modeLabel.setStyle(MONITOR_LABEL);

            modeBox.getItems().setAll(SimulationData.RunwayMode.values()); // Populate dropdown with enum values
            modeBox.setStyle(COMBO_STYLE);
            modeBox.setMaxWidth(Double.MAX_VALUE);

            statusBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(SimulationData.RunwayStatus item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.toString());
                    setStyle("-fx-text-fill: white;");
                }
            });

            modeBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(SimulationData.RunwayMode item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.toString());
                    setStyle("-fx-text-fill: white;");
                }
            });

            VBox content = new VBox(10, statusLabel, statusBox, modeLabel, modeBox);
            content.setAlignment(Pos.TOP_LEFT);

            root.getChildren().addAll(title, content);

            bindTo(cfg); // Initially bind this card to the provided runway config

            // Listeners to update bound data object when user changes dropdowns - guards against null values
            statusBox.valueProperty().addListener((obs, o, n) -> {
                if (applyingSnapshot) return;
                if (bound != null && n != null) {
                    pushUndoSnapshot();
                    bound.status = n;
                }
            });
            modeBox.valueProperty().addListener((obs, o, n) -> {
                if (applyingSnapshot) return;
                if (bound != null && n != null) {
                    pushUndoSnapshot();
                    bound.mode = n;
                }
            });
        }

        /**
         * Binds this card to a specific runway configuration object.
         *
         * The card title and control values are updated to reflect the
         * provided runway data.
         *
         * @param cfg the runway configuration to bind to
         */
        void bindTo(SimulationData.RunwayConfig cfg) {
            this.bound = cfg;
            title.setText("Runway " + cfg.id + ":");
            statusBox.setValue(cfg.status);
            modeBox.setValue(cfg.mode);
        }

        /**
         * Enables or disables this runway card.
         *
         * Disabled cards remain visible but are styled differently and their
         * controls cannot be edited.
         *
         * @param enabled true to enable the card, false to disable it
         */
        void setEnabled(boolean enabled) {
            root.setStyle(enabled ? CARD : CARD_DISABLED);
            statusBox.setDisable(!enabled);
            modeBox.setDisable(!enabled);
            title.setStyle(enabled ? MONITOR_TITLE : "-fx-text-fill: #7d8a93;");
            root.setOpacity(enabled ? 1.0 : 0.82);
        }
    }

    // ================================================
    // ----- Helper Methods for Undo/Redo Buttons -----
    // ================================================

    /**
     * Saves the current configuration state to the undo stack.
     *
     * This method is called before a user change is applied so that the
     * previous state can be restored using undo.
     *
     * Any existing redo history is cleared when a new snapshot is added.
     */
    private void pushUndoSnapshot(){
        if (applyingSnapshot) return;

        // Snapshot before the change is applied
        undoStack.add(data.copy());
        // Once you make a new change, redo history is invalid
        redoStack.clear();
        updateUndoRedoButtons();
    }

    /**
     * Replaces the current page configuration with a previously saved snapshot.
     *
     * This updates the stored simulation data, reloads all controls, rebuilds
     * the runway cards, refreshes the grid, and clears any visible error message.
     *
     * @param snapshot the configuration snapshot to apply
     */
    private void applySnapshot(SimulationData snapshot) {
        applyingSnapshot = true;
        try {
            data = snapshot.copy();     // replace current data
            loadIntoControls(data);     // sliders and labels
            buildRunwayCards();         // cards list
            refreshRunwayGrid();        // bind cards to data.runways
            errorLabel.setText("");
        }
        finally {
            applyingSnapshot = false;
            updateUndoRedoButtons();
        }
    }

    /**
     * Restores the most recent configuration state from the undo stack.
     *
     * The current state is first saved to the redo stack so that the
     * change can be reversed using redo.
     */
    private void undo() {
        if (undoStack.isEmpty()) return;
        redoStack.add(data.copy());     // Save current for redo
        SimulationData prev = undoStack.remove(undoStack.size() - 1);
        applySnapshot(prev);
    }

    /**
     * Restores the most recent configuration state from the redo stack.
     *
     * The current state is first saved to the undo stack so that the
     * change can be reversed again using undo.
     */
    private void redo() {
        if (redoStack.isEmpty()) return;
        undoStack.add(data.copy());     // Save current for undo
        SimulationData next = redoStack.remove(redoStack.size() - 1);
        applySnapshot(next);
    }

    /**
     * Updates the enabled state of the undo and redo buttons.
     *
     * Each button is disabled when its corresponding history stack is empty.
     */
    private void updateUndoRedoButtons() {
        if (undoBtn != null) undoBtn.setDisable(undoStack.isEmpty());
        if (redoBtn != null) redoBtn.setDisable(redoStack.isEmpty());
    }

    // =============================
    // ----- Atc "Window" View -----
    // =============================

    /**
     * Builds the decorative ATC window view shown at the top of the page.
     *
     * This creates the background image view, scales it responsively to fit
     * the available space, and overlays the custom window frame effect.
     *
     * @return the completed ATC view container
     */
    private StackPane buildAtcView() {
        double windowHeight = 265;

        // Build window
        StackPane airportWindow = new StackPane();
        airportWindow.setPrefHeight(windowHeight);
        airportWindow.setMinHeight(windowHeight);
        airportWindow.setMaxHeight(windowHeight);
        airportWindow.setMaxWidth(Double.MAX_VALUE);
        airportWindow.setStyle("-fx-background-color: transparent;");

        var url = getClass().getResource("/com/group32/cs261project/ui/assets/atc_tower4.png"); // view image

        if (url != null) {
            Image img = new Image(url.toExternalForm());
            ImageView imageView = new ImageView(img);

            imageView.setPreserveRatio(true);
            imageView.setFitWidth(1600);
            imageView.setSmooth(true);

            airportWindow.layoutBoundsProperty().addListener((obs, oldBounds, bounds) -> {
                double paneW = bounds.getWidth();
                double paneH = bounds.getHeight();

                if (paneW <= 0 || paneH <= 0 || img.getWidth() <= 0 || img.getHeight() <= 0) {
                    return;
                }

                double imageRatio = img.getWidth() / img.getHeight();
                double paneRatio = paneW / paneH;

                if (imageRatio > paneRatio) {
                    imageView.setFitHeight(paneH);
                    imageView.setFitWidth(0);
                } else {
                    imageView.setFitWidth(paneW);
                    imageView.setFitHeight(0);
                }
            });

            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(airportWindow.widthProperty());
            clip.heightProperty().bind(airportWindow.heightProperty());
            airportWindow.setClip(clip);

            airportWindow.getChildren().add(imageView);
        } else {
            airportWindow.setStyle("-fx-background-color: #444444;");
        }

        airportWindow.getChildren().add(buildWindowOverlay());

        return airportWindow;
    }

    /**
     * Builds the custom overlay used to create the ATC window frame effect.
     *
     * The overlay consists of shaped cutouts, border lines, and divider lines
     * which together create a stylised multi-panel control-tower window.
     *
     * @return the overlay pane drawn above the background image
     */
    private Pane buildWindowOverlay() {
        double windowHeight = 266;
        double frameThickness = 4;
        double dividerThickness = 4;
        double middleCut = 18;

        String pageBg = "rgb(14, 26, 40)";     // matches page background
        String frameColor = "#2b2f34";
        String dividerColor = "#3c434a";

        Pane overlay = new Pane();
        overlay.setMouseTransparent(true);
        overlay.setPrefHeight(windowHeight);
        overlay.prefWidthProperty().bind(root.widthProperty());

        overlay.widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW.doubleValue();
            overlay.getChildren().clear();

            double x0 = 0.0;
            double x1 = w * 0.25;
            double x2 = w * 0.50;
            double x3 = w * 0.75;
            double x4 = w;

            // ----- Cutout shapes -----

            // Top-left triangle
            Polygon topLeftCut = new Polygon(
                    x0, 0.0,
                    x1, 0.0,
                    x1, middleCut
            );
            topLeftCut.setFill(javafx.scene.paint.Color.web(pageBg));

            // Top middle-left rectangle
            Rectangle topMidLeftCut = new Rectangle(x1, 0.0, x2 - x1, middleCut);
            topMidLeftCut.setFill(javafx.scene.paint.Color.web(pageBg));

            // Top middle-right rectangle
            Rectangle topMidRightCut = new Rectangle(x2, 0.0, x3 - x2, middleCut);
            topMidRightCut.setFill(javafx.scene.paint.Color.web(pageBg));

            // Top-right triangle
            Polygon topRightCut = new Polygon(
                    x3, middleCut,
                    x3, 0.0,
                    x4, 0.0
            );
            topRightCut.setFill(javafx.scene.paint.Color.web(pageBg));

            // Bottom-left triangle
            Polygon bottomLeftCut = new Polygon(
                    x0, windowHeight,
                    x1, windowHeight - middleCut,
                    x1, windowHeight
            );
            bottomLeftCut.setFill(javafx.scene.paint.Color.web(pageBg));

            // Bottom middle-left rectangle
            Rectangle bottomMidLeftCut = new Rectangle(x1, windowHeight - middleCut, x2 - x1, middleCut);
            bottomMidLeftCut.setFill(javafx.scene.paint.Color.web(pageBg));

            // Bottom middle-right rectangle
            Rectangle bottomMidRightCut = new Rectangle(x2, windowHeight - middleCut, x3 - x2, middleCut);
            bottomMidRightCut.setFill(javafx.scene.paint.Color.web(pageBg));

            // Bottom-right triangle
            Polygon bottomRightCut = new Polygon(
                    x3, windowHeight,
                    x3, windowHeight - middleCut,
                    x4, windowHeight
            );
            bottomRightCut.setFill(javafx.scene.paint.Color.web(pageBg));

            // ----- Frame Lines -----

            // Top edge
            Line topLeft = new Line(x0, 0.0, x1, middleCut);
            Line topMid1 = new Line(x1, middleCut, x2, middleCut);
            Line topMid2 = new Line(x2, middleCut, x3, middleCut);
            Line topRight = new Line(x3, middleCut, x4, 0.0);

            // Bottom edge
            Line bottomLeft = new Line(x0, windowHeight, x1, windowHeight - middleCut);
            Line bottomMid1 = new Line(x1, windowHeight - middleCut, x2, windowHeight - middleCut);
            Line bottomMid2 = new Line(x2, windowHeight - middleCut, x3, windowHeight - middleCut);
            Line bottomRight = new Line(x3, windowHeight - middleCut, x4, windowHeight);

            // Left and right outer vertical edges
            Line leftOuter = new Line(x0, 0.0, x0, windowHeight);
            Line rightOuter = new Line(x4, 0.0, x4, windowHeight);

            // Dividers with correct lengths
            Line div1 = new Line(x1, middleCut, x1, windowHeight - middleCut);
            Line div2 = new Line(x2, middleCut, x2, windowHeight - middleCut);
            Line div3 = new Line(x3, middleCut, x3, windowHeight - middleCut);

            for (Line line : new Line[]{
                    topLeft, topMid1, topMid2, topRight,
                    bottomLeft, bottomMid1, bottomMid2, bottomRight,
                    leftOuter, rightOuter
            }) {
                line.setStroke(javafx.scene.paint.Color.web(frameColor));
                line.setStrokeWidth(frameThickness);
            }

            for (Line line : new Line[]{div1, div2, div3}) {
                line.setStroke(javafx.scene.paint.Color.web(dividerColor, 0.7));
                line.setStrokeWidth(dividerThickness);
            }

            overlay.getChildren().addAll(
                    // cutouts first
                    topLeftCut, topMidLeftCut, topMidRightCut, topRightCut,
                    bottomLeftCut, bottomMidLeftCut, bottomMidRightCut, bottomRightCut,

                    // frame lines on top
                    topLeft, topMid1, topMid2, topRight,
                    bottomLeft, bottomMid1, bottomMid2, bottomRight,
                    leftOuter, rightOuter,
                    div1, div2, div3
            );
        });

        return overlay;
    }
}