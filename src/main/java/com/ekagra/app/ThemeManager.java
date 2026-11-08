package com.ekagra.app;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Parent;

/** Switches between light and dark themes (FR-41). */
public final class ThemeManager {

    private static final String DARK_CLASS = "dark";
    private static boolean dark;

    private ThemeManager() {
    }

    public static void apply(boolean darkMode) {
        dark = darkMode;
        Application.setUserAgentStylesheet(dark
            ? new PrimerDark().getUserAgentStylesheet()
            : new PrimerLight().getUserAgentStylesheet());
    }

    public static boolean isDark() {
        return dark;
    }

    /** Adds/removes the "dark" style class so app.css can adjust brand colours. */
    public static void decorate(Parent root) {
        root.getStyleClass().remove(DARK_CLASS);
        if (dark) {
            root.getStyleClass().add(DARK_CLASS);
        }
    }
}
