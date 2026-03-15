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
import javafx.geometry.VPos;
import javafx.scene.text.TextAlignment;
import java.time.Duration;

import com.group32.cs261project.frontend.HelpOverlay;
import com.group32.cs261project.frontend.HelpText;

/**
 * RunComparePage displays a side-by-side comparison of two completed simulation runs.
 *
 * The page presents differences between the selected runs using three main views:
 * a general information summary, metric comparison cards, and bar charts for key
 * count- and time-based outputs. It also includes an additional configuration section
 * so the user can relate differences in results back to differences in simulation inputs.
 *
 * The page expects exactly two completed runs to be passed in through
 * {@link RunCompareSelectionData} when the page is entered.
 */
public class RunComparePage implements Page {

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

    private static final String TITLE_TEXT =
            "-fx-text-fill: #eaf6ff;";

    private static final String SUBTITLE_TEXT =
            "-fx-text-fill: #cccccc;";

    private static final String LABEL_TEXT =
            "-fx-text-fill: #dcebf5;";

    private static final String SMALL_TEXT =
            "-fx-text-fill: #b8cad6;";

    private static final String MONITOR_TITLE =
            "-fx-text-fill: #63e6ff;";

    private static final String MONITOR_LABEL =
            "-fx-text-fill: #d7f8ff;";

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
    private static final int CHART_W = 750;
    private static final int CHART_H = 230;

    // Fixed short legend names
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

    /**
     * Constructs the comparison page and initialises the static layout.
     *
     * The page consists of a header, a scrollable comparison area, and a
     * bottom navigation bar allowing the user to return to the configuration page.
     *
     * @param app reference to the main application controller
     */
    public RunComparePage(App app) {
        this.app = app;

        root.setStyle(PAGE_BG);
        root.setPadding(new Insets(18));

        // ----- Header -----
        VBox header = new VBox(4);

        Label title = new Label("Compare Simulation Runs");
        title.setFont(Font.font(null, FontWeight.BOLD, 30));
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

    // Returns the root view for the comparison page.
    @Override
    public Parent getView() {
        return HelpOverlay.wrap(root, "Compare Simulation Runs", HelpText.comparisonPage());
    }

    /**
     * Populates the page when it becomes active.
     *
     * If valid comparison data is provided, the selected runs are compared
     * and the page is populated with summary cards and charts. If the required
     * data is missing, a placeholder message is shown instead.
     *
     * @param data navigation data expected to contain two selected runs
     */
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

    /**
     * Called when leaving the comparison page.
     *
     * No cleanup is required because the page content is rebuilt whenever
     * the page is entered.
     */
    @Override
    public void onExit() {
        // no-op
    }

    /**
     * Populates the page using the result of a completed run comparison.
     *
     * This method clears any previous content and rebuilds the comparison layout
     * using summary cards, chart cards, and additional configuration details for
     * the two selected runs.
     *
     * @param result the comparison result containing both runs and their metrics
     */
    private void fillFrom(RunCompareResult result) {
        content.getChildren().clear();

        // Extract both runs and their associated configuration and metrics.
        RunCompareCompletedRun leftRun = result.leftRun();
        RunCompareCompletedRun rightRun = result.rightRun();

        SimConfig leftConfig = leftRun.config();
        SimConfig rightConfig = rightRun.config();
        SimulationReport leftReport = leftRun.report();
        SimulationReport rightReport = rightRun.report();

        // Top section mirrors the ResultsPage layout:
        // left side = general info
        // right side = metric summary cards
        GridPane statsRow = new GridPane();
        statsRow.setHgap(22);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        c1.setHgrow(Priority.ALWAYS);
        c1.setFillWidth(true);

        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        c2.setHgrow(Priority.ALWAYS);
        c2.setFillWidth(true);

        statsRow.getColumnConstraints().setAll(c1, c2);

        VBox left = buildGeneralInfoCard(leftRun, rightRun, leftConfig, rightConfig, leftReport, rightReport);
        VBox right = buildRightCards(leftReport, rightReport);

        left.setMaxWidth(Double.MAX_VALUE);
        right.setMaxWidth(Double.MAX_VALUE);

        statsRow.add(left, 0, 0);
        statsRow.add(right, 1, 0);

        // Put charts side by side
        VBox countCard = buildCountChartCard(leftReport, rightReport);
        VBox timeCard = buildTimeChartCard(leftReport, rightReport);

        // Chart row: Displays metric comparisons visually
        GridPane chartRow = new GridPane();
        chartRow.setHgap(22);

        ColumnConstraints chartC1 = new ColumnConstraints();
        chartC1.setPercentWidth(50);
        chartC1.setHgrow(Priority.ALWAYS);
        chartC1.setFillWidth(true);

        ColumnConstraints chartC2 = new ColumnConstraints();
        chartC2.setPercentWidth(50);
        chartC2.setHgrow(Priority.ALWAYS);
        chartC2.setFillWidth(true);

        chartRow.getColumnConstraints().setAll(chartC1, chartC2);

        countCard.setMaxWidth(Double.MAX_VALUE);
        timeCard.setMaxWidth(Double.MAX_VALUE);

        chartRow.add(countCard, 0, 0);
        chartRow.add(timeCard, 1, 0);

        // Bottom section: additional configuration parameters for the two runs.
        VBox additionalConfigCard = buildAdditionalConfigCard(leftConfig, rightConfig);
        
        GridPane bottomRow = new GridPane();
        bottomRow.setHgap(22);

        ColumnConstraints bottomC1 = new ColumnConstraints();
        bottomC1.setPercentWidth(50);
        bottomC1.setHgrow(Priority.ALWAYS);
        bottomC1.setFillWidth(true);

        ColumnConstraints bottomC2 = new ColumnConstraints();
        bottomC2.setPercentWidth(50);
        bottomC2.setHgrow(Priority.ALWAYS);
        bottomC2.setFillWidth(true);

        bottomRow.getColumnConstraints().setAll(bottomC1, bottomC2);

        content.getChildren().addAll(
                statsRow,
                chartRow,
                additionalConfigCard
        );
    }

    /**
     * Builds the general information card for the two compared runs.
     *
     * This card shows identifying and high-level configuration information
     * such as run IDs, completion time, flow rates, runway count, simulation
     * duration, and maximum takeoff queue time.
     *
     * @param leftRun the left-hand run in the comparison
     * @param rightRun the right-hand run in the comparison
     * @param leftConfig configuration for the left-hand run
     * @param rightConfig configuration for the right-hand run
     * @param leftReport report for the left-hand run
     * @param rightReport report for the right-hand run
     * @return a card displaying general run information
     */
    private VBox buildGeneralInfoCard(RunCompareCompletedRun leftRun,
                                      RunCompareCompletedRun rightRun,
                                      SimConfig leftConfig,
                                      SimConfig rightConfig,
                                      SimulationReport leftReport,
                                      SimulationReport rightReport) {
        VBox card = new VBox(14);
        card.setStyle(CARD);

        // Heading
        Label header = new Label("General Information");
        header.setFont(Font.font(null, FontWeight.BOLD, 20));
        header.setStyle(MONITOR_LABEL);

        // ----- Table-like comparison rows -----
        GridPane grid = createFourColumnGrid();
        addComparisonHeaderRow(grid, LEFT_LEGEND, RIGHT_LEGEND);

        // Run ID
        addComparisonRow(grid, 1, "Run ID:",
                leftRun.runId(),
                rightRun.runId(),
                "-");

        // TIme of completion
        addComparisonRow(grid, 2, "Completed at:",
                TIME_FMT.format(leftRun.completedAt()),
                TIME_FMT.format(rightRun.completedAt()),
                "-");

        // Inbound flow rate
        addComparisonRow(grid, 3, "Inbound flow rate:",
                formatPerHour(leftConfig.inboundRatePerHour()),
                formatPerHour(rightConfig.inboundRatePerHour()),
                formatDelta(rightConfig.inboundRatePerHour() - leftConfig.inboundRatePerHour(), " aircraft/hour"));

        // Outbound flow rate
        addComparisonRow(grid, 4, "Outbound flow rate:",
                formatPerHour(leftConfig.outboundRatePerHour()),
                formatPerHour(rightConfig.outboundRatePerHour()),
                formatDelta(rightConfig.outboundRatePerHour() - leftConfig.outboundRatePerHour(), " aircraft/hour"));

        // Number of runways
        addComparisonRow(grid, 5, "Number of runways:",
                formatWhole(safeInt(leftReport.numRunways())),
                formatWhole(safeInt(rightReport.numRunways())),
                formatDelta(safeInt(rightReport.numRunways()) - safeInt(leftReport.numRunways()), ""));

        // Simulation duration
        long leftSimulationHours = Duration.between(
                leftConfig.startTime(),
                leftConfig.endTime()
        ).toHours();

        long rightSimulationHours = Duration.between(
                rightConfig.startTime(),
                rightConfig.endTime()
        ).toHours();

        addComparisonRow(grid, 6, "Simulation duration:",
                formatWhole(leftSimulationHours) + " hours",
                formatWhole(rightSimulationHours) + " hours",
                formatDelta(rightSimulationHours - leftSimulationHours, " hours"));

        // Max. time in takeoff queue
        addComparisonRow(grid, 7, "Max. time in takeoff queue:",
                formatWhole(leftConfig.maxTakeoffWaitMinutes()) + " minutes",
                formatWhole(rightConfig.maxTakeoffWaitMinutes()) + " minutes",
                formatDelta(rightConfig.maxTakeoffWaitMinutes() - leftConfig.maxTakeoffWaitMinutes(), " minutes"));

        card.getChildren().addAll(header, grid);
        return card;
    }

    /**
     * Builds the collection of metric comparison cards shown on the right-hand side.
     *
     * The cards group related simulation metrics into logical sections so the
     * user can compare congestion, delays, cancellations, and diversions in a
     * structured way.
     *
     * @param leftReport report for the left-hand run
     * @param rightReport report for the right-hand run
     * @return a VBox containing the metric comparison cards
     */
    private VBox buildRightCards(SimulationReport leftReport, SimulationReport rightReport) {
        VBox right = new VBox(16);

        right.getChildren().addAll(
                // ----- Takeoff Queue -----
                buildMetricCard("Take-off Queue",
                        "Maximum number of planes in take-off queue:",
                        formatWhole(safeInt(leftReport.maxTakeoffQueueSize())),
                        formatWhole(safeInt(rightReport.maxTakeoffQueueSize())),
                        formatDelta(safeInt(rightReport.maxTakeoffQueueSize()) - safeInt(leftReport.maxTakeoffQueueSize()), ""),
                        "Average wait time in take-off queue:",
                        formatMinutes(safeDouble(leftReport.avgTakeoffWaitMinutes())),
                        formatMinutes(safeDouble(rightReport.avgTakeoffWaitMinutes())),
                        formatDelta(safeDouble(rightReport.avgTakeoffWaitMinutes()) - safeDouble(leftReport.avgTakeoffWaitMinutes()), " minutes")),

                // ----- Holding Pattern -----
                buildMetricCard("Holding Pattern",
                        "Maximum number of planes in holding pattern:",
                        formatWhole(safeInt(leftReport.maxHoldingSize())),
                        formatWhole(safeInt(rightReport.maxHoldingSize())),
                        formatDelta(safeInt(rightReport.maxHoldingSize()) - safeInt(leftReport.maxHoldingSize()), ""),
                        "Average hold time for inbound aircraft:",
                        formatMinutes(safeDouble(leftReport.avgHoldingTimeMinutes())),
                        formatMinutes(safeDouble(rightReport.avgHoldingTimeMinutes())),
                        formatDelta(safeDouble(rightReport.avgHoldingTimeMinutes()) - safeDouble(leftReport.avgHoldingTimeMinutes()), " minutes")),

                // ----- Delays -----
                buildMetricCard("Delays",
                        "Maximum inbound delay:",
                        formatMinutes(safeLong(leftReport.maxArrivalDelayMinutes())),
                        formatMinutes(safeLong(rightReport.maxArrivalDelayMinutes())),
                        formatDelta(safeLong(rightReport.maxArrivalDelayMinutes()) - safeLong(leftReport.maxArrivalDelayMinutes()), " minutes"),
                        "Average inbound delay:",
                        formatMinutes(safeDouble(leftReport.avgArrivalDelayMinutes())),
                        formatMinutes(safeDouble(rightReport.avgArrivalDelayMinutes())),
                        formatDelta(safeDouble(rightReport.avgArrivalDelayMinutes()) - safeDouble(leftReport.avgArrivalDelayMinutes()), " minutes")),

                // ----- Cancellations and Diversions -----
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

    /**
     * Creates a comparison card containing two related metrics.
     *
     * Each metric is shown as a row in a four-column comparison grid containing
     * the metric label, the left value, the right value, and the difference.
     *
     * @param titleText card title
     * @param label1 label for the first metric
     * @param left1 left-run value for the first metric
     * @param right1 right-run value for the first metric
     * @param delta1 difference for the first metric
     * @param label2 label for the second metric
     * @param left2 left-run value for the second metric
     * @param right2 right-run value for the second metric
     * @param delta2 difference for the second metric
     * @return a card displaying two compared metrics
    */
    private VBox buildMetricCard(String titleText,
                                 String label1, String left1, String right1, String delta1,
                                 String label2, String left2, String right2, String delta2) {
        VBox card = new VBox(12);
        card.setStyle(CARD);

        // Header
        Label header = new Label(titleText);
        header.setFont(Font.font(null, FontWeight.BOLD, 20));
        header.setStyle(MONITOR_LABEL);

        // Table-like comparison rows
        GridPane grid = createFourColumnGrid();
        addComparisonHeaderRow(grid, LEFT_LEGEND, RIGHT_LEGEND);
        addComparisonRow(grid, 1, label1, left1, right1, delta1);
        addComparisonRow(grid, 2, label2, left2, right2, delta2);

        card.getChildren().addAll(header, grid);
        return card;
    }

    /**
     * Builds the bar chart card for count-based metrics.
     *
     * This chart compares queue sizes and event totals such as cancellations
     * and diversions for the two selected runs.
     *
     * @param leftReport report for the left-hand run
     * @param rightReport report for the right-hand run
     * @return a chart card for count-based metrics
     */
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

    /**
     * Builds the bar chart card for time-based metrics.
     *
     * This chart compares average wait, hold, and delay values in minutes
     * for the two selected runs.
     *
     * @param leftReport report for the left-hand run
     * @param rightReport report for the right-hand run
     * @return a chart card for time-based metrics
     */
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

    /**
     * Wraps a chart canvas in a styled card with a heading.
     *
     * @param heading title displayed above the chart
     * @param canvas chart canvas to display
     * @return a chart card containing the supplied canvas
     */
    private VBox buildChartCard(String heading, Canvas canvas) {
        VBox card = new VBox(10);
        card.setStyle(CARD);

        Label header = new Label(heading);
        header.setFont(Font.font(null, FontWeight.BOLD, 20));
        header.setStyle(MONITOR_LABEL);

        card.getChildren().addAll(header, canvas);
        return card;
    }

     /**
     * Draws a grouped bar chart comparing the two selected runs.
     *
     * Each metric is represented by a pair of bars: one for the left run
     * and one for the right run. The chart uses a shared axis so values can
     * be compared directly across both runs.
     *
     * @param canvas canvas on which the chart is drawn
     * @param labels x-axis labels for each metric group
     * @param leftValues metric values for the left run
     * @param rightValues metric values for the right run
     * @param yAxisLabel label for the y-axis units
     */
    private void drawGroupedBarChart(Canvas canvas,
                                     String[] labels,
                                     double[] leftValues,
                                     double[] rightValues,
                                     String yAxisLabel) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double W = canvas.getWidth();
        double H = canvas.getHeight();

        // Fixed padding reserves space for the y-axis labels, x-axis labels, and legend.
        final double PAD_LEFT = 70;
        final double PAD_RIGHT = 20;
        final double PAD_TOP = 46;
        final double PAD_BOTTOM = 56;

        double chartW = W - PAD_LEFT - PAD_RIGHT;
        double chartH = H - PAD_TOP - PAD_BOTTOM;

        // Clear any previous drawing before rendering the updated chart.
        gc.clearRect(0, 0, W, H);
        gc.setFill(Color.TRANSPARENT);
        gc.fillRect(0, 0, W, H);

        // Draw legend first so the colour meaning of each run is always visible.
        drawLegend(gc);

        // Find the data range across both runs so both sets of bars share one scale.
        double maxVal = 0;
        double minVal = 0;
        for (double v : leftValues) {
            if (v > maxVal) {
                maxVal = v;
            }
            if (v < minVal) {
                minVal = v;
            }
        }
        for (double v : rightValues) {
            if (v > maxVal) {
                maxVal = v;
            }
            if (v < minVal) {
                minVal = v;
            }
        }
        if (maxVal == 0) {
            maxVal = 1;
        }

        // Round the axis maximum to a "nice" value so tick labels remain readable.
        double niceMax = niceNumber(maxVal * 1.20, true);
        int tickCount = 5;

        // small negative leeway if needed
        double niceMin = 0;
        if (minVal < 0) {
            niceMin = minVal * 1.2;   
        }

        double tickStep = niceNumber((niceMax - niceMin) / tickCount, false);
        niceMax = tickStep * tickCount;

        // Axes
        gc.setStroke(CHART_AXIS);
        gc.setLineWidth(1.5);
        gc.strokeLine(PAD_LEFT, PAD_TOP, PAD_LEFT, PAD_TOP + chartH);
        gc.strokeLine(PAD_LEFT, PAD_TOP + chartH, PAD_LEFT + chartW, PAD_TOP + chartH);

        // Grid lines and y-axis tick labels
        gc.setFont(Font.font(null, FontWeight.BOLD, 11));
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

        // Y-axis unit label - rotate sideways
        gc.save();

        gc.translate(PAD_LEFT - 44, PAD_TOP + chartH / 2);
        gc.rotate(-90);

        gc.setFont(Font.font(null, FontWeight.BOLD, 11));
        gc.setFill(CHART_SUBTEXT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("(" + yAxisLabel + ")", 0, 0);

        gc.restore();

        // Calculate the layout of each grouped pair of bars.
        int n = labels.length;
        double groupW = chartW / n;
        double innerGroupW = groupW * 0.72;
        double pairGap = groupW * 0.08;
        double barW = (innerGroupW - pairGap) / 2.0;
        double startOffset = (groupW - innerGroupW) / 2.0;

        for (int i = 0; i < n; i++) {
            // Convert metric values into bar heights using the shared chart scale.
            double leftBarH = ((leftValues[i] - niceMin) / (niceMax - niceMin)) * chartH;
            double rightBarH = ((rightValues[i] - niceMin) / (niceMax - niceMin)) * chartH;

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
            gc.setFont(Font.font(null, FontWeight.BOLD, 11));
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

    /**
     * Draws the fixed chart legend identifying the left and right run colours.
     *
     * @param gc graphics context used to draw the legend
     */
    private void drawLegend(GraphicsContext gc) {
        double y = 18;
        double x = 88;

        gc.setFill(LEFT_RUN_COLOR);
        gc.fillRoundRect(x, y - 9, 12, 12, 3, 3);
        gc.setFill(CHART_TEXT);
        gc.setFont(Font.font(null, FontWeight.BOLD, 11));
        gc.fillText(LEFT_LEGEND, x + 18, y);

        double x2 = x + 120;
        gc.setFill(RIGHT_RUN_COLOR);
        gc.fillRoundRect(x2, y - 9, 12, 12, 3, 3);
        gc.setFill(CHART_TEXT);
        gc.fillText(RIGHT_LEGEND, x2 + 18, y);
    }

    /**
     * Builds the additional configuration comparison card.
     *
     * This card compares lower-level simulation parameters that may help explain
     * why the two runs produced different outputs, such as fuel thresholds,
     * runway occupancy time, and emergency probabilities.
     *
     * @param leftConfig configuration for the left-hand run
     * @param rightConfig configuration for the right-hand run
     * @return a card displaying additional configuration differences
     */
    private VBox buildAdditionalConfigCard(SimConfig leftConfig, SimConfig rightConfig) {
        VBox card = new VBox(14);
        card.setStyle(CARD);
        card.setMaxWidth(Double.MAX_VALUE);

        // Heading
        Label header = new Label("Additional Configuration");
        header.setFont(Font.font(null, FontWeight.BOLD, 20));
        header.setStyle(MONITOR_LABEL);

        // Table-like comparison rows
        GridPane grid = createFourColumnGrid();
        addComparisonHeaderRow(grid, LEFT_LEGEND, RIGHT_LEGEND);

        // Threshold for fuel levels to be considered an emergency
        addComparisonRow(grid, 1, "Low-fuel threshold:",
                formatWhole(leftConfig.lowFuelMinutes()) + " minutes",
                formatWhole(rightConfig.lowFuelMinutes()) + " minutes",
                formatDelta(rightConfig.lowFuelMinutes() - leftConfig.lowFuelMinutes(), " minutes"));

        // Fuel diversion threshold
        addComparisonRow(grid, 2, "Fuel diversion threshold:",
                formatWhole(leftConfig.fuelDiversionMinutes()) + " minutes",
                formatWhole(rightConfig.fuelDiversionMinutes()) + " minutes",
                formatDelta(rightConfig.fuelDiversionMinutes() - leftConfig.fuelDiversionMinutes(), " minutes"));

        // Runway occupancy time 
        addComparisonRow(grid, 3, "Runway occupancy time:",
                formatWhole(leftConfig.runwayOccupancySeconds()) + " seconds",
                formatWhole(rightConfig.runwayOccupancySeconds()) + " seconds",
                formatDelta(rightConfig.runwayOccupancySeconds() - leftConfig.runwayOccupancySeconds(), " seconds"));

        // Chance of mechanical emergency
        addComparisonRow(grid, 4, "Mechanical emergency probability:",
                formatProbability(leftConfig.mechanicalEmergencyProbability()),
                formatProbability(rightConfig.mechanicalEmergencyProbability()),
                formatDelta(rightConfig.mechanicalEmergencyProbability() - leftConfig.mechanicalEmergencyProbability(), "%"));

        // Change of passenger health emergency
        addComparisonRow(grid, 5, "Passenger health emergency probability:",
                formatProbability(leftConfig.passengerHealthEmergencyProbability()),
                formatProbability(rightConfig.passengerHealthEmergencyProbability()),
                formatDelta(rightConfig.passengerHealthEmergencyProbability() - leftConfig.passengerHealthEmergencyProbability(), "%"));

        // Show seed separately because matching seeds indicate comparable randomness.
        addComparisonRow(grid, 6, "Random seed:",
                String.valueOf(leftConfig.randomSeed()),
                String.valueOf(rightConfig.randomSeed()),
                leftConfig.randomSeed() == rightConfig.randomSeed() ? "0" : "Different");

        card.getChildren().addAll(header, grid);
        return card;
    }

    /**
     * Displays a placeholder message when the page is opened without
     * any comparison data. This prevents the view from appearing empty
     * and informs the user that no runs were selected for comparison.
     */
    private void showNoComparisonData() {
        content.getChildren().clear();

        VBox card = new VBox(12);
        card.setStyle(CARD);

        Label header = new Label("Compare Simulation Runs");
        header.setFont(Font.font(null, FontWeight.BOLD, 20));
        header.setStyle(MONITOR_LABEL);

        Label body = new Label("No comparison data was provided.");
        body.setWrapText(true);
        body.setStyle(SMALL_TEXT);

        card.getChildren().addAll(header, body);
        content.getChildren().add(card);
    }

    /**
     * Creates a standard four-column comparison grid.
     *
     * The grid is used throughout the page to align metric names, left values,
     * right values, and calculated differences consistently.
     *
     *  metric name | left value | right value | delta
     * 
     * @return a configured four-column GridPane
     */
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

    /**
     * Adds the header row for a comparison grid.
     * 
     * The row labels the columns for the left run, the right run,
     * and the calculated difference between them. A spacer is placed
     * in the first column to align the headers with the metric labels
     * used in the rows below.
     * 
     * @param grid grid to populate
     * @param leftHeaderText heading for the left-run column
     * @param rightHeaderText heading for the right-run column
     */
    private void addComparisonHeaderRow(GridPane grid, String leftHeaderText, String rightHeaderText) {
        Label spacer = new Label("");
        Label leftHeader = new Label(leftHeaderText);
        Label rightHeader = new Label(rightHeaderText);
        Label deltaHeader = new Label("Difference (Right - Left)");

        leftHeader.setFont(Font.font(null, FontWeight.BOLD, 12));
        rightHeader.setFont(Font.font(null, FontWeight.BOLD, 12));
        deltaHeader.setFont(Font.font(null, FontWeight.BOLD, 12));

        leftHeader.setStyle(MONITOR_LABEL);
        rightHeader.setStyle(MONITOR_LABEL);
        deltaHeader.setStyle(MONITOR_LABEL);

        deltaHeader.setMaxWidth(Double.MAX_VALUE);
        deltaHeader.setAlignment(Pos.CENTER_RIGHT);

        grid.add(spacer, 0, 0);
        grid.add(leftHeader, 1, 0);
        grid.add(rightHeader, 2, 0);
        grid.add(deltaHeader, 3, 0);
    }

    /**
     * Adds a single comparison row to a four-column grid.
     *
     * @param grid grid to populate
     * @param row target row index
     * @param labelText metric label
     * @param leftValueText value for the left run
     * @param rightValueText value for the right run
     * @param deltaText displayed difference between right and left
     */
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
        deltaValue.setFont(Font.font(null, FontWeight.BOLD, 12));
        deltaValue.setStyle(MONITOR_TITLE);

        grid.add(label, 0, row);
        grid.add(leftValue, 1, row);
        grid.add(rightValue, 2, row);
        grid.add(deltaValue, 3, row);
    }

    // ----- Formatting and utility helpers -----
    // These methods standardise numeric formatting and null-handling across the
    // comparison view and charts so values are displayed consistently.

    /**
     * Returns a non-null integer value for display and charting.
     *
     * @param value value that may be null
     * @return the original value, or 0 if null
     */
    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * Returns a non-null long value for display and charting.
     *
     * @param value value that may be null
     * @return the original value, or 0 if null
     */
    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * Returns a non-null double value for display and charting.
     *
     * @param value value that may be null
     * @return the original value, or 0.0 if null
     */
    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    /**
     * Formats a flow-rate value in aircraft per hour.
     *
     * @param value numeric value to format
     * @return formatted flow-rate string
     */
    private String formatPerHour(double value) {
        return String.format("%.2f aircraft/hour", value);
    }

    /**
     * Formats a time value in minutes.
     *
     * @param value numeric value to format
     * @return formatted minutes string
     */
    private String formatMinutes(double value) {
        return String.format("%.2f minutes", value);
    }

    /**
     * Formats a number without decimal places when it is effectively whole,
     * otherwise formats it to two decimal places.
     *
     * @param value numeric value to format
     * @return formatted numeric string
     */
    private String formatWhole(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0000001) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format("%.2f", value);
    }

    /**
     * Formats a probability value as a percentage.
     *
     * @param value probability value
     * @return formatted percentage string
     */
    private String formatProbability(double value) {
        return String.format("%.0f%%", value);
    }

    /**
     * Formats the difference between two values with an explicit sign
     * and optional suffix.
     *
     * @param value difference value to format
     * @param suffix unit suffix to append
     * @return formatted difference string
     */
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

    /**
     * Formats a chart label value using a short numeric representation.
     *
     * @param value numeric value to format
     * @return formatted chart label string
     */
    private String formatChartValue(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0000001) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format("%.1f", value);
    }

    /**
     * Returns a rounded human-friendly number for chart axis scaling.
     *
     * This is used to generate readable axis limits and tick intervals.
     *
     * @param val raw numeric value
     * @param round whether to round or ceiling to the nearest nice number
     * @return a human-friendly scaled value
     */
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

    /**
     * Estimates the rendered width of bold text for canvas alignment.
     *
     * @param s text to measure
     * @param fontSize font size used for rendering
     * @return estimated text width
     */
    private double measureText(String s, double fontSize) {
        Text t = new Text(s);
        t.setFont(Font.font(null, FontWeight.BOLD, fontSize));
        return t.getLayoutBounds().getWidth();
    }
}