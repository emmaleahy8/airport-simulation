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

    private SimulationPaneVisual visual;

    private SimulationData currentConfig;

    // NEW: Pause button
    private Button pauseBtn;

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

        HBox mainLayout = new HBox(10);
        mainLayout.setPadding(new Insets(18));

        Label title = new Label("Running Simulation");
        title.setFont(Font.font(28));
        BorderPane.setMargin(title, new Insets(18, 18, 14, 18));
        root.setTop(title);

        VBox leftColumn = new VBox(20);
        leftColumn.setPrefWidth(400);
        leftColumn.prefWidthProperty().bind(mainLayout.widthProperty().multiply(0.33));
        leftColumn.setMinWidth(300);

        VBox metricsCard = buildMetricsCard();
        VBox runwayCard = buildRunwayCard();
        VBox.setVgrow(runwayCard, Priority.ALWAYS);

        metricsCard.setMaxWidth(Double.MAX_VALUE);
        runwayCard.setMaxWidth(Double.MAX_VALUE);
        leftColumn.setFillWidth(true);

        leftColumn.getChildren().addAll(metricsCard, runwayCard);

        visual = new SimulationPaneVisual();

        StackPane simulationPane = new StackPane();
        simulationPane.setAlignment(Pos.CENTER);
        simulationPane.prefWidthProperty().bind(mainLayout.widthProperty().multiply(0.67));
        simulationPane.getChildren().add(visual);

        HBox.setHgrow(simulationPane, Priority.ALWAYS);

        mainLayout.getChildren().addAll(leftColumn, simulationPane);
        root.setCenter(mainLayout);

        // Pause Button
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
        buttons.setPadding(new Insets(0, 18, 18, 18));

        root.setBottom(buttons);
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

        sim.start(currentConfig, snap -> Platform.runLater(() -> {

            simTimeVal.setText(String.valueOf(snap.simMinute));
            holdingVal.setText(String.valueOf(snap.holdingQueue));
            takeoffVal.setText(String.valueOf(snap.takeoffQueue));

            updateRunwayTable(snap);

            visual.update(snap);

        }));
    }

    @Override
    public void onExit() {
        if (spinnerAnim != null) {
            spinnerAnim.stop();
        }
    }

    private VBox buildMetricsCard() {

        VBox card = new VBox(12);
        card.setStyle(CARD);

        Label header = new Label("Live Status");
        header.setFont(Font.font(18));
        header.setStyle("-fx-font-weight: 800;");

        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);

        addRow(gp, 0, "Simulation time:", simTimeVal, "min");
        addRow(gp, 1, "Number of Aircraft in Holding Queue:", holdingVal, "");
        addRow(gp, 2, "Number of Aircraft in Take-off Queue:", takeoffVal, "");

        card.getChildren().addAll(header, gp);

        return card;
    }

    private VBox buildRunwayCard() {

        VBox card = new VBox(12);
        card.setStyle(CARD);

        Label header = new Label("Runway Configuration");
        header.setFont(Font.font(18));
        header.setStyle("-fx-font-weight: 800;");

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

        card.getChildren().addAll(header, new Separator(), runwayTable);

        return card;
    }

    private void addRow(GridPane gp, int row, String labelText, Label valueLabel, String suffix) {

        Label l = new Label(labelText);

        valueLabel.setStyle("-fx-font-weight: 800;");
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);

        HBox valueBox = new HBox(6);
        valueBox.setAlignment(Pos.CENTER_RIGHT);
        valueBox.getChildren().add(valueLabel);

        if (suffix != null && !suffix.isBlank()) {
            Label s = new Label(suffix);
            valueBox.getChildren().add(s);
        }

        gp.add(l, 0, row);
        gp.add(valueBox, 1, row);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setHgrow(Priority.ALWAYS);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(220);
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

        h0.setStyle("-fx-font-weight: 800;");
        h1.setStyle("-fx-font-weight: 800;");
        h2.setStyle("-fx-font-weight: 800;");

        runwayTable.add(h0, 0, 0);
        runwayTable.add(h1, 1, 0);
        runwayTable.add(h2, 2, 0);

        for (int i = 0; i < runwayCount; i++) {

            int row = i + 1;

            Label name = new Label("Runway " + (i + 1) + ":");
            Label mode = new Label("-");
            Label status = new Label("-");

            mode.setStyle("-fx-font-weight: 700;");
            status.setStyle("-fx-font-weight: 700;");

            runwayModeVals.add(mode);
            runwayStatusVals.add(status);

            runwayTable.add(name, 0, row);
            runwayTable.add(mode, 1, row);
            runwayTable.add(status, 2, row);
        }
    }

    private void updateRunwayTable(SimulationData d) {

        d.ensureRunwayListSize();

        int n = Math.min(d.runways.size(), runwayModeVals.size());

        for (int i = 0; i < n; i++) {

            var r = d.runways.get(i);

            runwayModeVals.get(i).setText(pretty(r.mode.name()));
            runwayStatusVals.get(i).setText(pretty(r.status.name()));
        }
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