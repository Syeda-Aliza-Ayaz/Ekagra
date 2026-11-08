package com.ekagra.model;

public enum Priority {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4);

    private final String label;
    private final int weight;

    Priority(String label, int weight) {
        this.label = label;
        this.weight = weight;
    }

    public String getLabel() { return label; }

    /** Higher = more important; used for sorting. */
    public int getWeight() { return weight; }

    /** CSS class used for the coloured chip. */
    public String styleClass() {
        return "chip-" + name().toLowerCase();
    }

    @Override
    public String toString() {
        return label;
    }
}
