package com.ekagra.service;

import com.ekagra.dao.SessionDao;
import com.ekagra.dao.TaskDao;
import com.ekagra.model.FocusSession;
import com.ekagra.model.SessionType;
import com.ekagra.pattern.observer.AppEvent;
import com.ekagra.pattern.observer.AppEventBus;
import com.ekagra.pattern.observer.TimerListener;
import com.ekagra.pattern.state.PomodoroTimer;
import com.ekagra.pattern.state.TimerConfig;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * Owns the one Pomodoro timer of the logged-in student, drives it every second,
 * and saves finished sessions (FR-10 .. FR-14).
 * The timer keeps running while the user switches screens.
 */
public final class FocusService implements TimerListener {

    /** Stopped sessions shorter than this are not saved (accidental clicks). */
    public static final int MIN_SAVED_SECONDS = 60;

    private static FocusService instance;

    private final int userId;
    private final PomodoroTimer timer;
    private final Timeline ticker;
    private final SessionDao sessionDao = new SessionDao();
    private final TaskDao taskDao = new TaskDao();
    private final SettingsService settings = new SettingsService();
    private Consumer<FocusSession> onFocusCompleted = s -> { };

    private FocusService(int userId) {
        this.userId = userId;
        this.timer = new PomodoroTimer(userId, loadConfig());
        this.timer.addListener(this);                 // first listener: saves the session before the UI hears about it
        this.ticker = new Timeline(new KeyFrame(Duration.seconds(1), e -> timer.tick()));
        this.ticker.setCycleCount(Animation.INDEFINITE);
        this.ticker.play();
    }

    /** One instance per logged-in student. */
    public static synchronized FocusService get() {
        int uid = SessionContext.getInstance().getCurrentUser().getId();
        if (instance == null || instance.userId != uid) {
            reset();
            instance = new FocusService(uid);
        }
        return instance;
    }

    public static synchronized boolean isStarted() {
        return instance != null;
    }

    /** Called on logout / exit: saves a running session and stops the clock. */
    public static synchronized void reset() {
        if (instance != null) {
            instance.timer.stop();
            instance.ticker.stop();
            instance = null;
        }
    }

    public PomodoroTimer getTimer() {
        return timer;
    }

    /** Hook for gamification (XP/badges) when a focus session completes. */
    public void setOnFocusCompleted(Consumer<FocusSession> listener) {
        this.onFocusCompleted = listener == null ? s -> { } : listener;
    }

    // ---------- persistence (Observer callback) ----------

    @Override
    public void onSessionFinished(PomodoroTimer t, FocusSession session) {
        boolean worthSaving = session.isCompleted() || session.getActualSec() >= MIN_SAVED_SECONDS;
        if (!worthSaving) {
            return;
        }
        sessionDao.insert(session);
        if (session.getType() == SessionType.FOCUS) {
            if (session.getTaskId() != null && session.getActualMinutes() > 0) {
                taskDao.addSpentMinutes(session.getTaskId(), session.getActualMinutes());
                if (t.getTask() != null && t.getTask().getId() == session.getTaskId()) {
                    t.getTask().setSpentMin(t.getTask().getSpentMin() + session.getActualMinutes());
                }
            }
            if (session.isCompleted()) {
                onFocusCompleted.accept(session);
                AppEventBus.publish(new AppEvent.FocusCompleted(userId, session));
            }
        }
    }

    public void rateSession(FocusSession session, int rating) {
        if (rating < 1 || rating > 5) {
            throw new ValidationException("Rating must be between 1 and 5.");
        }
        session.setFocusRating(rating);
        if (session.getId() > 0) {
            sessionDao.setRating(session.getId(), rating);
        }
    }

    // ---------- config ----------

    public TimerConfig loadConfig() {
        try {
            return new TimerConfig(
                settings.getInt(userId, SettingsService.FOCUS_MIN, 25),
                settings.getInt(userId, SettingsService.SHORT_BREAK_MIN, 5),
                settings.getInt(userId, SettingsService.LONG_BREAK_MIN, 15),
                settings.getInt(userId, SettingsService.SESSIONS_BEFORE_LONG, 4),
                settings.getBoolean(userId, SettingsService.AUTO_START_BREAKS, true));
        } catch (IllegalArgumentException e) {
            return TimerConfig.DEFAULT;
        }
    }

    public void saveConfig(TimerConfig config) {
        settings.set(userId, SettingsService.FOCUS_MIN, config.focusMin());
        settings.set(userId, SettingsService.SHORT_BREAK_MIN, config.shortBreakMin());
        settings.set(userId, SettingsService.LONG_BREAK_MIN, config.longBreakMin());
        settings.set(userId, SettingsService.SESSIONS_BEFORE_LONG, config.sessionsBeforeLongBreak());
        settings.set(userId, SettingsService.AUTO_START_BREAKS, config.autoStartBreaks());
        timer.setConfig(config);
    }

    // ---------- history & stats ----------

    public List<FocusSession> getRecentSessions(int limit) {
        return sessionDao.findRecent(userId, limit);
    }

    public SessionDao.FocusStats getTodayStats() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return sessionDao.focusStats(userId, start, start.plusDays(1));
    }
}
