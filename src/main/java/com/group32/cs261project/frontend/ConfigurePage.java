package com.group32.cs261project.frontend;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
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

public class ConfigurePage implements Page {

    private final App app;
    private final SimulationService sim;

    private final BorderPane root = new BorderPane();

    // Sliders
    private final Slider runwaySlider = new Slider(1, 10, 2);
    private final Slider inboundSlider = new Slider(0, 30, 30);
    private final Slider outboundSlider = new Slider(0, 30, 30);
    private final Slider passengerHealthEmergencySlider = new Slider(0, 100, 0);
    private final Slider mechanicalEmergencySlider = new Slider(0, 100, 0);

    // Labels showing current slider values
    private final Label runwayValue = new Label();
    private final Label inboundValue = new Label();
    private final Label outboundValue = new Label();
    private final Label passengerHealthEmergencyValue = new Label();
    private final Label mechanicalEmergencyValue = new Label();

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
        "-fx-padding: 16;" +
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
        "-fx-text-fill: #63e6ff; -fx-font-weight: 700;";

    private static final String MONITOR_LABEL =
        "-fx-text-fill: #d7f8ff; -fx-font-weight: 700;";

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
        "-fx-text-fill: white;" +
        "-fx-prompt-text-fill: white;" +
        "-fx-border-color: #2d6678;" +
        "-fx-border-radius: 6;" +
        "-fx-background-radius: 6;";

    public ConfigurePage(App app, SimulationService sim) {
        this.app = app;
        this.sim = sim;

        // Apply page background style and padding
        root.setStyle(PAGE_BG);
        root.setPadding(new Insets(16));

        // ----- Header (title + subtitle) -----
        VBox header = new VBox(4); // Vertical box with spacing of 4px between title and subtitle
        Label title = new Label("✈ Airport Traffic Simulation System");
        title.setFont(Font.font(30));
        title.setStyle(TITLE_TEXT);
        Label subtitle = new Label("model aircraft throughput and analyse runway configurations");
        subtitle.setStyle(SUBTITLE_TEXT);

        header.getChildren().addAll(title, subtitle);
        header.setPadding(new Insets(0, 0, 12, 0));
        root.setTop(header);

        // ----- Main Layout: left sidebar + right content -----
        VBox pageBody = new VBox(5);
        pageBody.setPadding(new Insets(6, 0, 0, 0));
        pageBody.setFillWidth(true);

        StackPane atcView = buildAtcView();
        atcView.setMaxWidth(Double.MAX_VALUE);

        HBox bottomDashboard = new HBox(18);
        bottomDashboard.setAlignment(Pos.BOTTOM_LEFT);
        bottomDashboard.setFillHeight(true);

        VBox leftSidebar = buildLeftSidebar();
        VBox rightContent = buildRightContent();

        HBox.setHgrow(rightContent, Priority.ALWAYS); // Let right side expand to fill remaining width
        VBox.setVgrow(bottomDashboard, Priority.ALWAYS);
        bottomDashboard.getChildren().addAll(leftSidebar, rightContent);

        pageBody.getChildren().addAll(atcView, bottomDashboard);

        root.setCenter(pageBody);

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
    }

    // ----- Page Interface Methods -----
    @Override
    public Parent getView() {
        return root;
    }

    @Override
    public void onEnter(Object ignored) {
        // keep last state
    }

    // ----- UI Builder for left side content -----
    private VBox buildLeftSidebar() {
        VBox left = new VBox(14); // Vertical box with spacing of 16px between sections
        left.setPrefWidth(330);
        left.setAlignment(Pos.BOTTOM_LEFT);
        VBox.setVgrow(left, Priority.ALWAYS);

        // ----- Simulation Control panel -----
        VBox simPanel = new VBox(12); // Vertical box with spacing of 14px between controls
        simPanel.setStyle(PANEL); // Apply panel styling 

        Label simTitle = new Label("Simulation Control");
        simTitle.setFont(Font.font(20));
        simTitle.setStyle(MONITOR_LABEL);

        // Number of runways
        VBox runwayRow = new VBox(6); // Vertical box with spacing of 6px between label and slider
        Label runwayLabel = new Label("Number of Runways: ");
        HBox runwayLine = new HBox(6, runwayLabel, runwayValue); // Horizontal box to put label and value on the same line with spacing of 6px
        runwayLine.setAlignment(Pos.CENTER_LEFT);
        runwayRow.getChildren().addAll(runwayLine, runwaySlider);

        // Style runway labels
        runwayLabel.setStyle(LABEL_TEXT);
        runwayValue.setStyle(MONITOR_TITLE);

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

        simPanel.getChildren().addAll(simTitle, runwayRow, runBtn, undoRedoReset);

        // ----- Aircraft Generation panel -----
        VBox aircraftPanel = new VBox(12); // Vertical box with spacing of 14px between controls
        aircraftPanel.setStyle(PANEL);

        Label aircraftTitle = new Label("Aircraft Generation");
        aircraftTitle.setFont(Font.font(20));
        aircraftTitle.setStyle(MONITOR_LABEL);

        // Inbound flow rate slider + label
        VBox inboundBox = new VBox(5);
        HBox inboundLine = new HBox(5, new Label("Arrival Rate:"), inboundValue, new Label("/hour"));
        inboundLine.setAlignment(Pos.CENTER_LEFT);
        inboundBox.getChildren().addAll(inboundLine, inboundSlider);

        // Outbound flow rate slider + label
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
        VBox emergencyPanel = new VBox(12); // Vertical box with spacing of 14px between controls
        emergencyPanel.setStyle(PANEL);

        Label emergencyTitle = new Label("Emergency Generation");
        emergencyTitle.setFont(Font.font(20));
        emergencyTitle.setStyle(MONITOR_TITLE);

        // Passenger Emergency slider + label
        VBox passengerHealthEmergencyBox = new VBox(5);
        HBox passengerHealthEmergencyLine = new HBox(5, new Label("Passenger Health Emergencies:"), passengerHealthEmergencyValue, new Label("%"));
        passengerHealthEmergencyLine.setAlignment(Pos.CENTER_LEFT);
        passengerHealthEmergencyBox.getChildren().addAll(passengerHealthEmergencyLine, passengerHealthEmergencySlider);

        // Mechanical Emergency slider + label
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

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        left.getChildren().addAll(spacer, simPanel, aircraftPanel, emergencyPanel);
        
        return left;
    }

    // ----- UI Builder for the right side content -----
    private VBox buildRightContent() {
        VBox right = new VBox(16);
        right.setAlignment(Pos.TOP_LEFT);

        // ----- "Monitor wall" -----
        VBox monitorWall = new VBox(12);
        monitorWall.setFillWidth(true);
        monitorWall.setAlignment(Pos.TOP_LEFT);
        monitorWall.setStyle(SCREEN_WALL);

        Label runwayTitle = new Label("Runway Configuration Screens");
        runwayTitle.setFont(Font.font(20));
        runwayTitle.setStyle(MONITOR_LABEL);

        runwayGrid.setHgap(18);
        runwayGrid.setVgap(18);
        runwayGrid.setPadding(new Insets(8));

        if (runwayGrid.getColumnConstraints().isEmpty()) {
            for (int c = 0; c < 5; c++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setHgrow(Priority.ALWAYS);
                cc.setFillWidth(true);
                cc.setPercentWidth(20);
                runwayGrid.getColumnConstraints().add(cc);
            }
        }

        monitorWall.getChildren().addAll(runwayTitle, runwayGrid, errorLabel);

        errorLabel.setStyle("-fx-text-fill: #ff7b7b; -fx-font-weight: 700;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        right.getChildren().addAll(spacer, monitorWall);

        return right;
    }

    // Initialize slider properties (tick marks, labels, etc.)
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
    }

    // ----- Runway Cards -----
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
            int row = idx / 5;
            int col = idx % 5;

            runwayGrid.add(card.root, col, row);
        }
    }

    // ----- Data Handling -----
    private void loadIntoControls(SimulationData d) {
        d.ensureRunwayListSize();

        // Set UI slider positions from stored data
        runwaySlider.setValue(d.runwayCount);
        inboundSlider.setValue(d.inboundRatePerHour);
        outboundSlider.setValue(d.outboundRatePerHour);
        passengerHealthEmergencySlider.setValue(d.passengerHealthEmergencyProbability);
        mechanicalEmergencySlider.setValue(d.mechanicalEmergencyProbability);

        // Set numeric labels accordingly
        runwayValue.setText(String.valueOf(d.runwayCount));
        inboundValue.setText(String.valueOf(d.inboundRatePerHour));
        outboundValue.setText(String.valueOf(d.outboundRatePerHour));
        passengerHealthEmergencyValue.setText(String.valueOf(d.passengerHealthEmergencyProbability));
        mechanicalEmergencyValue.setText(String.valueOf(d.mechanicalEmergencyProbability));
    }

    // ----- Run Simulation Button Handler -----
    private void onRun() {
        // Read values from sliders
        int runways = (int) Math.round(runwaySlider.getValue());
        int in = (int) Math.round(inboundSlider.getValue());
        int out = (int) Math.round(outboundSlider.getValue());

        // Validate runway count and flow rates - if invalid, show error message and don't proceed
        if (runways < 1 || runways > 10) {
            errorLabel.setText("Runway count must be between 1 and 10.");
            return;
        }
        if (in < 0 || in > 30 || out < 0 || out > 30) {
            errorLabel.setText("Flow rates must be between 0 and 30 per hour.");
            return;
        }

        errorLabel.setText(""); // Clear error message

        // Write validated values back into data
        data.runwayCount = runways;
        data.inboundRatePerHour = in;
        data.outboundRatePerHour = out;
        data.ensureRunwayListSize();

        app.switchTo(AppState.RUNNING, data.copy()); // Navigate to Running Page and pass a copy of the current config data
    }

    // ----- Runway card component -----
    private class RunwayCard {
        private final VBox root = new VBox(10); // Set runway card size and spacing

        private final Label title = new Label();
        private final ComboBox<SimulationData.RunwayStatus> statusBox = new ComboBox<>();
        private final ComboBox<SimulationData.RunwayMode> modeBox = new ComboBox<>();

        private SimulationData.RunwayConfig bound;

        // Runway card constructor
        RunwayCard(SimulationData.RunwayConfig cfg) {
            root.setStyle(CARD);
            root.setMinWidth(180);
            root.setPrefWidth(220);
            root.setMinHeight(170);

            title.setFont(Font.font(20));
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

        // Binding method - connects this UI card to a specific RunwayConfig data object
        void bindTo(SimulationData.RunwayConfig cfg) {
            this.bound = cfg;
            title.setText("Runway " + cfg.id + ":");
            statusBox.setValue(cfg.status);
            modeBox.setValue(cfg.mode);
        }

        // Enable or disable this card
        void setEnabled(boolean enabled) {
            root.setStyle(enabled ? CARD : CARD_DISABLED);
            statusBox.setDisable(!enabled);
            modeBox.setDisable(!enabled);
            title.setStyle(enabled ? MONITOR_TITLE : "-fx-text-fill: #7d8a93; -fx-font-weight: 700;");
            root.setOpacity(enabled ? 1.0 : 0.82);
        }
    }

    // ----- Helper Methods for Undo/Redo Buttons -----
    private void pushUndoSnapshot(){
        if (applyingSnapshot) return;

        // Snapshot before the change is applied
        undoStack.add(data.copy());
        // Once you make a new change, redo history is invalid
        redoStack.clear();
        updateUndoRedoButtons();
    }

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

    private void undo() {
        if (undoStack.isEmpty()) return;
        redoStack.add(data.copy());     // Save current for redo
        SimulationData prev = undoStack.remove(undoStack.size() - 1);
        applySnapshot(prev);
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        undoStack.add(data.copy());     // Save current for undo
        SimulationData next = redoStack.remove(redoStack.size() - 1);
        applySnapshot(next);
    }

    private void updateUndoRedoButtons() {
        if (undoBtn != null) undoBtn.setDisable(undoStack.isEmpty());
        if (redoBtn != null) redoBtn.setDisable(redoStack.isEmpty());
    }

    // ----- Atc "Window" View -----
    private StackPane buildAtcView() {
        double windowHeight = 310;

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

    // ----- Create window panes and add 3D effect -----
    private Pane buildWindowOverlay() {
        double windowHeight = 310;
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