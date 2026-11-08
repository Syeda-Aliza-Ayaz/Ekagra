package com.ekagra.service;

import com.ekagra.app.Navigator;
import com.ekagra.app.Screen;
import com.ekagra.model.Category;
import com.ekagra.model.FocusSession;
import com.ekagra.pattern.observer.ActivityListener;
import com.ekagra.pattern.state.PomodoroTimer;
import com.ekagra.tracking.ActivitySnapshot;
import com.ekagra.ui.DesktopToast;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * FOCUS GUARD (FR-13): connects the activity tracker to the Pomodoro timer.
 * It observes activity; when a distracting app is opened during a focus session it counts a
 * distraction on the timer and pops up a warning on top of whatever app the student is in.
 * "It's for study" removes the distraction and stops warning about that app for this session.
 */
public class FocusGuard implements ActivityListener {

    /** If the student stays in the distracting app, remind again after this long (not counted again). */
    static final Duration REMIND_AFTER = Duration.ofSeconds(90);

    private final PomodoroTimer timer;
    private final BooleanSupplier enabled;
    private final Set<String> allowedThisSession = new HashSet<>();
    private FocusSession watchedSession;
    private Instant lastWarning = Instant.EPOCH;

    public FocusGuard(PomodoroTimer timer, BooleanSupplier enabled) {
        this.timer = timer;
        this.enabled = enabled;
    }

    @Override
    public void onSample(ActivitySnapshot s) {
        if (!timer.isFocusing() || !enabled.getAsBoolean()) {
            return;
        }
        FocusSession session = timer.getCurrentSession();
        if (session != watchedSession) {          // new focus session: forget previous exceptions
            watchedSession = session;
            allowedThisSession.clear();
            lastWarning = Instant.EPOCH;
        }
        if (s.category() != Category.DISTRACTING) {
            return;
        }
        String app = s.window().processName();
        if (allowedThisSession.contains(app)) {
            return;
        }
        if (s.switchedApp()) {
            timer.recordDistraction();
            warn(s, true);
        } else if (Duration.between(lastWarning, Instant.now()).compareTo(REMIND_AFTER) > 0) {
            warn(s, false);
        }
    }

    private void warn(ActivitySnapshot s, boolean counted) {
        lastWarning = Instant.now();
        String what = describe(s);
        String app = s.window().processName();
        String message = (counted ? "You opened " : "You're still on ") + what + " during your focus session"
            + " (" + timer.getFormattedRemaining() + " left).";
        DesktopToast.show(DesktopToast.Kind.WARNING, "Focus Guard", message, Duration.ofSeconds(9),
            new DesktopToast.Action("Back to focus", true, () -> {
                Navigator.bringToFront();
                Navigator.goTo(Screen.FOCUS);
            }),
            new DesktopToast.Action("It's for study", false, () -> {
                allowedThisSession.add(app);
                if (counted) {
                    timer.forgiveDistraction();
                }
            }));
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    /** "YouTube" from the title if a known site matched, otherwise the app name. */
    private static String describe(ActivitySnapshot s) {
        String title = s.window().title() == null ? "" : s.window().title();
        for (String site : new String[]{"YouTube", "Netflix", "Instagram", "Facebook", "TikTok", "Reddit", "Twitch"}) {
            if (title.toLowerCase().contains(site.toLowerCase())) {
                return site;
            }
        }
        return com.ekagra.model.ActivityLog.displayName(s.window().processName());
    }
}
