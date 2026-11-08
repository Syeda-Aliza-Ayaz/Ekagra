package com.ekagra.pattern.state;

/** Shared behaviour of the two "clock is ticking" states (focus and break). */
public abstract class RunningState implements TimerState {

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public void start(PomodoroTimer timer) {
        // already running
    }

    @Override
    public void pause(PomodoroTimer timer) {
        timer.setState(new PausedState(this));
    }

    @Override
    public void resume(PomodoroTimer timer) {
        // not paused
    }

    @Override
    public void stop(PomodoroTimer timer) {
        timer.finishSession(false);
        timer.prepare(timer.getCurrentType());
        timer.setState(new IdleState());
    }

    @Override
    public void tick(PomodoroTimer timer) {
        timer.countDownOneSecond();
        if (timer.getRemainingSeconds() <= 0) {
            onTimeUp(timer);
        }
    }

    /** What happens when the countdown reaches zero. */
    protected abstract void onTimeUp(PomodoroTimer timer);
}
