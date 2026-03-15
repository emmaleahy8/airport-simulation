package com.group32.cs261project.frontend;

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

/**
 * JavaFX pane responsible for rendering the live simulation visual.
 *
 * This component displays the holding queue, take-off queue, radar view,
 * runways, and animated aircraft movements during the simulation.
 * It receives simulation snapshots from the frontend service layer and
 * updates the visual state accordingly.
 *
 * The pane also handles event-based animations such as aircraft landing
 * and take-off, together with playback-speed-dependent timing.
 */
public class SimulationPaneVisual extends Pane {

    // ----- Styling -----
    private static final Color GREEN = Color.web("#39e75f");
    private static final Color GREEN_DIM = Color.web("#1f6f2c");
    private static final Color RUNWAY_FILL = Color.web("#0a2a10");
    private static final Color RADAR_FILL = Color.web("#071407");
    private static final Color EMERGENCY_RED = Color.web("#ff3b3b");

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
    private final Arc radarSweepArc = new Arc();
    private final Line radarHorizontalLine = new Line();
    private final Line radarVerticalLine = new Line();

    // ----- Runways -----
    private final List<Rectangle> runwayRects = new ArrayList<>();
    private final List<Label> runwayLabels = new ArrayList<>();
    private final List<Line> runwayTopLines = new ArrayList<>();
    private final List<Line> runwayBottomLines = new ArrayList<>();
    private final List<Point2D> runwayLandingEndPoints = new ArrayList<>();
    private final List<Point2D> runwayTakeoffRunEndPoints = new ArrayList<>();
    private final List<Point2D> runwayLandingStartPoints = new ArrayList<>();
    private final List<Point2D> runwayTakeoffQueuePoints = new ArrayList<>();
    private final List<Point2D> runwayTakeoffRunStartPoints = new ArrayList<>();

    private final Map<Integer, Long> runwayBusyUntilMs = new HashMap<>();

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

    /**
     * Creates a new simulation visual pane.
     *
     * This constructor initialises the background, queue, and action layers,
     * creates the radar display, starts the radar sweep animation, and sets
     * up listeners so that the layout updates when the pane is resized.
     */
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

        // Create radar rings 
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
     * Updates the visual using the latest simulation snapshot.
     *
     * This method refreshes the layout, updates cached emergency information,
     * redraws queue aircraft, and plays any newly received landing or take-off
     * animations.
     *
     * @param snapshot the latest simulation snapshot
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

    /**
     * Recomputes the layout of all visual elements based on the current pane size.
     *
     * This includes the titles, radar, queue slot positions, and runway layout.
     *
     * @param snapshot the simulation snapshot whose runway count is used when rebuilding the runway layout
     */
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

    /**
     * Positions the section titles within the visual.
     *
     * @param w the current pane width
     * @param h the current pane height
     */
    private void layoutTitles(double w, double h) {
        holdingTitle.setLayoutX(250);
        holdingTitle.setLayoutY(25);

        takeoffTitle.setLayoutX(260);
        takeoffTitle.setLayoutY(h - 250);
    }

    /**
     * Lays out the radar display, including its outer boundary, rings,
     * centre point, axis lines, and sweep arc.
     *
     * @param w the current pane width
     * @param h the current pane height
     */
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

    /**
     * Computes the slot positions used to display aircraft in the holding queue.
     *
     * The positions are distributed across the radar area so that holding
     * aircraft appear visually scattered rather than arranged in a simple line.
     */
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

    /**
     * Computes the slot positions used to display aircraft in the take-off queue.
     *
     * @param w the current pane width
     * @param h the current pane height
     */
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

    /**
     * Lays out the runway visuals and related waypoint positions.
     *
     * Existing runway nodes are removed and recreated so that the displayed
     * runway count and geometry match the current simulation configuration.
     *
     * @param runwayCount the number of runways to display
     * @param w the current pane width
     * @param h the current pane height
     */
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
        runwayLandingStartPoints.clear();
        runwayLandingEndPoints.clear();
        runwayTakeoffRunEndPoints.clear();
        runwayTakeoffQueuePoints.clear();
        runwayTakeoffRunStartPoints.clear();

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
            runwayLandingStartPoints.add(new Point2D(columnX, midY));
            runwayLandingEndPoints.add(new Point2D(columnX + runwayWidth, midY));

            runwayCentres.add(new Point2D(columnX + runwayWidth / 2.0, midY));
            runwayRightExitPoints.add(new Point2D(w + 140, midY - 120));
            
            runwayTakeoffQueuePoints.add(new Point2D(columnX - 20, midY));
            runwayTakeoffRunStartPoints.add(new Point2D(columnX, midY));
            runwayTakeoffRunEndPoints.add(new Point2D(columnX + runwayWidth - 10, midY));
        }
    }

    /**
     * Renders aircraft currently shown in the holding queue and take-off queue.
     *
     * Existing plane visuals are updated where possible, new visuals are created
     * when needed, and visuals no longer required are removed.
     *
     * @param snapshot the current simulation snapshot
     */
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
            .filter(p -> !landingNow.contains(p.id))
            .filter(p -> !isSuppressedFromQueues(p.id))
            .collect(Collectors.toList());

        for (int i = 0; i < Math.min(holdingPlanes.size(), holdingSlots.size()); i++) {
            SimulationData.PlaneDTO p = holdingPlanes.get(i);
            desiredIds.add(p.id);
            Point2D pos = holdingSlots.get(i);
            boolean emergency = knownEmergencyByPlaneId.getOrDefault(p.id, p.emergency);
            upsertQueuePlane(p.id, safeCode(p), pos, true, emergency);
        }

        for (int i = 0; i < Math.min(snapshot.outboundQueueIds.size(), takeoffSlots.size()); i++) {
            String id = snapshot.outboundQueueIds.get(i);
            SimulationData.PlaneDTO p = planeById.get(id);
            if (p == null) continue;

            desiredIds.add(p.id);
            Point2D pos = takeoffSlots.get(i);
            upsertQueuePlane(p.id, safeCode(p), pos, false, false);
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

    /**
     * Creates or updates a queue aircraft visual at the given target position.
     *
     * If the aircraft already exists, its label, style, and position are updated.
     * Otherwise a new aircraft visual is created and added to the queue layer.
     *
     * @param planeId the aircraft identifier
     * @param code the aircraft display code
     * @param target the target visual position
     * @param holding true if the aircraft is in the holding queue
     * @param emergency true if the aircraft is an emergency aircraft
     */
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

    /**
     * Repositions any existing queue aircraft so that they align with the
     * current slot layout after a relayout operation.
     *
     * @param snapshot the simulation snapshot used to determine aircraft positions
     */
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

    /**
     * Plays newly received event animations from the current snapshot.
     *
     * At present this includes landing and take-off event animations.
     *
     * @param snapshot the current simulation snapshot
     */
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

    /**
     * Animates an aircraft landing event.
     *
     * The aircraft moves from its holding position to the runway, waits if the
     * runway is visually occupied, rolls across the runway, and then fades out.
     *
     * @param landed the landing event to animate
     * @param emergency whether the aircraft should be shown as an emergency
     */
    private void animateLandingEvent(SimulationData.PlaneLanded landed, boolean emergency) {
        landingNow.add(landed.planeId());
        suppressFromQueues(landed.planeId(), 2500);

        int runwayIdx = landed.runwayIndex();
        if (runwayIdx < 0
                || runwayIdx >= runwayLeftEntryPoints.size()
                || runwayIdx >= runwayLandingStartPoints.size()
                || runwayIdx >= runwayLandingEndPoints.size()) {
            landingNow.remove(landed.planeId());
            return;
        }

        Point2D holdingFallback = runwayLeftEntryPoints.get(runwayIdx);
        Point2D runwayStart = runwayLandingStartPoints.get(runwayIdx);
        Point2D runwayEnd = runwayLandingEndPoints.get(runwayIdx);

        PlaneVisual existing = queuePlaneNodes.remove(landed.planeId());
        if (existing != null) {
            if (existing.moveTransition != null) {
                existing.moveTransition.stop();
                existing.moveTransition = null;
            }
            queueLayer.getChildren().remove(existing.root);
        }

        Point2D previousHoldingPos = findPreviousHoldingSlot(landed.planeId());

        PlaneVisual moving;
        double startX;
        double startY;

        if (existing != null) {
            moving = existing;
            moving.setHoldingStyle(false);
            moving.setEmergencyStyle(emergency);

            startX = moving.root.getLayoutX() + moving.root.getTranslateX();
            startY = moving.root.getLayoutY() + moving.root.getTranslateY();

            moving.root.setLayoutX(startX);
            moving.root.setLayoutY(startY);
            moving.root.setTranslateX(0);
            moving.root.setTranslateY(0);
        } else if (previousHoldingPos != null) {
            moving = new PlaneVisual(landed.planeId());
            moving.setCode(landed.planeId());
            moving.setHoldingStyle(false);
            moving.setEmergencyStyle(emergency);

            startX = anchoredX(moving, previousHoldingPos);
            startY = anchoredY(moving, previousHoldingPos);

            moving.root.setLayoutX(startX);
            moving.root.setLayoutY(startY);
        } else {
            moving = new PlaneVisual(landed.planeId());
            moving.setCode(landed.planeId());
            moving.setHoldingStyle(false);
            moving.setEmergencyStyle(emergency);

            startX = anchoredX(moving, holdingFallback);
            startY = anchoredY(moving, holdingFallback);

            moving.root.setLayoutX(startX);
            moving.root.setLayoutY(startY);
        }

        double runwayStartX = anchoredX(moving, runwayStart);
        double runwayStartY = anchoredY(moving, runwayStart);

        double runwayEndX = anchoredX(moving, runwayEnd);
        double runwayEndY = anchoredY(moving, runwayEnd);

        moving.planeIcon.setRotate(0);

        actionLayer.getChildren().add(moving.root);
        moving.root.toFront();

        Duration approachDuration = durationForDistance(startX, startY, runwayStartX, runwayStartY, 1600.0);

         // plane stays on runway for exactly runwayOccupancySeconds
        double occupancySeconds = Math.max(0.1, landed.runwayOccupancySeconds());
        Duration runwayDuration = runwayOccupancyDuration(occupancySeconds);

        // If runway is still visually occupied, wait before entering it
        Duration waitBeforeRunway = reserveRunway(runwayIdx, approachDuration, occupancySeconds);

        double dx1 = runwayStartX - startX;
        double dy1 = runwayStartY - startY;

        double dx2 = runwayEndX - startX;
        double dy2 = runwayEndY - startY;

        TranslateTransition approach = new TranslateTransition(approachDuration, moving.root);
        approach.setFromX(0);
        approach.setFromY(0);
        approach.setToX(dx1);
        approach.setToY(dy1);
        approach.setInterpolator(Interpolator.EASE_BOTH);

        PauseTransition wait = new PauseTransition(waitBeforeRunway);

        TranslateTransition runwayRoll = new TranslateTransition(runwayDuration, moving.root);
        runwayRoll.setFromX(dx1);
        runwayRoll.setFromY(dy1);
        runwayRoll.setToX(dx2);
        runwayRoll.setToY(dy2);
        runwayRoll.setInterpolator(Interpolator.LINEAR);

        SequentialTransition seq = new SequentialTransition(approach, wait, runwayRoll);

        Duration totalDuration = approachDuration.add(waitBeforeRunway).add(runwayDuration);
        FadeTransition fade = new FadeTransition(scaledMillis(140), moving.root);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setDelay(totalDuration.subtract(scaledMillis(120)));

        ParallelTransition pt = new ParallelTransition(seq, fade);
        activeEventAnimations.add(pt);

        pt.setOnFinished(e -> {
            landingNow.remove(landed.planeId());
            activeEventAnimations.remove(pt);
            actionLayer.getChildren().remove(moving.root);
        });

        pt.play();
    }

    /**
     * Animates an aircraft take-off event.
     *
     * The aircraft moves from the take-off queue to the runway, waits if needed,
     * rolls along the runway, climbs away, and then fades out.
     *
     * @param tookOff the take-off event to animate
     * @param emergency whether the aircraft should be shown as an emergency
     */
    private void animateTakeoffEvent(SimulationData.PlaneTookOff tookOff, boolean emergency) {
        int runwayIdx = tookOff.runwayIndex();
        if (runwayIdx < 0
                || runwayIdx >= runwayTakeoffRunStartPoints.size()
                || runwayIdx >= runwayTakeoffRunEndPoints.size()
                || runwayIdx >= runwayRightExitPoints.size()) {
            return;
        }

        Point2D runwayStart = runwayTakeoffRunStartPoints.get(runwayIdx);
        Point2D runwayEnd = runwayTakeoffRunEndPoints.get(runwayIdx);
        Point2D exit = runwayRightExitPoints.get(runwayIdx);

        PlaneVisual existing = queuePlaneNodes.remove(tookOff.planeId());
        if (existing != null) {
            if (existing.moveTransition != null) {
                existing.moveTransition.stop();
                existing.moveTransition = null;
            }
            queueLayer.getChildren().remove(existing.root);
        }

        PlaneVisual moving;
        double startX;
        double startY;

        if (existing != null) {
            moving = existing;
            moving.setHoldingStyle(false);
            moving.setEmergencyStyle(emergency);

            startX = moving.root.getLayoutX() + moving.root.getTranslateX();
            startY = moving.root.getLayoutY() + moving.root.getTranslateY();

            moving.root.setLayoutX(startX);
            moving.root.setLayoutY(startY);
            moving.root.setTranslateX(0);
            moving.root.setTranslateY(0);
        } else {
            moving = new PlaneVisual(tookOff.planeId());
            moving.setCode(tookOff.planeId());
            moving.setHoldingStyle(false);
            moving.setEmergencyStyle(emergency);

            // fallback if for some reason the queue node is missing
            Point2D fallback = runwayLeftEntryPoints.get(runwayIdx);
            startX = anchoredX(moving, fallback);
            startY = anchoredY(moving, fallback);

            moving.root.setLayoutX(startX);
            moving.root.setLayoutY(startY);
        }

        double runwayStartX = anchoredX(moving, runwayStart);
        double runwayStartY = anchoredY(moving, runwayStart);

        double runwayEndX = anchoredX(moving, runwayEnd);
        double runwayEndY = anchoredY(moving, runwayEnd);

        double exitX = anchoredX(moving, exit);
        double exitY = anchoredY(moving, exit);

        moving.planeIcon.setRotate(0);

        actionLayer.getChildren().add(moving.root);
        moving.root.toFront();

        Duration taxiDuration = durationForDistance(startX, startY, runwayStartX, runwayStartY, 1800.0);

        double occupancySeconds = Math.max(0.1, tookOff.runwayOccupancySeconds());
        Duration runwayDuration = runwayOccupancyDuration(occupancySeconds);

        Duration waitBeforeRunway = reserveRunway(runwayIdx, taxiDuration, occupancySeconds);

        double dx1 = runwayStartX - startX;
        double dy1 = runwayStartY - startY;

        double dx2 = runwayEndX - startX;
        double dy2 = runwayEndY - startY;

        double dx3 = exitX - startX;
        double dy3 = exitY - startY;

        TranslateTransition taxi = new TranslateTransition(taxiDuration, moving.root);
        taxi.setFromX(0);
        taxi.setFromY(0);
        taxi.setToX(dx1);
        taxi.setToY(dy1);
        taxi.setInterpolator(Interpolator.EASE_BOTH);

        PauseTransition wait = new PauseTransition(waitBeforeRunway);

        TranslateTransition roll = new TranslateTransition(runwayDuration, moving.root);
        roll.setFromX(dx1);
        roll.setFromY(dy1);
        roll.setToX(dx2);
        roll.setToY(dy2);
        roll.setInterpolator(Interpolator.LINEAR);

        Duration climbDuration = durationForDistance(runwayEndX, runwayEndY, exitX, exitY, 1800.0);
        TranslateTransition climb = new TranslateTransition(climbDuration, moving.root);
        climb.setFromX(dx2);
        climb.setFromY(dy2);
        climb.setToX(dx3);
        climb.setToY(dy3);
        climb.setInterpolator(Interpolator.EASE_OUT);

        SequentialTransition seq = new SequentialTransition(taxi, wait, roll, climb);

        Duration totalDuration = taxiDuration
                .add(waitBeforeRunway)
                .add(runwayDuration)
                .add(climbDuration);

        FadeTransition fade = new FadeTransition(scaledMillis(200), moving.root);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setDelay(totalDuration.subtract(scaledMillis(180)));

        ParallelTransition pt = new ParallelTransition(seq, fade);

        activeEventAnimations.add(pt);

        pt.setOnFinished(e -> {
            activeEventAnimations.remove(pt);
            actionLayer.getChildren().remove(moving.root);
        });

        pt.play();
    }

    // ===================
    // ----- Helpers -----
    // ===================

    /**
     * Sets the playback speed used by the visual animation system.
     *
     * Changing the playback speed resets transient animations so that future
     * movement timings remain consistent with the selected speed.
     *
     * @param playbackSpeed the playback speed multiplier
     */
    public void setPlaybackSpeed(double playbackSpeed) {
        this.playbackSpeed = Math.max(0.1, playbackSpeed);
        Platform.runLater(this::resetTransientAnimations);
    }

    /**
     * Creates a styled section title label for the visual.
     *
     * @param text the title text
     * @return the configured title label
    */
    private static Label makeTitle(String text) {
        Label l = new Label(text);
        l.setFont(Font.font(20));
        l.setStyle(HEADING);
        return l;
    }

    /**
     * Returns a safe display code for the given aircraft snapshot.
     *
     * The aircraft code is preferred if present, otherwise the aircraft ID
     * is used. If neither is available, a placeholder is returned.
     *
     * @param p the aircraft snapshot
     * @return a safe display string for the aircraft
     */
    private static String safeCode(SimulationData.PlaneDTO p) {
        if (p.code != null && !p.code.isBlank()) return p.code;
        if (p.id != null && !p.id.isBlank()) return p.id;
        return "?";
    }

    /**
     * Animates a queue aircraft visual to a new position.
     *
     * Any existing movement animation on the plane is stopped before the
     * new movement begins.
     *
     * @param pv the plane visual to move
     * @param targetX the target x-coordinate
     * @param targetY the target y-coordinate
     * @param duration the movement duration
     */
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

    /**
     * Returns the x-coordinate required to place a plane visual so that its
     * internal anchor point aligns with the supplied point.
     *
     * @param pv the plane visual
     * @param point the anchor target point
     * @return the layout x-coordinate for the plane visual
     */
    private double anchoredX(PlaneVisual pv, Point2D point) {
            return point.getX() - pv.getAnchorOffsetX();
        }

    /**
     * Returns the y-coordinate required to place a plane visual so that its
     * internal anchor point aligns with the supplied point.
     *
     * @param pv the plane visual
     * @param point the anchor target point
     * @return the layout y-coordinate for the plane visual
     */
    private double anchoredY(PlaneVisual pv, Point2D point) {
        return point.getY() - pv.getAnchorOffsetY();
    }

    /**
     * Converts runway occupancy time in simulation seconds into a visual
     * animation duration.
     *
     * The duration is adjusted using the current effective visual speed.
     *
     * @param runwayOccupancySeconds the runway occupancy time in seconds
     * @return the corresponding visual animation duration
     */
    private Duration runwayOccupancyDuration(double runwayOccupancySeconds) {
        double secs = Math.max(0.1, runwayOccupancySeconds);

        double visualMillis = secs * 4.2;   // 60 -> 228 ms
        visualMillis = Math.max(140, Math.min(visualMillis, 360));

        return Duration.millis(visualMillis / effectiveVisualSpeed());
    }

    /**
     * Reserves a runway for a future animation and returns any waiting time
     * required before the aircraft may enter it.
     *
     * This prevents multiple aircraft from appearing to occupy the same runway
     * at the same time in the visual.
     *
     * @param runwayIdx the runway index
     * @param earliestEntryTime the earliest time the aircraft could enter
     * @param runwayOccupancySeconds the visual runway occupancy duration basis
     * @return the wait duration before runway entry
     */
    private Duration reserveRunway(int runwayIdx, Duration earliestEntryTime, double runwayOccupancySeconds) {
        long now = System.currentTimeMillis();
        long earliestEntryMs = now + (long) earliestEntryTime.toMillis();
        long freeAtMs = runwayBusyUntilMs.getOrDefault(runwayIdx, now);

        long actualEntryMs = Math.max(earliestEntryMs, freeAtMs);
        Duration wait = Duration.millis(actualEntryMs - earliestEntryMs);
        Duration occ = runwayOccupancyDuration(runwayOccupancySeconds);

        runwayBusyUntilMs.put(runwayIdx, actualEntryMs + (long) occ.toMillis());
        return wait;
    }

    /**
     * Computes an animation duration based on travel distance and speed.
     *
     * @param startX the starting x-coordinate
     * @param startY the starting y-coordinate
     * @param endX the ending x-coordinate
     * @param endY the ending y-coordinate
     * @param pixelsPerSecond the movement speed in pixels per second
     * @return the calculated animation duration
     */
    private Duration durationForDistance(double startX, double startY, double endX, double endY, double pixelsPerSecond) {
        double dx = endX - startX;
        double dy = endY - startY;
        double distance = Math.hypot(dx, dy);

        double millis = (distance / pixelsPerSecond) * 1000.0;

        return Duration.millis(millis / effectiveVisualSpeed());
    }

    /**
     * Converts a base millisecond duration into a playback-speed-adjusted
     * visual duration.
     *
     * @param baseMillis the unscaled duration in milliseconds
     * @return the scaled duration
     */
    private Duration scaledMillis(double baseMillis) {
        return Duration.millis(baseMillis / effectiveVisualSpeed());
    }

    /**
     * Returns the effective visual speed multiplier used for animation timing.
     *
     * Low playback speeds are adjusted non-linearly so that aircraft movement
     * remains visually understandable.
     *
     * @return the effective animation speed multiplier
     */
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

    /**
     * Stops and clears all transient event animations currently in progress.
     *
     * Queue aircraft are also reset so that no partial translate transitions
     * remain active.
     */
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

    /**
     * Finds the previous holding-slot position of the specified aircraft
     * from the last simulation snapshot.
     *
     * @param planeId the aircraft identifier
     * @return the previous holding-slot position, or null if unavailable
     */
    private Point2D findPreviousHoldingSlot(String planeId) {
        if (previousSnapshot == null || previousSnapshot.planes == null) return null;

        List<SimulationData.PlaneDTO> holdingPlanes = previousSnapshot.planes.stream()
                .filter(Objects::nonNull)
                .filter(p -> p.status == SimulationData.PlaneStatus.HOLDING)
                .filter(p -> !isSuppressedFromQueues(p.id))
                .collect(Collectors.toList());

        for (int i = 0; i < Math.min(holdingPlanes.size(), holdingSlots.size()); i++) {
            SimulationData.PlaneDTO p = holdingPlanes.get(i);
            if (p != null && Objects.equals(p.id, planeId)) {
                return holdingSlots.get(i);
            }
        }

        return null;
    }

    /**
     * Updates the emergency-status cache using the supplied simulation snapshot.
     *
     * Once an aircraft has been observed as an emergency aircraft, that state
     * can be reused for subsequent visual events.
     *
     * @param snapshot the current simulation snapshot
     */
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

    /**
     * Temporarily suppresses an aircraft from queue rendering.
     *
     * This is used while event animations are playing so that a plane does not
     * appear simultaneously in both the queue and the action layer.
     *
     * @param planeId the aircraft identifier
     * @param millis the suppression duration in milliseconds
     */
    private void suppressFromQueues(String planeId, long millis) {
        if (planeId == null) return;
        queueSuppressedUntil.put(planeId, System.currentTimeMillis() + millis);
    }

    /**
     * Checks whether the specified aircraft is currently suppressed from
     * queue rendering.
     *
     * @param planeId the aircraft identifier
     * @return true if the aircraft is currently suppressed
     */
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

    /**
     * Removes expired suppression entries from the queue suppression map.
     */
    private void cleanupSuppressedPlanes() {
        long now = System.currentTimeMillis();
        queueSuppressedUntil.entrySet().removeIf(e -> e.getValue() < now);
    }

    // ============================
    // ----- Plane Animations -----
    // ============================

    /**
     * UI object representing a single aircraft in the simulation visual.
     *
     * A plane visual contains the aircraft icon, label, and any movement
     * transition currently being applied to it.
     */
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

        /**
         * Creates a new plane visual for the given aircraft code.
         *
         * @param code the aircraft display code
         */
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

        /**
         * Applies either normal or emergency styling to this plane visual.
         *
         * @param emergency true to apply emergency styling, false for normal styling
         */
        void setEmergencyStyle(boolean emergency) {
            if (emergency) {
                planeIcon.setStyle(
                    "-fx-text-fill: #ff2b2b;" +
                    "-fx-stroke: #ff2b2b;" +
                    "-fx-stroke-width: 0.6;"
                );
                codeLabel.setStyle(
                    "-fx-text-fill: #ff8a8a;" +
                    "-fx-stroke: #ff8a8a;" +
                    "-fx-stroke-width: 0.4;"
                );

                DropShadow redGlow = new DropShadow();
                redGlow.setColor(EMERGENCY_RED);
                redGlow.setRadius(8);
                redGlow.setSpread(0.18);
                planeIcon.setEffect(redGlow);
            } else {
                planeIcon.setTextFill(Color.WHITE);
                codeLabel.setTextFill(Color.WHITE);
                planeIcon.setEffect(new Glow(0.8));
            }
        }

        /**
         * Sets the displayed aircraft code for this plane visual.
         *
         * @param code the aircraft code to display
         */
        void setCode(String code) {
            codeLabel.setText(code);
        }

        /**
        * Updates the aircraft icon styling depending on whether the plane is
        * displayed in the holding queue.
        *
        * @param holding true if the aircraft is in the holding queue
        */
        void setHoldingStyle(boolean holding) {
            planeIcon.setRotate(holding ? -20 : 0);
        }

        /**
         * Sets the position of the aircraft code label relative to the plane icon.
         *
         * @param x the label x-offset
         * @param y the label y-offset
         */
        void setLabelPosition(double x, double y) {
            codeLabel.setLayoutX(x);
            codeLabel.setLayoutY(y);
        }

        // Returns the x-offset of the internal aircraft anchor point.
        double getAnchorOffsetX() {
            return ANCHOR_X;
        }

        // Returns the y-offset of the internal aircraft anchor point.
        double getAnchorOffsetY() {
            return ANCHOR_Y;
        }
    }

    /**
     * Updates the radar sweep guide line and arc to reflect the current
     * sweep angle.
     */
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