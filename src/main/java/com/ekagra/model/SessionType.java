package com.ekagra.model;

public enum SessionType {
    FOCUS("Focus"),
    SHORT_BREAK("Short break"),
    LONG_BREAK("Long break");

    private final String label;

    SessionType(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }

    public boolean isBreak() {
        return this != FOCUS;
    }

    @Override
    public String toString() {
        return label;
    }
}
