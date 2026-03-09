package com.group32.cs261project.frontend;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors;

public class SimulationPaneVisual extends Pane {

    // ---------- Styling ----------
    private static final Color BG = Color.web("#000000");
    private static final Color GREEN = Color.web("#39e75f");
    private static final Color GREEN_DIM = Color.web("#1f6f2c");
    private static final Color RUNWAY_FILL = Color.web("#0a2a10");
    private static final Color RADAR_FILL = Color.web("#071407");

    private static final double PREF_W = 1200;
    private static final double PREF_H = 760;

    private static final String HEADING =
            "-fx-text-fill: #51ed73; -fx-font-weight: 700;";

    private static final String RUNWAY_TITLE =
            "-fx-text-fill: #51ed73; -fx-font-weight: 700;";

    // ----- Main layers -----
    private final Pane backgroundLayer = new Pane();
    private final Pane queueLayer = new Pane();
    private final Pane actionLayer = new Pane();

    // ----- Titles -----
    private final Label holdingTitle = makeTitle("Holding Queue:");
    private final Label takeoffTitle = makeTitle("Take-off Queue:");

    // ----- Radar -----
    private final Circle radarOuter = new Circle();
    private final List<Circle> radarMainRings = new ArrayList<>();
    private final List<Circle> radarSubtleRings = new ArrayList<>();
    private final Line radarGuideLine = new Line();
    private final Circle radarCentreDot = new Circle();

    private double radarCx;
    private double radarCy;
    private double radarRadius;
    private double radarSweepAngleDeg = -45;
    private double holdingRotationDeg = 0;

    // ----- Runways -----
    private final List<Rectangle> runwayRects = new ArrayList<>();
    private final List<Label> runwayLabels = new ArrayList<>();
    private final List<Line> runwayTopLines = new ArrayList<>();
    private final List<Line> runwayBottomLines = new ArrayList<>();

    // ----- Slot positions -----
    private final List<Point2D> holdingSlots = new ArrayList<>();
    private final List<Point2D> takeoffSlots = new ArrayList<>();
    private final List<Point2D> runwayLeftEntryPoints = new ArrayList<>();
    private final List<Point2D> runwayCentres = new ArrayList<>();
    private final List<Point2D> runwayRightExitPoints = new ArrayList<>();

    // ----- Simulation Speed -----
    private double simulationSpeed = 1.0;
    private boolean animationsEnabled = true;

    // ----- Queue visuals -----
    private final Map<String, PlaneVisual> queuePlaneNodes = new HashMap<>();

    // ----- State tracking -----
    private SimulationData previousSnapshot;
    private int processedEventCount = 0;

    public SimulationPaneVisual() {

        setPrefSize(PREF_W, PREF_H);
        setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        getChildren().addAll(backgroundLayer, queueLayer, actionLayer);

        backgroundLayer.getChildren().addAll(
                holdingTitle,
                takeoffTitle,
                radarOuter,
                radarGuideLine,
                radarCentreDot
        );

        backgroundLayer.setStyle("""
                -fx-background-color: #001107;
                -fx-background-radius: 16;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 12, 0.2, 0, 4);
                """);

        queueLayer.setPickOnBounds(false);
        actionLayer.setPickOnBounds(false);

        // create radar rings
        for (int i = 0; i < 4; i++) {
            Circle ring = new Circle();
            ring.setFill(null);
            ring.setStroke(GREEN);
            ring.setOpacity(0.75);

            radarMainRings.add(ring);
            backgroundLayer.getChildren().add(ring);
        }

        for (int i = 0; i < 4; i++) {
            Circle ring = new Circle();
            ring.setFill(null);
            ring.setStroke(GREEN);
            ring.setOpacity(0.20);

            radarSubtleRings.add(ring);
            backgroundLayer.getChildren().add(ring);
        }

        radarOuter.setFill(RADAR_FILL);
        radarOuter.setStroke(GREEN);

        DropShadow radarGlow = new DropShadow();
        radarGlow.setColor(GREEN);
        radarGlow.setRadius(10);
        radarGlow.setSpread(0.32);

        radarOuter.setEffect(radarGlow);

        radarGuideLine.setStroke(GREEN);
        radarGuideLine.setStrokeWidth(1.2);
        radarGuideLine.setEffect(radarGlow);

        for (Circle ring : radarMainRings) {
            ring.setEffect(radarGlow);
        }

        for (Circle ring : radarSubtleRings) {
            ring.setEffect(radarGlow);
        }

        // Radar sweep animation
        Timeline sweepTimeline = new Timeline(
                new KeyFrame(Duration.millis(40), e -> {

                    radarSweepAngleDeg += 2;

                    if (radarSweepAngleDeg >= 360) {
                        radarSweepAngleDeg -= 360;
                    }

                    updateRadarSweepLine();
                })
        );

        sweepTimeline.setCycleCount(Timeline.INDEFINITE);
        sweepTimeline.play();

        radarCentreDot.setFill(GREEN);

        widthProperty().addListener((obs, o, n) ->
                relayout(previousSnapshot != null ? previousSnapshot : new SimulationData())
        );

        heightProperty().addListener((obs, o, n) ->
                relayout(previousSnapshot != null ? previousSnapshot : new SimulationData())
        );

        relayout(new SimulationData());
    }

    /**
     * Called whenever a new snapshot arrives.
     */
    public void update(SimulationData snapshot) {

        if (snapshot == null) return;

        Platform.runLater(() -> {

            relayout(snapshot);
            playNewEvents(snapshot);
            renderQueues(snapshot);

            previousSnapshot = snapshot.copy();
        });
    }

        // =========================================================
    // Layout
    // =========================================================

    private void relayout(SimulationData snapshot) {

        double w = Math.max(getWidth(), getPrefWidth());
        double h = Math.max(getHeight(), getPrefHeight());

        backgroundLayer.setPrefSize(w, h);
        queueLayer.setPrefSize(w, h);
        actionLayer.setPrefSize(w, h);

        layoutTitles(w, h);
        layoutRadar(w, h);
        layoutHoldingSlots();
        layoutTakeoffSlots(w, h);
        layoutRunways(snapshot.runwayCount, w, h);

        if (previousSnapshot != null) {
            snapExistingQueuePlanesToCurrentSnapshot(previousSnapshot);
        }
    }

    private void layoutTitles(double w, double h) {

        holdingTitle.setLayoutX(250);
        holdingTitle.setLayoutY(25);

        takeoffTitle.setLayoutX(260);
        takeoffTitle.setLayoutY(h - 250);
    }

    private void layoutRadar(double w, double h) {

        radarCx = 320;
        radarCy = 310;

        radarRadius = Math.min(210, h * 0.28);

        radarOuter.setCenterX(radarCx);
        radarOuter.setCenterY(radarCy);
        radarOuter.setRadius(radarRadius);
        radarOuter.setStrokeWidth(0.8);

        List<Double> mainRadii = new ArrayList<>();

        for (int i = 0; i < radarMainRings.size(); i++) {

            double rr = radarRadius * ((i + 1.0) / (radarMainRings.size() + 1.0));

            mainRadii.add(rr);

            Circle ring = radarMainRings.get(i);

            ring.setCenterX(radarCx);
            ring.setCenterY(radarCy);
            ring.setRadius(rr);
            ring.setStrokeWidth(0.6);
        }

        List<Double> subtleRadii = new ArrayList<>();

        subtleRadii.add((radarRadius + mainRadii.get(3)) / 2.0);
        subtleRadii.add((mainRadii.get(3) + mainRadii.get(2)) / 2.0);
        subtleRadii.add((mainRadii.get(2) + mainRadii.get(1)) / 2.0);
        subtleRadii.add((mainRadii.get(1) + mainRadii.get(0)) / 2.0);

        for (int i = 0; i < radarSubtleRings.size(); i++) {

            Circle ring = radarSubtleRings.get(i);

            ring.setCenterX(radarCx);
            ring.setCenterY(radarCy);
            ring.setRadius(subtleRadii.get(i));
            ring.setStrokeWidth(0.5);
        }

        radarCentreDot.setCenterX(radarCx);
        radarCentreDot.setCenterY(radarCy);
        radarCentreDot.setRadius(8);

        updateRadarSweepLine();
    }

    private void layoutHoldingSlots() {

        holdingSlots.clear();

        double cx = radarOuter.getCenterX();
        double cy = radarOuter.getCenterY();

        double r1 = radarRadius * (1.0 / 5.0);
        double r2 = radarRadius * (2.0 / 5.0);
        double r3 = radarRadius * (3.0 / 5.0);
        double r4 = radarRadius * (4.0 / 5.0);

        double s1 = (r1 + r2) / 2.0;
        double s2 = (r2 + r3) / 2.0;
        double s3 = (r3 + r4) / 2.0;
        double s4 = (r4 + radarRadius) / 2.0;

        double[] holdingRadii = { s4, r4, s3, r3, s2, r2, s1, r1 };
        double[] anglesDeg = { -35, 62, 150, 255, 18, 205, 118, 310 };

        for (int i = 0; i < holdingRadii.length; i++) {

            double angle = Math.toRadians(anglesDeg[i]);
            double r = holdingRadii[i];

            double x = cx + Math.cos(angle) * r;
            double y = cy + Math.sin(angle) * r;

            holdingSlots.add(new Point2D(x, y));
        }
    }

    private void layoutTakeoffSlots(double w, double h) {

        takeoffSlots.clear();

        int cols = 5;
        int rows = 3;

        double rightX = 530;
        double gapX = 120;

        double topY = h - 180;
        double gapY = 48;

        for (int col = 0; col < cols; col++) {

            double x = rightX - col * gapX;

            for (int row = 0; row < rows; row++) {

                double y = topY + row * gapY;

                takeoffSlots.add(new Point2D(x, y));
            }
        }
    }

        private void layoutRunways(int runwayCount, double w, double h) {

        runwayCount = Math.max(1, Math.min(10, runwayCount));

        backgroundLayer.getChildren().removeAll(runwayRects);
        backgroundLayer.getChildren().removeAll(runwayLabels);
        backgroundLayer.getChildren().removeAll(runwayTopLines);
        backgroundLayer.getChildren().removeAll(runwayBottomLines);

        runwayRects.clear();
        runwayLabels.clear();
        runwayTopLines.clear();
        runwayBottomLines.clear();

        runwayLeftEntryPoints.clear();
        runwayCentres.clear();
        runwayRightExitPoints.clear();

        double columnX = w * 0.69;
        double runwayWidth = Math.min(290, w * 0.23);

        double top = 70;
        double bottom = h - 70;
        double totalHeight = bottom - top;
        double band = totalHeight / runwayCount;
        double runwayHeight = Math.min(40, band * 0.46);

        for (int i = 0; i < runwayCount; i++) {

            double bandY = top + i * band;
            double runwayY = bandY + (band - runwayHeight) / 2.0;

            Rectangle rect = new Rectangle(columnX, runwayY, runwayWidth, runwayHeight);
            rect.setFill(RUNWAY_FILL);
            rect.setStroke(null);

            Line topLine = new Line(
                    columnX, runwayY,
                    columnX + runwayWidth, runwayY
            );
            topLine.setStroke(GREEN);
            topLine.setStrokeWidth(2);

            Line bottomLine = new Line(
                    columnX, runwayY + runwayHeight,
                    columnX + runwayWidth, runwayY + runwayHeight
            );
            bottomLine.setStroke(GREEN);
            bottomLine.setStrokeWidth(2);

            runwayTopLines.add(topLine);
            runwayBottomLines.add(bottomLine);

            DropShadow runwayGlow = new DropShadow();
            runwayGlow.setColor(GREEN);
            runwayGlow.setRadius(10);
            runwayGlow.setSpread(0.3);

            topLine.setEffect(runwayGlow);
            bottomLine.setEffect(runwayGlow);

            Label label = new Label("Runway " + (i + 1));
            label.setFont(Font.font(16));
            label.setStyle(RUNWAY_TITLE);
            label.setLayoutX(columnX + runwayWidth / 2.0 - 40);
            label.setLayoutY(runwayY - 28);

            runwayLabels.add(label);
            runwayRects.add(rect);

            backgroundLayer.getChildren().addAll(rect, topLine, bottomLine, label);

            double midY = runwayY + runwayHeight / 2.0;
            runwayLeftEntryPoints.add(new Point2D(columnX - 110, midY));
            runwayCentres.add(new Point2D(columnX + runwayWidth / 2.0, midY));
            runwayRightExitPoints.add(new Point2D(columnX + runwayWidth + 90, midY));
        }
    }

    // =========================================================
    // Queue rendering
    // =========================================================

    private void renderQueues(SimulationData snapshot) {

        List<String> desiredIds = new ArrayList<>();

        List<SimulationData.PlaneDTO> holdingPlanes = snapshot.planes.stream()
                .filter(p -> p.status == SimulationData.PlaneStatus.HOLDING)
                .collect(Collectors.toList());

        for (int i = 0; i < Math.min(holdingPlanes.size(), holdingSlots.size()); i++) {
            SimulationData.PlaneDTO p = holdingPlanes.get(i);
            desiredIds.add(p.id);

            Point2D pos = holdingSlots.get(i);
            upsertQueuePlane(p.id, safeCode(p), pos, true);
        }

        Map<String, SimulationData.PlaneDTO> planeById = snapshot.planes.stream()
                .collect(Collectors.toMap(p -> p.id, p -> p, (a, b) -> a));

        for (int i = 0; i < Math.min(snapshot.outboundQueueIds.size(), takeoffSlots.size()); i++) {
            String id = snapshot.outboundQueueIds.get(i);
            SimulationData.PlaneDTO p = planeById.get(id);

            if (p == null) continue;

            desiredIds.add(p.id);

            Point2D pos = takeoffSlots.get(i);
            upsertQueuePlane(p.id, safeCode(p), pos, false);
        }

        Set<String> desiredSet = new HashSet<>(desiredIds);

        List<String> toRemove = queuePlaneNodes.keySet().stream()
                .filter(id -> !desiredSet.contains(id))
                .toList();

        for (String id : toRemove) {
            PlaneVisual pv = queuePlaneNodes.remove(id);
            if (pv != null) queueLayer.getChildren().remove(pv.root);
        }
    }

    private void upsertQueuePlane(String planeId, String code, Point2D target, boolean holding) {

        PlaneVisual pv = queuePlaneNodes.get(planeId);
        boolean isNew = false;

        if (pv == null) {
            pv = new PlaneVisual(code);
            queuePlaneNodes.put(planeId, pv);
            queueLayer.getChildren().add(pv.root);
            isNew = true;
        } else {
            pv.setCode(code);
        }

        double x = target.getX() - pv.getAnchorOffsetX();
        double y = target.getY() - pv.getAnchorOffsetY();

        if (holding || isNew) {
            pv.root.setLayoutX(x);
            pv.root.setLayoutY(y);
            pv.root.setTranslateX(0);
            pv.root.setTranslateY(0);
        } else {
            double currentX = pv.root.getLayoutX() + pv.root.getTranslateX();
            double currentY = pv.root.getLayoutY() + pv.root.getTranslateY();

            if (Math.abs(currentX - x) > 1 || Math.abs(currentY - y) > 1) {
                animateNodeTo(pv.root, x, y, Duration.millis(250));
            }
        }

        pv.setHoldingStyle(holding);

        if (holding) {
            pv.setLabelPosition(34, -2);
        } else {
            pv.setLabelPosition(40, -2);
        }
    }

    private void snapExistingQueuePlanesToCurrentSnapshot(SimulationData snapshot) {

        if (snapshot == null || snapshot.planes == null) return;

        List<SimulationData.PlaneDTO> holdingPlanes = snapshot.planes.stream()
                .filter(Objects::nonNull)
                .filter(p -> p.status == SimulationData.PlaneStatus.HOLDING)
                .collect(Collectors.toList());

        for (int i = 0; i < Math.min(holdingPlanes.size(), holdingSlots.size()); i++) {
            SimulationData.PlaneDTO p = holdingPlanes.get(i);

            if (p == null || p.id == null) continue;

            PlaneVisual pv = queuePlaneNodes.get(p.id);

            if (pv != null) {
                Point2D pos = holdingSlots.get(i);
                pv.root.setLayoutX(pos.getX() - pv.getAnchorOffsetX());
                pv.root.setLayoutY(pos.getY() - pv.getAnchorOffsetY());
                pv.root.setTranslateX(0);
                pv.root.setTranslateY(0);
            }
        }
    }

    // ----- Simulation Speed -----

    public void setPlaybackSpeed(double playbackSpeed) {
        this.simulationSpeed = playbackSpeed;
    }

    public void setAnimationsEnabled(boolean animationsEnabled) {
        this.animationsEnabled = animationsEnabled;
    }

    // ----- Event Animations -----

    private void playNewEvents(SimulationData snapshot) {

        if (snapshot.events == null) return;

        if (processedEventCount > snapshot.events.size()) {
            processedEventCount = 0;
        }

        for (int i = processedEventCount; i < snapshot.events.size(); i++) {
            SimulationData.SimEvent ev = snapshot.events.get(i);

            if (ev instanceof SimulationData.PlaneLanded landed) {
                animateLandingEvent(landed);
            } else if (ev instanceof SimulationData.PlaneTookOff tookOff) {
                animateTakeoffEvent(tookOff);
            }
        }

        processedEventCount = snapshot.events.size();
    }

    private void animateLandingEvent(SimulationData.PlaneLanded landed) {

        int runwayIdx = landed.runwayIndex();
        if (runwayIdx < 0 || runwayIdx >= runwayLeftEntryPoints.size()) return;

        Point2D start = runwayLeftEntryPoints.get(runwayIdx);
        Point2D end = runwayCentres.get(runwayIdx);

        PlaneVisual moving = new PlaneVisual(landed.planeId());
        moving.root.setLayoutX(start.getX());
        moving.root.setLayoutY(start.getY());

        actionLayer.getChildren().add(moving.root);

        TranslateTransition move = new TranslateTransition(Duration.millis(700), moving.root);
        move.setToX(end.getX() - start.getX());
        move.setToY(end.getY() - start.getY());
        move.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition fade = new FadeTransition(Duration.millis(220), moving.root);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setDelay(Duration.millis(520));

        ParallelTransition pt = new ParallelTransition(move, fade);
        pt.setOnFinished(e -> actionLayer.getChildren().remove(moving.root));
        pt.play();
    }

    private void animateTakeoffEvent(SimulationData.PlaneTookOff tookOff) {

        int runwayIdx = tookOff.runwayIndex();
        if (runwayIdx < 0 || runwayIdx >= runwayCentres.size()) return;

        Point2D start = runwayCentres.get(runwayIdx);
        Point2D end = runwayRightExitPoints.get(runwayIdx);

        PlaneVisual moving = new PlaneVisual(tookOff.planeId());
        moving.root.setLayoutX(start.getX());
        moving.root.setLayoutY(start.getY());

        actionLayer.getChildren().add(moving.root);

        TranslateTransition move = new TranslateTransition(Duration.millis(850), moving.root);
        move.setToX(end.getX() - start.getX());
        move.setToY(end.getY() - start.getY());
        move.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(260), moving.root);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setDelay(Duration.millis(620));

        ParallelTransition pt = new ParallelTransition(move, fade);
        pt.setOnFinished(e -> actionLayer.getChildren().remove(moving.root));
        pt.play();
    }

    private void animateNodeTo(Pane node, double targetX, double targetY, Duration duration) {

        double currentX = node.getLayoutX() + node.getTranslateX();
        double currentY = node.getLayoutY() + node.getTranslateY();

        node.setLayoutX(currentX);
        node.setLayoutY(currentY);
        node.setTranslateX(0);
        node.setTranslateY(0);

        TranslateTransition tt = new TranslateTransition(duration, node);
        tt.setToX(targetX - currentX);
        tt.setToY(targetY - currentY);
        tt.setInterpolator(Interpolator.EASE_BOTH);

        tt.setOnFinished(e -> {
            node.setLayoutX(targetX);
            node.setLayoutY(targetY);
            node.setTranslateX(0);
            node.setTranslateY(0);
        });

        tt.play();
    }

    // ----- Plane visual node -----

    private static class PlaneVisual {

        final Pane root = new Pane();
        final StackPane iconWrap = new StackPane();
        final Label planeIcon = new Label("✈");
        final Label codeLabel = new Label();

        static final double ROOT_W = 170;
        static final double ROOT_H = 60;

        static final double ANCHOR_X = 24;
        static final double ANCHOR_Y = 24;

        PlaneVisual(String code) {

            root.setPrefSize(ROOT_W, ROOT_H);
            root.setMinSize(ROOT_W, ROOT_H);
            root.setMaxSize(ROOT_W, ROOT_H);
            root.setPickOnBounds(false);

            planeIcon.setTextFill(GREEN);
            planeIcon.setFont(Font.font(26));
            planeIcon.setEffect(new Glow(0.8));

            codeLabel.setTextFill(Color.web("#7CFF95"));
            codeLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
            codeLabel.setEffect(new Glow(0.85));

            iconWrap.setPrefSize(28, 28);
            iconWrap.setMinSize(28, 28);
            iconWrap.setMaxSize(28, 28);
            iconWrap.getChildren().add(planeIcon);

            iconWrap.setLayoutX(ANCHOR_X - 14);
            iconWrap.setLayoutY(ANCHOR_Y - 14);

            root.getChildren().addAll(iconWrap, codeLabel);

            setCode(code);

            codeLabel.setLayoutX(40);
            codeLabel.setLayoutY(16);
        }

        void setCode(String code) {
            codeLabel.setText(code);
        }

        void setHoldingStyle(boolean holding) {
            planeIcon.setRotate(holding ? -20 : 0);
        }

        void setLabelPosition(double x, double y) {
            codeLabel.setLayoutX(x);
            codeLabel.setLayoutY(y);
        }

        double getAnchorOffsetX() {
            return ANCHOR_X;
        }

        double getAnchorOffsetY() {
            return ANCHOR_Y;
        }
    }

    // ----- Helpers -----

    private static Label makeTitle(String text) {

        Label l = new Label(text);

        l.setFont(Font.font(20));
        l.setStyle(HEADING);

        return l;
    }

    private static String safeCode(SimulationData.PlaneDTO p) {

        if (p.code != null && !p.code.isBlank()) return p.code;
        if (p.id != null && !p.id.isBlank()) return p.id;

        return "?";
    }

    private void updateRadarSweepLine() {

        double angle = Math.toRadians(radarSweepAngleDeg);

        double x2 = radarCx + Math.cos(angle) * radarRadius;
        double y2 = radarCy + Math.sin(angle) * radarRadius;

        radarGuideLine.setStartX(radarCx);
        radarGuideLine.setStartY(radarCy);

        radarGuideLine.setEndX(x2);
        radarGuideLine.setEndY(y2);
    }
}