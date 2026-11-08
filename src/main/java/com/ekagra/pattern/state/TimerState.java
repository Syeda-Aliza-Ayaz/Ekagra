package com.ekagra.pattern.state;

/**
 * STATE pattern: what each timer button does depends on the timer's current state.
 * The {@link PomodoroTimer} (context) simply forwards every call to its current state object.
 *
 * <pre>
 *   Idle --start--> Focus --(time up)--> Break --(time up)--> Idle
 *              \--> Break (if a break was selected)
 *   Focus/Break --pause--> Paused --resume--> (previous state)
 *   any running --stop--> Idle
 * </pre>
 */
public interface TimerState {

    String getName();

    boolean isRunning();

    default boolean isPaused() {
        return false;
    }

    void start(PomodoroTimer timer);

    void pause(PomodoroTimer timer);

    void resume(PomodoroTimer timer);

    void stop(PomodoroTimer timer);

    /** Skip the rest of the current session (mainly used to skip a break). */
    void skip(PomodoroTimer timer);

    /** Called once per second. */
    void tick(PomodoroTimer timer);
}
