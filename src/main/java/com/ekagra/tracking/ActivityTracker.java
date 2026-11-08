package com.ekagra.tracking;

import com.ekagra.dao.ActivityDao;
import com.ekagra.model.ActivityLog;
import com.ekagra.model.Category;
import com.ekagra.pattern.observer.ActivityListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Samples the active window every few seconds and turns samples into {@link ActivityLog} rows (FR-15, FR-17).
 * Consecutive samples of the same app + category are merged into one row, so the database holds
 * "Chrome, Distracting, 10:02–10:19" instead of hundreds of 2-second rows.
 *
 * Contains no threading or JavaFX code: {@link #sample()} is called by a scheduler in production
 * and directly by unit tests. SUBJECT of the Observer pattern ({@link ActivityListener}).
 */
public class ActivityTracker {

    /** How often (in samples) the open row is written to the DB while it keeps growing. */
    private static final int SAVE_EVERY_N_SAMPLES = 5;
    /** A gap longer than this between samples (e.g. laptop slept) starts a new row. */
    private static final Duration MAX_GAP = Duration.ofSeconds(15);

    private final int userId;
    private final WindowProvider provider;
    private final AppClassifier classifier;
    private final ActivityDao dao;
    private final Supplier<LocalDateTime> clock;
    private final List<ActivityListener> listeners = new CopyOnWriteArrayList<>();

    private volatile long idleThresholdMillis;
    private volatile boolean paused;
    private boolean idle;
    private ActivityLog current;
    private LocalDateTime lastSampleAt;
    private int unsavedSamples;

    public ActivityTracker(int userId, WindowProvider provider, AppClassifier classifier, ActivityDao dao,
                           Supplier<LocalDateTime> clock, Duration idleThreshold) {
        this.userId = userId;
        this.provider = provider;
        this.classifier = classifier;
        this.dao = dao;
        this.clock = clock;
        this.idleThresholdMillis = idleThreshold.toMillis();
    }

    /** One tracking step. Safe to call from a background thread. */
    public synchronized void sample() {
        LocalDateTime now = clock.get();
        if (paused) {
            closeSegment();
            return;
        }

        long idleMs = provider.idleMillis();
        if (idleMs >= idleThresholdMillis) {
            if (!idle) {
                idle = true;
                closeSegment();          // time after the last input is not counted
                listeners.forEach(l -> l.onIdleChanged(true));
            }
            lastSampleAt = now;
            return;
        }
        if (idle) {
            idle = false;
            listeners.forEach(l -> l.onIdleChanged(false));
        }

        Optional<WindowInfo> maybeWindow = provider.foregroundWindow();
        if (maybeWindow.isEmpty()) {
            closeSegment();
            lastSampleAt = now;
            return;
        }
        WindowInfo window = maybeWindow.get();
        Category category = classifier.classify(window);

        boolean continues = current != null
            && current.getAppName().equalsIgnoreCase(window.processName())
            && current.getCategory() == category
            && lastSampleAt != null
            && Duration.between(lastSampleAt, now).compareTo(MAX_GAP) <= 0;

        boolean switched;
        if (continues) {
            current.setWindowTitle(window.title());
            current.extendTo(now);
            if (++unsavedSamples >= SAVE_EVERY_N_SAMPLES) {
                dao.update(current);
                unsavedSamples = 0;
            }
            switched = false;
        } else {
            closeSegment();
            current = new ActivityLog(userId, window.processName(), window.title(), category, now);
            dao.insert(current);
            unsavedSamples = 0;
            switched = true;
        }
        lastSampleAt = now;

        ActivitySnapshot snapshot = new ActivitySnapshot(window, category, current, switched);
        listeners.forEach(l -> l.onSample(snapshot));
    }

    /** Saves the open row; called when switching apps, going idle, pausing or shutting down. */
    public synchronized void closeSegment() {
        if (current != null) {
            dao.update(current);
            current = null;
            unsavedSamples = 0;
        }
    }

    public synchronized void setPaused(boolean paused) {
        if (this.paused == paused) {
            return;
        }
        this.paused = paused;
        if (paused) {
            closeSegment();
        }
        listeners.forEach(l -> l.onTrackingChanged(!paused));
    }

    public void setIdleThreshold(Duration threshold) {
        this.idleThresholdMillis = threshold.toMillis();
    }

    public boolean isPaused() {
        return paused;
    }

    public synchronized boolean isIdle() {
        return idle;
    }

    public synchronized Optional<ActivityLog> getCurrentSegment() {
        return Optional.ofNullable(current);
    }

    public AppClassifier getClassifier() {
        return classifier;
    }

    public void addListener(ActivityListener l) {
        listeners.add(l);
    }

    public void removeListener(ActivityListener l) {
        listeners.remove(l);
    }
}
