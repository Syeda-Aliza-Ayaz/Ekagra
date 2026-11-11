package com.ekagra.ui.tour;

import com.ekagra.app.Screen;

/** A single presentation milestone in the interactive guided demo tour. */
public record TourStep(
    Screen screen,
    String title,
    String description,
    Runnable action
) {
    public TourStep(Screen screen, String title, String description) {
        this(screen, title, description, null);
    }
}
