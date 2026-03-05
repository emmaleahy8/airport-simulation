package com.group32.cs261project.frontend;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

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

    // Labels showing current slider values
    private final Label runwayValue = new Label();
    private final Label inboundValue = new Label();
    private final Label outboundValue = new Label();

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
    private static final String PAGE_BG = "-fx-background-color: white;";
    private static final String PANEL =
            "-fx-background-color: white;" + 
            "-fx-border-color: #111;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 16;";
    // Runway cards (active)
    private static final String CARD =
            "-fx-background-color: white;" +
            "-fx-border-color: #111;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 14;";
    // Runway cards (disabled)
    private static final String CARD_DISABLED =
            "-fx-background-color: #e9ecef;" +
            "-fx-border-color: #111;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 14;";
    // Button styles
    private static final String BTN_PRIMARY =
            "-fx-background-color: #111; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 10 14;";
    private static final String BTN_DANGER =
            "-fx-background-color: #f48484; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 10 14;";
    private static final String BTN_SUCCESS =
            "-fx-background-color: #0aa84f; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 10 14;";
    private static final String BTN_SOFT =
            "-fx-background-color: #2f6dff; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 8 12;";
    private static final String BTN_SOFT_2 =
            "-fx-background-color: #2f6dff; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 8 12;";
    private static final String BTN_SOFT_3 =
            "-fx-background-color: #2f6dff; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 8 12;";

    public ConfigurePage(App app, SimulationService sim) {
        this.app = app;
        this.sim = sim;

        // Apply page background style and padding
        root.setStyle(PAGE_BG);
        root.setPadding(new Insets(16));

        // ----- Header (title + subtitle) -----
        VBox header = new VBox(4); // Vertical box with spacing of 4px between title and subtitle
        Label title = new Label("✈ Airport Traffic Simulation System");
        title.setFont(Font.font(26));
        Label subtitle = new Label("model aircraft throughput and analyse runway configurations");
        subtitle.setStyle("-fx-text-fill: #444;");

        header.getChildren().addAll(title, subtitle);
        header.setPadding(new Insets(0, 0, 12, 0));
        root.setTop(header);

        // ----- Main Layout: left sidebar + right content -----
        HBox main = new HBox(18); // Horizontal box with spacing of 18px between sidebar and content
        main.setPadding(new Insets(8, 0, 0, 0));

        VBox leftSidebar = buildLeftSidebar();
        VBox rightContent = buildRightContent();

        HBox.setHgrow(rightContent, Priority.ALWAYS); // Let right side expand to fill remaining width

        main.getChildren().addAll(leftSidebar, rightContent);
        root.setCenter(main);

        // Initial Setup
        initSliders();
        loadIntoControls(data);
        buildRunwayCards();
        refreshRunwayGrid();

        runwaySlider.setOnMousePressed(e -> pushUndoSnapshot());
        inboundSlider.setOnMousePressed(e -> pushUndoSnapshot());
        outboundSlider.setOnMousePressed(e -> pushUndoSnapshot());
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
        VBox left = new VBox(16); // Vertical box with spacing of 16px between sections
        left.setPrefWidth(380);

        // Simulation Control panel
        VBox simPanel = new VBox(14); // Vertical box with spacing of 14px between controls
        simPanel.setStyle(PANEL); // Apply panel styling (white background, border, padding)

        Label simTitle = new Label("Simulation Control");
        simTitle.setFont(Font.font(20));

        // Number of runways
        VBox runwayRow = new VBox(6); // Vertical box with spacing of 6px between label and slider
        Label runwayLabel = new Label("Number of Runways: ");
        HBox runwayLine = new HBox(6, runwayLabel, runwayValue); // Horizontal box to put label and value on the same line with spacing of 6px
        runwayLine.setAlignment(Pos.CENTER_LEFT);
        runwayRow.getChildren().addAll(runwayLine, runwaySlider);

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
        redoBtn.setStyle(BTN_SOFT_2);
        resetBtn.setStyle(BTN_SOFT_3);

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

        // Aircraft Generation panel
        VBox aircraftPanel = new VBox(14); // Vertical box with spacing of 14px between controls
        aircraftPanel.setStyle(PANEL);

        Label aircraftTitle = new Label("Aircraft Generation");
        aircraftTitle.setFont(Font.font(20));

        // Inbound flow rate slider + label
        VBox inboundBox = new VBox(6);
        HBox inboundLine = new HBox(6, new Label("Arrival Rate:"), inboundValue, new Label("/hour"));
        inboundLine.setAlignment(Pos.CENTER_LEFT);
        inboundBox.getChildren().addAll(inboundLine, inboundSlider);

        // Outbound flow rate slider + label
        VBox outboundBox = new VBox(6);
        HBox outboundLine = new HBox(6, new Label("Departure Rate:"), outboundValue, new Label("/hour"));
        outboundLine.setAlignment(Pos.CENTER_LEFT);
        outboundBox.getChildren().addAll(outboundLine, outboundSlider);

        aircraftPanel.getChildren().addAll(aircraftTitle, inboundBox, outboundBox);

        left.getChildren().addAll(simPanel, aircraftPanel);
        return left;
    }

    // ----- UI Builder for the right side content -----
    private VBox buildRightContent() {
        VBox right = new VBox(10); // Vertical box with spacing of 10px between elements
        right.setStyle(PANEL);

        Label runwayTitle = new Label("Runway Configuration:");
        runwayTitle.setFont(Font.font(20));

        // Grid spacing and padding
        runwayGrid.setHgap(18);
        runwayGrid.setVgap(18);
        runwayGrid.setPadding(new Insets(8));

        // Make 5 equal-width columns
        for (int c = 0; c < 5; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            cc.setPercentWidth(20);
            runwayGrid.getColumnConstraints().add(cc);
        }

        right.getChildren().addAll(runwayTitle, runwayGrid, errorLabel);
        VBox.setVgrow(runwayGrid, Priority.ALWAYS);

        // Error label styling
        errorLabel.setStyle("-fx-text-fill: #b00020;");
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

        // Set numeric labels accordingly
        runwayValue.setText(String.valueOf(d.runwayCount));
        inboundValue.setText(String.valueOf(d.inboundRatePerHour));
        outboundValue.setText(String.valueOf(d.outboundRatePerHour));
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

    // ----- Runway card component (inner class) -----

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

            title.setFont(Font.font(16));

            Label statusLabel = new Label("Operational Status");
            statusLabel.setStyle("-fx-font-weight: 700;");

            statusBox.getItems().setAll(SimulationData.RunwayStatus.values()); // Populate dropdown with enum values

            Label modeLabel = new Label("Operating Mode");
            modeLabel.setStyle("-fx-font-weight: 700;");

            modeBox.getItems().setAll(SimulationData.RunwayMode.values()); // Populate dropdown with enum values

            root.getChildren().addAll(title, statusLabel, statusBox, modeLabel, modeBox);

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
            root.setOpacity(enabled ? 1.0 : 0.85);
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
}