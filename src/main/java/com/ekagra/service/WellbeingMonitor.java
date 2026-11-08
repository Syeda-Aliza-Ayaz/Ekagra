package com.ekagra.service;

import com.ekagra.app.Navigator;
import com.ekagra.app.Screen;
import com.ekagra.model.ScreenLimit;
import com.ekagra.pattern.factory.NotificationFactory;
import com.ekagra.pattern.observer.ActivityListener;
import com.ekagra.pattern.state.PomodoroTimer;
import com.ekagra.tracking.ActivitySnapshot;
import com.ekagra.ui.DesktopToast;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Watches activity (Observer) and sends wellbeing nudges (FR-21, FR-22, FR-23):
 * break reminders after N minutes of continuous use, 20-20-20 eye breaks,
 * and warnings at 80 % / 100 % of the daily screen limit and per-app limits.
 * Each limit warning is sent at most once per day.
 */
public class WellbeingMonitor implements ActivityListener {

    static final int EYE_REST_EVERY_SEC = 20 * 60;
    /** Limits are checked against the database once a minute (30 samples). */
    static final int LIMIT_CHECK_EVERY_SAMPLES = 30;

    private final int userId;
    private final WellbeingService wellbeing;
    private final PomodoroTimer timer;

    private int continuousSec;
    private int eyeSec;
    private int samplesSinceLimitCheck = LIMIT_CHECK_EVERY_SAMPLES - 3;  // first check soon after login
    private LocalDate day = LocalDate.now();
    private boolean warned80;
    private boolean warned100;
    private final Set<String> appLimitsWarned = new HashSet<>();

    public WellbeingMonitor(int userId, WellbeingService wellbeing, PomodoroTimer timer) {
        this.userId = userId;
        this.wellbeing = wellbeing;
        this.timer = timer;
    }

    @Override
    public void onSample(ActivitySnapshot s) {
        rollOverDayIfNeeded();
        if (!wellbeing.areRemindersEnabled()) {
            return;
        }
        boolean onPomodoroBreak = timer.isActive() && timer.getCurrentType().isBreak();
        if (onPomodoroBreak) {
            continuousSec = 0;       // a Pomodoro break counts as a real break
        } else {
            continuousSec += TrackingService.SAMPLE_SECONDS;
        }
        eyeSec += TrackingService.SAMPLE_SECONDS;

        // break reminder: never interrupt a running focus session; it waits until the session ends
        int breakEverySec = wellbeing.getBreakEveryMin() * 60;
        if (continuousSec >= breakEverySec && !timer.isFocusing()) {
            NotificationService.notify(NotificationFactory.breakReminder(userId, continuousSec / 60),
                new DesktopToast.Action("Start a 5-min break", true, () -> {
                    Navigator.bringToFront();
                    Navigator.goTo(Screen.FOCUS);
                }));
            continuousSec = 0;
        }

        if (wellbeing.isEyeRestEnabled() && eyeSec >= EYE_REST_EVERY_SEC) {
            DesktopToast.show(DesktopToast.Kind.INFO, "20-20-20 eye break",
                NotificationFactory.eyeRest(userId).getMessage(), java.time.Duration.ofSeconds(20));
            eyeSec = 0;
        }

        if (++samplesSinceLimitCheck >= LIMIT_CHECK_EVERY_SAMPLES) {
            samplesSinceLimitCheck = 0;
            checkLimits();
        }
    }

    @Override
    public void onIdleChanged(boolean idle) {
        if (idle) {           // stepping away for 5+ minutes is a real break
            continuousSec = 0;
            eyeSec = 0;
        }
    }

    private void checkLimits() {
        int usedMin = wellbeing.screenSecondsToday() / 60;
        int limit = wellbeing.getDailyLimitMin();
        if (!warned100 && usedMin >= limit) {
            warned100 = true;
            warned80 = true;
            NotificationService.notify(NotificationFactory.screenLimitReached(userId, limit));
        } else if (!warned80 && usedMin >= limit * 0.8) {
            warned80 = true;
            NotificationService.notify(NotificationFactory.screenLimitWarning(userId, usedMin, limit));
        }
        for (ScreenLimit l : wellbeing.appLimits()) {
            String key = l.getAppName().toLowerCase();
            if (!appLimitsWarned.contains(key) && wellbeing.secondsOnAppToday(l.getAppName()) / 60 >= l.getLimitMin()) {
                appLimitsWarned.add(key);
                NotificationService.notify(NotificationFactory.appLimitReached(userId, l.getAppName(), l.getLimitMin()));
            }
        }
    }

    private void rollOverDayIfNeeded() {
        LocalDate now = LocalDate.now();
        if (!now.equals(day)) {
            day = now;
            warned80 = false;
            warned100 = false;
            appLimitsWarned.clear();
        }
    }

    /** Minutes of screen use since the last break (shown on the Wellbeing screen). */
    public int getContinuousMinutes() {
        return continuousSec / 60;
    }
}
