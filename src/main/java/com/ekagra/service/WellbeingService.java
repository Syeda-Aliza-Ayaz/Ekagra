package com.ekagra.service;

import com.ekagra.dao.ActivityDao;
import com.ekagra.dao.MoodDao;
import com.ekagra.dao.ScreenLimitDao;
import com.ekagra.dao.SleepDao;
import com.ekagra.model.Category;
import com.ekagra.model.MoodEntry;
import com.ekagra.model.ScreenLimit;
import com.ekagra.model.SleepEntry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Mood, sleep, screen-time limits and the Wellbeing Score (FR-21 .. FR-26). */
public class WellbeingService {

    public static final int DEFAULT_DAILY_LIMIT_MIN = 360;
    public static final int DEFAULT_BREAK_EVERY_MIN = 50;

    private final int userId;
    private final MoodDao moodDao = new MoodDao();
    private final SleepDao sleepDao = new SleepDao();
    private final ScreenLimitDao limitDao = new ScreenLimitDao();
    private final ActivityDao activityDao = new ActivityDao();
    private final SettingsService settings = new SettingsService();

    public WellbeingService(int userId) {
        this.userId = userId;
    }

    // ---------- mood ----------

    public void saveMood(int mood, int stress, String note) {
        if (mood < 1 || mood > 5 || stress < 1 || stress > 5) {
            throw new ValidationException("Please pick how you feel and your stress level.");
        }
        boolean first = moodDao.findByDate(userId, LocalDate.now()).isEmpty();
        MoodEntry entry = new MoodEntry(userId, LocalDate.now(), mood, stress,
            note == null || note.isBlank() ? null : note.trim());
        moodDao.upsert(entry);
        if (first) {     // XP only for the first check-in of the day, not for edits
            com.ekagra.pattern.observer.AppEventBus.publish(new com.ekagra.pattern.observer.AppEvent.MoodLogged(userId, entry));
        }
    }

    public Optional<MoodEntry> todaysMood() {
        return moodDao.findByDate(userId, LocalDate.now());
    }

    public List<MoodEntry> moodHistory(int days) {
        LocalDate today = LocalDate.now();
        return moodDao.findBetween(userId, today.minusDays(days - 1L), today);
    }

    // ---------- sleep ----------

    public SleepEntry saveSleep(LocalTime bedtime, LocalTime wake) {
        if (bedtime == null || wake == null) {
            throw new ValidationException("Please enter both bedtime and wake-up time.");
        }
        SleepEntry s = new SleepEntry(userId, LocalDate.now(), bedtime, wake);
        if (s.getDurationMin() > 16 * 60) {
            throw new ValidationException("That's more than 16 hours. Check the times.");
        }
        sleepDao.upsert(s);
        return s;
    }

    public Optional<SleepEntry> lastNight() {
        return sleepDao.findByDate(userId, LocalDate.now());
    }

    public List<SleepEntry> sleepHistory(int days) {
        LocalDate today = LocalDate.now();
        return sleepDao.findBetween(userId, today.minusDays(days - 1L), today);
    }

    // ---------- limits & reminder settings ----------

    public int getDailyLimitMin() {
        return settings.getInt(userId, SettingsService.DAILY_SCREEN_LIMIT_MIN, DEFAULT_DAILY_LIMIT_MIN);
    }

    public void setDailyLimitMin(int minutes) {
        if (minutes < 30 || minutes > 24 * 60) {
            throw new ValidationException("Daily limit must be between 30 minutes and 24 hours.");
        }
        settings.set(userId, SettingsService.DAILY_SCREEN_LIMIT_MIN, minutes);
    }

    public int getBreakEveryMin() {
        return settings.getInt(userId, SettingsService.BREAK_REMINDER_MIN, DEFAULT_BREAK_EVERY_MIN);
    }

    public void setBreakEveryMin(int minutes) {
        if (minutes < 10 || minutes > 240) {
            throw new ValidationException("Break reminders must be every 10 to 240 minutes.");
        }
        settings.set(userId, SettingsService.BREAK_REMINDER_MIN, minutes);
    }

    public boolean isEyeRestEnabled() {
        return settings.getBoolean(userId, SettingsService.EYE_REST, true);
    }

    public void setEyeRestEnabled(boolean on) {
        settings.set(userId, SettingsService.EYE_REST, on);
    }

    public boolean areRemindersEnabled() {
        return settings.getBoolean(userId, SettingsService.REMINDERS, true);
    }

    public void setRemindersEnabled(boolean on) {
        settings.set(userId, SettingsService.REMINDERS, on);
    }

    public List<ScreenLimit> appLimits() {
        return limitDao.findByUser(userId);
    }

    public ScreenLimit addAppLimit(String app, int minutes) {
        if (app == null || app.isBlank()) {
            throw new ValidationException("Type an app or website name, e.g. Instagram.");
        }
        if (minutes < 5 || minutes > 24 * 60) {
            throw new ValidationException("Limit must be at least 5 minutes.");
        }
        boolean exists = appLimits().stream().anyMatch(l -> l.getAppName().equalsIgnoreCase(app.trim()));
        if (exists) {
            throw new ValidationException("There's already a limit for " + app.trim() + ".");
        }
        ScreenLimit l = new ScreenLimit(userId, app.trim(), minutes);
        limitDao.insert(l);
        return l;
    }

    public void removeAppLimit(ScreenLimit l) {
        limitDao.delete(l.getId());
    }

    // ---------- usage ----------

    public int screenSecondsToday() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return activityDao.totalSeconds(userId, start, start.plusDays(1));
    }

    public int secondsOnAppToday(String app) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return activityDao.secondsMatching(userId, app, start, start.plusDays(1));
    }

    // ---------- wellbeing score (FR-26) ----------

    /** One part of the score, 0..100. */
    public record ScorePart(String name, int score, String detail) {
    }

    /** Total 0..100 (or -1 when there's no data at all) plus the parts it was built from. */
    public record WellbeingScore(int total, List<ScorePart> parts) {
        public boolean hasData() {
            return total >= 0;
        }
    }

    public WellbeingScore todaysScore() {
        return scoreFor(LocalDate.now());
    }

    public WellbeingScore scoreFor(LocalDate day) {
        LocalDateTime start = day.atStartOfDay();
        Map<Category, Integer> byCat = activityDao.secondsByCategory(userId, start, start.plusDays(1));
        int screenSec = byCat.values().stream().mapToInt(Integer::intValue).sum();
        return computeScore(sleepDao.findByDate(userId, day).orElse(null), moodDao.findByDate(userId, day).orElse(null),
            screenSec, byCat.get(Category.DISTRACTING), getDailyLimitMin());
    }

    /**
     * Pure calculation (unit-tested). Each available part counts equally; missing parts are skipped.
     * <ul>
     *   <li>Sleep: 7–9 h = 100, falling to 0 at 4 h or 12 h.</li>
     *   <li>Mood: average of good mood and low stress.</li>
     *   <li>Screen time: up to 75 % of the limit = 100, 100 % = 70, 150 % or more = 0.</li>
     *   <li>Distraction balance: 0 % distracting = 100, 50 % or more = 0.</li>
     * </ul>
     */
    public static WellbeingScore computeScore(SleepEntry sleep, MoodEntry mood, int screenSec, int distractingSec,
                                              int dailyLimitMin) {
        List<ScorePart> parts = new ArrayList<>();
        if (sleep != null) {
            double h = sleep.getHours();
            double s;
            if (h >= 7 && h <= 9) {
                s = 100;
            } else if (h < 7) {
                s = Math.max(0, (h - 4) / 3 * 100);
            } else {
                s = Math.max(0, (12 - h) / 3 * 100);
            }
            parts.add(new ScorePart("Sleep", (int) Math.round(s), String.format("%.1f h last night", h)));
        }
        if (mood != null) {
            double s = ((mood.getMood() - 1) * 25 + (5 - mood.getStress()) * 25) / 2.0;
            parts.add(new ScorePart("Mood", (int) Math.round(s), mood.getMoodLabel() + ", " + mood.getStressLabel().toLowerCase()));
        }
        if (screenSec >= 5 * 60) {
            double ratio = screenSec / 60.0 / dailyLimitMin;
            double s;
            if (ratio <= 0.75) {
                s = 100;
            } else if (ratio <= 1.0) {
                s = 100 - (ratio - 0.75) / 0.25 * 30;
            } else {
                s = Math.max(0, 70 - (ratio - 1.0) / 0.5 * 70);
            }
            parts.add(new ScorePart("Screen time", (int) Math.round(s),
                Math.round(ratio * 100) + "% of your daily limit"));

            double share = (double) distractingSec / screenSec;
            double d = Math.max(0, 100 - share / 0.5 * 100);
            parts.add(new ScorePart("Distraction balance", (int) Math.round(d),
                Math.round(share * 100) + "% of screen time distracting"));
        }
        if (parts.isEmpty()) {
            return new WellbeingScore(-1, parts);
        }
        int total = (int) Math.round(parts.stream().mapToInt(ScorePart::score).average().orElse(0));
        return new WellbeingScore(total, parts);
    }
}
