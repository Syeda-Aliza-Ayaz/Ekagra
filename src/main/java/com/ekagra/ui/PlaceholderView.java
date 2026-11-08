package com.ekagra.ui;

import com.ekagra.app.Screen;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/** Temporary view for screens that are scheduled for a later build phase. */
public final class PlaceholderView {

    private PlaceholderView() {
    }

    public static Parent create(Screen screen) {
        FontIcon icon = new FontIcon(screen.getIcon());
        icon.getStyleClass().add("placeholder-icon");

        Label title = new Label(screen.getTitle());
        title.getStyleClass().add("h2");

        Label desc = new Label(screen.getDescription());
        desc.getStyleClass().add("muted");
        desc.setWrapText(true);
        desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label phase = new Label("Coming in Phase " + screen.getPhase());
        phase.getStyleClass().add("chip");

        VBox card = new VBox(12, icon, title, desc, phase);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("card");
        card.setMaxWidth(460);
        card.setPrefHeight(280);

        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }
}
