package com.ekagra.pattern.state;

import com.ekagra.model.FocusSession;
import com.ekagra.model.SessionType;
import com.ekagra.model.Task;
import com.ekagra.pattern.observer.TimerListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * The Pomodoro timer: CONTEXT of the State pattern and SUBJECT of the Observer pattern.
 * It has no JavaFX code, so it can be unit-tested by calling {@link #tick()} directly.
 */
public class PomodoroTimer {

    private final int userId;
    private final Supplier<LocalDateTime> clock;
    private final List<TimerListener> listeners = new CopyOnWriteArrayList<>();

    private TimerState state = new IdleState();
    private TimerConfig config;
    private SessionType currentType = SessionType.FOCUS;
    private int plannedSeconds;
    private int remainingSeconds;
    private int elapsedSeconds;
    private int completedFocusCount;
    private Task task;
    private FocusSession currentSession;

    public PomodoroTimer(int userId, TimerConfig config) {
        this(userId, config, LocalDateTime::now);
    }

    public PomodoroTimer(int userId, TimerConfig config, Supplier<LocalDateTime> clock) {
        this.userId = userId;
        this.config = config;
        this.clock = clock;
        prepare(SessionType.FOCUS);
    }

    // ---------- public controls (delegate to the current state) ----------

    public void start()  { state.start(this); }
    public void pause()  { state.pause(this); }
    public void resume() { state.resume(this); }
    public void stop()   { state.stop(this); }
    public void skip()   { state.skip(this); }
    public void tick()   { state.tick(this); }

    /** Choose focus / short break / long break. Only allowed while idle. */
    public void selectType(SessionType type) {
        if (state instanceof IdleState) {
            prepare(type);
            notifyStateChanged();
        }
    }

    public void setConfig(TimerConfig newConfig) {
        this.config = newConfig;
        if (state instanceof IdleState) {
            prepare(currentType);
            notifyStateChanged();
        }
    }

    public void setTask(Task task) {
        this.task = task;
        if (currentSession != null && currentSession.getType() == SessionType.FOCUS) {
            currentSession.setTaskId(task == null ? null : task.getId());
        }
        notifyStateChanged();
    }

    /** Called by Focus Guard (Phase 3) when a distracting app is opened during focus. */
    public void recordDistraction() {
        if (currentSession != null && currentSession.getType() == SessionType.FOCUS) {
            currentSession.addDistraction();
            notifyStateChanged();
        }
    }

    /** Undo the last distraction (Focus Guard false alarm: "it's for study"). */
    public void forgiveDistraction() {
        if (currentSession != null && currentSession.getDistractions() > 0) {
            currentSession.setDistractions(currentSession.getDistractions() - 1);
            notifyStateChanged();
        }
    }

    /** True only while a focus session is actively counting down (not paused, not a break). */
    public boolean isFocusing() {
        return state instanceof FocusState;
    }

    // ---------- listeners ----------

    public void addListener(TimerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TimerListener listener) {
        listeners.remove(listener);
    }

    // ---------- used by the state classes ----------

    void setState(TimerState newState) {
        this.state = newState;
        notifyStateChanged();
    }

    void prepare(SessionType type) {
        currentType = type;
        plannedSeconds = config.minutesFor(type) * 60;
        remainingSeconds = plannedSeconds;
        elapsedSeconds = 0;
    }

    void beginSession() {
        Integer taskId = (currentType == SessionType.FOCUS && task != null) ? task.getId() : null;
        currentSession = new FocusSession(userId, taskId, currentType, config.minutesFor(currentType), clock.get());
        elapsedSeconds = 0;
    }

    void countDownOneSecond() {
        remainingSeconds--;
        elapsedSeconds++;
        listeners.forEach(l -> l.onTick(this));
    }

    void finishSession(boolean completed) {
        if (currentSession == null) {
            return;
        }
        FocusSession finished = currentSession;
        currentSession = null;
        finished.setEndedAt(clock.get());
        finished.setActualSec(elapsedSeconds);
        finished.setCompleted(completed);
        listeners.forEach(l -> l.onSessionFinished(this, finished));
    }

    void incrementCompletedFocus() {
        completedFocusCount++;
    }

    boolean isLongBreakDue() {
        return completedFocusCount > 0 && completedFocusCount % config.sessionsBeforeLongBreak() == 0;
    }

    private void notifyStateChanged() {
        listeners.forEach(l -> l.onStateChanged(this));
    }

    // ---------- read-only view for the UI ----------

    public TimerState getState() { return state; }
    public TimerConfig getConfig() { return config; }
    public SessionType getCurrentType() { return currentType; }
    public int getPlannedSeconds() { return plannedSeconds; }
    public int getRemainingSeconds() { return remainingSeconds; }
    public int getCompletedFocusCount() { return completedFocusCount; }
    public Task getTask() { return task; }
    public FocusSession getCurrentSession() { return currentSession; }
    public boolean isActive() { return !(state instanceof IdleState); }

    /** 0..1 how much of the current session has passed. */
    public double getProgress() {
        return plannedSeconds == 0 ? 0 : 1.0 - (double) remainingSeconds / plannedSeconds;
    }

    /** Position within the current cycle, e.g. 2 of 4 before the long break. */
    public int getCyclePosition() {
        return completedFocusCount % config.sessionsBeforeLongBreak();
    }

    public String getFormattedRemaining() {
        int s = Math.max(0, remainingSeconds);
        return String.format("%02d:%02d", s / 60, s % 60);
    }
}
