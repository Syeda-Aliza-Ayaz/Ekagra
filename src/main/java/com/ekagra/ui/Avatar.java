package com.ekagra.ui;

import com.ekagra.model.User;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/** Round avatar showing the user's initials on their chosen colour. */
public class Avatar extends StackPane {

    private final Circle circle;
    private final Label initials;

    public Avatar(double size) {
        circle = new Circle(size / 2);
        initials = new Label();
        initials.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + (size * 0.38) + "px;");
        getChildren().addAll(circle, initials);
        setAlignment(Pos.CENTER);
        setMinSize(size, size);
        setMaxSize(size, size);
    }

    public Avatar(User user, double size) {
        this(size);
        setUser(user);
    }

    public void setUser(User user) {
        set(user.getInitials(), user.getAvatarColor());
    }

    public void set(String text, String colorHex) {
        initials.setText(text);
        circle.setFill(Color.web(colorHex));
    }
}
