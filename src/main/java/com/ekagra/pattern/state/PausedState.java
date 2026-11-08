package com.ekagra.pattern.state;

/** Clock frozen. Remembers which state to return to on resume. */
public class PausedState implements TimerState {

    private final RunningState previous;

    public PausedState(RunningState previous) {
        this.previous = previous;
    }

    @Override
    public String getName() {
        return "Paused";
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public boolean isPaused() {
        return true;
    }

    @Override
    public void start(PomodoroTimer timer) {
        resume(timer);
    }

    @Override
    public void pause(PomodoroTimer timer) {
        // already paused
    }

    @Override
    public void resume(PomodoroTimer timer) {
        timer.setState(previous);
    }

    @Override
    public void stop(PomodoroTimer timer) {
        previous.stop(timer);
    }

    @Override
    public void skip(PomodoroTimer timer) {
        previous.skip(timer);
    }

    @Override
    public void tick(PomodoroTimer timer) {
        // time does not pass while paused
    }

    public RunningState getPrevious() {
        return previous;
    }
}
