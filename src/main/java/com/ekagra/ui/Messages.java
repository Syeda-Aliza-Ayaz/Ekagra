package com.ekagra.ui;

import javafx.scene.control.Label;

/** Helpers for the inline success / error labels used on forms. */
public final class Messages {

    private Messages() {
    }

    public static void error(Label label, String text) {
        show(label, text, "error-label");
    }

    public static void success(Label label, String text) {
        show(label, text, "success-label");
    }

    public static void hide(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    private static void show(Label label, String text, String styleClass) {
        label.getStyleClass().removeAll("error-label", "success-label");
        label.getStyleClass().add(styleClass);
        label.setText(text);
        label.setWrapText(true);
        label.setVisible(true);
        label.setManaged(true);
    }
}
