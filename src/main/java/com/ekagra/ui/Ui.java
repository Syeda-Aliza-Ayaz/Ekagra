package com.ekagra.ui;

import com.ekagra.app.Navigator;
import com.ekagra.app.ThemeManager;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Function;

/** Small reusable UI building blocks so controllers stay short. */
public final class Ui {

    private Ui() {
    }

    /** Runs slow work (e.g. writing a PDF) off the UI thread, then reports back on the UI thread. */
    public static <T> void background(java.util.concurrent.Callable<T> work, java.util.function.Consumer<T> onDone,
                                      java.util.function.Consumer<Throwable> onError) {
        javafx.concurrent.Task<T> task = new javafx.concurrent.Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> onDone.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException()));
        Thread t = new Thread(task, "ekagra-background");
        t.setDaemon(true);
        t.start();
    }

    /** Gives a dialog the app stylesheet, the current theme and the main window as owner. */
    public static void style(Dialog<?> dialog) {
        dialog.initOwner(Navigator.getStage());
        var pane = dialog.getDialogPane();
        pane.getStylesheets().add(Navigator.stylesheet());
        if (ThemeManager.isDark()) {
            pane.getStyleClass().add("dark");
        }
    }

    public static boolean confirm(String title, String message, String okText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        ButtonType ok = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(ok, ButtonType.CANCEL);
        style(alert);
        return alert.showAndWait().filter(b -> b == ok).isPresent();
    }

    public static Button iconButton(String icon, String tooltip, Runnable action) {
        Button b = new Button(null, new FontIcon(icon));
        b.getStyleClass().add("icon-button");
        b.setTooltip(new Tooltip(tooltip));
        b.setOnAction(e -> {
            action.run();
            e.consume();
        });
        return b;
    }

    /** Small coloured dot that marks a category (text next to it stays in normal ink). */
    public static javafx.scene.shape.Circle categoryDot(com.ekagra.model.Category c) {
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(5);
        dot.getStyleClass().addAll("cat-dot", c.styleClass());
        return dot;
    }

    public static Label chip(String text, String... extraClasses) {
        Label l = new Label(text);
        l.getStyleClass().add("chip");
        l.getStyleClass().addAll(extraClasses);
        l.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);   // chips never get squeezed into "..."
        return l;
    }

    public static Label label(String text, String... classes) {
        Label l = new Label(text);
        l.getStyleClass().addAll(classes);
        return l;
    }

    /** Stat tile whose number counts up when it appears. */
    public static VBox statTile(int value, String caption, String icon) {
        VBox tile = statTile(String.valueOf(value), caption, icon);
        Label v = (Label) tile.getChildren().get(1);
        Animations.countUp(v, value, String::valueOf);
        return tile;
    }

    /** A small "big number + caption" tile. */
    public static VBox statTile(String value, String caption, String icon) {
        FontIcon fi = new FontIcon(icon);
        fi.getStyleClass().add("stat-icon");
        Label v = label(value, "stat-value");
        Label c = label(caption, "muted", "small");
        VBox box = new VBox(4, fi, v, c);
        box.getStyleClass().addAll("card", "stat-tile");
        return box;
    }

    /** Lets a ComboBox contain a null item that is displayed as e.g. "All subjects". */
    public static <T> void nullableCombo(ComboBox<T> combo, String nullText, Function<T, String> toText) {
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? nullText : toText.apply(item));
            }
        });
        combo.setButtonCell(combo.getCellFactory().call(null));
    }

    /**
     * Standard spinner setup: modern "−  value  +" layout, and typed text is committed
     * when focus leaves (by default JavaFX only commits on Enter).
     */
    public static void spinner(Spinner<?> spinner) {
        if (!spinner.getStyleClass().contains(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL)) {
            spinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
        }
        spinner.focusedProperty().addListener((obs, was, now) -> {
            if (!now) {
                try {
                    spinner.increment(0);
                } catch (RuntimeException ignored) {
                    spinner.getEditor().setText(String.valueOf(spinner.getValue()));
                }
            }
        });
    }

    public static Node emptyState(String icon, String title, String text) {
        FontIcon fi = new FontIcon(icon);
        fi.getStyleClass().add("placeholder-icon");
        Label t = label(title, "h3");
        Label d = label(text, "muted");
        d.setWrapText(true);
        VBox box = new VBox(10, fi, t, d);
        box.getStyleClass().add("empty-state");
        box.setAlignment(javafx.geometry.Pos.CENTER);
        return box;
    }
}
