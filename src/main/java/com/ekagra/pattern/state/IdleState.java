package com.ekagra.pattern.state;

import com.ekagra.model.SessionType;

/** Nothing running. Only "start" does anything. */
public class IdleState implements TimerState {

    @Override
    public String getName() {
        return "Ready";
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void start(PomodoroTimer timer) {
        timer.beginSession();
        timer.setState(timer.getCurrentType() == SessionType.FOCUS ? new FocusState() : new BreakState());
    }

    @Override
    public void pause(PomodoroTimer timer) {
        // nothing to pause
    }

    @Override
    public void resume(PomodoroTimer timer) {
        // nothing to resume
    }

    @Override
    public void stop(PomodoroTimer timer) {
        // already stopped
    }

    @Override
    public void skip(PomodoroTimer timer) {
        // nothing to skip
    }

    @Override
    public void tick(PomodoroTimer timer) {
        // time does not pass while idle
    }
}
