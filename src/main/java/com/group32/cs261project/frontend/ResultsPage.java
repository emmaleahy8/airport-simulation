package com.group32.cs261project.frontend;

import com.group32.cs261project.App;
import com.group32.cs261project.backend.compare.RunCompareCompletedRun;
import com.group32.cs261project.backend.compare.RunCompareHistory;
import com.group32.cs261project.frontend.compare.RunCompareSelectionData;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class ResultsPage implements Page {

    private final App app;
    private final SimulationService sim;

    private final BorderPane root = new BorderPane();

    // Main Heading
    private final Label title = new Label("End-of-Run Summary");

    // Left card dynamic labels - initially set to "-" until populated with real data onEnter()
    private final Label inboundRateVal  = new Label("-");
    private final Label outboundRateVal = new Label("-");
    private final Label runwayCountVal  = new Label("-");
    private final Label maxWaitVal      = new Label("-");

    private final Label runwayModesVal  = new Label("-");
    private final Label runwayStatusVal = new Label("-");

    // Right card dynamic labels - initially set to "-" until populated with real data onEnter()
    private final Label maxTakeoffQueueVal  = new Label("-");
    private final Label avgTakeoffWaitVal   = new Label("-");

    private final Label maxHoldingVal  = new Label("-");
    private final Label avgHoldingVal  = new Label("-");

    private final Label maxInboundDelayVal  = new Label("-");
    private final Label avgInboundDelayVal  = new Label("-");

    private final Label cancelledVal = new Label("-");
    private final Label divertedVal  = new Label("-");

    // compare-runs buttons
    private final Button comparePreviousButton = new Button("Compare with Previous Run");
    private final Button compareAnyTwoButton = new Button("Compare Any Two Runs");

    // Canvas for bar charts
    private static final int CHART_W = 860;
    private static final int CHART_H = 220;
    private final Canvas countChart = new Canvas(CHART_W, CHART_H);
    private final Canvas timeChart  = new Canvas(CHART_W, CHART_H);

    // ----- Styles -----
    private static final String PAGE_BG = "-fx-background-color: white;";
    private static final String CARD =
            "-fx-background-color: white;" +
            "-fx-border-color: #111;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 18;";

    // Chart colour palette (one per bar)
    private static final Color[] PALETTE = {
        Color.web("#4C6EF5"),
        Color.web("#F76707"),
        Color.web("#2F9E44"),
        Color.web("#E03131")
    };

    // ----- Constructor: Builds UI Layout -----
    public ResultsPage(App app, SimulationService sim) {
        this.app = app;
        this.sim = sim;

        root.setStyle(PAGE_BG);
        root.setPadding(new Insets(18));

        // ----- Title at Top -----
        title.setFont(Font.font(28));
        BorderPane.setMargin(title, new Insets(0, 0, 14, 0));
        root.setTop(title);

        // ----- Scrollable centre content -----
        VBox content = new VBox(22);

        // Row 1: stats cards (left + right)
        HBox statsRow = new HBox(22);
        VBox left = buildGeneralInfoCard();
        VBox right = buildRightCards();
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setPrefWidth(520);
        right.setPrefWidth(420);
        statsRow.getChildren().addAll(left, right);

        // Row 2: Count chart
        VBox countCard = buildChartCard("Count Metrics", countChart);

        // Row 3: Time chart
        VBox timeCard = buildChartCard("Time Metrics (minutes)", timeChart);

        content.getChildren().addAll(statsRow, countCard, timeCard);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white; -fx-background: white;");
        root.setCenter(scroll);

        // ----- Bottom Buttons -----
        Button back = new Button("Back to Configure");
        back.setOnAction(e -> app.switchTo(AppState.CONFIGURE, null));
        back.setStyle("-fx-background-color: #111; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 10 14;");

        comparePreviousButton.setStyle("-fx-background-color: #111; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 10 14;");
        comparePreviousButton.setVisible(false);
        comparePreviousButton.setManaged(false);
        comparePreviousButton.setOnAction(e -> openCompareWithPreviousRun());

        compareAnyTwoButton.setStyle("-fx-background-color: #111; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 10 14;");
        compareAnyTwoButton.setVisible(false);
        compareAnyTwoButton.setManaged(false);
        compareAnyTwoButton.setOnAction(e -> app.switchTo(AppState.COMPARE_HISTORY, null));

        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.getChildren().addAll(back, comparePreviousButton, compareAnyTwoButton);

        BorderPane.setMargin(bottomBar, new Insets(14, 0, 0, 0));
        root.setBottom(bottomBar);
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
        updateCompareButtonState();
    }

    // ----- UI Builder: Chart wrapper card -----
    private VBox buildChartCard(String heading, Canvas canvas) {
        VBox card = new VBox(10);
        card.setStyle(CARD);

        Label h = new Label(heading);
        h.setFont(Font.font(null, FontWeight.BOLD, 16));

        card.getChildren().addAll(h, canvas);
        return card;
    }

    // ----- UI Builder: Left Card (General Information) -----
    private VBox buildGeneralInfoCard() {
        VBox card = new VBox(14);
        card.setStyle(CARD);

        Label header = new Label("General Information");
        header.setFont(Font.font(18));
        header.setStyle("-fx-font-weight: 800;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        addRow(grid, 0, "Inbound flow rate:", inboundRateVal);
        addRow(grid, 1, "Outbound flow rate:", outboundRateVal);
        addRow(grid, 2, "Number of runways:", runwayCountVal);
        addRow(grid, 3, "Maximum wait time:", maxWaitVal);

        Separator sep = new Separator();
        sep.setPadding(new Insets(8, 0, 8, 0));

        VBox runwayInfo = new VBox(10);

        Label modesHeader = new Label("Runway operating modes:");
        modesHeader.setStyle("-fx-font-weight: 800;");
        runwayModesVal.setWrapText(true);

        Label statusHeader = new Label("Runway operational statuses:");
        statusHeader.setStyle("-fx-font-weight: 800;");
        runwayStatusVal.setWrapText(true);

        runwayInfo.getChildren().addAll(
                modesHeader, runwayModesVal,
                statusHeader, runwayStatusVal
        );
        VBox.setVgrow(runwayInfo, Priority.ALWAYS);

        card.getChildren().addAll(header, grid, sep, runwayInfo);
        VBox.setVgrow(runwayInfo, Priority.ALWAYS);
        return card;
    }

    // ----- UI Builder: Right Column -----
    private VBox buildRightCards() {
        VBox right = new VBox(16);

        right.getChildren().addAll(
                smallCard("Take-off Queue",
                        "Maximum number of planes in take-off queue:", maxTakeoffQueueVal,
                        "Average wait time in take-off queue:", avgTakeoffWaitVal),
                smallCard("Holding Pattern",
                        "Maximum number of planes in holding pattern:", maxHoldingVal,
                        "Average hold time for inbound aircraft:", avgHoldingVal),
                smallCard("Delays",
                        "Maximum inbound delay:", maxInboundDelayVal,
                        "Average inbound delay:", avgInboundDelayVal),
                smallCard("Cancellations and Diversions",
                        "Total planes cancelled:", cancelledVal,
                        "Inbound aircraft diverted due to low fuel:", divertedVal)
        );
        return right;
    }

    // ----- UI Builder: small reusable cards for right column -----
    private VBox smallCard(String titleText,
                           String label1, Label value1,
                           String label2, Label value2) {
        VBox card = new VBox(12);
        card.setStyle(CARD);

        Label header = new Label(titleText);
        header.setFont(Font.font(18));
        header.setStyle("-fx-font-weight: 800;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        addRow(grid, 0, label1, value1);
        addRow(grid, 1, label2, value2);

        card.getChildren().addAll(header, grid);
        return card;
    }

    // ----- Helper Method for Adding Row to Grid -----
    private void addRow(GridPane grid, int row, String labelText, Label valueLabel) {
        Label l = new Label(labelText);

        valueLabel.setStyle("-fx-font-weight: 800;");
        valueLabel.setAlignment(Pos.CENTER_RIGHT);
        valueLabel.setMaxWidth(Double.MAX_VALUE);

        grid.add(l, 0, row);
        grid.add(valueLabel, 1, row);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setHgrow(Priority.ALWAYS);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(140);
        c1.setHgrow(Priority.NEVER);

        grid.getColumnConstraints().setAll(c0, c1);
    }

    private void updateCompareButtonState() {
        RunCompareHistory history = sim.getRunCompareHistory();
        boolean hasAtLeastTwoRuns = history != null && history.hasAtLeast(2);

        comparePreviousButton.setVisible(hasAtLeastTwoRuns);
        comparePreviousButton.setManaged(hasAtLeastTwoRuns);

        compareAnyTwoButton.setVisible(hasAtLeastTwoRuns);
        compareAnyTwoButton.setManaged(hasAtLeastTwoRuns);
    }

    private void openCompareWithPreviousRun() {
        RunCompareHistory history = sim.getRunCompareHistory();
        if (history == null || !history.hasAtLeast(2)) {
            return;
        }

        RunCompareCompletedRun latestRun = history.getLatestRun();
        RunCompareCompletedRun previousRun = history.getPreviousRun();

        if (latestRun == null || previousRun == null) {
            return;
        }

        RunCompareSelectionData selectionData =
                new RunCompareSelectionData(previousRun, latestRun);

        app.switchTo(AppState.COMPARE, selectionData);
    }

    // ----- Results Data -----
    private void fillFrom(SimulationData d) {
        d.ensureRunwayListSize();

        // General info
        inboundRateVal.setText(String.format("%.2f aircraft/hour", (double) d.inboundRatePerHour));
        outboundRateVal.setText(String.format("%.2f aircraft/hour", (double) d.outboundRatePerHour));
        runwayCountVal.setText(String.valueOf(d.runwayCount));
        maxWaitVal.setText(String.format("%.2f minutes", (double) d.maxWaitMinutes));

        // Runway Mode/Status Lists
        StringBuilder modes = new StringBuilder();
        StringBuilder status = new StringBuilder();
        for (int i = 0; i < d.runways.size(); i++) {
            var r = d.runways.get(i);
            modes.append("Runway ").append(r.id).append(": ").append(r.mode).append("\n");
            status.append("Runway ").append(r.id).append(": ").append(r.status).append("\n");
        }
        runwayModesVal.setText(modes.toString().trim());
        runwayStatusVal.setText(status.toString().trim());

        // Take-off Queue
        maxTakeoffQueueVal.setText(String.valueOf(d.maxTakeoffQueue));
        avgTakeoffWaitVal.setText(String.format("%.2f minutes", d.avgTakeoffQueue));

        // Holding Pattern
        maxHoldingVal.setText(String.valueOf(d.maxHoldingQueue));
        avgHoldingVal.setText(String.format("%.2f minutes", d.avgHoldingQueue));

        // Delays
        maxInboundDelayVal.setText(String.format("%.2f minutes", (double) d.maxInboundDelayMin));
        avgInboundDelayVal.setText(String.format("%.2f minutes", d.avgInboundDelayMin));

        // Cancellations/Diversions
        cancelledVal.setText(String.valueOf(d.cancelledCount));
        divertedVal.setText(String.valueOf(d.divertedCount));

        // ----- Draw bar charts -----
        drawCountChart(d);
        drawTimeChart(d);
    }

    // ============================================================
    //  BAR CHART 1 – Count Metrics
    // ============================================================
    private void drawCountChart(SimulationData d) {
        String[] labels = {
            "Max Takeoff\nQueue",
            "Avg Takeoff\nQueue",
            "Max Holding\nQueue",
            "Cancelled",
            "Diverted"
        };
        double[] values = {
            d.maxTakeoffQueue,
            d.avgTakeoffQueue,
            d.maxHoldingQueue,
            d.cancelledCount,
            d.divertedCount
        };
        drawBarChart(countChart, labels, values, "Number of Planes");
    }

    // ============================================================
    //  BAR CHART 2 – Time Metrics (minutes)
    // ============================================================
    private void drawTimeChart(SimulationData d) {
        String[] labels = {
            "Max Wait",
            "Avg Takeoff\nWait",
            "Avg Hold\nTime",
            "Max Inbound\nDelay",
            "Avg Inbound\nDelay"
        };
        double[] values = {
            d.maxWaitMinutes,
            d.avgTakeoffQueue,
            d.avgHoldingQueue,
            d.maxInboundDelayMin,
            d.avgInboundDelayMin
        };
        drawBarChart(timeChart, labels, values, "Minutes");
    }

    // ============================================================
    //  Generic bar-chart renderer using JavaFX Canvas
    // ============================================================
    private void drawBarChart(Canvas canvas, String[] labels, double[] values, String yAxisLabel) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double W = canvas.getWidth();
        double H = canvas.getHeight();

        final double PAD_LEFT = 58;
        final double PAD_RIGHT = 20;
        final double PAD_TOP = 16;
        final double PAD_BOTTOM = 54;

        double chartW = W - PAD_LEFT - PAD_RIGHT;
        double chartH = H - PAD_TOP - PAD_BOTTOM;

        gc.clearRect(0, 0, W, H);
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, W, H);

        double maxVal = 0;
        for (double v : values) {
            if (v > maxVal) {
                maxVal = v;
            }
        }
        if (maxVal == 0) {
            maxVal = 1;
        }

        double niceMax = niceNumber(maxVal * 1.10, true);
        int tickCount = 5;
        double tickStep = niceNumber(niceMax / tickCount, false);
        niceMax = tickStep * tickCount;

        gc.setStroke(Color.web("#333333"));
        gc.setLineWidth(1.5);
        gc.strokeLine(PAD_LEFT, PAD_TOP, PAD_LEFT, PAD_TOP + chartH);
        gc.strokeLine(PAD_LEFT, PAD_TOP + chartH, PAD_LEFT + chartW, PAD_TOP + chartH);

        gc.setFont(Font.font(11));
        gc.setFill(Color.web("#555555"));
        for (int t = 0; t <= tickCount; t++) {
            double tickVal = t * tickStep;
            double y = PAD_TOP + chartH - (tickVal / niceMax) * chartH;

            gc.setStroke(Color.web("#e0e0e0"));
            gc.setLineWidth(0.8);
            gc.strokeLine(PAD_LEFT + 1, y, PAD_LEFT + chartW, y);

            String lbl = tickVal == (long) tickVal
                    ? String.valueOf((long) tickVal)
                    : String.format("%.1f", tickVal);

            gc.setFill(Color.web("#555555"));
            gc.fillText(lbl, PAD_LEFT - measureText(lbl, 11) - 4, y + 4);
        }

        gc.setFont(Font.font(11));
        gc.setFill(Color.web("#888888"));
        gc.fillText("(" + yAxisLabel + ")", 2, PAD_TOP + chartH / 2);

        int n = labels.length;
        double groupW = chartW / n;
        double barW = groupW * 0.55;
        double barGap = (groupW - barW) / 2.0;

        for (int i = 0; i < n; i++) {
            double barH = (values[i] / niceMax) * chartH;
            double barX = PAD_LEFT + i * groupW + barGap;
            double barY = PAD_TOP + chartH - barH;

            Color base = PALETTE[i % PALETTE.length];

            gc.setFill(base);
            gc.fillRoundRect(barX, barY, barW, barH, 4, 4);

            gc.setStroke(base.darker());
            gc.setLineWidth(1);
            gc.strokeRoundRect(barX, barY, barW, barH, 4, 4);

            String valStr = values[i] == (long) values[i]
                    ? String.valueOf((long) values[i])
                    : String.format("%.1f", values[i]);

            gc.setFont(Font.font(null, FontWeight.BOLD, 11));
            gc.setFill(Color.web("#222222"));
            double valX = barX + barW / 2 - measureText(valStr, 11) / 2;
            gc.fillText(valStr, valX, Math.max(PAD_TOP + 12, barY - 4));

            gc.setFont(Font.font(11));
            gc.setFill(Color.web("#333333"));
            String[] lines = labels[i].split("\n");
            double labelCentreX = PAD_LEFT + i * groupW + groupW / 2;
            double lineY = PAD_TOP + chartH + 14;
            for (String line : lines) {
                double lineX = labelCentreX - measureText(line, 11) / 2;
                gc.fillText(line, lineX, lineY);
                lineY += 14;
            }
        }
    }

    /** Returns a "nice" round number >= val when round=true, otherwise the nearest step. */
    private double niceNumber(double val, boolean round) {
        if (val <= 0) {
            return 1;
        }
        double exp = Math.floor(Math.log10(val));
        double frac = val / Math.pow(10, exp);
        double nice;
        if (round) {
            if (frac < 1.5) {
                nice = 1;
            } else if (frac < 3) {
                nice = 2;
            } else if (frac < 7) {
                nice = 5;
            } else {
                nice = 10;
            }
        } else {
            if (frac <= 1) {
                nice = 1;
            } else if (frac <= 2) {
                nice = 2;
            } else if (frac <= 5) {
                nice = 5;
            } else {
                nice = 10;
            }
        }
        return nice * Math.pow(10, exp);
    }

    /** Approximate text width for a given font size (JavaFX Text for measurement). */
    private double measureText(String s, double fontSize) {
        Text t = new Text(s);
        t.setFont(Font.font(fontSize));
        return t.getLayoutBounds().getWidth();
    }
}