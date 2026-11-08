package com.ekagra.pattern.state;

import com.ekagra.model.SessionType;

/** A short or long break is counting down. Afterwards the timer waits for the next focus session. */
public class BreakState extends RunningState {

    @Override
    public String getName() {
        return "On a break";
    }

    @Override
    public void skip(PomodoroTimer timer) {
        timer.finishSession(false);
        timer.prepare(SessionType.FOCUS);
        timer.setState(new IdleState());
    }

    @Override
    protected void onTimeUp(PomodoroTimer timer) {
        timer.finishSession(true);
        timer.prepare(SessionType.FOCUS);
        timer.setState(new IdleState());
    }
}
