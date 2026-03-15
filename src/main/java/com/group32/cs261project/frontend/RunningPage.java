package com.group32.cs261project.frontend;

import javafx.animation.Animation;
import javafx.animation.PathTransition;
import javafx.animation.RotateTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.Group;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

import com.group32.cs261project.App;
import com.group32.cs261project.frontend.HelpOverlay;
import com.group32.cs261project.frontend.HelpText;

/**
 * Page displaying the live simulation while it is running.
 *
 * This page shows real-time simulation metrics, a visual representation
 * of aircraft movement, runway configuration controls, and simulation
 * speed controls. It also allows the user to pause, restart, end the
 * simulation, and apply runway configuration changes during runtime.
 */
public class RunningPage implements Page {

    private final App app;
    private final SimulationService sim;

    private final BorderPane root = new BorderPane();

    private final Label simTimeVal = new Label("0");
    private final Label holdingVal = new Label("0");
    private final Label takeoffVal = new Label("0");

    private final GridPane runwayTable = new GridPane();
    private final List<RunwayEditorCard> runwayEditorCards = new ArrayList<>();

    private PathTransition spinnerAnim;
    private Timeline sweepAnim;

    private SimulationPaneVisual visual;

    private SimulationData currentConfig;

    private StackPane simulationPane;
    private VBox fastModeOverlay;
    private Label fastModeNote;

    // Simulation Speed Slider
    private final Slider speedSlider = new Slider(1, 19, 10);
    private final Label speedValueLabel = new Label("1.0x");
    private SimulationData latestSnapshot;

    // Buttons
    private Button pauseBtn;
    private Button applyRunwayChangesBtn;
    private final Label changesAppliedLabel = new Label("Changes applied!");

    // ----- Styling -----
    private static final String PAGE_BG =
        "-fx-background-color: rgb(14, 26, 40);";

    private static final String CARD =
            "-fx-background-color: rgba(8, 18, 28, 0.96);" +
            "-fx-border-color: #1f3b52;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 14;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 14, 0.2, 0, 3);";

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

    private static final String RUNWAY_EDIT_CARD =
            "-fx-background-color: linear-gradient(to bottom, #071018 0%, #081722 55%, #06111a 100%);" +
            "-fx-border-color: #1dd6ff;" +
            "-fx-border-width: 1.4;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(29,214,255,0.16), 12, 0.22, 0, 0);";

    private static final String RUNWAY_EDIT_CARD_DISABLED =
            "-fx-background-color: linear-gradient(to bottom, #1a232b 0%, #182028 100%);" +
            "-fx-border-color: #46535e;" +
            "-fx-border-width: 1.1;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 10;" +
            "-fx-opacity: 0.74;";

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
            
    private static final String TITLE_TEXT = "-fx-text-fill: #eaf6ff;";
    private static final String SUBTITLE_TEXT = "-fx-text-fill: #cccccc;";
    private static final String LABEL_TEXT = "-fx-text-fill: #dcebf5;";
    private static final String SMALL_TEXT = "-fx-text-fill: #b8cad6;";
    private static final String MONITOR_TITLE = "-fx-text-fill: #63e6ff;";
    private static final String MONITOR_LABEL = "-fx-text-fill: #d7f8ff;";

    /**
     * Creates a new running page.
     *
     * This constructor builds the page layout, including the live metrics
     * panel, runway configuration editor, simulation speed controls,
     * control buttons, and simulation visual area.
     *
     * @param app the main application controller used for page navigation
     * @param sim the simulation service used to control the running simulation
     */
    public RunningPage(App app, SimulationService sim) {
        this.app = app;
        this.sim = sim;

        root.setStyle(PAGE_BG);

        HBox mainLayout = new HBox(10);
        mainLayout.setPadding(new Insets(18));
        mainLayout.setMinWidth(1500);

        // Page title
        Label title = new Label("Running Simulation");
        title.setFont(Font.font(null, FontWeight.BOLD, 30));
        title.setStyle(TITLE_TEXT);
        BorderPane.setMargin(title, new Insets(18, 18, 14, 18));
        root.setTop(title);

        // ----- Left Column -----
        VBox leftColumn = new VBox(20);
        leftColumn.setPrefWidth(400);
        leftColumn.prefWidthProperty().bind(mainLayout.widthProperty().multiply(0.33));
        leftColumn.setMinWidth(300);

        VBox metricsCard = buildMetricsCard();
        VBox runwayCard = buildRunwayCard();
        VBox speedCard = buildSpeedCard();
        HBox buttons = buildControlButtons();
        
        VBox.setVgrow(runwayCard, Priority.ALWAYS);

        metricsCard.setMaxWidth(Double.MAX_VALUE);
        runwayCard.setMaxWidth(Double.MAX_VALUE);
        speedCard.setMaxWidth(Double.MAX_VALUE);
        leftColumn.setFillWidth(true);

        leftColumn.getChildren().addAll(metricsCard, runwayCard, speedCard, buttons);

        visual = new SimulationPaneVisual();
        fastModeOverlay = buildFastModeOverlay();

        // ----- Simulation View -----
        simulationPane = new StackPane();
        simulationPane.setAlignment(Pos.CENTER);
        simulationPane.prefWidthProperty().bind(mainLayout.widthProperty().multiply(0.67));
        simulationPane.setMinHeight(875);

        visual.prefWidthProperty().bind(simulationPane.widthProperty());
        visual.prefHeightProperty().bind(simulationPane.heightProperty());

        fastModeOverlay.prefWidthProperty().bind(simulationPane.widthProperty());
        fastModeOverlay.prefHeightProperty().bind(simulationPane.heightProperty());

        simulationPane.getChildren().addAll(visual, fastModeOverlay);

        HBox.setHgrow(simulationPane, Priority.ALWAYS);

        mainLayout.getChildren().addAll(leftColumn, simulationPane);

        // Scrollpane
        ScrollPane scroll = new ScrollPane(mainLayout);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPannable(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        root.setCenter(scroll);    
    }

    /**
     * Returns the root view for this page wrapped in the help overlay.
     *
     * @return the page view
     */
    @Override
    public Parent getView() {
        return HelpOverlay.wrap(root, "Running", HelpText.runningPage());
    }

    /**
     * Called when the application navigates to this page.
     *
     * The provided data is expected to be a {@link SimulationData} object
     * containing the simulation configuration. This method resets the
     * displayed metrics, rebuilds the runway editor, starts the simulation,
     * and begins receiving live snapshots from the backend.
     *
     * @param data the simulation configuration passed during navigation
     */
    @Override
    public void onEnter(Object data) {
        currentConfig = ((SimulationData) data).copy();
        currentConfig.ensureRunwayListSize();

        simTimeVal.setText("0");
        holdingVal.setText("0");
        takeoffVal.setText("0");

        rebuildRunwayTable(currentConfig.runwayCount);
        updateRunwayTable(currentConfig);

        // Restart spinner 
        if (spinnerAnim != null) {
            spinnerAnim.playFromStart();
        }
        if (sweepAnim != null) {
            sweepAnim.playFromStart();
        }

        speedSlider.setValue(5);  // maps to 0.5x
        speedValueLabel.setText(formatSpeed(0.5));
        sim.setPlaybackSpeed(0.5);
        visual.setPlaybackSpeed(0.5);
        updateVisualMode(0.5);

        sim.resume();
        pauseBtn.setText("Pause");

        sim.start(currentConfig, snap -> Platform.runLater(() -> {

            latestSnapshot = snap.copy();

            simTimeVal.setText(String.valueOf(snap.simMinute));
            holdingVal.setText(String.valueOf(snap.holdingQueue));
            takeoffVal.setText(String.valueOf(snap.takeoffQueue));

            updateRunwayTable(snap);

            if (mapSliderToSpeed(speedSlider.getValue()) <= 2.0) {
                visual.update(snap);
            }
        }));
    }

    /**
     * Called when the application navigates away from this page.
     *
     * This implementation stops the spinner animation if it is running.
     */
    @Override
    public void onExit() {
        if (spinnerAnim != null) {
            spinnerAnim.stop();
        }
    }

    /**
     * Builds the simulation speed control card.
     *
     * This card contains the speed slider and the label showing the
     * currently selected playback speed.
     *
     * @return the simulation speed control card
     */
    private VBox buildSpeedCard() {
        VBox box = new VBox(8);

        Label title = new Label("Simulation Speed");
        title.setStyle(MONITOR_LABEL);
        title.setFont(Font.font(null, FontWeight.BOLD, 16));

        speedSlider.setMin(1);
        speedSlider.setMax(19);
        speedSlider.setValue(10);
        speedSlider.setBlockIncrement(1);
        speedSlider.setMajorTickUnit(1);
        speedSlider.setMinorTickCount(0);
        speedSlider.setSnapToTicks(true);
        speedSlider.setShowTickMarks(false);
        speedSlider.setShowTickLabels(false);

        speedValueLabel.setStyle(MONITOR_TITLE);

        // Listener
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double speed = mapSliderToSpeed(newVal.doubleValue());
            speedValueLabel.setText(formatSpeed(speed));

            sim.setPlaybackSpeed(speed);
            visual.setPlaybackSpeed(speed);
            updateVisualMode(speed);
        });

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerRow.getChildren().addAll(title, spacer, speedValueLabel);

        box.getChildren().addAll(headerRow, speedSlider);
        return box;
    }

    /**
     * Builds the live metrics card.
     *
     * This card displays key real-time simulation information such as
     * simulation time, holding queue size, and take-off queue size.
     *
     * @return the live metrics card
     */
    private VBox buildMetricsCard() {

        VBox card = new VBox(12);
        card.setStyle(CARD);
        card.setFocusTraversable(false);

        Label header = new Label("Live Status");
        header.setFont(Font.font(null, FontWeight.BOLD, 20));
        header.setStyle(MONITOR_LABEL);

        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);

        addRow(gp, 0, "Simulation time:", simTimeVal, "min");
        addRow(gp, 1, "Number of Aircraft in Holding Queue:", holdingVal, "");
        addRow(gp, 2, "Number of Aircraft in Take-off Queue:", takeoffVal, "");

        card.getChildren().addAll(header, gp);

        card.setMaxHeight(Double.MAX_VALUE);

        return card;
    }

    /**
     * Builds the runway configuration editor card.
     *
     * This card allows the user to view and modify runway operating
     * modes and statuses while the simulation is running.
     *
     * @return the runway configuration editor card
     */
    private VBox buildRunwayCard() {
        VBox card = new VBox(12);
        card.setStyle(CARD);
        card.setFocusTraversable(false);

        Label header = new Label("Runway Configuration");
        header.setFont(Font.font(null, FontWeight.BOLD, 20));
        header.setStyle(MONITOR_LABEL);

        Label sub = new Label("Change runway status and mode while the simulation is running");
        sub.setStyle(SMALL_TEXT);

        runwayTable.getChildren().clear();
        runwayTable.getColumnConstraints().clear();
        runwayTable.setHgap(10);
        runwayTable.setVgap(10);
        runwayTable.setPadding(new Insets(2, 0, 2, 0));

        ScrollPane scroll = new ScrollPane(runwayTable);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setPrefViewportHeight(280);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        changesAppliedLabel.setStyle(
            "-fx-text-fill: #39e75f;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;"
        );
        changesAppliedLabel.setVisible(false);
        changesAppliedLabel.setManaged(false);

        applyRunwayChangesBtn = new Button("Apply Changes");
        applyRunwayChangesBtn.setStyle(BTN_PRIMARY);
        applyRunwayChangesBtn.setOnAction(e -> {
            applyRunwayChanges();
            showChangesAppliedMessage();
        });

        HBox applyRow = new HBox(10, applyRunwayChangesBtn, changesAppliedLabel);
        applyRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(header, sub, new Separator(), scroll, applyRow);

        return card;
    }

    /**
     * Builds the animated radar-style loading graphic used in fast mode.
     *
     * The graphic includes a circular track, rotating radar sweep,
     * and moving aircraft icon.
     *
     * @param paneSize the size of the containing pane
     * @param radius the radius of the radar track
     * @return the completed animated loading graphic
     */
    private Node buildPlaneSpinnerWithTrack(double paneSize, double radius) {
        StackPane wrap = new StackPane();
        wrap.setAlignment(Pos.CENTER);

        Pane pane = new Pane();
        pane.setPrefSize(paneSize, paneSize);
        pane.setMinSize(paneSize, paneSize);
        pane.setMaxSize(paneSize, paneSize);

        double cx = paneSize / 2.0;
        double cy = paneSize / 2.0;

        // Background disc
        Circle bg = new Circle(cx, cy, radius + 12);
        bg.setFill(Color.web("#071407"));
        bg.setStroke(Color.web("#163d1f"));
        bg.setStrokeWidth(1.5);

        // Outer ring
        Circle track = new Circle(cx, cy, radius);
        track.setFill(null);
        track.setStroke(Color.web("#39e75f"));
        track.setStrokeWidth(2.5);
        track.setEffect(new Glow(0.45));

        // Inner rings
        Circle innerRing1 = new Circle(cx, cy, radius * 0.66);
        innerRing1.setFill(null);
        innerRing1.setStroke(Color.web("#1f6f2c"));
        innerRing1.setStrokeWidth(1.3);

        Circle innerRing2 = new Circle(cx, cy, radius * 0.33);
        innerRing2.setFill(null);
        innerRing2.setStroke(Color.web("#163d1f"));
        innerRing2.setStrokeWidth(1.0);

        // Crosshair
        Line hLine = new Line(cx - radius, cy, cx + radius, cy);
        hLine.setStroke(Color.web("#1f6f2c"));
        hLine.setStrokeWidth(1);

        Line vLine = new Line(cx, cy - radius, cx, cy + radius);
        vLine.setStroke(Color.web("#1f6f2c"));
        vLine.setStrokeWidth(1);

        // Radar sweep wedge
        Arc wedge = new Arc(cx, cy, radius, radius, -28, 56);
        wedge.setType(ArcType.ROUND);
        wedge.setFill(Color.web("#39e75f", 0.14));
        wedge.setStroke(null);
        wedge.setEffect(new Glow(0.8));

        // Plane path
        Circle path = new Circle(cx, cy, radius);

        // Plane icon
        Label plane = new Label("✈");
        plane.setStyle(
            "-fx-font-size: 18px;" +
            "-fx-text-fill: #d6ffe2;"
        );
        plane.setEffect(new Glow(1.0));

        // Bright leading edge
        double frontAngleDeg = -28; // leading edge of the wedge
        double theta = Math.toRadians(frontAngleDeg);

        double ex = cx + radius * Math.cos(theta);
        double ey = cy - radius * Math.sin(theta);

        Line sweepEdge = new Line(cx, cy, ex, ey);
        sweepEdge.setStroke(Color.web("#7CFF9B"));
        sweepEdge.setStrokeWidth(2.2);
        sweepEdge.setOpacity(0.95);
        sweepEdge.setEffect(new Glow(0.9));

        // Put them in one group
        Group sweepGroup = new Group(wedge, sweepEdge);

        // Rotate around the radar centre
        Rotate sweepRotate = new Rotate(0, cx, cy);
        sweepGroup.getTransforms().add(sweepRotate);

        // Animate the angle 
        sweepAnim = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(sweepRotate.angleProperty(), 0)),
            new KeyFrame(Duration.seconds(3.2), new KeyValue(sweepRotate.angleProperty(), 360))
        );
        sweepAnim.setCycleCount(Animation.INDEFINITE);

        spinnerAnim = new PathTransition(Duration.seconds(1.8), path, plane);
        spinnerAnim.setCycleCount(Animation.INDEFINITE);
        spinnerAnim.setInterpolator(Interpolator.LINEAR);
        spinnerAnim.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);

        // Centre dot
        Circle centreDot = new Circle(cx, cy, 3.5);
        centreDot.setFill(Color.web("#39e75f"));
        centreDot.setEffect(new Glow(0.8));

        pane.getChildren().addAll(
            bg,
            innerRing2,
            innerRing1,
            hLine,
            vLine,
            track,
            sweepGroup,
            centreDot,
            plane
        );

        Label hint = new Label("Simulation running...");
        hint.setStyle(
            "-fx-text-fill: #39e75f;" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;"
        );
        hint.setEffect(new Glow(0.25));

        VBox box = new VBox(10, pane, hint);
        box.setAlignment(Pos.CENTER);

        wrap.getChildren().add(box);

        sweepAnim.play();
        spinnerAnim.play();

        return wrap;
    }

    /**
     * Builds the overlay shown when the simulation is running in fast mode.
     *
     * In fast mode, the main visual is hidden and replaced with a message
     * and animated indicator informing the user that the visual has been
     * disabled at high speed.
     *
     * @return the fast mode overlay
     */
    private VBox buildFastModeOverlay() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setStyle(
            "-fx-background-color: rgb(14, 26, 40);" +
            "-fx-background-radius: 16;"
        );

        Node loadingIcon = buildPlaneSpinnerWithTrack(240, 95);

        Label title = new Label("Fast Mode Enabled");
        title.setFont(Font.font(null, FontWeight.BOLD, 16));
        title.setStyle(MONITOR_LABEL);

        Label note = new Label("Reduce speed to see visual");
        note.setFont(Font.font(null, FontWeight.BOLD, 14));
        note.setStyle(SMALL_TEXT);

        box.getChildren().addAll(loadingIcon, title, note);

        box.setVisible(false);
        box.setManaged(false);

        return box;
    }

    /**
     * Adds a labelled metric row to the given grid pane.
     *
     * Each row consists of a label, a value label, and an optional suffix.
     *
     * @param gp the grid pane to add the row to
     * @param row the target row index
     * @param labelText the descriptive label text
     * @param valueLabel the label displaying the metric value
     * @param suffix the optional suffix displayed beside the value
     */
    private void addRow(GridPane gp, int row, String labelText, Label valueLabel, String suffix) {
        Label l = new Label(labelText);
        l.setStyle(SMALL_TEXT);

        valueLabel.setStyle(MONITOR_TITLE);
        valueLabel.setFont(Font.font(null, FontWeight.BOLD, 12));
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);

        HBox valueBox = new HBox(6);
        valueBox.setAlignment(Pos.CENTER_RIGHT);
        valueBox.getChildren().add(valueLabel);

        if (suffix != null && !suffix.isBlank()) {
            Label s = new Label(suffix);
            s.setStyle(SMALL_TEXT);
            valueBox.getChildren().add(s);
        }

        gp.add(l, 0, row);
        gp.add(valueBox, 1, row);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setHgrow(Priority.ALWAYS);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(50);
        c1.setHgrow(Priority.NEVER);

        gp.getColumnConstraints().setAll(c0, c1);
    }

    /**
     * Rebuilds the runway editor table for the given number of runways.
     *
     * Existing runway editor cards are removed and replaced with new cards
     * bound to the current runway configuration.
     *
     * @param runwayCount the number of runways to display
     */
    private void rebuildRunwayTable(int runwayCount) {
        runwayTable.getChildren().clear();
        runwayEditorCards.clear();
        runwayTable.getColumnConstraints().clear();

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(50);
        c0.setHgrow(Priority.ALWAYS);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        c1.setHgrow(Priority.ALWAYS);

        runwayTable.getColumnConstraints().setAll(c0, c1);

        for (int i = 0; i < runwayCount; i++) {

            SimulationData.RunwayConfig cfg = currentConfig.runways.get(i);
            RunwayEditorCard card = new RunwayEditorCard(cfg);
            runwayEditorCards.add(card);

            int col = i % 2;
            int row = i / 2;

            runwayTable.add(card.root, col, row);
        }
    }

    // ===================
    // ----- Helpers -----
    // ===================

    /**
     * Updates the runway editor table to match the supplied simulation data.
     *
     * If the number of runway editor cards no longer matches the runway
     * configuration, the table is rebuilt.
     *
     * @param d the latest simulation snapshot
     */
    private void updateRunwayTable(SimulationData d) {
        d.ensureRunwayListSize();

        if (runwayEditorCards.size() != d.runways.size()) {
            currentConfig = d.copy();
            rebuildRunwayTable(currentConfig.runwayCount);
        }
    }

    /**
     * Applies the current runway editor selections to the running simulation.
     *
     * The updated runway modes and statuses are copied into a new
     * {@link SimulationData} object and passed to the simulation service.
     */
    private void applyRunwayChanges() {
        if (currentConfig == null) {
            return;
        }

        SimulationData updated = currentConfig.copy();
        updated.ensureRunwayListSize();

        int n = Math.min(updated.runways.size(), runwayEditorCards.size());

        for (int i = 0; i < n; i++) {
            RunwayEditorCard card = runwayEditorCards.get(i);
            updated.runways.get(i).status = card.getStatus();
            updated.runways.get(i).mode = card.getMode();
        }

        currentConfig = updated.copy();
        sim.updateConfig(updated);

        for (int i = 0; i < n; i++) {
            runwayEditorCards.get(i).bindTo(currentConfig.runways.get(i));
        }
    }

    /**
     * UI component representing an editable runway configuration card.
     *
     * Each card allows the user to view and modify the status and mode
     * of a single runway during the simulation.
     */
    private class RunwayEditorCard {
        private final VBox root = new VBox(6);
        private final Label title = new Label();

        private final ComboBox<SimulationData.RunwayStatus> statusBox = new ComboBox<>();
        private final ComboBox<SimulationData.RunwayMode> modeBox = new ComboBox<>();

        /**
         * Creates a runway editor card bound to the given runway configuration.
         *
         * @param cfg the initial runway configuration for this card
         */
        RunwayEditorCard(SimulationData.RunwayConfig cfg) {
            root.setStyle(RUNWAY_EDIT_CARD);
            root.setMaxWidth(Double.MAX_VALUE);
            root.setPrefWidth(160);

            title.setStyle(MONITOR_TITLE);
            title.setFont(Font.font(null, FontWeight.BOLD, 14));

            Label statusLabel = new Label("Status");
            statusLabel.setStyle(MONITOR_LABEL);
            statusLabel.setFont(Font.font(null, FontWeight.BOLD, 11));

            Label modeLabel = new Label("Mode");
            modeLabel.setStyle(MONITOR_LABEL);
            modeLabel.setFont(Font.font(null, FontWeight.BOLD, 11));

            statusBox.getItems().setAll(SimulationData.RunwayStatus.values());
            modeBox.getItems().setAll(SimulationData.RunwayMode.values());

            statusBox.setStyle(COMBO_STYLE);
            modeBox.setStyle(COMBO_STYLE);

            statusBox.setMaxWidth(Double.MAX_VALUE);
            modeBox.setMaxWidth(Double.MAX_VALUE);

            statusBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(SimulationData.RunwayStatus item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.toString());
                    setTextFill(Color.WHITE);                    
                    setStyle("-fx-background-color: #0d1d29;");
                }
            });

            modeBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(SimulationData.RunwayMode item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.toString());
                    setTextFill(Color.WHITE);   
                    setStyle("-fx-background-color: #0d1d29;");
                }
            });

            statusBox.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(SimulationData.RunwayStatus item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.toString());
                    setTextFill(Color.WHITE);   
                    setStyle("-fx-background-color: #0d1d29;");
                }
            });

            modeBox.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(SimulationData.RunwayMode item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.toString());
                    setTextFill(Color.WHITE);   
                    setStyle("-fx-background-color: #0d1d29;");
                }
            });

            VBox content = new VBox(5, statusLabel, statusBox, modeLabel, modeBox);
            content.setAlignment(Pos.TOP_LEFT);

            root.getChildren().addAll(title, content);

            bindTo(cfg);
        }

        /**
         * Binds this editor card to the given runway configuration.
         *
         * The displayed title and combo box selections are updated to reflect
         * the supplied runway data.
         *
         * @param cfg the runway configuration to bind to
         */
        void bindTo(SimulationData.RunwayConfig cfg) {
            title.setText("Runway " + (cfg.id));
            statusBox.setValue(cfg.status);
            modeBox.setValue(cfg.mode);
            setEnabled(true);
        }

        /**
         * Enables or disables this runway editor card.
         *
         * Disabled cards remain visible but cannot be edited.
         *
         * @param enabled true to enable the card, false to disable it
         */
        void setEnabled(boolean enabled) {
            root.setStyle(enabled ? RUNWAY_EDIT_CARD : RUNWAY_EDIT_CARD_DISABLED);
            statusBox.setDisable(!enabled);
            modeBox.setDisable(!enabled);
            title.setStyle(enabled ? MONITOR_TITLE : "-fx-text-fill: #ffffff; -fx-font-weight: 700;");
            root.setOpacity(enabled ? 1.0 : 0.82);
        }

        // Returns the runway status currently selected in this card.
        SimulationData.RunwayStatus getStatus() {
            return statusBox.getValue();
        }

        // Returns the runway mode currently selected in this card.
        SimulationData.RunwayMode getMode() {
            return modeBox.getValue();
        }
    }

    /**
     * Builds the main simulation control buttons.
     *
     * These controls allow the user to pause or resume the simulation,
     * end the run, or restart and return to the configuration page.
     *
     * @return the control button row
     */
    private HBox buildControlButtons() {
        pauseBtn = new Button("Pause");
        pauseBtn.setStyle(BTN_SOFT);
        pauseBtn.setPrefWidth(70);   // wide enough for "Resume"
        pauseBtn.setMinWidth(70);
        pauseBtn.setMaxWidth(70);
        pauseBtn.setOnAction(e -> togglePause());

        Button endBtn = new Button("End Simulation");
        endBtn.setStyle(BTN_PRIMARY);
        endBtn.setOnAction(e -> onEnd());

        Button restartBtn = new Button("Restart");
        restartBtn.setStyle(BTN_SOFT);
        restartBtn.setOnAction(e -> onRestart());

        HBox buttons = new HBox(14, endBtn, pauseBtn, restartBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);

        return buttons;
    }

    /**
     * Maps a slider position to the corresponding playback speed multiplier.
     *
     * Slider values below 10 represent decimal speeds, 10 represents normal
     * speed, and values above 10 represent integer multiples above 1x.
     *
     * @param sliderValue the raw slider value
     * @return the corresponding playback speed
     */
    private double mapSliderToSpeed(double sliderValue) {
        int v = (int) Math.round(sliderValue);

        if (v <= 9) {
            return v / 10.0;   // 1->0.1, 2->0.2, ..., 9->0.9
        }
        if (v == 10) {
            return 1.0;        // 10->1.0
        }
        return v - 9;          // 11->2.0, 12->3.0, ..., 19->10.0
    }

    /**
     * Updates whether the main simulation visual or the fast mode overlay
     * should be displayed.
     *
     * At high speeds, the visual is hidden and the fast mode overlay is shown.
     *
     * @param speed the current playback speed
     */
    private void updateVisualMode(double speed) {
        boolean hideVisual = speed >= 3.0;

        visual.setVisible(!hideVisual);
        visual.setManaged(!hideVisual);

        fastModeOverlay.setVisible(hideVisual);
        fastModeOverlay.setManaged(hideVisual);
    }

    /**
     * Formats a playback speed value for display.
     *
     * @param speed the playback speed multiplier
     * @return the formatted speed string
     */
    private String formatSpeed(double speed) {
        return (speed < 1.0)
                ? String.format("%.1fx", speed)
                : (speed == 1.0 ? "1.0x" : String.format("%.0fx", speed));
    }

    /**
     * Shows a temporary confirmation message after runway changes are applied.
     *
     * The message is displayed briefly and then fades out automatically.
     */
    private void showChangesAppliedMessage() {
        changesAppliedLabel.setVisible(true);
        changesAppliedLabel.setManaged(true);
        changesAppliedLabel.setOpacity(1.0);

        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        FadeTransition fade = new FadeTransition(Duration.seconds(0.8), changesAppliedLabel);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        fade.setOnFinished(e -> {
            changesAppliedLabel.setVisible(false);
            changesAppliedLabel.setManaged(false);
            changesAppliedLabel.setOpacity(1.0);
        });

        new SequentialTransition(pause, fade).play();
    }

    /**
     * Toggles the paused state of the simulation.
     *
     * If the simulation is paused it is resumed, otherwise it is paused.
     * The pause button text is updated accordingly.
     */
    private void togglePause() {
        if (sim.isPaused()) {

            sim.resume();
            pauseBtn.setText("Pause");

        } else {

            sim.pause();
            pauseBtn.setText("Resume");
        }
    }

    /**
     * Ends the current simulation and opens the results page.
     *
     * If the simulation is still running, it is stopped first so that a
     * final report can be generated.
     */
    private void onEnd() {
        SimulationData report;

        if (sim.isRunning()) {
            report = sim.stop();
        } else {
            report = (latestSnapshot != null) ? latestSnapshot.copy() : currentConfig.copy();
        }

        app.switchTo(AppState.RESULTS, report);
    }

    /**
     * Stops the simulation if necessary and returns to the configuration page.
     */
    private void onRestart() {
        if (sim.isRunning()) {
            sim.stop();
        }
        app.switchTo(AppState.CONFIGURE, null);
    }
}