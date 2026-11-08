package com.ekagra.pattern.state;

import com.ekagra.model.SessionType;

/** Pomodoro lengths chosen by the user (FR-10). */
public record TimerConfig(int focusMin, int shortBreakMin, int longBreakMin, int sessionsBeforeLongBreak,
                          boolean autoStartBreaks) {

    public static final TimerConfig DEFAULT = new TimerConfig(25, 5, 15, 4, true);

    public TimerConfig {
        if (focusMin < 1 || shortBreakMin < 1 || longBreakMin < 1 || sessionsBeforeLongBreak < 1) {
            throw new IllegalArgumentException("Timer lengths must be at least 1");
        }
    }

    public int minutesFor(SessionType type) {
        return switch (type) {
            case FOCUS -> focusMin;
            case SHORT_BREAK -> shortBreakMin;
            case LONG_BREAK -> longBreakMin;
        };
    }
}
