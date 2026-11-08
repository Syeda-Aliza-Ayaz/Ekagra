package com.ekagra.pattern.observer;

import com.ekagra.tracking.ActivitySnapshot;

/**
 * OBSERVER pattern: notified by the activity tracker. Used by the Activity screen (live view),
 * the top-bar tracking indicator and Focus Guard.
 */
public interface ActivityListener {

    /** A new 2-second sample was recorded. */
    default void onSample(ActivitySnapshot snapshot) {
    }

    /** The user became idle (no input for N minutes) or came back. */
    default void onIdleChanged(boolean idle) {
    }

    /** Tracking was paused (privacy mode) or resumed. */
    default void onTrackingChanged(boolean active) {
    }
}
