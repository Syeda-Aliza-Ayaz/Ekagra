package com.ekagra.ui;

import com.ekagra.app.Navigator;
import com.ekagra.app.ThemeManager;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Duration;

/**
 * A small always-on-top notification in the bottom-right corner of the screen.
 * Unlike an in-app message it is visible even while the student is in another app
 * (that's the point of Focus Guard), and it does not steal keyboard focus.
 */
public final class DesktopToast {

    public enum Kind {
        INFO("fth-info", "toast-info"),
        SUCCESS("fth-check-circle", "toast-success"),
        WARNING("fth-alert-triangle", "toast-warning");

        final String icon;
        final String styleClass;

        Kind(String icon, String styleClass) {
            this.icon = icon;
            this.styleClass = styleClass;
        }
    }

    public record Action(String text, boolean primary, Runnable run) {
    }

    private static Stage hiddenOwner;
    private static Stage current;

    private DesktopToast() {
    }

    public static void show(Kind kind, String title, String message, Duration autoHide, Action... actions) {
        closeCurrent();

        FontIcon icon = new FontIcon(kind.icon);
        icon.getStyleClass().add("toast-icon");
        Label t = Ui.label(title, "toast-title");
        Label m = Ui.label(message, "toast-message");
        m.setWrapText(true);
        m.setMaxWidth(290);
        VBox text = new VBox(3, t, m);
        HBox.setHgrow(text, Priority.ALWAYS);

        Stage stage = new Stage(StageStyle.TRANSPARENT);
        Button close = Ui.iconButton("fth-x", "Dismiss", () -> hide(stage));
        HBox top = new HBox(12, icon, text, close);
        top.setAlignment(Pos.TOP_LEFT);

        VBox card = new VBox(12, top);
        if (actions.length > 0) {
            HBox buttons = new HBox(8);
            buttons.setAlignment(Pos.CENTER_RIGHT);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            buttons.getChildren().add(spacer);
            for (Action a : actions) {
                Button b = new Button(a.text());
                if (a.primary()) {
                    b.getStyleClass().add("accent");
                }
                b.setOnAction(e -> {
                    hide(stage);
                    a.run().run();
                });
                buttons.getChildren().add(b);
            }
            card.getChildren().add(buttons);
        }
        card.getStyleClass().addAll("toast", kind.styleClass);
        card.setPrefWidth(380);

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: transparent; -fx-padding: 14;");   // room for the shadow
        if (ThemeManager.isDark()) {
            root.getStyleClass().add("dark");
        }
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(Navigator.stylesheet());

        stage.initOwner(owner());
        stage.setAlwaysOnTop(true);
        stage.setScene(scene);
        stage.setOpacity(0);
        stage.show();

        Rectangle2D area = Screen.getPrimary().getVisualBounds();
        stage.setX(area.getMaxX() - stage.getWidth() - 6);
        stage.setY(area.getMaxY() - stage.getHeight() - 6);
        stage.setOpacity(1);

        card.setTranslateX(60);
        card.setOpacity(0);
        TranslateTransition slide = new TranslateTransition(javafx.util.Duration.millis(320), card);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.SPLINE(0.2, 0.8, 0.2, 1));
        FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(220), card);
        fade.setToValue(1);
        new ParallelTransition(slide, fade).play();

        PauseTransition timer = new PauseTransition(javafx.util.Duration.millis(autoHide.toMillis()));
        timer.setOnFinished(e -> {
            if (card.isHover()) {
                timer.playFromStart();     // don't vanish while the user is reading / about to click
            } else {
                hide(stage);
            }
        });
        timer.play();
        current = stage;
    }

    private static void hide(Stage stage) {
        if (!stage.isShowing()) {
            return;
        }
        FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(180), stage.getScene().getRoot());
        fade.setToValue(0);
        fade.setOnFinished(e -> stage.close());
        fade.play();
        if (current == stage) {
            current = null;
        }
    }

    private static void closeCurrent() {
        if (current != null) {
            current.close();
            current = null;
        }
    }

    /** An invisible utility window owns the toasts so they get no taskbar button
     *  and still appear when the main window is minimised. */
    private static Stage owner() {
        if (hiddenOwner == null) {
            hiddenOwner = new Stage(StageStyle.UTILITY);
            hiddenOwner.setOpacity(0);
            hiddenOwner.setWidth(1);
            hiddenOwner.setHeight(1);
            hiddenOwner.setX(-32000);
            hiddenOwner.setY(-32000);
            hiddenOwner.setScene(new Scene(new StackPane()));
            hiddenOwner.show();
        }
        return hiddenOwner;
    }

    public static void shutdown() {
        closeCurrent();
        if (hiddenOwner != null) {
            hiddenOwner.close();
            hiddenOwner = null;
        }
    }
}
