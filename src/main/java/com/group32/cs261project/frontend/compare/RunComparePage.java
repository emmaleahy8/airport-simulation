package com.group32.cs261project.frontend.compare;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.group32.cs261project.App;
import com.group32.cs261project.backend.compare.RunCompareCompletedRun;
import com.group32.cs261project.backend.compare.RunCompareResult;
import com.group32.cs261project.backend.compare.RunCompareService;
import com.group32.cs261project.backend.metrics.SimulationReport;
import com.group32.cs261project.backend.sim.SimConfig;
import com.group32.cs261project.frontend.AppState;
import com.group32.cs261project.frontend.Page;

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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class RunComparePage implements Page {

    // ----- Shared page styling -----
    // Matches the updated dark dashboard style used by the newer frontend pages.
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

    // ----- Date/time formatter used in the run summary -----
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    // ----- Chart constants -----
    private static final int CHART_W = 860;
    private static final int CHART_H = 230;

    // Fixed short legend names to avoid the overlap caused by long run IDs.
    private static final String LEFT_LEGEND = "Left Run";
    private static final String RIGHT_LEGEND = "Right Run";

    private static final Color LEFT_RUN_COLOR = Color.web("#4C6EF5");
    private static final Color RIGHT_RUN_COLOR = Color.web("#F76707");
    private static final Color CHART_GRID = Color.web("#355063");
    private static final Color CHART_AXIS = Color.web("#6f8796");
    private static final Color CHART_TEXT = Color.web("#dcebf5");
    private static final Color CHART_SUBTEXT = Color.web("#b8cad6");

    private final App app;
    private final RunCompareService compareService = new RunCompareService();

    private final BorderPane root = new BorderPane();
    private final VBox content = new VBox(22);

    public RunComparePage(App app) {
        this.app = app;

        root.setStyle(PAGE_BG);
        root.setPadding(new Insets(18));

        // ----- Header -----
        // Uses a title + subtitle layout consistent with the updated frontend style.
        VBox header = new VBox(4);

        Label title = new Label("Compare Simulation Runs");
        title.setFont(Font.font(30));
        title.setStyle(TITLE_TEXT);

        Label subtitle = new Label("Review key differences between two completed simulation runs.");
        subtitle.setStyle(SUBTITLE_TEXT);

        header.getChildren().addAll(title, subtitle);
        header.setPadding(new Insets(0, 0, 12, 0));
        root.setTop(header);

        // ----- Main scrollable page content -----
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        root.setCenter(scroll);

        // ----- Bottom action row -----
        Button back = new Button("Back to Configure");
        back.setStyle(BTN_PRIMARY);
        back.setOnAction(e -> app.switchTo(AppState.CONFIGURE, null));

        HBox bottomBar = new HBox(back);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        BorderPane.setMargin(bottomBar, new Insets(14, 0, 0, 0));
        root.setBottom(bottomBar);
    }

    @Override
    public Parent getView() {
        return root;
    }

    @Override
    public void onEnter(Object data) {
        // The page only works when navigation passes exactly two selected runs.
        if (!(data instanceof RunCompareSelectionData selectionData)
                || selectionData.leftRun() == null
                || selectionData.rightRun() == null) {
            showNoComparisonData();
            return;
        }

        RunCompareResult result = compareService.compare(selectionData.leftRun(), selectionData.rightRun());
        fillFrom(result);
    }

    @Override
    public void onExit() {
        // no-op
    }

    private void fillFrom(RunCompareResult result) {
        content.getChildren().clear();

        RunCompareCompletedRun leftRun = result.leftRun();
        RunCompareCompletedRun rightRun = result.rightRun();

        SimConfig leftConfig = leftRun.config();
        SimConfig rightConfig = rightRun.config();
        SimulationReport leftReport = leftRun.report();
        SimulationReport rightReport = rightRun.report();

        // Top section mirrors the ResultsPage layout:
        // left side = general info
        // right side = metric summary cards
        HBox statsRow = new HBox(22);
        VBox left = buildGeneralInfoCard(leftRun, rightRun, leftConfig, rightConfig, leftReport, rightReport);
        VBox right = buildRightCards(leftReport, rightReport);

        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setPrefWidth(520);
        right.setPrefWidth(420);
        statsRow.getChildren().addAll(left, right);

        content.getChildren().addAll(
                statsRow,
                buildCountChartCard(leftReport, rightReport),
                buildTimeChartCard(leftReport, rightReport),
                buildAdditionalConfigCard(leftConfig, rightConfig),
                buildConfigDifferencesCard(result.configDifferences()),
                buildRawMetricDeltasCard(result.metricDeltas())
        );
    }

    // ----- General summary card -----
    // Shows run identity, completion time, and headline configuration values.
    private VBox buildGeneralInfoCard(RunCompareCompletedRun leftRun,
                                      RunCompareCompletedRun rightRun,
                                      SimConfig leftConfig,
                                      SimConfig rightConfig,
                                      SimulationReport leftReport,
                                      SimulationReport rightReport) {
        VBox card = new VBox(14);
        card.setStyle(CARD);

        Label header = new Label("General Information");
        header.setFont(Font.font(20));
        header.setStyle(MONITOR_LABEL);

        GridPane grid = createFourColumnGrid();
        addComparisonHeaderRow(grid, LEFT_LEGEND, RIGHT_LEGEND);

        addComparisonRow(grid, 1, "Run ID:",
                leftRun.runId(),
                rightRun.runId(),
                "-");

        addComparisonRow(grid, 2, "Completed at:",
                TIME_FMT.format(leftRun.completedAt()),
                TIME_FMT.format(rightRun.completedAt()),
                "-");

        addComparisonRow(grid, 3, "Inbound flow rate:",
                formatPerHour(leftConfig.inboundRatePerHour()),
                formatPerHour(rightConfig.inboundRatePerHour()),
                formatDelta(rightConfig.inboundRatePerHour() - leftConfig.inboundRatePerHour(), " aircraft/hour"));

        addComparisonRow(grid, 4, "Outbound flow rate:",
                formatPerHour(leftConfig.outboundRatePerHour()),
                formatPerHour(rightConfig.outboundRatePerHour()),
                formatDelta(rightConfig.outboundRatePerHour() - leftConfig.outboundRatePerHour(), " aircraft/hour"));

        addComparisonRow(grid, 5, "Number of runways:",
                formatWhole(safeInt(leftReport.numRunways())),
                formatWhole(safeInt(rightReport.numRunways())),
                formatDelta(safeInt(rightReport.numRunways()) - safeInt(leftReport.numRunways()), ""));

        addComparisonRow(grid, 6, "Maximum wait time:",
                formatWhole(leftConfig.maxTakeoffWaitMinutes()) + " minutes",
                formatWhole(rightConfig.maxTakeoffWaitMinutes()) + " minutes",
                formatDelta(rightConfig.maxTakeoffWaitMinutes() - leftConfig.maxTakeoffWaitMinutes(), " minutes"));

        card.getChildren().addAll(header, grid);
        return card;
    }

    // ----- Right-side metric cards -----
    // These mirror the content buckets from ResultsPage so the compare page stays familiar.
    private VBox buildRightCards(SimulationReport leftReport, SimulationReport rightReport) {
        VBox right = new VBox(16);

        right.getChildren().addAll(
                buildMetricCard("Take-off Queue",
                        "Maximum number of planes in take-off queue:",
                        formatWhole(safeInt(leftReport.maxTakeoffQueueSize())),
                        formatWhole(safeInt(rightReport.maxTakeoffQueueSize())),
                        formatDelta(safeInt(rightReport.maxTakeoffQueueSize()) - safeInt(leftReport.maxTakeoffQueueSize()), ""),
                        "Average wait time in take-off queue:",
                        formatMinutes(safeDouble(leftReport.avgTakeoffWaitMinutes())),
                        formatMinutes(safeDouble(rightReport.avgTakeoffWaitMinutes())),
                        formatDelta(safeDouble(rightReport.avgTakeoffWaitMinutes()) - safeDouble(leftReport.avgTakeoffWaitMinutes()), " minutes")),

                buildMetricCard("Holding Pattern",
                        "Maximum number of planes in holding pattern:",
                        formatWhole(safeInt(leftReport.maxHoldingSize())),
                        formatWhole(safeInt(rightReport.maxHoldingSize())),
                        formatDelta(safeInt(rightReport.maxHoldingSize()) - safeInt(leftReport.maxHoldingSize()), ""),
                        "Average hold time for inbound aircraft:",
                        formatMinutes(safeDouble(leftReport.avgHoldingTimeMinutes())),
                        formatMinutes(safeDouble(rightReport.avgHoldingTimeMinutes())),
                        formatDelta(safeDouble(rightReport.avgHoldingTimeMinutes()) - safeDouble(leftReport.avgHoldingTimeMinutes()), " minutes")),

                buildMetricCard("Delays",
                        "Maximum inbound delay:",
                        formatMinutes(safeLong(leftReport.maxArrivalDelayMinutes())),
                        formatMinutes(safeLong(rightReport.maxArrivalDelayMinutes())),
                        formatDelta(safeLong(rightReport.maxArrivalDelayMinutes()) - safeLong(leftReport.maxArrivalDelayMinutes()), " minutes"),
                        "Average inbound delay:",
                        formatMinutes(safeDouble(leftReport.avgArrivalDelayMinutes())),
                        formatMinutes(safeDouble(rightReport.avgArrivalDelayMinutes())),
                        formatDelta(safeDouble(rightReport.avgArrivalDelayMinutes()) - safeDouble(leftReport.avgArrivalDelayMinutes()), " minutes")),

                buildMetricCard("Cancellations and Diversions",
                        "Total planes cancelled:",
                        formatWhole(safeInt(leftReport.numCancellations())),
                        formatWhole(safeInt(rightReport.numCancellations())),
                        formatDelta(safeInt(rightReport.numCancellations()) - safeInt(leftReport.numCancellations()), ""),
                        "Inbound aircraft diverted due to low fuel:",
                        formatWhole(safeInt(leftReport.numDiverted())),
                        formatWhole(safeInt(rightReport.numDiverted())),
                        formatDelta(safeInt(rightReport.numDiverted()) - safeInt(leftReport.numDiverted()), ""))
        );

        return right;
    }

    private VBox buildMetricCard(String titleText,
                                 String label1, String left1, String right1, String delta1,
                                 String label2, String left2, String right2, String delta2) {
        VBox card = new VBox(12);
        card.setStyle(CARD);

        Label header = new Label(titleText);
        header.setFont(Font.font(20));
        header.setStyle(MONITOR_LABEL);

        GridPane grid = createFourColumnGrid();
        addComparisonHeaderRow(grid, LEFT_LEGEND, RIGHT_LEGEND);
        addComparisonRow(grid, 1, label1, left1, right1, delta1);
        addComparisonRow(grid, 2, label2, left2, right2, delta2);

        card.getChildren().addAll(header, grid);
        return card;
    }

    // ----- Count comparison chart -----
    // Visual comparison for queue sizes and event totals.
    private VBox buildCountChartCard(SimulationReport leftReport, SimulationReport rightReport) {
        Canvas canvas = new Canvas(CHART_W, CHART_H);

        String[] labels = {
                "Max Takeoff\nQueue",
                "Max Holding\nQueue",
                "Cancelled",
                "Diverted"
        };

        double[] leftValues = {
                safeInt(leftReport.maxTakeoffQueueSize()),
                safeInt(leftReport.maxHoldingSize()),
                safeInt(leftReport.numCancellations()),
                safeInt(leftReport.numDiverted())
        };

        double[] rightValues = {
                safeInt(rightReport.maxTakeoffQueueSize()),
                safeInt(rightReport.maxHoldingSize()),
                safeInt(rightReport.numCancellations()),
                safeInt(rightReport.numDiverted())
        };

        drawGroupedBarChart(canvas, labels, leftValues, rightValues, "Number of Planes");

        return buildChartCard("Count Metrics Comparison", canvas);
    }

    // ----- Time comparison chart -----
    // Visual comparison for wait/hold/delay metrics in minutes.
    private VBox buildTimeChartCard(SimulationReport leftReport, SimulationReport rightReport) {
        Canvas canvas = new Canvas(CHART_W, CHART_H);

        String[] labels = {
                "Avg Takeoff\nWait",
                "Avg Hold\nTime",
                "Max Inbound\nDelay",
                "Avg Inbound\nDelay"
        };

        double[] leftValues = {
                safeDouble(leftReport.avgTakeoffWaitMinutes()),
                safeDouble(leftReport.avgHoldingTimeMinutes()),
                safeLong(leftReport.maxArrivalDelayMinutes()),
                safeDouble(leftReport.avgArrivalDelayMinutes())
        };

        double[] rightValues = {
                safeDouble(rightReport.avgTakeoffWaitMinutes()),
                safeDouble(rightReport.avgHoldingTimeMinutes()),
                safeLong(rightReport.maxArrivalDelayMinutes()),
                safeDouble(rightReport.avgArrivalDelayMinutes())
        };

        drawGroupedBarChart(canvas, labels, leftValues, rightValues, "Minutes");

        return buildChartCard("Time Metrics Comparison (minutes)", canvas);
    }

    private VBox buildChartCard(String heading, Canvas canvas) {
        VBox card = new VBox(10);
        card.setStyle(CARD);

        Label header = new Label(heading);
        header.setFont(Font.font(20));
        header.setStyle(MONITOR_LABEL);

        card.getChildren().addAll(header, canvas);
        return card;
    }

    // ----- Chart renderer -----
    // Draws grouped bars so each metric shows one bar for the left run and one for the right run.
    // This reuses the same Canvas-based approach as the ResultsPage charts.
    private void drawGroupedBarChart(Canvas canvas,
                                     String[] labels,
                                     double[] leftValues,
                                     double[] rightValues,
                                     String yAxisLabel) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double W = canvas.getWidth();
        double H = canvas.getHeight();

        // Extra top padding is intentional so the legend has room and does not overlap the chart.
        final double PAD_LEFT = 58;
        final double PAD_RIGHT = 20;
        final double PAD_TOP = 46;
        final double PAD_BOTTOM = 56;

        double chartW = W - PAD_LEFT - PAD_RIGHT;
        double chartH = H - PAD_TOP - PAD_BOTTOM;

        gc.clearRect(0, 0, W, H);
        gc.setFill(Color.TRANSPARENT);
        gc.fillRect(0, 0, W, H);

        drawLegend(gc);

        double maxVal = 0;
        for (double v : leftValues) {
            if (v > maxVal) {
                maxVal = v;
            }
        }
        for (double v : rightValues) {
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

        // Axes
        gc.setStroke(CHART_AXIS);
        gc.setLineWidth(1.5);
        gc.strokeLine(PAD_LEFT, PAD_TOP, PAD_LEFT, PAD_TOP + chartH);
        gc.strokeLine(PAD_LEFT, PAD_TOP + chartH, PAD_LEFT + chartW, PAD_TOP + chartH);

        // Grid lines and y-axis tick labels
        gc.setFont(Font.font(11));
        gc.setFill(CHART_SUBTEXT);
        for (int t = 0; t <= tickCount; t++) {
            double tickVal = t * tickStep;
            double y = PAD_TOP + chartH - (tickVal / niceMax) * chartH;

            gc.setStroke(CHART_GRID);
            gc.setLineWidth(0.8);
            gc.strokeLine(PAD_LEFT + 1, y, PAD_LEFT + chartW, y);

            String lbl = tickVal == (long) tickVal
                    ? String.valueOf((long) tickVal)
                    : String.format("%.1f", tickVal);

            gc.setFill(CHART_SUBTEXT);
            gc.fillText(lbl, PAD_LEFT - measureText(lbl, 11) - 4, y + 4);
        }

        // Y-axis unit label
        gc.setFont(Font.font(11));
        gc.setFill(CHART_SUBTEXT);
        gc.fillText("(" + yAxisLabel + ")", 2, PAD_TOP + chartH / 2);

        int n = labels.length;
        double groupW = chartW / n;
        double innerGroupW = groupW * 0.72;
        double pairGap = groupW * 0.08;
        double barW = (innerGroupW - pairGap) / 2.0;
        double startOffset = (groupW - innerGroupW) / 2.0;

        for (int i = 0; i < n; i++) {
            double leftBarH = (leftValues[i] / niceMax) * chartH;
            double rightBarH = (rightValues[i] / niceMax) * chartH;

            double leftBarX = PAD_LEFT + i * groupW + startOffset;
            double rightBarX = leftBarX + barW + pairGap;

            double leftBarY = PAD_TOP + chartH - leftBarH;
            double rightBarY = PAD_TOP + chartH - rightBarH;

            // Left run bar
            gc.setFill(LEFT_RUN_COLOR);
            gc.fillRoundRect(leftBarX, leftBarY, barW, leftBarH, 4, 4);
            gc.setStroke(LEFT_RUN_COLOR.darker());
            gc.setLineWidth(1);
            gc.strokeRoundRect(leftBarX, leftBarY, barW, leftBarH, 4, 4);

            // Right run bar
            gc.setFill(RIGHT_RUN_COLOR);
            gc.fillRoundRect(rightBarX, rightBarY, barW, rightBarH, 4, 4);
            gc.setStroke(RIGHT_RUN_COLOR.darker());
            gc.setLineWidth(1);
            gc.strokeRoundRect(rightBarX, rightBarY, barW, rightBarH, 4, 4);

            // Value labels above bars
            String leftValStr = formatChartValue(leftValues[i]);
            String rightValStr = formatChartValue(rightValues[i]);

            gc.setFont(Font.font(null, FontWeight.BOLD, 11));
            gc.setFill(CHART_TEXT);
            gc.fillText(
                    leftValStr,
                    leftBarX + barW / 2 - measureText(leftValStr, 11) / 2,
                    Math.max(PAD_TOP + 12, leftBarY - 4)
            );
            gc.fillText(
                    rightValStr,
                    rightBarX + barW / 2 - measureText(rightValStr, 11) / 2,
                    Math.max(PAD_TOP + 12, rightBarY - 4)
            );

            // X-axis labels
            gc.setFont(Font.font(11));
            gc.setFill(CHART_TEXT);
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

    // Fixed-position legend avoids overlap with long generated run IDs.
    private void drawLegend(GraphicsContext gc) {
        double y = 18;
        double x = 88;

        gc.setFill(LEFT_RUN_COLOR);
        gc.fillRoundRect(x, y - 9, 12, 12, 3, 3);
        gc.setFill(CHART_TEXT);
        gc.setFont(Font.font(11));
        gc.fillText(LEFT_LEGEND, x + 18, y);

        double x2 = x + 120;
        gc.setFill(RIGHT_RUN_COLOR);
        gc.fillRoundRect(x2, y - 9, 12, 12, 3, 3);
        gc.setFill(CHART_TEXT);
        gc.fillText(RIGHT_LEGEND, x2 + 18, y);
    }

    // ----- Additional config card -----
    // This compares config fields that help explain why two runs behaved differently.
    private VBox buildAdditionalConfigCard(SimConfig leftConfig, SimConfig rightConfig) {
        VBox card = new VBox(14);
        card.setStyle(CARD);

        Label header = new Label("Additional Configuration");
        header.setFont(Font.font(20));
        header.setStyle(MONITOR_LABEL);

        GridPane grid = createFourColumnGrid();
        addComparisonHeaderRow(grid, LEFT_LEGEND, RIGHT_LEGEND);

        addComparisonRow(grid, 1, "Low-fuel threshold:",
                formatWhole(leftConfig.lowFuelMinutes()) + " minutes",
                formatWhole(rightConfig.lowFuelMinutes()) + " minutes",
                formatDelta(rightConfig.lowFuelMinutes() - leftConfig.lowFuelMinutes(), " minutes"));

        addComparisonRow(grid, 2, "Fuel diversion threshold:",
                formatWhole(leftConfig.fuelDiversionMinutes()) + " minutes",
                formatWhole(rightConfig.fuelDiversionMinutes()) + " minutes",
                formatDelta(rightConfig.fuelDiversionMinutes() - leftConfig.fuelDiversionMinutes(), " minutes"));

        addComparisonRow(grid, 3, "Runway occupancy time:",
                formatWhole(leftConfig.runwayOccupancySeconds()) + " seconds",
                formatWhole(rightConfig.runwayOccupancySeconds()) + " seconds",
                formatDelta(rightConfig.runwayOccupancySeconds() - leftConfig.runwayOccupancySeconds(), " seconds"));

        addComparisonRow(grid, 4, "Mechanical emergency probability:",
                formatProbability(leftConfig.mechanicalEmergencyProbability()),
                formatProbability(rightConfig.mechanicalEmergencyProbability()),
                formatDelta(rightConfig.mechanicalEmergencyProbability() - leftConfig.mechanicalEmergencyProbability(), ""));

        addComparisonRow(grid, 5, "Passenger health emergency probability:",
                formatProbability(leftConfig.passengerHealthEmergencyProbability()),
                formatProbability(rightConfig.passengerHealthEmergencyProbability()),
                formatDelta(rightConfig.passengerHealthEmergencyProbability() - leftConfig.passengerHealthEmergencyProbability(), ""));

        addComparisonRow(grid, 6, "Random seed:",
                String.valueOf(leftConfig.randomSeed()),
                String.valueOf(rightConfig.randomSeed()),
                leftConfig.randomSeed() == rightConfig.randomSeed() ? "0" : "Different");

        card.getChildren().addAll(header, grid);
        return card;
    }

    // ----- Config differences card -----
    // Displays the compare-service summary of changed config fields in a readable list.
    private VBox buildConfigDifferencesCard(Map<String, String> configDifferences) {
        VBox card = new VBox(14);
        card.setStyle(CARD);

        Label header = new Label("Configuration Differences");
        header.setFont(Font.font(20));
        header.setStyle(MONITOR_LABEL);

        VBox body = new VBox(10);

        if (configDifferences == null || configDifferences.isEmpty()) {
            Label none = new Label("No configuration differences.");
            none.setStyle(SMALL_TEXT);
            body.getChildren().add(none);
        } else {
            for (Map.Entry<String, String> entry : configDifferences.entrySet()) {
                VBox row = new VBox(4);

                Label key = new Label(entry.getKey());
                key.setStyle(MONITOR_LABEL);

                Label value = new Label(entry.getValue());
                value.setWrapText(true);
                value.setStyle(SMALL_TEXT);

                Separator sep = new Separator();
                sep.setOpacity(0.35);

                row.getChildren().addAll(key, value, sep);
                body.getChildren().add(row);
            }
        }

        Separator topSep = new Separator();
        topSep.setOpacity(0.35);

        card.getChildren().addAll(header, topSep, body);
        return card;
    }

    // ----- Raw metric deltas card -----
    // Keeps the lower-level compare-service metric list for users who want the full delta summary.
    private VBox buildRawMetricDeltasCard(Map<String, Double> metricDeltas) {
        VBox card = new VBox(14);
        card.setStyle(CARD);

        Label header = new Label("Metric Deltas (Right - Left)");
        header.setFont(Font.font(20));
        header.setStyle(MONITOR_LABEL);

        VBox body = new VBox(10);

        if (metricDeltas == null || metricDeltas.isEmpty()) {
            Label none = new Label("No metric deltas.");
            none.setStyle(SMALL_TEXT);
            body.getChildren().add(none);
        } else {
            GridPane grid = new GridPane();
            grid.setHgap(16);
            grid.setVgap(10);

            ColumnConstraints c0 = new ColumnConstraints();
            c0.setHgrow(Priority.ALWAYS);

            ColumnConstraints c1 = new ColumnConstraints();
            c1.setMinWidth(160);
            c1.setHgrow(Priority.NEVER);

            grid.getColumnConstraints().setAll(c0, c1);

            int row = 0;
            for (Map.Entry<String, Double> entry : metricDeltas.entrySet()) {
                Label metricName = new Label(entry.getKey());
                metricName.setStyle(LABEL_TEXT);

                Label metricDelta = new Label(formatDelta(entry.getValue(), ""));
                metricDelta.setAlignment(Pos.CENTER_RIGHT);
                metricDelta.setMaxWidth(Double.MAX_VALUE);
                metricDelta.setStyle(MONITOR_TITLE);

                grid.add(metricName, 0, row);
                grid.add(metricDelta, 1, row);
                row++;
            }

            body.getChildren().add(grid);
        }

        Separator topSep = new Separator();
        topSep.setOpacity(0.35);

        card.getChildren().addAll(header, topSep, body);
        return card;
    }

    private void showNoComparisonData() {
        content.getChildren().clear();

        VBox card = new VBox(12);
        card.setStyle(CARD);

        Label header = new Label("Compare Simulation Runs");
        header.setFont(Font.font(20));
        header.setStyle(MONITOR_LABEL);

        Label body = new Label("No comparison data was provided.");
        body.setWrapText(true);
        body.setStyle(SMALL_TEXT);

        card.getChildren().addAll(header, body);
        content.getChildren().add(card);
    }

    // Creates a consistent 4-column comparison grid:
    // metric name | left value | right value | delta
    private GridPane createFourColumnGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(250);
        c0.setHgrow(Priority.NEVER);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);

        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);

        ColumnConstraints c3 = new ColumnConstraints();
        c3.setMinWidth(140);
        c3.setHgrow(Priority.NEVER);

        grid.getColumnConstraints().setAll(c0, c1, c2, c3);
        return grid;
    }

    private void addComparisonHeaderRow(GridPane grid, String leftHeaderText, String rightHeaderText) {
        Label spacer = new Label("");
        Label leftHeader = new Label(leftHeaderText);
        Label rightHeader = new Label(rightHeaderText);
        Label deltaHeader = new Label("Delta");

        leftHeader.setStyle(MONITOR_LABEL);
        rightHeader.setStyle(MONITOR_LABEL);
        deltaHeader.setStyle(MONITOR_LABEL);

        grid.add(spacer, 0, 0);
        grid.add(leftHeader, 1, 0);
        grid.add(rightHeader, 2, 0);
        grid.add(deltaHeader, 3, 0);
    }

    // Helper for adding one comparison row to the 4-column grid.
    private void addComparisonRow(GridPane grid, int row, String labelText,
                                  String leftValueText, String rightValueText, String deltaText) {
        Label label = new Label(labelText);
        label.setStyle(LABEL_TEXT);

        Label leftValue = new Label(leftValueText);
        leftValue.setWrapText(true);
        leftValue.setStyle(SMALL_TEXT);

        Label rightValue = new Label(rightValueText);
        rightValue.setWrapText(true);
        rightValue.setStyle(SMALL_TEXT);

        Label deltaValue = new Label(deltaText);
        deltaValue.setWrapText(true);
        deltaValue.setAlignment(Pos.CENTER_RIGHT);
        deltaValue.setMaxWidth(Double.MAX_VALUE);
        deltaValue.setStyle(MONITOR_TITLE);

        grid.add(label, 0, row);
        grid.add(leftValue, 1, row);
        grid.add(rightValue, 2, row);
        grid.add(deltaValue, 3, row);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private String formatPerHour(double value) {
        return String.format("%.2f aircraft/hour", value);
    }

    private String formatMinutes(double value) {
        return String.format("%.2f minutes", value);
    }

    private String formatWhole(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0000001) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format("%.2f", value);
    }

    private String formatProbability(double value) {
        return String.format("%.4f", value);
    }

    private String formatDelta(double value, String suffix) {
        if (suffix == null) {
            suffix = "";
        }

        String sign = value > 0 ? "+" : "";

        if (Math.abs(value - Math.rint(value)) < 0.0000001) {
            return sign + (long) Math.rint(value) + suffix;
        }
        return sign + String.format("%.2f", value) + suffix;
    }

    private String formatChartValue(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0000001) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format("%.1f", value);
    }

    // Produces human-friendly tick spacing for chart axes.
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

    // Approximate text width so labels can be centered on the Canvas.
    private double measureText(String s, double fontSize) {
        Text t = new Text(s);
        t.setFont(Font.font(fontSize));
        return t.getLayoutBounds().getWidth();
    }
}