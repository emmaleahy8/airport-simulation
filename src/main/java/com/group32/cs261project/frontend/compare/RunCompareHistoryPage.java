package com.group32.cs261project.frontend.compare;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.group32.cs261project.App;
import com.group32.cs261project.backend.compare.RunCompareCompletedRun;
import com.group32.cs261project.backend.compare.RunCompareHistory;
import com.group32.cs261project.frontend.AppState;
import com.group32.cs261project.frontend.Page;
import com.group32.cs261project.frontend.SimulationService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.time.Duration;

import com.group32.cs261project.frontend.HelpOverlay;
import com.group32.cs261project.frontend.HelpText;

/**
 * RunCompareHistoryPage displays the history of previously completed simulation runs
 * and allows the user to select two runs for comparison.
 *
 * <p>This page reads the stored run history from {@link RunCompareHistory} via the
 * {@link SimulationService} and renders each run as a dashboard-style card
 * containing configuration parameters and key simulation metrics.</p>
 *
 * The user may select exactly two runs using checkboxes. When two runs are selected,
 * the "Compare Selected Runs" button becomes enabled and the user can navigate to
 * the comparison page to analyse differences between the two simulations.</p>
 *
 * Each run card contains:
 *      Run identity and completion time</li>
 *      Simulation configuration parameters</li>
 *      Key output metrics from the simulation report</li>
 *
 * This page is part of the run comparison workflow.
 */

public class RunCompareHistoryPage implements Page {

    // ----- Page styling -----
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

    private static final String CARD_SELECTED =
            "-fx-background-color: rgba(8, 18, 28, 0.98);" +
            "-fx-border-color: #1a6eff;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 14;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(26,110,255,0.28), 18, 0.22, 0, 2);";

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

    private static final String ERROR_TEXT =
        "-fx-text-fill: #ff4d4f; -fx-font-weight: bold; -fx-font-size: 14px;";

    private static final String STATUS_TEXT =
            "-fx-text-fill: #b8cad6; -fx-font-size: 16px;";

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

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private final App app;
    private final SimulationService sim;

    private final BorderPane root = new BorderPane();

    private final Label title = new Label("Run History");
    private final Label subtitle = new Label("Select exactly two completed runs to compare.");
    private final Label statusLabel = new Label("Select two runs.");

    private final VBox runsBox = new VBox(12);

    private final List<RunSelectionRow> selectionRows = new ArrayList<>();

    private final Button compareButton = new Button("Compare Selected Runs");
    private final Button backButton = new Button("Back to Results");

    /**
     * Constructs the RunCompareHistoryPage UI.
     *
     * The constructor builds the static layout of the page including the
     * header, scrollable list of run cards, and bottom action bar.
     *
     * The run list itself is populated dynamically when the page is entered
     * via {@link #onEnter(Object)}.
     *
     * @param app reference to the main application controller
     * @param sim simulation service used to retrieve stored run history
     */
    public RunCompareHistoryPage(App app, SimulationService sim) {
        this.app = app;
        this.sim = sim;

        root.setStyle(PAGE_BG);
        root.setPadding(new Insets(18));

        // ----- Header -----
        title.setFont(Font.font(null, FontWeight.BOLD, 30));
        title.setStyle(TITLE_TEXT);

        subtitle.setStyle(SUBTITLE_TEXT);
        subtitle.setWrapText(true);

        VBox header = new VBox(4, title, subtitle);
        header.setPadding(new Insets(0, 0, 12, 0));
        root.setTop(header);

        // ----- Main list of stored runs -----
        runsBox.setFillWidth(true);

        ScrollPane scrollPane = new ScrollPane(runsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        root.setCenter(scrollPane);

        // ----- Bottom action row -----
        compareButton.setStyle(BTN_PRIMARY);
        compareButton.setOnAction(e -> compareSelectedRuns());

        backButton.setStyle(BTN_SOFT);
        backButton.setOnAction(e -> app.switchTo(AppState.RESULTS, null));

        statusLabel.setStyle(SMALL_TEXT);

        HBox bottomBar = new HBox(10, backButton, compareButton, statusLabel);
        bottomBar.setAlignment(Pos.CENTER_LEFT);

        BorderPane.setMargin(bottomBar, new Insets(14, 0, 0, 0));
        root.setBottom(bottomBar);

        updateCompareButtonState();
    }

    // Returns JavaFX root node for this page
    @Override
    public Parent getView() {
        return HelpOverlay.wrap(root, "Run History", HelpText.compareAnyTwoRunsPage());
    }

    // Called when the page becomes active
    // The run history list is rebuilt each time the page is entered so simulation runs immediately appear in the history
    @Override
    public void onEnter(Object data) {
        rebuildHistoryList();
    }

    // Called when the page is exited
    // No cleanup is required because the page state is reconstructed each time it is entered
    @Override
    public void onExit() {
        // no-op
    }

    /**
     * Rebuilds the run history list displayed on the page.
     *
     * This method retrieves all stored runs from {@link RunCompareHistory}
     * and generates a dashboard card for each run. Runs are displayed with
     * the most recent first so that recent simulations are easier to locate.
     *
     * If no runs exist, a message is shown instead.
     */
    private void rebuildHistoryList() {
        runsBox.getChildren().clear();
        selectionRows.clear();

        RunCompareHistory history = sim.getRunCompareHistory();

        if (history == null || history.size() == 0) {
            Label emptyLabel = new Label("No completed runs have been stored yet.");
            emptyLabel.setStyle(SMALL_TEXT);
            runsBox.getChildren().add(emptyLabel);
            updateCompareButtonState();
            return;
        }

        List<RunCompareCompletedRun> runs = history.getAllRuns();

        // Newest first so the most recent runs are easier to find.
        for (int i = runs.size() - 1; i >= 0; i--) {
            RunCompareCompletedRun run = runs.get(i);
            RunSelectionRow row = new RunSelectionRow(run);

            selectionRows.add(row);
            runsBox.getChildren().add(buildRunCard(row));
        }

        updateCompareButtonState();
    }

    /**
     * Creates a visual card representing a single completed simulation run.
     *
     * The card displays configuration parameters and key simulation metrics
     * allowing the user to quickly inspect runs before selecting them for
     * comparison.</p>
     *
     * Each card contains:
     *     Run identity and completion time</li>
     *     Simulation configuration (runways, arrival/departure rates, etc.)</li>
     *     Key simulation metrics (holding size, takeoff queue size, etc.)</li>
     * 
     * @param row wrapper object containing the run and its selection checkbox
     * @return a VBox representing the run card UI
     */
    private VBox buildRunCard(RunSelectionRow row) {
        VBox card = new VBox(10);
        card.setStyle(row.checkBox.isSelected() ? CARD_SELECTED : CARD);
        card.setFocusTraversable(false);
        row.card = card;

        // ----- Top row -----
        // Keeps selection control and main run identity together.
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox selectBox = row.checkBox;

        Label runIdLabel = new Label("Run ID: " + row.run.runId());
        runIdLabel.setStyle(MONITOR_LABEL);
        runIdLabel.setWrapText(true);

        topRow.getChildren().addAll(selectBox, runIdLabel);

        // ----- Run details -----
        Label completedLabel = new Label("Completed: " + TIME_FMT.format(row.run.completedAt()));
        completedLabel.setStyle(SMALL_TEXT);

        Label seedLabel = new Label("Seed: " + row.run.config().randomSeed());
        seedLabel.setStyle(SMALL_TEXT);

        // Separator
        Separator sep = new Separator();
        sep.setOpacity(0.35);

        // ----- Configuration Section -----
        Label configTitle = new Label("Configuration");
        configTitle.setStyle(MONITOR_TITLE);

        // Runway count
        Label runwayCountLabel = new Label("Runways: " + row.run.config().runwayCount());
        runwayCountLabel.setStyle(LABEL_TEXT);

        // Inbound rate
        Label inboundLabel = new Label(String.format(
                "Arrival rate: %.0f aircraft/hour", row.run.config().inboundRatePerHour()));
        inboundLabel.setStyle(LABEL_TEXT);

        // Outbound rate
        Label outboundLabel = new Label(String.format(
                "Departure rate: %.0f aircraft/hour", row.run.config().outboundRatePerHour()));
        outboundLabel.setStyle(LABEL_TEXT);

        // Passenger health emergency probability
        Label passengerEmergencyLabel = new Label(String.format(
                "Passenger health emergencies: %.0f%%",
                row.run.config().passengerHealthEmergencyProbability()));
        passengerEmergencyLabel.setStyle(LABEL_TEXT);

        // Mechanical emergency probability
        Label mechanicalEmergencyLabel = new Label(String.format(
                "Mechanical emergencies: %.0f%%",
                row.run.config().mechanicalEmergencyProbability()));
        mechanicalEmergencyLabel.setStyle(LABEL_TEXT);

        // Simulation duration
        // Calculate simulation duration from configured start and end time
       long simulationHours = Duration.between(
                row.run.config().startTime(),
                row.run.config().endTime()
        ).toHours();
        Label simulationHoursLabel = new Label(String.format(
                "Simulation duration: %d hours",
                simulationHours));
        simulationHoursLabel.setStyle(LABEL_TEXT);

        // Max. time in takeoff queue
        Label maxTakeoffQueueTimeLabel = new Label(String.format(
                "Max. time in takeoff queue: %d mins",
                row.run.config().maxTakeoffWaitMinutes()));
        maxTakeoffQueueTimeLabel.setStyle(LABEL_TEXT);

        // Separator
        Separator sep2 = new Separator();
        sep2.setOpacity(0.35);

        // ----- Metrics section -----
        Label resultsTitle = new Label("Key Results");
        resultsTitle.setStyle(MONITOR_TITLE);

        // Max. holding size
        Label maxHoldingLabel = new Label("Max. holding size: " + row.run.report().maxHoldingSize());
        maxHoldingLabel.setStyle(LABEL_TEXT);

        // Max. takeoff queue size
        Label maxTakeoffLabel = new Label("Max. takeoff queue size: " + row.run.report().maxTakeoffQueueSize());
        maxTakeoffLabel.setStyle(LABEL_TEXT);

        // Number of diversions
        Label divertedLabel = new Label("Diversions: " + row.run.report().numDiverted());
        divertedLabel.setStyle(LABEL_TEXT);

        // Number of cancellations
        Label cancellationsLabel = new Label("Cancellations: " + row.run.report().numCancellations());
        cancellationsLabel.setStyle(LABEL_TEXT);

        VBox details = new VBox(6,
                completedLabel,
                seedLabel,
                sep,
                configTitle,
                runwayCountLabel,
                inboundLabel,
                outboundLabel,
                passengerEmergencyLabel,
                mechanicalEmergencyLabel,
                simulationHoursLabel,
                maxTakeoffQueueTimeLabel,
                sep2,
                resultsTitle,
                maxHoldingLabel,
                maxTakeoffLabel,
                divertedLabel,
                cancellationsLabel
        );

        card.getChildren().addAll(topRow, details);

        return card;
    }

    /**
     * Initiates comparison between the two selected runs.
     *
     * If exactly two runs are selected, their data is packaged into
     * a {@link RunCompareSelectionData} object and passed to the
     * comparison page.
     *
     * If fewer or more runs are selected, the action is ignored
     * and the UI state is refreshed.
     */
    private void compareSelectedRuns() {
        List<RunCompareCompletedRun> selectedRuns = getSelectedRuns();
        if (selectedRuns.size() != 2) {
            updateCompareButtonState();
            return;
        }

        RunCompareSelectionData selectionData =
                new RunCompareSelectionData(selectedRuns.get(0), selectedRuns.get(1));

        app.switchTo(AppState.COMPARE, selectionData);
    }
 
    // Returns all runs currently selected by the user
    private List<RunCompareCompletedRun> getSelectedRuns() {
        List<RunCompareCompletedRun> selected = new ArrayList<>();

        for (RunSelectionRow row : selectionRows) {
            if (row.checkBox.isSelected()) {
                selected.add(row.run);
            }
        }

        return selected;
    }

    /**
     * Updates the state of the Compare button and helper status text.
     *
     * The compare button is only enabled when exactly two runs are selected.
     * The status message also guides the user by indicating how many runs
     * still need to be selected.
     */
    private void updateCompareButtonState() {
        int selectedCount = getSelectedRuns().size();
        boolean enabled = selectedCount == 2;

        compareButton.setDisable(!enabled);

        if (selectionRows.isEmpty()) {
            statusLabel.setText("No runs available.");
            statusLabel.setStyle(ERROR_TEXT);
        } else if (selectedCount == 0) {
            statusLabel.setText("Select two runs.");
            statusLabel.setStyle(SMALL_TEXT);
        } else if (selectedCount == 1) {
            statusLabel.setText("Select one more run.");
            statusLabel.setStyle(ERROR_TEXT);
        } else if (selectedCount == 2) {
            statusLabel.setText("Ready to compare.");
            statusLabel.setStyle(MONITOR_TITLE);
        } else {
            statusLabel.setText("Select exactly two runs.");
            statusLabel.setStyle(ERROR_TEXT);
        }

        // Refresh card highlighting so selected runs stand out visually.
        refreshSelectionStyles();
    }

    /**
     * Refreshes the visual styling of run cards based on selection state.
     *
     * Selected runs are highlighted using a different card style so the
     * user can easily see which runs are currently selected.
     */
    private void refreshSelectionStyles() {
        for (RunSelectionRow row : selectionRows) {
            if (row.card != null) {
                row.card.setStyle(row.checkBox.isSelected() ? CARD_SELECTED : CARD);
            }
        }
    }

    /**
     * Internal helper class representing a selectable run entry.
     *
     * This class links a {@link RunCompareCompletedRun} object with the
     * checkbox used to select it and the UI card representing it.
     */
    private final class RunSelectionRow {
        private final RunCompareCompletedRun run;
        private final CheckBox checkBox = new CheckBox();
        private VBox card;

        // Creates a selectable run row
        private RunSelectionRow(RunCompareCompletedRun run) {
            this.run = run;

            // Any checkbox change updates button state, status text, and selected card styling.
            this.checkBox.setOnAction(e -> updateCompareButtonState());
        }
    }
}