package com.ekagra.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

/** Left-hand branding panel shown on the login and sign-up screens. */
public class BrandPane extends VBox {

    public BrandPane() {
        getStyleClass().add("brand-pane");
        setSpacing(14);
        setPrefWidth(470);
        setMinWidth(380);
        setAlignment(Pos.CENTER_LEFT);

        LogoMark logo = new LogoMark(60, Color.WHITE, LogoMark.CORAL);

        Label wordmark = new Label("ekagra");
        wordmark.getStyleClass().add("brand-wordmark");
        Label meaning = new Label("one-pointed focus");
        meaning.getStyleClass().add("brand-meaning");
        VBox titleBox = new VBox(2, wordmark, meaning);

        Label tagline = new Label("Focus deeply. Live well.");
        tagline.getStyleClass().add("brand-tagline");
        VBox.setMargin(tagline, new Insets(14, 0, 0, 0));

        Label intro = new Label("A productivity and digital-wellbeing companion for students: "
            + "see where your time really goes and build healthier study habits.");
        intro.getStyleClass().add("brand-text");
        intro.setWrapText(true);

        VBox features = new VBox(11,
            feature("fth-monitor", "Track the apps and sites you actually use"),
            feature("fth-crosshair", "Stay in flow with Pomodoro + Focus Guard"),
            feature("fth-heart", "Balance study with breaks, mood and sleep"),
            feature("fth-bar-chart-2", "Get a daily Productivity Score and insights"));
        VBox.setMargin(features, new Insets(8, 0, 0, 0));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Region topSpacer = new Region();
        VBox.setVgrow(topSpacer, Priority.ALWAYS);

        Label footer = new Label("CT-263 · Object-Oriented Analysis & Design");
        footer.getStyleClass().add("brand-footer");

        getChildren().addAll(topSpacer, logo, titleBox, tagline, intro, features, spacer, footer);

        Animations.breathe(logo);
        Animations.appear(titleBox, 80);
        Animations.appear(tagline, 160);
        Animations.appear(intro, 220);
        Animations.stagger(features, 4);
    }

    private static HBox feature(String icon, String text) {
        FontIcon fi = new FontIcon(icon);
        fi.getStyleClass().add("brand-feature-icon");
        Label l = new Label(text);
        l.getStyleClass().add("brand-text");
        HBox row = new HBox(14, fi, l);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
