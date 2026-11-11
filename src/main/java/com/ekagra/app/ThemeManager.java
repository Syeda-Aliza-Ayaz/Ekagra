package com.ekagra.app;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.ekagra.service.SessionContext;
import com.ekagra.service.SettingsService;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Objects;

/**
 * Manages light/dark themes, curated harmonic palettes, and custom color accents.
 */
public final class ThemeManager {

    public record Palette(
        String id,
        String name,
        String primaryHex,
        String lightCanvas,
        String darkCanvas,
        String lightSubtle,
        String darkSubtle
    ) {
        @Override
        public String toString() {
            return name;
        }
    }

    public static final Palette OCEAN = new Palette(
        "ocean", "Ocean Focus", "#226192", "#EAE6DE", "#0F1820", "#DCE8F2", "#1A3247"
    );
    public static final Palette MATCHA = new Palette(
        "matcha", "Sage Matcha", "#2D5A43", "#E6EBE0", "#121A15", "#DCEBDD", "#1A2D21"
    );
    public static final Palette NORDIC = new Palette(
        "nordic", "Nordic Frost", "#0D9488", "#E5E9F0", "#14181F", "#CCFBF1", "#134E4A"
    );
    public static final Palette TERRACOTTA = new Palette(
        "terracotta", "Warm Terracotta", "#A8422B", "#F5EBE0", "#1F1512", "#FFEDD5", "#3B1C14"
    );
    public static final Palette AMETHYST = new Palette(
        "amethyst", "Dusk Amethyst", "#7C3AED", "#ECE4F0", "#171322", "#EDE9FE", "#2E1065"
    );
    public static final Palette OBSIDIAN = new Palette(
        "obsidian", "Obsidian Minimal", "#3B82F6", "#E2E8F0", "#0B0E14", "#DBEAFE", "#1E293B"
    );

    public static final List<Palette> ALL_PALETTES = List.of(
        OCEAN, MATCHA, NORDIC, TERRACOTTA, AMETHYST, OBSIDIAN
    );

    private static final String DARK_CLASS = "dark";
    private static boolean dark;
    private static Palette currentPalette = OCEAN;
    private static String customAccent = null;

    private ThemeManager() {
    }

    /** Initializes theme preferences for a logged-in user. */
    public static void initFromUser(int userId) {
        SettingsService s = new SettingsService();
        dark = "dark".equalsIgnoreCase(s.getString(userId, SettingsService.THEME, "light"));
        String paletteId = s.getString(userId, SettingsService.THEME_PALETTE, "ocean");
        currentPalette = findPalette(paletteId);
        customAccent = s.getString(userId, SettingsService.THEME_CUSTOM_ACCENT, null);
        if (customAccent != null && customAccent.isBlank()) {
            customAccent = null;
        }
        apply(dark);
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

    public static Palette getCurrentPalette() {
        return currentPalette;
    }

    public static String getCustomAccent() {
        return customAccent;
    }

    public static void setPalette(Palette palette) {
        currentPalette = palette != null ? palette : OCEAN;
        customAccent = null;
        persistCurrent();
    }

    public static void setCustomAccent(String hexColor) {
        customAccent = hexColor;
        persistCurrent();
    }

    /** Resets the theme to the factory default Ocean palette and clears custom accents. */
    public static void resetToDefault() {
        currentPalette = OCEAN;
        customAccent = null;
        dark = false;
        apply(false);
        persistCurrent();
    }

    private static void persistCurrent() {
        var user = SessionContext.getInstance().getCurrentUser();
        if (user != null) {
            SettingsService s = new SettingsService();
            s.set(user.getId(), SettingsService.THEME, dark ? "dark" : "light");
            s.set(user.getId(), SettingsService.THEME_PALETTE, currentPalette.id());
            s.set(user.getId(), SettingsService.THEME_CUSTOM_ACCENT, customAccent != null ? customAccent : "");
        }
    }

    public static Palette findPalette(String id) {
        if (id == null) return OCEAN;
        for (Palette p : ALL_PALETTES) {
            if (p.id().equalsIgnoreCase(id)) {
                return p;
            }
        }
        return OCEAN;
    }

    /** Decorates a root pane with the dark style class and dynamic CSS color variables. */
    public static void decorate(Parent root) {
        if (root == null) return;
        root.getStyleClass().remove(DARK_CLASS);
        if (dark) {
            root.getStyleClass().add(DARK_CLASS);
        }
        root.setStyle(buildStyleVars());
    }

    /** Generates dynamic CSS variables that override AtlantaFX and Ekagra defaults. */
    public static String buildStyleVars() {
        String primary = customAccent != null ? customAccent : currentPalette.primaryHex();
        String canvas = dark ? currentPalette.darkCanvas() : currentPalette.lightCanvas();
        String subtle = customAccent != null
            ? deriveSubtle(customAccent, dark)
            : (dark ? currentPalette.darkSubtle() : currentPalette.lightSubtle());
        String muted = customAccent != null
            ? deriveMuted(customAccent, dark)
            : (dark ? deriveMuted(currentPalette.primaryHex(), true) : deriveMuted(currentPalette.primaryHex(), false));
        String fg = deriveFg(primary, dark);

        return String.format(
            "-color-accent-emphasis: %s; " +
            "-color-accent-fg: %s; " +
            "-color-accent-subtle: %s; " +
            "-color-accent-muted: %s; " +
            "-ek-blue: %s; " +
            "-ek-canvas: %s; ",
            primary, fg, subtle, muted, primary, canvas
        );
    }

    private static String deriveSubtle(String hex, boolean isDark) {
        try {
            Color c = Color.web(hex);
            Color bg = isDark ? Color.web("#17222C") : Color.web("#FBFAF7");
            return toHex(bg.interpolate(c, isDark ? 0.22 : 0.16));
        } catch (Exception e) {
            return isDark ? "#1A3247" : "#DCE8F2";
        }
    }

    private static String deriveMuted(String hex, boolean isDark) {
        try {
            Color c = Color.web(hex);
            Color bg = isDark ? Color.web("#17222C") : Color.web("#FBFAF7");
            return toHex(bg.interpolate(c, isDark ? 0.40 : 0.35));
        } catch (Exception e) {
            return isDark ? "#24496A" : "#A9C6DD";
        }
    }

    private static String deriveFg(String hex, boolean isDark) {
        try {
            Color c = Color.web(hex);
            if (isDark) {
                return toHex(c.deriveColor(0, 0.85, 1.35, 1.0));
            } else {
                return toHex(c.deriveColor(0, 1.05, 0.92, 1.0));
            }
        } catch (Exception e) {
            return hex;
        }
    }

    private static String toHex(Color c) {
        int r = (int) Math.round(Math.clamp(c.getRed() * 255.0, 0, 255));
        int g = (int) Math.round(Math.clamp(c.getGreen() * 255.0, 0, 255));
        int b = (int) Math.round(Math.clamp(c.getBlue() * 255.0, 0, 255));
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
