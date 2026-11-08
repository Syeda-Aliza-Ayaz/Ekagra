package com.ekagra.tracking;

import java.util.Optional;

/**
 * Source of "what is the user doing right now". The real implementation asks Windows;
 * tests use a fake one. (Dependency inversion: the tracker doesn't care where data comes from.)
 */
public interface WindowProvider {

    /** The foreground window, or empty if none (e.g. screen locked). */
    Optional<WindowInfo> foregroundWindow();

    /** Milliseconds since the last keyboard or mouse input. */
    long idleMillis();

    /** Whether this provider works on the current machine. */
    default boolean isAvailable() {
        return true;
    }
}
