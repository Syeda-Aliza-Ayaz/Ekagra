package com.ekagra.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.util.Duration;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;

/** Circular countdown: a track ring, a progress arc and the remaining time in the middle. */
public class TimerRing extends StackPane {

    private final Arc arc;
    private Timeline sweep;
    private final Label timeLabel = new Label("25:00");
    private final Label captionLabel = new Label("Focus");

    public TimerRing(double size) {
        double stroke = size * 0.05;
        double radius = size / 2 - stroke;

        Circle track = new Circle(radius);
        track.getStyleClass().add("ring-track");
        track.setStrokeWidth(stroke);

        arc = new Arc(0, 0, radius, radius, 90, 0);
        arc.setType(ArcType.OPEN);
        arc.setStrokeWidth(stroke);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.getStyleClass().add("ring-progress");

        timeLabel.getStyleClass().add("ring-time");
        timeLabel.setStyle("-fx-font-size: " + (size * 0.2) + "px;");
        captionLabel.getStyleClass().add("ring-caption");
        VBox text = new VBox(2, timeLabel, captionLabel);
        text.setAlignment(Pos.CENTER);

        // track and arc share centre (0,0) inside a Group; the full track circle fixes the Group's bounds,
        // so the arc never shifts as its length changes
        Group rings = new Group(track, arc);
        getChildren().addAll(rings, text);
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
    }

    public void update(String time, String caption, double progress, boolean isBreak) {
        timeLabel.setText(time);
        captionLabel.setText(caption);
        double p = Math.max(0, Math.min(1, progress));
        double target = -360 * p;
        if (sweep != null) {
            sweep.stop();
        }
        // moving forward by a small step (one tick): glide smoothly; otherwise (reset/jump): snap
        double delta = arc.getLength() - target;
        if (delta > 0 && delta < 20) {
            sweep = new Timeline(new KeyFrame(Duration.millis(950), new KeyValue(arc.lengthProperty(), target)));
            sweep.play();
        } else {
            arc.setLength(target);
        }
        arc.setVisible(p > 0.001);
        arc.getStyleClass().remove("break");
        if (isBreak) {
            arc.getStyleClass().add("break");
        }
    }
}
