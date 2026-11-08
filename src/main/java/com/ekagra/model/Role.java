package com.ekagra.model;

public enum Role {
    STUDENT("Student"),
    MENTOR("Mentor");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
