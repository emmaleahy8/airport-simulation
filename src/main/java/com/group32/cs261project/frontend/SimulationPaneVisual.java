package com.group32.cs261project.frontend;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.animation.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;

import java.util.*;
import java.util.stream.Collectors;

public class SimulationPaneVisual extends Pane {

    // ----- Styling -----
    private static final Color BG = Color.web("#000000");
    private static final Color GREEN = Color.web("#39e75f");
    private static final Color GREEN_DIM = Color.web("#1f6f2c");
    private static final Color RUNWAY_FILL = Color.web("#0a2a10");
    private static final Color RADAR_FILL = Color.web("#071407");
    private static final Color EMERGENCY_RED = Color.web("#ff3b3b");
    private static final Color EMERGENCY_LABEL = Color.web("#ff8a8a");

    private static final double PREF_W = 1200;
    private static final double PREF_H = 760;

    private static final String HEADING = "-fx-text-fill: #51ed73; -fx-font-weight: 700;";
    private static final String RUNWAY_TITLE = "-fx-text-fill: #51ed73; -fx-font-weight: 700;";

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
    private final Arc radarSweepArc = new Arc();
    private final Line radarHorizontalLine = new Line();
    private final Line radarVerticalLine = new Line();

    // ----- Runways -----
    private final List<Rectangle> runwayRects = new ArrayList<>();
    private final List<Label> runwayLabels = new ArrayList<>();
    private final List<Line> runwayTopLines = new ArrayList<>();
    private final List<Line> runwayBottomLines = new ArrayList<>();
    private final List<Point2D> runwayLandingEndPoints = new ArrayList<>();
    private final List<Point2D> runwayTakeoffStartPoints = new ArrayList<>();
    private final List<Point2D> runwayTakeoffRunEndPoints = new ArrayList<>();

    // ----- Slot positions -----
    private final List<Point2D> holdingSlots = new ArrayList<>();
    private final List<Point2D> takeoffSlots = new ArrayList<>();
    private final List<Point2D> runwayLeftEntryPoints = new ArrayList<>();
    private final List<Point2D> runwayCentres = new ArrayList<>();
    private final List<Point2D> runwayRightExitPoints = new ArrayList<>();

    // ----- Queue visuals -----
    private final Map<String, PlaneVisual> queuePlaneNodes = new HashMap<>();

    // ----- State tracking -----
    private SimulationData previousSnapshot;

    // ----- Playback Speed -----
    private double playbackSpeed = 1.0;
    private static final double VISUAL_SLOWDOWN = 3.0;
    private final List<Animation> activeEventAnimations = new ArrayList<>();

    // ----- Planes -----
    private final Set<String> landingNow = new HashSet<>();
    private final Set<String> takingOffNow = new HashSet<>();
    private final Map<String, Boolean> knownEmergencyByPlaneId = new HashMap<>();
    private final Map<String, Long> queueSuppressedUntil = new HashMap<>();

    public SimulationPaneVisual() {
        setPrefSize(PREF_W, PREF_H);
        setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        getChildren().addAll(backgroundLayer, queueLayer, actionLayer);

        backgroundLayer.getChildren().addAll(
            holdingTitle, 
            takeoffTitle, 
            radarOuter,
            radarSweepArc,
            radarHorizontalLine,
            radarVerticalLine, 
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
            ring.setOpacity(0.20); // subtler
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

        radarSweepArc.setType(ArcType.ROUND);
        radarSweepArc.setFill(Color.web("#39e75f", 0.12));
        radarSweepArc.setStroke(null);
        radarSweepArc.setEffect(radarGlow);

        radarHorizontalLine.setStroke(GREEN_DIM);
        radarHorizontalLine.setStrokeWidth(1.0);

        radarVerticalLine.setStroke(GREEN_DIM);
        radarVerticalLine.setStrokeWidth(1.0);
 
        radarHorizontalLine.setEffect(new Glow(0.25));
        radarVerticalLine.setEffect(new Glow(0.25));

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

        widthProperty().addListener((obs, o, n) -> relayout(previousSnapshot != null ? previousSnapshot : new SimulationData()));
        heightProperty().addListener((obs, o, n) -> relayout(previousSnapshot != null ? previousSnapshot : new SimulationData()));

        relayout(new SimulationData());
    }

    /**
     * Call this whenever a new snapshot arrives from SimulationService.
     */
    public void update(SimulationData snapshot) {
        if (snapshot == null) return;

            Platform.runLater(() -> {
            relayout(snapshot);
            updateEmergencyCache(snapshot);
            renderQueues(snapshot);
            playNewEvents(snapshot);
            previousSnapshot = snapshot.copy();
        });
    }

    // ----- Layout -----

    private void relayout(SimulationData snapshot) {
        double w = Math.max(getWidth(), getPrefWidth());
        double h = Math.max(getHeight(), getPrefHeight());

        backgroundLayer.setPrefSize(w, h);
        queueLayer.setPrefSize(w, h);
        actionLayer.setPrefSize(w, h);

        backgroundLayer.setStyle("""
            -fx-background-color: #001107;
            -fx-background-radius: 16;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 12, 0.2, 0, 4);
        """);

        layoutTitles(w, h);
        layoutRadar(w, h);
        layoutHoldingSlots();
        layoutTakeoffSlots(w, h);
        layoutRunways(snapshot.runwayCount, w, h);

        // If planes already exist, snap them to their current slot positions after relayout
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

        radarHorizontalLine.setStartX(radarCx - radarRadius);
        radarHorizontalLine.setStartY(radarCy);
        radarHorizontalLine.setEndX(radarCx + radarRadius);
        radarHorizontalLine.setEndY(radarCy);

        radarVerticalLine.setStartX(radarCx);
        radarVerticalLine.setStartY(radarCy - radarRadius);
        radarVerticalLine.setEndX(radarCx);
        radarVerticalLine.setEndY(radarCy + radarRadius);

        radarSweepArc.setCenterX(radarCx);
        radarSweepArc.setCenterY(radarCy);
        radarSweepArc.setRadiusX(radarRadius);
        radarSweepArc.setRadiusY(radarRadius);
        radarSweepArc.setLength(56);   // width of sweep

        // 4 main rings
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

        // 4 subtle rings: between outer edge + main1, main1 + main2, main2 + main3, main3 + main4
        List<Double> subtleRadii = new ArrayList<>();
        subtleRadii.add((radarRadius + mainRadii.get(3)) / 2.0); // between outer edge and outermost main ring
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

        // outside first in queue
        double[] holdingRadii = { s4, r4, s3, r3, s2, r2, s1, r1 };

        // deliberately scattered, not spiral-like
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

        double rightX = 530;   // move whole queue block left/right
        double gapX = 120;     // spacing between columns
        double topY = h - 180; // top row y
        double gapY = 48;      // spacing between rows

        for (int col = 0; col < cols; col++) {
            double x = rightX - col * gapX;   // right to left

            for (int row = 0; row < rows; row++) {
                double y = topY + row * gapY; // top, middle, bottom
                takeoffSlots.add(new Point2D(x, y));
            }
        }
    }   

    private void layoutRunways(int runwayCount, double w, double h) {
        runwayCount = Math.max(1, Math.min(10, runwayCount));

        // clear old runway nodes
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
        runwayLandingEndPoints.clear();
        runwayTakeoffRunEndPoints.clear();
        runwayTakeoffStartPoints.clear();

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
                columnX,
                runwayY,
                columnX + runwayWidth,
                runwayY
            );
            topLine.setStroke(GREEN);
            topLine.setStrokeWidth(2);

            Line bottomLine = new Line(
                columnX,
                runwayY + runwayHeight,
                columnX + runwayWidth,
                runwayY + runwayHeight
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
            runwayLeftEntryPoints.add(new Point2D(columnX - 20, midY));
            runwayCentres.add(new Point2D(columnX + runwayWidth / 2.0, midY));
            
            runwayRightExitPoints.add(new Point2D(w + 140, midY - 120));
            
            runwayLandingEndPoints.add(new Point2D(columnX + runwayWidth, midY));
            
            runwayTakeoffStartPoints.add(new Point2D(columnX - 20, midY));
            runwayTakeoffRunEndPoints.add(new Point2D(columnX + runwayWidth - 10, midY));
        }
    }

    // ----- Queue rendering -----

    private void renderQueues(SimulationData snapshot) {
        cleanupSuppressedPlanes();
        List<String> desiredIds = new ArrayList<>();

        Map<String, SimulationData.PlaneDTO> planeById = snapshot.planes.stream()
                .filter(Objects::nonNull)
                .filter(p -> p.id != null)
                .collect(Collectors.toMap(p -> p.id, p -> p, (a, b) -> a));

        List<SimulationData.PlaneDTO> holdingPlanes = snapshot.planes.stream()
            .filter(Objects::nonNull)
            .filter(p -> p.status == SimulationData.PlaneStatus.HOLDING)
            .filter(p -> !isSuppressedFromQueues(p.id))
            .collect(Collectors.toList());

        for (int i = 0; i < Math.min(holdingPlanes.size(), holdingSlots.size()); i++) {
            SimulationData.PlaneDTO p = holdingPlanes.get(i);
            desiredIds.add(p.id);
            Point2D pos = holdingSlots.get(i);
            upsertQueuePlane(p.id, safeCode(p), pos, true, p.emergency);
        }

        for (int i = 0; i < Math.min(snapshot.outboundQueueIds.size(), takeoffSlots.size()); i++) {
            String id = snapshot.outboundQueueIds.get(i);
            SimulationData.PlaneDTO p = planeById.get(id);
            if (p == null) continue;

            desiredIds.add(p.id);
            Point2D pos = takeoffSlots.get(i);
            upsertQueuePlane(p.id, safeCode(p), pos, false, p.emergency);
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

    private void upsertQueuePlane(String planeId, String code, Point2D target, boolean holding, boolean emergency) {
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

        // position / movement
        if (holding || isNew) {
            if (pv.moveTransition != null) {
                pv.moveTransition.stop();
                pv.moveTransition = null;
            }

            pv.root.setLayoutX(x);
            pv.root.setLayoutY(y);
            pv.root.setTranslateX(0);
            pv.root.setTranslateY(0);
        } else {
            // Existing takeoff queue planes animate between slots
            double currentX = pv.root.getLayoutX() + pv.root.getTranslateX();
            double currentY = pv.root.getLayoutY() + pv.root.getTranslateY();

            if (Math.abs(currentX - x) > 1 || Math.abs(currentY - y) > 1) {
                animateNodeTo(pv, x, y, scaledMillis(25)); //***** 
            }
        }

        pv.setHoldingStyle(holding);
        pv.setEmergencyStyle(emergency);

        // label placement
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
                if (pv.moveTransition != null) {
                    pv.moveTransition.stop();
                    pv.moveTransition = null;
                }

                Point2D pos = holdingSlots.get(i);
                pv.root.setLayoutX(pos.getX() - pv.getAnchorOffsetX());
                pv.root.setLayoutY(pos.getY() - pv.getAnchorOffsetY());
                pv.root.setTranslateX(0);
                pv.root.setTranslateY(0);
            }
        }
    }    

    // ----- Event animations -----

    private void playNewEvents(SimulationData snapshot) {
        if (snapshot.events == null) return;

        for (SimulationData.SimEvent ev : snapshot.events) {
            if (ev instanceof SimulationData.PlaneLanded landed) {
                boolean emergency = knownEmergencyByPlaneId.getOrDefault(
                        landed.planeId(),
                        landed.emergency()
                );
                animateLandingEvent(landed, emergency);

            } else if (ev instanceof SimulationData.PlaneTookOff tookOff) {
                boolean emergency = knownEmergencyByPlaneId.getOrDefault(
                        tookOff.planeId(),
                        false
                );
                animateTakeoffEvent(tookOff, emergency);
            }
        }
    }

    private void animateLandingEvent(SimulationData.PlaneLanded landed, boolean emergency) {
        landingNow.add(landed.planeId());
        suppressFromQueues(landed.planeId(), 2500);

        int runwayIdx = landed.runwayIndex();
        if (runwayIdx < 0 || runwayIdx >= runwayLeftEntryPoints.size() || runwayIdx >= runwayLandingEndPoints.size()) {
            return;
        }

        Point2D fallbackStart = runwayLeftEntryPoints.get(runwayIdx);
        Point2D previousHoldingPos = findPreviousHoldingSlot(landed.planeId());
        Point2D end = runwayLandingEndPoints.get(runwayIdx);

        PlaneVisual moving;
        double startX;
        double startY;

        // 1. Best case: plane already exists visually in queue -> reuse it
        PlaneVisual existing = queuePlaneNodes.remove(landed.planeId());

        if (existing != null) {
            if (existing.moveTransition != null) {
                existing.moveTransition.stop();
                existing.moveTransition = null;
            }

            queueLayer.getChildren().remove(existing.root);

            moving = existing;
            moving.setHoldingStyle(false);
            moving.setEmergencyStyle(emergency);

            startX = moving.root.getLayoutX() + moving.root.getTranslateX();
            startY = moving.root.getLayoutY() + moving.root.getTranslateY();

            moving.root.setLayoutX(startX);
            moving.root.setLayoutY(startY);
            moving.root.setTranslateX(0);
            moving.root.setTranslateY(0);
        }
        // 2. Next best: plane was in holding last snapshot -> start from that slot
        else if (previousHoldingPos != null) {
            moving = new PlaneVisual(landed.planeId());
            moving.setHoldingStyle(false);
            moving.setEmergencyStyle(emergency);

            startX = anchoredX(moving, previousHoldingPos);
            startY = anchoredY(moving, previousHoldingPos);

            moving.root.setLayoutX(startX);
            moving.root.setLayoutY(startY);
        }
        // 3. Fallback: old behaviour, start at runway entry
        else {
            moving = new PlaneVisual(landed.planeId());
            moving.setHoldingStyle(false);
            moving.setEmergencyStyle(emergency);

            startX = anchoredX(moving, fallbackStart);
            startY = anchoredY(moving, fallbackStart);

            moving.root.setLayoutX(startX);
            moving.root.setLayoutY(startY);
        }

        double endX = anchoredX(moving, end);
        double endY = anchoredY(moving, end);

        moving.planeIcon.setRotate(0);

        actionLayer.getChildren().add(moving.root);
        moving.root.toFront();

        Duration moveDuration = durationForDistance(startX, startY, endX, endY, 300.0);

        TranslateTransition move = new TranslateTransition(moveDuration, moving.root);
        move.setFromX(0);
        move.setFromY(0);
        move.setToX(endX - startX);
        move.setToY(endY - startY);
        move.setInterpolator(Interpolator.LINEAR);

        FadeTransition fade = new FadeTransition(scaledMillis(140), moving.root);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setDelay(moveDuration.subtract(scaledMillis(120)));

        ParallelTransition pt = new ParallelTransition(move, fade);
        activeEventAnimations.add(pt);

        pt.setOnFinished(e -> {
            landingNow.remove(landed.planeId());
            activeEventAnimations.remove(pt);
            actionLayer.getChildren().remove(moving.root);
        });

        pt.play();
    }

    private void animateTakeoffEvent(SimulationData.PlaneTookOff tookOff, boolean emergency) {
        int runwayIdx = tookOff.runwayIndex();
        if (runwayIdx < 0
            || runwayIdx >= runwayTakeoffStartPoints.size()
            || runwayIdx >= runwayTakeoffRunEndPoints.size()
            || runwayIdx >= runwayRightExitPoints.size()) {
            return;
        }

        Point2D start = runwayTakeoffStartPoints.get(runwayIdx);
        Point2D runwayEnd = runwayTakeoffRunEndPoints.get(runwayIdx);
        Point2D exit = runwayRightExitPoints.get(runwayIdx);

        PlaneVisual moving = new PlaneVisual(tookOff.planeId());

        double startX = anchoredX(moving, start);
        double startY = anchoredY(moving, start);

        double runEndX = anchoredX(moving, runwayEnd);
        double runEndY = anchoredY(moving, runwayEnd);

        double exitX = anchoredX(moving, exit);
        double exitY = anchoredY(moving, exit);

        // place it FIRST, before adding to the pane
        moving.root.setLayoutX(startX);
        moving.root.setLayoutY(startY);
        moving.root.setTranslateX(0);
        moving.root.setTranslateY(0);

        moving.setHoldingStyle(false);
        moving.setEmergencyStyle(emergency);
        moving.planeIcon.setRotate(0);

        actionLayer.getChildren().add(moving.root);
        moving.root.toFront();

        Duration rollDuration = durationForDistance(startX, startY, runEndX, runEndY, 300.0); //***** 
        TranslateTransition roll = new TranslateTransition(rollDuration, moving.root);        
        roll.setFromX(0);
        roll.setFromY(0);
        roll.setToX(runEndX - startX);
        roll.setToY(runEndY - startY);
        roll.setInterpolator(Interpolator.LINEAR);

        Duration climbDuration = durationForDistance(runEndX, runEndY, exitX, exitY, 1800.0);
        TranslateTransition climb = new TranslateTransition(climbDuration, moving.root);        
        climb.setFromX(runEndX - startX);
        climb.setFromY(runEndY - startY);
        climb.setToX(exitX - startX);
        climb.setToY(exitY - startY);
        climb.setInterpolator(Interpolator.EASE_OUT);

        Duration totalTakeoffDuration = rollDuration.add(climbDuration);
        FadeTransition fade = new FadeTransition(scaledMillis(200), moving.root);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setDelay(totalTakeoffDuration.subtract(scaledMillis(180)));

        SequentialTransition seq = new SequentialTransition(roll, climb);
        ParallelTransition pt = new ParallelTransition(seq, fade);

        activeEventAnimations.add(pt);

        pt.setOnFinished(e -> {
            activeEventAnimations.remove(pt);
            actionLayer.getChildren().remove(moving.root);
        });
        pt.play();
    }

    public void setPlaybackSpeed(double playbackSpeed) {
        this.playbackSpeed = Math.max(0.1, playbackSpeed);
        Platform.runLater(this::resetTransientAnimations);
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

    private void animateNodeTo(PlaneVisual pv, double targetX, double targetY, Duration duration) {
        Pane node = pv.root;

        if (pv.moveTransition != null) {
            pv.moveTransition.stop();
        }

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
            pv.moveTransition = null;
        });

        pv.moveTransition = tt;
        tt.play();
    }

    private double anchoredX(PlaneVisual pv, Point2D point) {
            return point.getX() - pv.getAnchorOffsetX();
        }

    private double anchoredY(PlaneVisual pv, Point2D point) {
        return point.getY() - pv.getAnchorOffsetY();
    }

    // Time on Runway 
    private Duration durationForDistance(double startX, double startY, double endX, double endY, double pixelsPerSecond) {
        double dx = endX - startX;
        double dy = endY - startY;
        double distance = Math.hypot(dx, dy);

        double millis = (distance / pixelsPerSecond) * 1000.0;

        return Duration.millis(millis / effectiveVisualSpeed());
    }

    private Duration scaledMillis(double baseMillis) {
        return Duration.millis(baseMillis / effectiveVisualSpeed());
    }

    private double effectiveVisualSpeed() {
        if (playbackSpeed <= 0.1) return 0.12;
        if (playbackSpeed <= 0.2) return 0.22;
        if (playbackSpeed <= 0.3) return 0.32;
        if (playbackSpeed <= 0.4) return 0.42;
        if (playbackSpeed <= 0.5) return 0.55;
        if (playbackSpeed <= 0.6) return 0.68;
        if (playbackSpeed <= 0.7) return 0.78;
        if (playbackSpeed <= 0.8) return 0.88;
        if (playbackSpeed <= 0.9) return 0.95;
        return playbackSpeed;
    }

    private void resetTransientAnimations() {
        for (Animation animation : activeEventAnimations) {
            if (animation != null) {
                animation.stop();
            }
        }
        activeEventAnimations.clear();
        actionLayer.getChildren().clear();

        for (PlaneVisual pv : queuePlaneNodes.values()) {
            if (pv.moveTransition != null) {
                pv.moveTransition.stop();
                pv.moveTransition = null;

                double currentX = pv.root.getLayoutX() + pv.root.getTranslateX();
                double currentY = pv.root.getLayoutY() + pv.root.getTranslateY();

                pv.root.setLayoutX(currentX);
                pv.root.setLayoutY(currentY);
                pv.root.setTranslateX(0);
                pv.root.setTranslateY(0);
            }
        }
    }

    private Point2D findPreviousHoldingSlot(String planeId) {
        if (previousSnapshot == null || previousSnapshot.planes == null) return null;

        List<SimulationData.PlaneDTO> holdingPlanes = previousSnapshot.planes.stream()
                .filter(Objects::nonNull)
                .filter(p -> p.status == SimulationData.PlaneStatus.HOLDING)
                .collect(Collectors.toList());

        for (int i = 0; i < Math.min(holdingPlanes.size(), holdingSlots.size()); i++) {
            SimulationData.PlaneDTO p = holdingPlanes.get(i);
            if (p != null && Objects.equals(p.id, planeId)) {
                return holdingSlots.get(i);
            }
        }

        return null;
    }

    private Set<String> getActiveEventPlaneIds(SimulationData snapshot) {
        Set<String> ids = new HashSet<>();
        if (snapshot == null || snapshot.events == null) return ids;

        for (SimulationData.SimEvent ev : snapshot.events) {
            if (ev instanceof SimulationData.PlaneLanded landed) {
                ids.add(landed.planeId());
            } else if (ev instanceof SimulationData.PlaneTookOff tookOff) {
                ids.add(tookOff.planeId());
            }
        }
        return ids;
    }

    private Boolean findEmergencyInPreviousSnapshot(String planeId) {
        if (previousSnapshot == null || previousSnapshot.planes == null) return null;

        for (SimulationData.PlaneDTO p : previousSnapshot.planes) {
            if (p != null && Objects.equals(p.id, planeId)) {
                return p.emergency;
            }
        }
        return null;
    }

    private void updateEmergencyCache(SimulationData snapshot) {
        if (snapshot == null || snapshot.planes == null) return;

        for (SimulationData.PlaneDTO p : snapshot.planes) {
            if (p != null && p.id != null) {
                if (p.emergency) {
                    knownEmergencyByPlaneId.put(p.id, true);
                } else {
                    knownEmergencyByPlaneId.putIfAbsent(p.id, false);
                }
            }
        }
    }

    private void suppressFromQueues(String planeId, long millis) {
        if (planeId == null) return;
        queueSuppressedUntil.put(planeId, System.currentTimeMillis() + millis);
    }

    private boolean isSuppressedFromQueues(String planeId) {
        if (planeId == null) return false;
        Long until = queueSuppressedUntil.get(planeId);
        if (until == null) return false;

        if (System.currentTimeMillis() > until) {
            queueSuppressedUntil.remove(planeId);
            return false;
        }
        return true;
    }

    private void cleanupSuppressedPlanes() {
        long now = System.currentTimeMillis();
        queueSuppressedUntil.entrySet().removeIf(e -> e.getValue() < now);
    }

    // ----- Plane Animations -----

    private static class PlaneVisual {
        final Pane root = new Pane();
        final StackPane iconWrap = new StackPane();
        final Label planeIcon = new Label("✈");
        final Label codeLabel = new Label();

        TranslateTransition moveTransition;

        static final double ROOT_W = 170;
        static final double ROOT_H = 60;

        // true anchor point for the aircraft position
        static final double ANCHOR_X = 24;
        static final double ANCHOR_Y = 24;

        PlaneVisual(String code) {
            root.setPrefSize(ROOT_W, ROOT_H);
            root.setMinSize(ROOT_W, ROOT_H);
            root.setMaxSize(ROOT_W, ROOT_H);
            root.setPickOnBounds(false);

            planeIcon.setFont(Font.font(26));
            planeIcon.setEffect(new Glow(0.8));

            codeLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 13));

            setEmergencyStyle(false);

            iconWrap.setPrefSize(28, 28);
            iconWrap.setMinSize(28, 28);
            iconWrap.setMaxSize(28, 28);
            iconWrap.getChildren().add(planeIcon);

            // centre the plane icon on the anchor point
            iconWrap.setLayoutX(ANCHOR_X - 14);
            iconWrap.setLayoutY(ANCHOR_Y - 14);

            root.getChildren().addAll(iconWrap, codeLabel);

            setCode(code);

            // default label position
            codeLabel.setLayoutX(40);
            codeLabel.setLayoutY(16);
        }

        void setEmergencyStyle(boolean emergency) {
            if (emergency) {
                planeIcon.setTextFill(EMERGENCY_RED);
                codeLabel.setTextFill(EMERGENCY_LABEL);

                DropShadow redGlow = new DropShadow();
                redGlow.setColor(EMERGENCY_RED);
                redGlow.setRadius(12);
                redGlow.setSpread(0.35);
                planeIcon.setEffect(redGlow);
            } else {
                planeIcon.setTextFill(GREEN);
                codeLabel.setTextFill(Color.web("#7CFF95"));
                planeIcon.setEffect(new Glow(0.8));
            }
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

    private void updateRadarSweepLine() {
        double angle = Math.toRadians(radarSweepAngleDeg);

        double x2 = radarCx + Math.cos(angle) * radarRadius;
        double y2 = radarCy + Math.sin(angle) * radarRadius;

        radarGuideLine.setStartX(radarCx);
        radarGuideLine.setStartY(radarCy);
        radarGuideLine.setEndX(x2);
        radarGuideLine.setEndY(y2);

        // arc trails behind the guide line
        radarSweepArc.setStartAngle(-radarSweepAngleDeg);
        radarSweepArc.setLength(56);
    }
}