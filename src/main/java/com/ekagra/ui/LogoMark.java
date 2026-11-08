package com.ekagra.ui;

import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * The Ekagra logo: concentric rings converging on a single coral point,
 * i.e. "eka-agra", one-pointed focus.
 */
public class LogoMark extends StackPane {

    public static final Color BLUE = Color.web("#226192");
    public static final Color CORAL = Color.web("#EF8557");

    public LogoMark(double size) {
        this(size, BLUE, CORAL);
    }

    public LogoMark(double size, Color ringColor, Color dotColor) {
        double r = size / 2;
        Circle outer = ring(r * 0.88, r * 0.14, ringColor, 0.45);
        Circle middle = ring(r * 0.56, r * 0.14, ringColor, 0.8);
        Circle dot = new Circle(r * 0.22, dotColor);
        getChildren().addAll(outer, middle, dot);
        setAlignment(Pos.CENTER);
        setMinSize(size, size);
        setMaxSize(size, size);
        setPrefSize(size, size);
    }

    private static Circle ring(double radius, double width, Color color, double opacity) {
        Circle c = new Circle(radius - width / 2);
        c.setFill(Color.TRANSPARENT);
        c.setStroke(color);
        c.setStrokeWidth(width);
        c.setOpacity(opacity);
        return c;
    }

    /** Renders the logo to an image for the window/taskbar icon. */
    public static Image icon(int size) {
        LogoMark mark = new LogoMark(size);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return mark.snapshot(params, null);
    }
}
