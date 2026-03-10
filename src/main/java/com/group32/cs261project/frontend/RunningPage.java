package com.group32.cs261project.frontend;

import javafx.animation.Animation;
import javafx.animation.PathTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.scene.control.Slider;
import javafx.animation.Interpolator;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.animation.RotateTransition;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;


import com.group32.cs261project.App;

public class RunningPage implements Page {

    private final App app;
    private final SimulationService sim;

    private final BorderPane root = new BorderPane();

    private final Label simTimeVal = new Label("0");
    private final Label holdingVal = new Label("0");
    private final Label takeoffVal = new Label("0");

    private final GridPane runwayTable = new GridPane();
    private final List<Label> runwayModeVals = new ArrayList<>();
    private final List<Label> runwayStatusVals = new ArrayList<>();

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

    // Pause button
    private Button pauseBtn;

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

    private static final String TITLE_TEXT = "-fx-text-fill: #eaf6ff;";
    private static final String SUBTITLE_TEXT = "-fx-text-fill: #cccccc;";
    private static final String LABEL_TEXT = "-fx-text-fill: #dcebf5;";
    private static final String SMALL_TEXT = "-fx-text-fill: #b8cad6;";
    private static final String MONITOR_TITLE = "-fx-text-fill: #63e6ff; -fx-font-weight: 700;";
    private static final String MONITOR_LABEL = "-fx-text-fill: #d7f8ff; -fx-font-weight: 700;";

    public RunningPage(App app, SimulationService sim) {
        this.app = app;
        this.sim = sim;

        root.setStyle(PAGE_BG);

        HBox mainLayout = new HBox(10);
        mainLayout.setPadding(new Insets(18));

        Label title = new Label("Running Simulation");
        title.setFont(Font.font(30));
        title.setStyle(TITLE_TEXT);
        BorderPane.setMargin(title, new Insets(18, 18, 14, 18));
        root.setTop(title);

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

        simulationPane = new StackPane();
        simulationPane.setAlignment(Pos.CENTER);
        simulationPane.prefWidthProperty().bind(mainLayout.widthProperty().multiply(0.67));
        
        visual.prefWidthProperty().bind(simulationPane.widthProperty());
        visual.prefHeightProperty().bind(simulationPane.heightProperty());

        fastModeOverlay.prefWidthProperty().bind(simulationPane.widthProperty());
        fastModeOverlay.prefHeightProperty().bind(simulationPane.heightProperty());

        simulationPane.getChildren().addAll(visual, fastModeOverlay);

        HBox.setHgrow(simulationPane, Priority.ALWAYS);

        mainLayout.getChildren().addAll(leftColumn, simulationPane);
        root.setCenter(mainLayout);
    }

    @Override
    public Parent getView() {
        return root;
    }

    @Override
    public void onEnter(Object data) {
        currentConfig = ((SimulationData) data).copy();
        currentConfig.ensureRunwayListSize();

        simTimeVal.setText("0");
        holdingVal.setText("0");
        takeoffVal.setText("0");

        rebuildRunwayTable(currentConfig.runwayCount);
        updateRunwayTable(currentConfig);

        if (spinnerAnim != null) {
            spinnerAnim.playFromStart();
        }
        if (sweepAnim != null) {
            sweepAnim.playFromStart();
        }

        speedSlider.setValue(10);  // maps to 1.0x
        speedValueLabel.setText(formatSpeed(1.0));
        sim.setPlaybackSpeed(1.0);
        visual.setPlaybackSpeed(1.0);
        updateVisualMode(1.0);

        sim.start(currentConfig, snap -> Platform.runLater(() -> {

            simTimeVal.setText(String.valueOf(snap.simMinute));
            holdingVal.setText(String.valueOf(snap.holdingQueue));
            takeoffVal.setText(String.valueOf(snap.takeoffQueue));

            updateRunwayTable(snap);

            if (mapSliderToSpeed(speedSlider.getValue()) <= 2.0) {
                visual.update(snap);
            }
        }));
    }

    @Override
    public void onExit() {
        if (spinnerAnim != null) {
            spinnerAnim.stop();
        }
    }

    // ----- Speed Slider -----
    private VBox buildSpeedCard() {
        VBox box = new VBox(8);

        Label title = new Label("Simulation Speed");
        title.setStyle(MONITOR_LABEL);
        title.setFont(Font.font(16));

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

    private VBox buildMetricsCard() {

        VBox card = new VBox(12);
        card.setStyle(CARD);

        Label header = new Label("Live Status");
        header.setFont(Font.font(20));
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

    private VBox buildRunwayCard() {

        VBox card = new VBox(12);
        card.setStyle(CARD);

        Label header = new Label("Runway Configuration");
        header.setFont(Font.font(20));
        header.setStyle(MONITOR_LABEL);

        runwayTable.setHgap(12);
        runwayTable.setVgap(10);
        runwayTable.setPadding(new Insets(6, 0, 0, 0));

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(110);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);

        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);

        runwayTable.getColumnConstraints().setAll(c0, c1, c2);

        VBox.setVgrow(runwayTable, Priority.ALWAYS);
        runwayTable.setMaxHeight(Double.MAX_VALUE);

        card.getChildren().addAll(header, new Separator(), runwayTable);

        return card;
    }

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

    private VBox buildFastModeOverlay() {

    VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setStyle(
            "-fx-background-color: rgb(14, 26, 40);" +
            "-fx-background-radius: 16;"
        );

        Node loadingIcon = buildPlaneSpinnerWithTrack(240, 95);

        Label title = new Label("Fast Mode Enabled");
        title.setFont(Font.font(16));
        title.setStyle(MONITOR_LABEL);

        Label note = new Label("Reduce speed to see visual");
        note.setFont(Font.font(14));
        note.setStyle(SMALL_TEXT);

        box.getChildren().addAll(loadingIcon, title, note);

        box.setVisible(false);
        box.setManaged(false);

        return box;
    }

    private void addRow(GridPane gp, int row, String labelText, Label valueLabel, String suffix) {

        Label l = new Label(labelText);
        l.setStyle(SMALL_TEXT);

        valueLabel.setStyle(MONITOR_TITLE);
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

    private void rebuildRunwayTable(int runwayCount) {

        runwayTable.getChildren().clear();
        runwayModeVals.clear();
        runwayStatusVals.clear();

        Label h0 = new Label("Runway");
        Label h1 = new Label("Operating Mode");
        Label h2 = new Label("Operational Status");

        h0.setStyle(MONITOR_LABEL);
        h1.setStyle(MONITOR_LABEL);
        h2.setStyle(MONITOR_LABEL);

        runwayTable.add(h0, 0, 0);
        runwayTable.add(h1, 1, 0);
        runwayTable.add(h2, 2, 0);

        for (int i = 0; i < runwayCount; i++) {

            int row = i + 1;

            Label name = new Label("Runway " + (i + 1) + ":");
            Label mode = new Label("-");
            Label status = new Label("-");

            name.setStyle(SMALL_TEXT);
            mode.setStyle(SMALL_TEXT);
            status.setStyle(SMALL_TEXT);

            runwayModeVals.add(mode);
            runwayStatusVals.add(status);

            runwayTable.add(name, 0, row);
            runwayTable.add(mode, 1, row);
            runwayTable.add(status, 2, row);
        }
    }

    // ----- Helpers -----

    private void updateRunwayTable(SimulationData d) {

        d.ensureRunwayListSize();

        int n = Math.min(d.runways.size(), runwayModeVals.size());

        for (int i = 0; i < n; i++) {

            var r = d.runways.get(i);

            runwayModeVals.get(i).setText(pretty(r.mode.name()));
            runwayStatusVals.get(i).setText(pretty(r.status.name()));
        }
    }

    // Buttons Control Card
    private HBox buildControlButtons() {
        pauseBtn = new Button("Pause");
        pauseBtn.setStyle(BTN_SOFT);
        pauseBtn.setOnAction(e -> togglePause());

        Button endBtn = new Button("End Simulation");
        endBtn.setStyle(BTN_PRIMARY);
        endBtn.setOnAction(e -> onEnd());

        Button restartBtn = new Button("Restart");
        restartBtn.setStyle(BTN_SOFT);
        restartBtn.setOnAction(e -> onRestart());

        HBox buttons = new HBox(14, pauseBtn, endBtn, restartBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);

        return buttons;
    }

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

    private void updateVisualMode(double speed) {
        boolean hideVisual = speed >= 3.0;

        visual.setVisible(!hideVisual);
        visual.setManaged(!hideVisual);

        fastModeOverlay.setVisible(hideVisual);
        fastModeOverlay.setManaged(hideVisual);
    }

    private String formatSpeed(double speed) {
        return (speed < 1.0)
                ? String.format("%.1fx", speed)
                : (speed == 1.0 ? "1.0x" : String.format("%.0fx", speed));
    }

    private String pretty(String enumName) {

        String lower = enumName.toLowerCase().replace('_', ' ');
        String[] parts = lower.split(" ");

        StringBuilder sb = new StringBuilder();

        for (String p : parts) {

            if (p.isBlank()) continue;

            sb.append(Character.toUpperCase(p.charAt(0)))
              .append(p.substring(1))
              .append(" ");
        }

        return sb.toString().trim();
    }

    private void togglePause() {

        if (sim.isPaused()) {

            sim.resume();
            pauseBtn.setText("Pause");

        } else {

            sim.pause();
            pauseBtn.setText("Resume");
        }
    }

    private void onEnd() {

        SimulationData report = sim.stop();
        app.switchTo(AppState.RESULTS, report);
    }

    private void onRestart() {

        sim.stop();
        app.switchTo(AppState.CONFIGURE, null);
    }
}