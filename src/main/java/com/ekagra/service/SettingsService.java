package com.ekagra.service;

import com.ekagra.dao.SettingsDao;

/** Typed access to per-user preferences (FR-41). Keys are defined here so they're never mistyped. */
public class SettingsService {

    public static final String THEME = "theme";                       // "light" | "dark"
    public static final String FOCUS_MIN = "focus.minutes";
    public static final String SHORT_BREAK_MIN = "focus.shortBreak";
    public static final String LONG_BREAK_MIN = "focus.longBreak";
    public static final String SESSIONS_BEFORE_LONG = "focus.sessionsBeforeLong";
    public static final String AUTO_START_BREAKS = "focus.autoStartBreaks";
    public static final String DAILY_SCREEN_LIMIT_MIN = "wellbeing.dailyLimit";
    public static final String BREAK_REMINDER_MIN = "wellbeing.breakEvery";
    public static final String EYE_REST = "wellbeing.eyeRest";
    public static final String REMINDERS = "wellbeing.reminders";
    public static final String IDLE_THRESHOLD_MIN = "tracking.idleMinutes";
    public static final String AUTO_TRACK = "tracking.autoStart";
    public static final String FOCUS_GUARD = "focus.guard";
    public static final String POPUPS = "notifications.popups";
    public static final String SCORE_STRATEGY = "score.strategy";

    private final SettingsDao dao;

    public SettingsService() {
        this(new SettingsDao());
    }

    public SettingsService(SettingsDao dao) {
        this.dao = dao;
    }

    public String getString(int userId, String key, String defaultValue) {
        return dao.get(userId, key).orElse(defaultValue);
    }

    public int getInt(int userId, String key, int defaultValue) {
        try {
            return dao.get(userId, key).map(Integer::parseInt).orElse(defaultValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(int userId, String key, boolean defaultValue) {
        return dao.get(userId, key).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    public void set(int userId, String key, Object value) {
        dao.set(userId, key, String.valueOf(value));
    }
}
