package com.ekagra.model;

import java.time.LocalDateTime;

/** A daily or weekly target (FR-27). */
public class Goal {

    /** What the goal measures. "MAX_" goals are limits: staying under the target counts as success. */
    public enum Type {
        STUDY_TIME("Focus time", "min", false, "fth-clock"),
        FOCUS_SESSIONS("Focus sessions", "sessions", false, "fth-target"),
        TASKS_DONE("Tasks completed", "tasks", false, "fth-check-square"),
        MAX_DISTRACTING("Distracting apps under", "min", true, "fth-slash"),
        MAX_SCREEN_TIME("Screen time under", "min", true, "fth-monitor");

        private final String label;
        private final String unit;
        private final boolean limit;
        private final String icon;

        Type(String label, String unit, boolean limit, String icon) {
            this.label = label;
            this.unit = unit;
            this.limit = limit;
            this.icon = icon;
        }

        public String getLabel() { return label; }
        public String getUnit() { return unit; }
        public boolean isLimit() { return limit; }
        public String getIcon() { return icon; }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum Period {
        DAILY("Today", "Daily"), WEEKLY("This week", "Weekly");

        private final String label;
        private final String adjective;

        Period(String label, String adjective) {
            this.label = label;
            this.adjective = adjective;
        }

        public String getLabel() { return label; }

        @Override
        public String toString() {
            return adjective;
        }
    }

    private int id;
    private int userId;
    private String title;
    private Type type;
    private Period period;
    private int targetValue;
    private boolean active = true;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Goal() {
    }

    public Goal(int userId, Type type, Period period, int targetValue) {
        this.userId = userId;
        this.type = type;
        this.period = period;
        this.targetValue = targetValue;
        this.title = defaultTitle(type, period, targetValue);
    }

    public static String defaultTitle(Type type, Period period, int target) {
        String amount = type.getUnit().equals("min") ? com.ekagra.util.Formats.minutes(target) : target + " " + type.getUnit();
        String when = period == Period.DAILY ? "a day" : "a week";
        return switch (type) {
            case STUDY_TIME -> "Focus for " + amount + " " + when;
            case FOCUS_SESSIONS -> "Complete " + amount + " " + when;
            case TASKS_DONE -> "Finish " + amount + " " + when;
            case MAX_DISTRACTING -> "Keep distracting apps under " + amount + " " + when;
            case MAX_SCREEN_TIME -> "Keep screen time under " + amount + " " + when;
        };
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public Period getPeriod() { return period; }
    public void setPeriod(Period period) { this.period = period; }

    public int getTargetValue() { return targetValue; }
    public void setTargetValue(int targetValue) { this.targetValue = targetValue; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
