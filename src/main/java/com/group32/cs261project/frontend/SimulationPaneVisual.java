package com.group32.cs261project.frontend;

import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.application.Platform;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javafx.scene.layout.Region;

public class SimulationPaneVisual extends VBox {

    private final Label title = new Label("Airfield Visual");
    private final StackPane canvasWrap = new StackPane(); // border + centering
    private final HBox runwayRow = new HBox(18);

    // Runway image
    private final Image runwayImg = new Image(
            Objects.requireNonNull(getClass().getResourceAsStream("resources/com/group32/cs261project/ui/assets/runway_image.png"))
    );

    private final List<ImageView> runwayViews = new ArrayList<>();
    private final List<Rectangle> statusTints = new ArrayList<>();
    private final List<Label> numberLabels = new ArrayList<>();

    // Take-off zone
    private final Pane takeoffZone = new Pane();

    // Plane image
    private final Image planeImg = new Image(
        Objects.requireNonNull(getClass().getResource("/com/group32/cs261project/ui/assets/plane_image.png")).toExternalForm()
    );

    private ImageView makePlaneNode() {
        ImageView plane_iv = new ImageView(planeImg);

        plane_iv.setPreserveRatio(true);
        plane_iv.setFitWidth(32);          
        plane_iv.setRotate(0);  // facing right

        return plane_iv;
    }

    private SimulationData lastSnap;

    // Plane nodes + last-known state
    private final Map<String, ImageView> planeNodes = new HashMap<>();
    private final Map<String, SimulationData.PlaneStatus> lastStatus = new HashMap<>(); 

    public SimulationPaneVisual() {
        setPadding(new Insets(18));
        setSpacing(12);
        setAlignment(Pos.TOP_CENTER);

        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 800;");

        canvasWrap.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #111;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;"
        );
        canvasWrap.setPadding(new Insets(18));
        VBox.setVgrow(canvasWrap, Priority.ALWAYS);

        runwayRow.setAlignment(Pos.CENTER);
        
        // ----- Layout container -----
        VBox layout = new VBox();
        layout.setFillWidth(true);

        // Spacer pushes runways downward
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Takeoff area placeholder (reserved space at bottom)
        takeoffZone.setPrefHeight(120);
        takeoffZone.setMinHeight(120);
        takeoffZone.setMaxHeight(120);
        takeoffZone.setMaxWidth(Double.MAX_VALUE);
        takeoffZone.prefWidthProperty().bind(canvasWrap.widthProperty().subtract(36)); // 18px padding * 2
        // takeoffZone.setStyle("-fx-background-color: transparent;");
        takeoffZone.setStyle("-fx-background-color: rgba(255,0,0,0.08);"); // debug checl

        // Add everything in order
        layout.getChildren().addAll(spacer, runwayRow, takeoffZone);

        // Align properly
        runwayRow.setAlignment(Pos.CENTER);
        layout.setAlignment(Pos.BOTTOM_CENTER);

        // Make VBox layout stretch its children
        layout.setMaxWidth(Double.MAX_VALUE);
        runwayRow.setMaxWidth(Double.MAX_VALUE);
        takeoffZone.setMaxWidth(Double.MAX_VALUE);

        // Add layout to canvas
        canvasWrap.getChildren().clear();
        canvasWrap.getChildren().add(layout);

        getChildren().addAll(title, canvasWrap);

        // redraw when resized (keeps scaling nice)
        widthProperty().addListener((o, a, b) -> redraw());
        heightProperty().addListener((o, a, b) -> redraw());
    }

    public void update(SimulationData snap) {
        if (takeoffZone.getWidth() <= 1) {
            Platform.runLater(() -> update(snap));
            return;
        }

        long out = snap.planes.stream()
                .filter(p -> p.direction == SimulationData.PlaneDirection.OUTBOUND)
                .count();

        System.out.println(
            "takeoffQueue=" + snap.takeoffQueue +
            " outboundQueueIds=" + snap.outboundQueueIds +
            " planes=" + snap.planes.size() +
            " OUTBOUND=" + out
        );

        lastSnap = snap;
        redraw();

        // Build lookup table
        Map<String, SimulationData.PlaneDTO> byId = new HashMap<>();
        for (var p : snap.planes) byId.put(p.code, p);

        // ----- Ensure FIFO queue planes exist and are positioned -----
        for (int slot = 0; slot < snap.outboundQueueIds.size(); slot++) {
            String code = snap.outboundQueueIds.get(slot);
            var dto = byId.get(code);
            if (dto == null) continue;

            ImageView node = planeNodes.get(code);
            if (node == null) {
                node = makePlaneNode();
                planeNodes.put(code, node);
                takeoffZone.getChildren().add(node);

                // Mark as entering so it doesn't get snapped mid-animation
                node.getProperties().put("entering", true);

                // Animate entering from left into correct slot
                animateEnterQueue(node, slot);
            } else {
                Boolean entering = (Boolean) node.getProperties().get("entering");
                if (entering == null || !entering) {
                    placeInQueueSlot(node, slot);  
                }
            }
        }

        // ----- Animate planes that leave the queue -----
        for (var dto : snap.planes) {
            if (dto.direction != SimulationData.PlaneDirection.OUTBOUND) continue;

            ImageView node = planeNodes.get(dto.code);
            if (node == null) continue; // could be mid-remove

            SimulationData.PlaneStatus prev = lastStatus.get(dto.code);
            SimulationData.PlaneStatus curr = dto.status;

            // Transition: OUTBOUND_QUEUE -> TAXI_TO_RUNWAY (start moving toward runway)
            if (prev == SimulationData.PlaneStatus.OUTBOUND_QUEUE && curr == SimulationData.PlaneStatus.TAXI_TO_RUNWAY) {

                int runwayIDx = Math.max(0, dto.assignedRunwayIndex);

                //Move to front then pivot and go to runway
                slideToFrontThenTakeoff(node, runwayIDx, snap);
            }

            lastStatus.put(dto.code, curr);
        }

        // ----- Remove nodes for planes no longer present -----
        var alive = byId.keySet();
        List<String> dead = new ArrayList<>();
        for (String code : planeNodes.keySet()) {
            if (!alive.contains(code)) dead.add(code);
        }
        for (String code : dead) {
            ImageView n = planeNodes.remove(code);
            lastStatus.remove(code);
            if (n != null) takeoffZone.getChildren().remove(n);
        }

        // Update lastStatus for all planes present in snapshot
        for (var dto : snap.planes) {
            lastStatus.put(dto.code, dto.status);
        }
    }

    private void redraw() {
        if (lastSnap == null) return;
        lastSnap.ensureRunwayListSize();

        if (runwayViews.size() != lastSnap.runwayCount) {
            rebuild(lastSnap.runwayCount);
        }

        // Decide runway image size based on available width
        double availableW = Math.max(300, canvasWrap.getWidth() - 40);
        if (availableW <= 1) availableW = 900; // fallback early layout

        int n = lastSnap.runwayCount;
        double gap = runwayRow.getSpacing();
        double per = (availableW - gap * (n - 1)) / n;

        // Clamp sizes 
        double imgW = Math.max(50, Math.min(120, per));
        double imgH = imgW * 1.4; 

        for (int i = 0; i < n; i++) {
            var rc = lastSnap.runways.get(i);

            ImageView iv = runwayViews.get(i);
            iv.setFitWidth(imgW);
            iv.setFitHeight(imgH);

            Rectangle tint = statusTints.get(i);
            tint.setWidth(imgW);
            tint.setHeight(imgH);
            tint.setArcWidth(10);
            tint.setArcHeight(10);
            tint.setFill(statusColor(rc.status));
            tint.setOpacity(tintOpacity(rc.status));

            numberLabels.get(i).setText(String.valueOf(rc.id));
        }
    }

    private void rebuild(int runwayCount) {
        runwayRow.getChildren().clear();
        runwayViews.clear();
        statusTints.clear();
        numberLabels.clear();

        for (int i = 0; i < runwayCount; i++) {

            ImageView iv = new ImageView(runwayImg);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);

            Rectangle tint = new Rectangle();
            tint.setMouseTransparent(true);

            StackPane imageLayer = new StackPane(iv, tint);
            imageLayer.setAlignment(Pos.CENTER);

            Label num = new Label(String.valueOf(i + 1));
            num.setStyle("-fx-font-weight: 800; -fx-text-fill: #111;");

            VBox tile = new VBox(8, imageLayer, num);
            tile.setAlignment(Pos.TOP_CENTER);

            runwayViews.add(iv);
            statusTints.add(tint);
            numberLabels.add(num);

            runwayRow.getChildren().add(tile);
        }
    }

    private Color statusColor(SimulationData.RunwayStatus status) {
        return switch (status) {
            case AVAILABLE -> Color.TRANSPARENT;
            case RUNWAY_INSPECTION -> Color.GOLD;
            case SNOW_CLEARANCE -> Color.DODGERBLUE;
            case EQUIPMENT_FAILURE -> Color.RED;
        };
    }

    private double tintOpacity(SimulationData.RunwayStatus status) {
        return switch (status) {
            case AVAILABLE -> 0.0;
            default -> 0.18;
        };
    }

    private double slotX(int slotIndex) {
        double rightPadding = 30;
        double spacing = 40;
        double w = takeoffZone.getWidth() > 1 ? takeoffZone.getWidth() : takeoffZone.getPrefWidth();
        double frontX = w - rightPadding;
        return frontX - slotIndex * spacing;
    }

    private double slotY() {
        return 60; // within takeoffZone
    }

    private void animateEnterQueue(ImageView plane, int slotIndex) {
        double startX = -50;    // offscreen left
        double startY = slotY();

        plane.setLayoutX(startX);
        plane.setLayoutY(startY);
        plane.setRotate(0);    // facing right

        TranslateTransition drive = new TranslateTransition(Duration.seconds(3.0), plane); // CHANGE**
        drive.setToX(slotX(slotIndex) - startX);
        drive.setToY(0);

        drive.setOnFinished(e -> plane.getProperties().put("entering", false));

        drive.play();
    }

    private double runwayTargetX(int runwayIndex, int runwayCount) {
        double w = canvasWrap.getWidth() > 1 ? canvasWrap.getWidth() : 900;
        double leftPad = 80, rightPad = 80;
        double span = w - leftPad - rightPad;
        double step = (runwayCount <= 1) ? 0 : span / (runwayCount - 1);

        return leftPad + runwayIndex * step;
    }

    private void placeInQueueSlot(ImageView plane, int slotIndex) {
        // We use layoutX/layoutY for absolute positioning inside takeoffZone
        plane.setTranslateX(0);
        plane.setTranslateY(0);

        plane.setLayoutX(slotX(slotIndex));
        plane.setLayoutY(slotY());
        plane.setRotate(0); // facing right while queued
        plane.setOpacity(1.0);
    }

    private void animateToRunway(ImageView plane, int runwayIndex, SimulationData snap) {
        // Ensure at front
        plane.setRotate(0);

        RotateTransition pivot = new RotateTransition(Duration.seconds(5.0), plane);
        pivot.setToAngle(-90); // up

        // drive up + adjust x a bit toward runway
        double currentX = plane.getLayoutX() + plane.getTranslateX();
        double targetX = runwayTargetX(runwayIndex, snap.runwayCount);

        TranslateTransition driveUp = new TranslateTransition(Duration.seconds(5.0), plane);
        driveUp.setToX(plane.getTranslateX() + (targetX - currentX));
        driveUp.setToY(plane.getTranslateY() - 220); // go upward

        FadeTransition fade = new FadeTransition(Duration.seconds(5.0), plane);
        fade.setToValue(0);

        /*
        SequentialTransition seq = new SequentialTransition(pivot, driveUp, fade); 
        */
        SequentialTransition seq = new SequentialTransition(pivot, driveUp); // temporarily removed fade
        seq.setOnFinished(e -> takeoffZone.getChildren().remove(plane));
        seq.play();
    }

    private void slideToFrontThenTakeoff(ImageView plane, int runwayIDx, SimulationData snap) {
        // Slide to slot 0 then pivot and go to runway
        double targetX = slotX(0);

        TranslateTransition slide = new TranslateTransition(Duration.seconds(0.8), plane);
        slide.setToX(0);
        slide.setToY(0);

        // Convert layout movement into translate for nice animation
        double dx = targetX - plane.getLayoutX();
        plane.setLayoutX(targetX);
        plane.setTranslateX(-dx);

        TranslateTransition slideIn = new TranslateTransition(Duration.seconds(0.8), plane);
        slideIn.setToX(0);

        slideIn.setOnFinished(e -> animateToRunway(plane, runwayIDx, snap));
        slideIn.play();
    }

}