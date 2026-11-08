package com.ekagra.model;

/** How an app or website is classified (FR-16). */
public enum Category {
    PRODUCTIVE("Productive", "fth-trending-up"),
    NEUTRAL("Neutral", "fth-minus-circle"),
    DISTRACTING("Distracting", "fth-alert-octagon");

    private final String label;
    private final String icon;

    Category(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }

    /** CSS class used for this category's colour. */
    public String styleClass() {
        return "cat-" + name().toLowerCase();
    }

    @Override
    public String toString() {
        return label;
    }
}
