package com.ekagra.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.util.Duration;

import java.util.function.IntFunction;

/** Small, reusable motion effects that make the UI feel alive without being distracting. */
public final class Animations {

    private static final Interpolator EASE_OUT = Interpolator.SPLINE(0.2, 0.8, 0.2, 1.0);

    private Animations() {
    }

    /** Page enter: fade in + slide up a few pixels. */
    public static void pageEnter(Node node) {
        node.setOpacity(0);
        node.setTranslateY(14);
        FadeTransition fade = new FadeTransition(Duration.millis(260), node);
        fade.setToValue(1);
        fade.setInterpolator(EASE_OUT);
        TranslateTransition slide = new TranslateTransition(Duration.millis(320), node);
        slide.setToY(0);
        slide.setInterpolator(EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    /** Children appear one after another (cards on a page, rows in a list). */
    public static void stagger(Parent parent, int maxItems) {
        int i = 0;
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (i >= maxItems) {
                break;
            }
            appear(child, 40L * i++);
        }
    }

    public static void appear(Node node, long delayMs) {
        node.setOpacity(0);
        node.setTranslateY(10);
        FadeTransition fade = new FadeTransition(Duration.millis(240), node);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), node);
        slide.setToY(0);
        slide.setInterpolator(EASE_OUT);
        ParallelTransition p = new ParallelTransition(fade, slide);
        p.setDelay(Duration.millis(delayMs));
        p.play();
    }

    /** Animates a number from 0 up to its value, e.g. on stat tiles. */
    public static void countUp(Label label, int target, IntFunction<String> format) {
        if (target <= 0) {
            label.setText(format.apply(target));
            return;
        }
        DoubleProperty value = new SimpleDoubleProperty(0);
        value.addListener((o, a, b) -> label.setText(format.apply((int) Math.round(b.doubleValue()))));
        Timeline t = new Timeline(new KeyFrame(Duration.millis(700), new KeyValue(value, target, EASE_OUT)));
        label.setText(format.apply(0));
        t.play();
    }

    /** Gentle "breathing" scale loop, used for the logo on the login screen. */
    public static void breathe(Node node) {
        ScaleTransition s = new ScaleTransition(Duration.seconds(2.4), node);
        s.setFromX(1);
        s.setFromY(1);
        s.setToX(1.07);
        s.setToY(1.07);
        s.setAutoReverse(true);
        s.setCycleCount(Animation.INDEFINITE);
        s.setInterpolator(Interpolator.EASE_BOTH);
        s.play();
    }

    /** Quick attention pulse (e.g. a counter that just changed). */
    public static void pulse(Node node) {
        ScaleTransition s = new ScaleTransition(Duration.millis(140), node);
        s.setToX(1.12);
        s.setToY(1.12);
        s.setAutoReverse(true);
        s.setCycleCount(2);
        s.play();
    }

    /** Every button in the scene shrinks slightly while pressed: tactile feedback. */
    public static void installPressFeedback(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            ButtonBase b = findButton(e.getPickResult().getIntersectedNode());
            if (b != null && !b.isDisabled()) {
                scale(b, 0.95);
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            ButtonBase b = findButton(e.getPickResult().getIntersectedNode());
            if (b != null) {
                scale(b, 1.0);
            }
        });
    }

    private static ButtonBase findButton(Node n) {
        while (n != null && !(n instanceof ButtonBase)) {
            n = n.getParent();
        }
        return (ButtonBase) n;
    }

    private static void scale(Node n, double to) {
        ScaleTransition s = new ScaleTransition(Duration.millis(90), n);
        s.setToX(to);
        s.setToY(to);
        s.play();
    }

    /** Replaces the jumpy default mouse-wheel scrolling with smooth, eased scrolling. */
    public static void smoothScroll(ScrollPane pane) {
        if (pane.getProperties().containsKey("ek-smooth")) {
            return;
        }
        pane.getProperties().put("ek-smooth", true);
        final double[] target = {pane.getVvalue()};
        final Timeline[] running = {null};
        pane.vvalueProperty().addListener((o, a, b) -> {
            if (running[0] == null || running[0].getStatus() != Animation.Status.RUNNING) {
                target[0] = b.doubleValue();
            }
        });
        pane.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.getDeltaY() == 0 || pane.getContent() == null) {
                return;
            }
            double contentH = pane.getContent().getBoundsInLocal().getHeight();
            double viewH = pane.getViewportBounds().getHeight();
            if (contentH <= viewH) {
                return;   // nothing to scroll; let parents handle it
            }
            double step = (e.getDeltaY() * 2.2) / (contentH - viewH);
            target[0] = Math.max(0, Math.min(1, target[0] - step));
            if (running[0] != null) {
                running[0].stop();
            }
            running[0] = new Timeline(new KeyFrame(Duration.millis(260),
                new KeyValue(pane.vvalueProperty(), target[0], EASE_OUT)));
            running[0].play();
            e.consume();
        });
    }

    /** Applies smooth scrolling to every ScrollPane inside a view. */
    public static void smoothScrollAll(Parent root) {
        if (root instanceof ScrollPane sp) {
            smoothScroll(sp);
        }
        root.lookupAll(".scroll-pane").forEach(n -> {
            if (n instanceof ScrollPane sp) {
                smoothScroll(sp);
            }
        });
    }
}
