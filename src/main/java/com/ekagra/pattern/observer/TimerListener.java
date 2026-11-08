package com.ekagra.pattern.observer;

import com.ekagra.model.FocusSession;
import com.ekagra.pattern.state.PomodoroTimer;

/**
 * OBSERVER pattern: anything that wants to react to the timer (the Focus screen, the top-bar
 * mini timer, the service that saves sessions) registers as a listener. The timer doesn't know
 * who is listening.
 */
public interface TimerListener {

    default void onTick(PomodoroTimer timer) {
    }

    default void onStateChanged(PomodoroTimer timer) {
    }

    /** A focus or break session ended (completed, stopped or skipped). */
    default void onSessionFinished(PomodoroTimer timer, FocusSession session) {
    }
}
