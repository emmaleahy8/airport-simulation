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

public class RunCompareHistoryPage implements Page {

    // ----- Shared page styling -----
    // Matches the updated dark dashboard style used across the newer frontend pages.
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

    // Slightly brighter border used when a run is selected.
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

    public RunCompareHistoryPage(App app, SimulationService sim) {
        this.app = app;
        this.sim = sim;

        root.setStyle(PAGE_BG);
        root.setPadding(new Insets(18));

        // ----- Header -----
        // Uses the newer title + subtitle style so this page matches the updated UI.
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bottomBar = new HBox(10, backButton, compareButton, spacer, statusLabel);
        bottomBar.setAlignment(Pos.CENTER_LEFT);

        BorderPane.setMargin(bottomBar, new Insets(14, 0, 0, 0));
        root.setBottom(bottomBar);

        updateCompareButtonState();
    }

    @Override
    public Parent getView() {
        return root;
    }

    @Override
    public void onEnter(Object data) {
        rebuildHistoryList();
    }

    @Override
    public void onExit() {
        // no-op
    }

    // Rebuilds the list from history each time the page is entered so the view always reflects
    // the current stored runs.
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

    // Builds a dashboard-style card for one completed run.
    private VBox buildRunCard(RunSelectionRow row) {
        VBox card = new VBox(10);
        card.setStyle(row.checkBox.isSelected() ? CARD_SELECTED : CARD);
        card.setFocusTraversable(false);

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

        Label inboundLabel = new Label(String.format("Inbound: %.2f aircraft/hour", row.run.config().inboundRatePerHour()));
        inboundLabel.setStyle(SMALL_TEXT);

        Label outboundLabel = new Label(String.format("Outbound: %.2f aircraft/hour", row.run.config().outboundRatePerHour()));
        outboundLabel.setStyle(SMALL_TEXT);

        Separator sep = new Separator();
        sep.setOpacity(0.35);

        Label maxHoldingLabel = new Label(String.format("Max holding size: %s", String.valueOf(row.run.report().maxHoldingSize())));
        maxHoldingLabel.setStyle(LABEL_TEXT);

        Label maxTakeoffLabel = new Label(String.format("Max takeoff queue size: %s", String.valueOf(row.run.report().maxTakeoffQueueSize())));
        maxTakeoffLabel.setStyle(LABEL_TEXT);

        Label divertedLabel = new Label(String.format("Diversions: %s", String.valueOf(row.run.report().numDiverted())));
        divertedLabel.setStyle(LABEL_TEXT);

        Label cancellationsLabel = new Label(String.format("Cancellations: %s", String.valueOf(row.run.report().numCancellations())));
        cancellationsLabel.setStyle(LABEL_TEXT);

        VBox details = new VBox(6,
                completedLabel,
                seedLabel,
                inboundLabel,
                outboundLabel,
                sep,
                maxHoldingLabel,
                maxTakeoffLabel,
                divertedLabel,
                cancellationsLabel
        );

        card.getChildren().addAll(topRow, details);
        return card;
    }

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

    private List<RunCompareCompletedRun> getSelectedRuns() {
        List<RunCompareCompletedRun> selected = new ArrayList<>();

        for (RunSelectionRow row : selectionRows) {
            if (row.checkBox.isSelected()) {
                selected.add(row.run);
            }
        }

        return selected;
    }

    // Keeps the compare button and helper message aligned with the current selection state.
    private void updateCompareButtonState() {
        int selectedCount = getSelectedRuns().size();
        boolean enabled = selectedCount == 2;

        compareButton.setDisable(!enabled);

        if (selectionRows.isEmpty()) {
            statusLabel.setText("No runs available.");
            statusLabel.setStyle(SMALL_TEXT);
        } else if (selectedCount == 0) {
            statusLabel.setText("Select two runs.");
            statusLabel.setStyle(SMALL_TEXT);
        } else if (selectedCount == 1) {
            statusLabel.setText("Select one more run.");
            statusLabel.setStyle(SMALL_TEXT);
        } else if (selectedCount == 2) {
            statusLabel.setText("Ready to compare.");
            statusLabel.setStyle(MONITOR_TITLE);
        } else {
            statusLabel.setText("Select exactly two runs.");
            statusLabel.setStyle(SMALL_TEXT);
        }

        // Refresh card highlighting so selected runs stand out visually.
        refreshSelectionStyles();
    }

    private void refreshSelectionStyles() {
        for (RunSelectionRow row : selectionRows) {
            if (row.card != null) {
                row.card.setStyle(row.checkBox.isSelected() ? CARD_SELECTED : CARD);
            }
        }
    }

    private final class RunSelectionRow {
        private final RunCompareCompletedRun run;
        private final CheckBox checkBox = new CheckBox();
        private VBox card;

        private RunSelectionRow(RunCompareCompletedRun run) {
            this.run = run;

            // Any checkbox change updates button state, status text, and selected card styling.
            this.checkBox.setOnAction(e -> updateCompareButtonState());
        }
    }
}