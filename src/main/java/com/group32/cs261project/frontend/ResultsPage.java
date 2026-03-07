package com.group32.cs261project.frontend;

import com.group32.cs261project.App;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

public class ResultsPage implements Page {

    private final App app;
    private final SimulationService sim;

    private final BorderPane root = new BorderPane();

    // Main Heading
    private final Label title = new Label("End-of-Run Summary");

    // Left card dynamic labels - initially set to "-" until populated with real data onEnter()
    private final Label inboundRateVal = new Label("-");
    private final Label outboundRateVal = new Label("-");
    private final Label runwayCountVal = new Label("-");
    private final Label maxWaitVal = new Label("-");

    private final Label runwayModesVal = new Label("-");
    private final Label runwayStatusVal = new Label("-");

    // Right card dynamic labels - initially set to "-" until populated with real data onEnter()
    private final Label maxTakeoffQueueVal = new Label("-");
    private final Label avgTakeoffWaitVal = new Label("-");

    private final Label maxHoldingVal = new Label("-");
    private final Label avgHoldingVal = new Label("-");

    private final Label maxInboundDelayVal = new Label("-");
    private final Label avgInboundDelayVal = new Label("-");

    private final Label cancelledVal = new Label("-");
    private final Label divertedVal = new Label("-");

    // ----- Styles -----
    private static final String PAGE_BG = "-fx-background-color: rgb(14, 26, 40);";
    private static final String CARD =
        "-fx-background-color: rgba(8, 18, 28, 0.96);" + 
        "-fx-border-color: #1f3b52;" +
        "-fx-border-width: 1.5;" +
        "-fx-border-radius: 14;" +
        "-fx-background-radius: 14;" +
        "-fx-padding: 16;" +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 14, 0.2, 0, 3);";

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

    // ----- Constructor: Builds UI Layout -----
    public ResultsPage(App app, SimulationService sim) {
        this.app = app;
        this.sim = sim;

        // Set background and padding for the whole page
        root.setStyle(PAGE_BG);
        root.setPadding(new Insets(18));

        // ----- Title at Top -----
        title.setFont(Font.font(28));
        title.setStyle(TITLE_TEXT);
        BorderPane.setMargin(title, new Insets(0, 0, 14, 0));
        root.setTop(title);

        // ----- Main Content (two columns) -----
        HBox main = new HBox(22);

        // Build left and right columns with helper methods
        VBox left = buildGeneralInfoCard();
        VBox right = buildRightCards();

        // Allow both columns to expand as the window grows
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);

        // Suggested widths so left column is larger tha right
        left.setPrefWidth(520);
        right.setPrefWidth(420);

        main.getChildren().addAll(left, right);
        root.setCenter(main);

        // ----- Back Button -----
        Button back = new Button("Back to Configure");
        back.setOnAction(e -> app.switchTo(AppState.CONFIGURE, null)); // When clicked, navigate to Configure Page
        back.setStyle(BTN_PRIMARY);
        BorderPane.setMargin(back, new Insets(14, 0, 0, 0));
        root.setBottom(back);
    }

    // ----- Page Interface Methods -----
    @Override
    public Parent getView() {
        return root;
    }

    @Override
    public void onEnter(Object data) {
        SimulationData d = (SimulationData) data;
        fillFrom(d);
    }

    // ----- UI Builder: Left Card (General Information) -----
    private VBox buildGeneralInfoCard() {
        VBox card = new VBox(14); // Creates a card with vertical spacing of 14 between elements
        card.setStyle(CARD);

        // Card header label (bold and larger font)
        Label header = new Label("General Information");
        header.setFont(Font.font(18));
        header.setStyle(MONITOR_LABEL);

        // Grid for summary stats
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        addRow(grid, 0, "Inbound flow rate:", inboundRateVal, "aircraft /hour");
        addRow(grid, 1, "Outbound flow rate:", outboundRateVal, "aircraft /hour");
        addRow(grid, 2, "Number of runways:", runwayCountVal, "");
        addRow(grid, 3, "Maximum wait time:", maxWaitVal, "minutes");

        // Separator between summary stats and runway lists
        Separator sep = new Separator();
        sep.setPadding(new Insets(8, 0, 8, 0));
        sep.setStyle("-fx-background-color: rgba(8, 18, 28, 0.96);");
        sep.setOpacity(0.45);

        VBox runwayInfo = new VBox(10); // Container for runway lists

        // Header for runway modes list
        Label modesHeader = new Label("Runway operating modes:");
        modesHeader.setStyle(MONITOR_LABEL);

        runwayModesVal.setWrapText(true);
        runwayModesVal.setStyle(LABEL_TEXT);
        
        // Header for runway statuses list
        Label statusHeader = new Label("Runway operational statuses:");
        statusHeader.setStyle(MONITOR_LABEL);

        runwayStatusVal.setWrapText(true);
        runwayStatusVal.setStyle(LABEL_TEXT);

        runwayInfo.getChildren().addAll(
                modesHeader, runwayModesVal,
                statusHeader, runwayStatusVal
        );

        // ----- Assemble and return card -----
        card.getChildren().addAll(header, grid, sep, runwayInfo);

        return card;
    }

    // ----- UI Builder: Right Column -----
    private VBox buildRightCards() {
        VBox right = new VBox(16); // Vertical stack of cards with spacing of 16 between them

        // Add cards: Take-off Queue, Holding Pattern, Delays, Cancellations/Diversions
        right.getChildren().addAll(
                smallCard("Take-off Queue",
                        "Maximum number of planes in take-off queue:", maxTakeoffQueueVal,
                        "Average wait time in take-off queue:", avgTakeoffWaitVal
                ),
                smallCard("Holding Pattern",
                        "Maximum number of planes in holding pattern:", maxHoldingVal,
                        "Average hold time for inbound aircraft:", avgHoldingVal
                ),
                smallCard("Delays",
                        "Maximum inbound delay:", maxInboundDelayVal,
                        "Average inbound delay:", avgInboundDelayVal
                ),
                smallCard("Cancellations and Diversions",
                        "Total planes cancelled:", cancelledVal,
                        "Inbound aircraft diverted due to low fuel:", divertedVal
                )
        );

        return right;
    }

    // ----- UI Builder: small reusable cards for right column -----
    private VBox smallCard(String titleText,
                           String label1, Label value1,
                           String label2, Label value2) {

        VBox card = new VBox(12); 
        card.setStyle(CARD);

        // Card title
        Label header = new Label(titleText);
        header.setFont(Font.font(18));
        header.setStyle(MONITOR_LABEL);

        // Build a 2-row grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        addRow(grid, 0, label1, value1, "minutes");
        addRow(grid, 1, label2, value2, "minutes");

        // Return the finished card
        card.getChildren().addAll(header, grid);
        return card;
    }

    // ----- Helper Method for Adding Row to Grid -----
    private void addRow(GridPane grid, int row, String labelText, Label valueLabel, String unit) {
        Label l = new Label(labelText); // Create label for the left column
        l.setStyle(LABEL_TEXT);

        // Format the right column value label (bold and right-aligned, expands to fill available space)
        valueLabel.setStyle(MONITOR_TITLE);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);
        valueLabel.setMaxWidth(Double.MAX_VALUE);

        Label unitLabel = new Label(unit);
        unitLabel.setStyle(LABEL_TEXT);

        HBox valueBox = new HBox(4, valueLabel, unitLabel);
        valueBox.setAlignment(Pos.CENTER_RIGHT);
        valueBox.setMaxWidth(Double.MAX_VALUE);

        grid.add(l, 0, row);
        grid.add(valueBox, 1, row);

        // ----- Column Constraints -----
        ColumnConstraints c0 = new ColumnConstraints(); // Column 0 grows to take remaining space for long labels
        c0.setHgrow(Priority.ALWAYS);

        ColumnConstraints c1 = new ColumnConstraints(); // Column 1 stayes compact so values line up neatly
        c1.setMinWidth(140);
        c1.setHgrow(Priority.NEVER);

        grid.getColumnConstraints().setAll(c0, c1);
    }

    // ----- Results Data -----
    private void fillFrom(SimulationData d) {
        d.ensureRunwayListSize();

        // General info
        inboundRateVal.setText(String.format("%.2f", (double) d.inboundRatePerHour));
        outboundRateVal.setText(String.format("%.2f", (double) d.outboundRatePerHour));
        runwayCountVal.setText(String.valueOf(d.runwayCount));
        maxWaitVal.setText(String.format("%.2f", (double) d.maxWaitMinutes));
        
        // Runway Mode/Status Lists
        StringBuilder modes = new StringBuilder();
        StringBuilder status = new StringBuilder();
        // Build multi-line strings for runway modes and statuses
        for (int i = 0; i < d.runways.size(); i++) {
            var r = d.runways.get(i);
            modes.append("Runway ").append(r.id).append(": ").append(r.mode).append("\n"); 
            status.append("Runway ").append(r.id).append(": ").append(r.status).append("\n");
        }
        runwayModesVal.setText(modes.toString().trim());
        runwayStatusVal.setText(status.toString().trim());

        runwayModesVal.setStyle(SMALL_TEXT);
        runwayStatusVal.setStyle(SMALL_TEXT);

        // Take-off Queue 
        maxTakeoffQueueVal.setText(String.valueOf(d.maxTakeoffQueue));
        avgTakeoffWaitVal.setText(String.format("%.2f", d.avgTakeoffQueue));

        // Holding Pattern
        maxHoldingVal.setText(String.valueOf(d.maxHoldingQueue));
        avgHoldingVal.setText(String.format("%.2f", d.avgHoldingQueue));

        // Delays
        maxInboundDelayVal.setText(String.format("%.2f", (double) d.maxInboundDelayMin));
        avgInboundDelayVal.setText(String.format("%.2f", d.avgInboundDelayMin));

        // Cancellations/Diversions
        cancelledVal.setText(String.valueOf(d.cancelledCount));
        divertedVal.setText(String.valueOf(d.divertedCount));
    }
}