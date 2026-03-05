package com.group32.cs261project.ui;

import javafx.animation.Animation;
import javafx.animation.PathTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.scene.layout.Region;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;

import com.group32.cs261project.App;

public class RunningPage implements Page {

    private final App app;
    private final SimulationService sim;

    private final BorderPane root = new BorderPane();

    // Live metrics - start at 0 before simulation begins
    private final Label simTimeVal = new Label("0");
    private final Label holdingVal = new Label("0");
    private final Label takeoffVal = new Label("0");

    // Runway table data structures
    private final GridPane runwayTable = new GridPane();
    private final List<Label> runwayModeVals = new ArrayList<>();
    private final List<Label> runwayStatusVals = new ArrayList<>();

    // Spinner animation (possibly temporary until we do graphics)
    private PathTransition spinnerAnim;
    
    // Simulation visual
    private SimulationPaneVisual visual;

    private final Pane takeoffZone = new Pane();
    
    // Keep last config for restart
    private SimulationData currentConfig;

    // ----- Styles -----
    private static final String PAGE_BG = "-fx-background-color: white;";
    private static final String CARD =
            "-fx-background-color: white;" +
            "-fx-border-color: #111;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 18;";

    private static final String BTN_PRIMARY =
            "-fx-background-color: #111; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 10 14;";

    private static final String BTN_SOFT =
            "-fx-background-color: #2f6dff; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 10 14;";

    public RunningPage(App app, SimulationService sim) {
        this.app = app;
        this.sim = sim;

        root.setStyle(PAGE_BG);
        HBox mainLayout = new HBox(10); // spacing between columns
        mainLayout.setPadding(new Insets(18));

        // Title
        Label title = new Label("Running Simulation");
        title.setFont(Font.font(28));
        BorderPane.setMargin(title, new Insets(18, 18, 14, 18));
        root.setTop(title);

        // Left Column
        VBox leftColumn = new VBox(20);
        leftColumn.setPrefWidth(400);
        leftColumn.prefWidthProperty().bind(mainLayout.widthProperty().multiply(0.33));
        leftColumn.setMinWidth(300);

        VBox metricsCard = buildMetricsCard();
        VBox runwayCard = buildRunwayCard();
        VBox.setVgrow(runwayCard, Priority.ALWAYS);

        // Make sure cards fit the column width
        metricsCard.setMaxWidth(Double.MAX_VALUE);
        runwayCard.setMaxWidth(Double.MAX_VALUE);
        leftColumn.setFillWidth(true);

        leftColumn.getChildren().addAll(metricsCard, runwayCard);
        
        /* 
        // Spinner (small, fixed size)
        Node spinner = buildPlaneSpinnerWithTrack(150, 50); // paneSize, radius

        StackPane simulationPane = new StackPane();
        simulationPane.setAlignment(Pos.CENTER);
        simulationPane.prefWidthProperty().bind(mainLayout.widthProperty().multiply(0.67));
        simulationPane.getChildren().add(spinner);
        */

        visual = new SimulationPaneVisual();
        StackPane simulationPane = new StackPane();
        simulationPane.setAlignment(Pos.CENTER);
        simulationPane.prefWidthProperty().bind(mainLayout.widthProperty().multiply(0.67));
        simulationPane.getChildren().add(visual);

        HBox.setHgrow(simulationPane, Priority.ALWAYS);
        mainLayout.getChildren().addAll(leftColumn, simulationPane);
        root.setCenter(mainLayout);

        // Buttons at bottom of page - end, restart
        Button endBtn = new Button("End Simulation");
        endBtn.setStyle(BTN_PRIMARY);
        endBtn.setOnAction(e -> onEnd()); // When "End Simulation" is clicked, stop simulation and navigate to results page

        Button restartBtn = new Button("Restart");
        restartBtn.setStyle(BTN_SOFT);
        restartBtn.setOnAction(e -> onRestart()); // When "Restart" is clicked, stop simulation and navigate back to configure page 

        HBox buttons = new HBox(14, endBtn, restartBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setPadding(new Insets(0, 18, 18, 18));
        root.setBottom(buttons);
    }

    // ----- Page Interface Methods -----
    @Override
    public Parent getView() {
        return root;
    }

    @Override
    public void onEnter(Object data) {
        currentConfig = ((SimulationData) data).copy();
        currentConfig.ensureRunwayListSize();

        // Reset UI
        simTimeVal.setText("0");
        holdingVal.setText("0");
        takeoffVal.setText("0");

        rebuildRunwayTable(currentConfig.runwayCount);
        updateRunwayTable(currentConfig);

        // Start spinner animation
        if (spinnerAnim != null) {
            spinnerAnim.playFromStart();
        }

        // Start simulation and update UI on snapshots 
        sim.start(currentConfig, snap -> Platform.runLater(() -> {
            simTimeVal.setText(String.valueOf(snap.simMinute));
            holdingVal.setText(String.valueOf(snap.holdingQueue));
            takeoffVal.setText(String.valueOf(snap.takeoffQueue));
            updateRunwayTable(snap);

            visual.update(snap);
        }));
    }

    // Cleanup - stop animation when leaving page
    @Override
    public void onExit() {
        if (spinnerAnim != null) {
            spinnerAnim.stop();
        }
    }

    // ----- UI Builders: Metrics Card-----
    private VBox buildMetricsCard() {
        VBox card = new VBox(12); 
        card.setStyle(CARD);

        // Heading 
        Label header = new Label("Live Status");
        header.setFont(Font.font(18));
        header.setStyle("-fx-font-weight: 800;");

        // Grid layout for metrics
        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);

        // Add rows for each metric
        addRow(gp, 0, "Simulation time:", simTimeVal, "min");
        addRow(gp, 1, "Number of Aircraft in Holding Queue:", holdingVal, "");
        addRow(gp, 2, "Number of Aircraft in Take-off Queue:", takeoffVal, "");

        // Return complete card
        card.getChildren().addAll(header, gp);
        return card;
    }

    // ----- UI Builders: Runway Table -----
    private VBox buildRunwayCard() {
        // Create a card for the runwat configuration table
        VBox card = new VBox(12);
        card.setStyle(CARD);

        // Header
        Label header = new Label("Runway Configuration");
        header.setFont(Font.font(18));
        header.setStyle("-fx-font-weight: 800;");

        // Spacing between columns and rows
        runwayTable.setHgap(12);
        runwayTable.setVgap(10);
        runwayTable.setPadding(new Insets(6, 0, 0, 0));

        // Column constraints
        ColumnConstraints c0 = new ColumnConstraints(); // Runway name column, has minimum width
        c0.setMinWidth(110); 

        ColumnConstraints c1 = new ColumnConstraints(); // Column expands with available space
        c1.setHgrow(Priority.ALWAYS);

        ColumnConstraints c2 = new ColumnConstraints(); // Column expands with available space
        c2.setHgrow(Priority.ALWAYS);

        runwayTable.getColumnConstraints().setAll(c0, c1, c2);

        VBox.setVgrow(runwayTable, Priority.ALWAYS);
        runwayTable.setMaxHeight(Double.MAX_VALUE);
        card.getChildren().addAll(header, new Separator(), runwayTable);

        // Let the whole card expand if its parent VBox allows it
        card.setMaxHeight(Double.MAX_VALUE);

        return card;
    }

    /**
     * Small plane loading spinner:
     * - fixed pane size
     * - light circle track
     * - plane (emoji) moves around circle using PathTransition
     */
    private Node buildPlaneSpinnerWithTrack(double paneSize, double radius) {
        StackPane wrap = new StackPane();
        wrap.setAlignment(Pos.CENTER);

        // Centre everything in a fixed-size pane to prevent resizing issues with the animation
        Pane pane = new Pane();
        pane.setPrefSize(paneSize, paneSize);
        pane.setMinSize(paneSize, paneSize);
        pane.setMaxSize(paneSize, paneSize);

        double cx = paneSize / 2.0;
        double cy = paneSize / 2.0;

        // Track circle - light grey stroke with no fill
        Circle track = new Circle(cx, cy, radius);
        track.setFill(null);
        track.setStroke(Color.LIGHTGRAY);
        track.setStrokeWidth(3);

        // pPath circle - invisible, used for animation path
        Circle path = new Circle(cx, cy, radius);

        // Plane label (use emoji)
        Label plane = new Label("✈");
        plane.setStyle("-fx-font-size: 22px;");
        // initial position doesn't matter; PathTransition will place it on the path

        // Animation to move plane along path in a circle, indefinitely 
        spinnerAnim = new PathTransition(Duration.seconds(1.8), path, plane); // Plane moves around the circle once every 1.8 seconds
        spinnerAnim.setCycleCount(Animation.INDEFINITE); // Loops forever
        spinnerAnim.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT); // Keep the plane rotated to follow the path direction

        // Add track and plane to pane
        pane.getChildren().addAll(track, plane);

        // Simulation is running text below the spinner
        Label hint = new Label("Simulation running…");
        hint.setStyle("-fx-text-fill: #444;");

        VBox box = new VBox(10, pane, hint);
        box.setAlignment(Pos.CENTER);

        wrap.getChildren().add(box);
        return wrap;
    }

    // ----- Helper to Add a Row to the Metrics Grid -----
    private void addRow(GridPane gp, int row, String labelText, Label valueLabel, String suffix) {
        Label l = new Label(labelText); // Left-hand label

        // Format labels
        valueLabel.setStyle("-fx-font-weight: 800;");
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);

        // Put value and suffix (if any) in an HBox to keep them together on the right side of the grid cell
        HBox valueBox = new HBox(6);
        valueBox.setAlignment(Pos.CENTER_RIGHT);
        valueBox.getChildren().add(valueLabel);

        if (suffix != null && !suffix.isBlank()) {
            Label s = new Label(suffix);
            s.setStyle("-fx-text-fill: #444;");
            valueBox.getChildren().add(s);
        }

        gp.add(l, 0, row);
        gp.add(valueBox, 1, row);

        // Column constraints
        ColumnConstraints c0 = new ColumnConstraints(); // Column expands 
        c0.setHgrow(Priority.ALWAYS);

        ColumnConstraints c1 = new ColumnConstraints(); // Columns has minimum width and stays stable
        c1.setMinWidth(220);
        c1.setHgrow(Priority.NEVER);

        gp.getColumnConstraints().setAll(c0, c1);
    }

    // ----- Runway Table Logic -----
    private void rebuildRunwayTable(int runwayCount) {
        // Clear existing table and labels
        runwayTable.getChildren().clear();
        runwayModeVals.clear();
        runwayStatusVals.clear();

        // Header row
        Label h0 = new Label("Runway");
        Label h1 = new Label("Operating Mode");
        Label h2 = new Label("Operational Status");
        h0.setStyle("-fx-font-weight: 800;");
        h1.setStyle("-fx-font-weight: 800;");
        h2.setStyle("-fx-font-weight: 800;");

        // Add header row at grid row 0
        runwayTable.add(h0, 0, 0);
        runwayTable.add(h1, 1, 0);
        runwayTable.add(h2, 2, 0);

        // ----- Data Rows -----
        for (int i = 0; i < runwayCount; i++) {
            int row = i + 1; 

            // Row labels - mode.status start as "-" until we get the first snapshot from the simulation
            Label name = new Label("Runway " + (i + 1) + ":");
            Label mode = new Label("-");
            Label status = new Label("-");

            mode.setStyle("-fx-font-weight: 700;");
            status.setStyle("-fx-font-weight: 700;");

            // Store references so they can be updated later without searching the grid
            runwayModeVals.add(mode);
            runwayStatusVals.add(status);

            // Add row cells to grid
            runwayTable.add(name, 0, row);
            runwayTable.add(mode, 1, row);
            runwayTable.add(status, 2, row);
        }
    }

    // ----- Runway Table Update Logic -----
    private void updateRunwayTable(SimulationData d) {
        d.ensureRunwayListSize();

        // Update rows (only up to the number of runways)
        int n = Math.min(d.runways.size(), runwayModeVals.size());
        for (int i = 0; i < n; i++) {
            var r = d.runways.get(i);
            runwayModeVals.get(i).setText(pretty(r.mode.name())); // Convert enum to pretty text
            runwayStatusVals.get(i).setText(pretty(r.status.name())); 
        }
    }

    // ----- Helper to Convert Enum Names to Pretty Text -----
    private String pretty(String enumName) {
        String lower = enumName.toLowerCase().replace('_', ' ');
        String[] parts = lower.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    // ----- Button Handlers -----
    private void onEnd() {
        // Stop the simulation and get final report, then navigate to results page
        SimulationData report = sim.stop();
        app.switchTo(AppState.RESULTS, report);
    }

    private void onRestart() {
        // Stop simulation and go back to Configure Page 
        sim.stop();
        app.switchTo(AppState.CONFIGURE, null);
    }
}