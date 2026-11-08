package com.ekagra.service;

import com.ekagra.dao.ActivityDao;
import com.ekagra.dao.AppRuleDao;
import com.ekagra.model.ActivityLog;
import com.ekagra.model.Category;
import com.ekagra.pattern.observer.ActivityListener;
import com.ekagra.tracking.ActivitySnapshot;
import com.ekagra.tracking.ActivityTracker;
import com.ekagra.tracking.AppClassifier;
import com.ekagra.tracking.Win32WindowProvider;
import com.ekagra.tracking.WindowProvider;
import javafx.application.Platform;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs the {@link ActivityTracker} on a background thread every 2 seconds (NFR-1: never blocks the UI),
 * forwards its events to the JavaFX thread, and answers "how was today spent" questions.
 * One instance per logged-in student.
 */
public final class TrackingService {

    public static final int SAMPLE_SECONDS = 2;

    private static TrackingService instance;

    private final int userId;
    private final WindowProvider provider;
    private final ActivityTracker tracker;
    private final ActivityDao activityDao = new ActivityDao();
    private final ScheduledExecutorService scheduler;
    private final List<ActivityListener> uiListeners = new CopyOnWriteArrayList<>();
    private final FocusGuard focusGuard;
    private final WellbeingMonitor wellbeingMonitor;

    private TrackingService(int userId) {
        this.userId = userId;
        SettingsService settings = new SettingsService();
        this.provider = new Win32WindowProvider();
        AppRuleDao ruleDao = new AppRuleDao();
        this.tracker = new ActivityTracker(userId, provider, new AppClassifier(ruleDao::findAll), activityDao,
            LocalDateTime::now, Duration.ofMinutes(settings.getInt(userId, SettingsService.IDLE_THRESHOLD_MIN, 5)));

        // bridge: tracker events arrive on the background thread -> re-deliver on the UI thread
        tracker.addListener(new ActivityListener() {
            @Override
            public void onSample(ActivitySnapshot s) {
                Platform.runLater(() -> uiListeners.forEach(l -> l.onSample(s)));
            }

            @Override
            public void onIdleChanged(boolean idle) {
                Platform.runLater(() -> uiListeners.forEach(l -> l.onIdleChanged(idle)));
            }

            @Override
            public void onTrackingChanged(boolean active) {
                Platform.runLater(() -> uiListeners.forEach(l -> l.onTrackingChanged(active)));
            }
        });

        focusGuard = new FocusGuard(FocusService.get().getTimer(),
            () -> settings.getBoolean(userId, SettingsService.FOCUS_GUARD, true));
        uiListeners.add(focusGuard);
        wellbeingMonitor = new WellbeingMonitor(userId, new WellbeingService(userId), FocusService.get().getTimer());
        uiListeners.add(wellbeingMonitor);

        tracker.setPaused(!settings.getBoolean(userId, SettingsService.AUTO_TRACK, true));

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ekagra-activity-tracker");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::safeSample, 1, SAMPLE_SECONDS, TimeUnit.SECONDS);
    }

    private void safeSample() {
        try {
            tracker.sample();
        } catch (RuntimeException e) {
            // never let one bad sample (e.g. DB busy) stop the schedule
            System.err.println("Tracker sample failed: " + e.getMessage());
        }
    }

    public static synchronized TrackingService get() {
        int uid = SessionContext.getInstance().getCurrentUser().getId();
        if (instance == null || instance.userId != uid) {
            reset();
            instance = new TrackingService(uid);
        }
        return instance;
    }

    public static synchronized boolean isStarted() {
        return instance != null;
    }

    /** Stops tracking and saves the open activity row (logout / exit). */
    public static synchronized void reset() {
        if (instance != null) {
            instance.scheduler.shutdownNow();
            instance.tracker.closeSegment();
            instance = null;
        }
    }

    // ---------- control ----------

    public boolean isAvailable() {
        return provider.isAvailable();
    }

    public boolean isPaused() {
        return tracker.isPaused();
    }

    public boolean isIdle() {
        return tracker.isIdle();
    }

    public void setPaused(boolean paused) {
        tracker.setPaused(paused);
    }

    public void setIdleMinutes(int minutes) {
        tracker.setIdleThreshold(Duration.ofMinutes(minutes));
    }

    public void reloadRules() {
        tracker.getClassifier().reload();
    }

    public void addListener(ActivityListener l) {
        uiListeners.add(l);
    }

    public void removeListener(ActivityListener l) {
        uiListeners.remove(l);
    }

    public WellbeingMonitor getWellbeingMonitor() {
        return wellbeingMonitor;
    }

    // ---------- statistics ----------

    public Map<Category, Integer> secondsByCategory(LocalDate day) {
        return activityDao.secondsByCategory(userId, day.atStartOfDay(), day.plusDays(1).atStartOfDay());
    }

    public List<ActivityDao.AppUsage> topApps(LocalDate day, int limit) {
        return activityDao.topApps(userId, day.atStartOfDay(), day.plusDays(1).atStartOfDay(), limit);
    }

    public List<ActivityDao.HourUsage> hourly(LocalDate day) {
        return activityDao.hourly(userId, day.atStartOfDay(), day.plusDays(1).atStartOfDay());
    }

    public List<ActivityLog> recent(int limit) {
        return activityDao.findRecent(userId, limit);
    }
}
