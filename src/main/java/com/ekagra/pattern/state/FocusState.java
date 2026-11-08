package com.ekagra.pattern.state;

import com.ekagra.model.SessionType;

/** A focus session is counting down. When it ends, the right kind of break starts. */
public class FocusState extends RunningState {

    @Override
    public String getName() {
        return "Focusing";
    }

    @Override
    public void skip(PomodoroTimer timer) {
        stop(timer);
    }

    @Override
    protected void onTimeUp(PomodoroTimer timer) {
        timer.finishSession(true);
        timer.incrementCompletedFocus();

        SessionType next = timer.isLongBreakDue() ? SessionType.LONG_BREAK : SessionType.SHORT_BREAK;
        timer.prepare(next);
        if (timer.getConfig().autoStartBreaks()) {
            timer.beginSession();
            timer.setState(new BreakState());
        } else {
            timer.setState(new IdleState());
        }
    }
}
