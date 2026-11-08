package com.ekagra.service;

import com.ekagra.dao.ActivityDao;
import com.ekagra.dao.SessionDao;
import com.ekagra.dao.TaskDao;
import com.ekagra.model.Category;
import com.ekagra.model.Student;
import com.ekagra.model.Task;
import com.ekagra.pattern.strategy.ScoreInputs;
import com.ekagra.pattern.strategy.ScoreStrategy;
import com.ekagra.pattern.strategy.ScoreStrategy.ScoreBreakdown;
import com.ekagra.util.Formats;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Dashboard & Analytics numbers (FR-29 .. FR-32). */
public class AnalyticsService {

    private final Student student;
    private final int userId;
    private final SessionDao sessionDao = new SessionDao();
    private final TaskDao taskDao = new TaskDao();
    private final ActivityDao activityDao = new ActivityDao();
    private final GoalService goals;
    private final WellbeingService wellbeing;
    private final SettingsService settings = new SettingsService();
    private ScoreStrategy strategy;

    public AnalyticsService(Student student) {
        this.student = student;
        this.userId = student.getId();
        this.goals = new GoalService(userId);
        this.wellbeing = new WellbeingService(userId);
        this.strategy = ScoreStrategy.byId(settings.getString(userId, SettingsService.SCORE_STRATEGY, "BALANCED"));
    }

    // ---------- strategy ----------

    public ScoreStrategy getStrategy() {
        return strategy;
    }

    /** Swaps the scoring formula at runtime (Strategy pattern) and remembers the choice. */
    public void setStrategy(ScoreStrategy s) {
        this.strategy = s;
        settings.set(userId, SettingsService.SCORE_STRATEGY, s.getId());
    }

    // ---------- per-day summary ----------

    public record DaySummary(LocalDate date, ScoreBreakdown score, int focusMinutes, int completedSessions,
                             int tasksDone, Map<Category, Integer> secondsByCategory, boolean hasData) {
        public int screenSeconds() {
            return secondsByCategory.values().stream().mapToInt(Integer::intValue).sum();
        }

        public String dayLabel() {
            return date.equals(LocalDate.now()) ? "Today" : date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        }
    }

    public ScoreInputs inputsFor(LocalDate day, List<Task> allTasks) {
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = from.plusDays(1);
        SessionDao.FocusStats focus = sessionDao.focusStats(userId, from, to);
        int done = taskDao.countCompletedBetween(userId, from, to);
        int dueOrOverdue = (int) allTasks.stream()
            .filter(t -> t.getDeadline() != null && t.getDeadline().isBefore(to))
            .filter(t -> t.getCreatedAt() == null || t.getCreatedAt().isBefore(to))
            .filter(t -> !t.isDone() || (t.getCompletedAt() != null && !t.getCompletedAt().isBefore(to)))
            .count();
        Map<Category, Integer> cats = activityDao.secondsByCategory(userId, from, to);
        var goalProgress = goals.goalsWithProgress(day);
        int achieved = (int) goalProgress.stream().filter(GoalService.GoalProgress::isAchieved).count();
        int wb = wellbeing.scoreFor(day).total();
        return new ScoreInputs(focus.focusMinutes(), student.getDailyTargetMin(), done, dueOrOverdue,
            cats.get(Category.PRODUCTIVE), cats.get(Category.DISTRACTING), achieved, goalProgress.size(), wb);
    }

    public DaySummary summaryFor(LocalDate day) {
        return summaryFor(day, taskDao.findByUser(userId));
    }

    private DaySummary summaryFor(LocalDate day, List<Task> tasks) {
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = from.plusDays(1);
        ScoreInputs in = inputsFor(day, tasks);
        SessionDao.FocusStats focus = sessionDao.focusStats(userId, from, to);
        boolean hasData = focus.focusSeconds() > 0 || in.tasksCompleted() > 0 || in.wellbeingScore() >= 0
            || in.productiveSeconds() + in.distractingSeconds() > 0
            || activityDao.totalSeconds(userId, from, to) > 0;
        // a day the student didn't use the app at all scores 0 instead of "neutral" defaults
        ScoreBreakdown score = hasData ? strategy.score(in) : new ScoreBreakdown(0, strategy.score(in).parts());
        return new DaySummary(day, score, focus.focusMinutes(), focus.completedSessions(),
            in.tasksCompleted(), activityDao.secondsByCategory(userId, from, to), hasData);
    }

    /** The last {@code days} days, oldest first, ending today. */
    public List<DaySummary> lastDays(int days) {
        return days(LocalDate.now().minusDays(days - 1L), days);
    }

    /** {@code count} consecutive days starting at {@code from} (never beyond today). */
    public List<DaySummary> days(LocalDate from, int count) {
        List<Task> tasks = taskDao.findByUser(userId);
        LocalDate today = LocalDate.now();
        List<DaySummary> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            LocalDate d = from.plusDays(i);
            if (d.isAfter(today)) {
                break;
            }
            out.add(summaryFor(d, tasks));
        }
        return out;
    }

    /** Consecutive days with at least one completed focus session (today may still be in progress). */
    public int focusStreak() {
        LocalDate day = LocalDate.now();
        if (completedOn(day) == 0) {
            day = day.minusDays(1);
        }
        int streak = 0;
        while (completedOn(day) > 0 && streak < 365) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    private int completedOn(LocalDate d) {
        return sessionDao.focusStats(userId, d.atStartOfDay(), d.plusDays(1).atStartOfDay()).completedSessions();
    }

    public List<ActivityDao.AppUsage> topApps(int days, int limit) {
        LocalDate today = LocalDate.now();
        return activityDao.topApps(userId, today.minusDays(days - 1L).atStartOfDay(), today.plusDays(1).atStartOfDay(), limit);
    }

    /** Open tasks ordered by deadline (soonest first; tasks without deadline last). */
    public List<Task> upNext(int limit) {
        return taskDao.findByUser(userId).stream()
            .filter(t -> !t.isDone())
            .sorted(Comparator.comparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())))
            .limit(limit)
            .toList();
    }

    // ---------- insights (FR-32) ----------

    public enum Tone { POSITIVE, NEUTRAL, WARNING }

    public record Insight(String icon, String text, Tone tone) {
    }

    /** Plain rule-based observations: no AI, every rule can be explained in the viva. */
    public List<Insight> insights(List<DaySummary> week) {
        List<Insight> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDateTime weekStart = today.minusDays(6).atStartOfDay();
        LocalDateTime weekEnd = today.plusDays(1).atStartOfDay();

        // 1. most productive time of day (best 2-hour window of productive time over the week)
        int[] productiveByHour = new int[24];
        for (ActivityDao.HourUsage h : activityDao.hourly(userId, weekStart, weekEnd)) {
            if (h.category() == Category.PRODUCTIVE) {
                productiveByHour[h.hour()] += h.seconds();
            }
        }
        int bestHour = -1;
        int bestSum = 0;
        for (int h = 0; h < 23; h++) {
            int sum = productiveByHour[h] + productiveByHour[h + 1];
            if (sum > bestSum) {
                bestSum = sum;
                bestHour = h;
            }
        }
        if (bestHour >= 0 && bestSum >= 15 * 60) {
            out.add(new Insight("fth-sunrise", "You're most productive between " + hour(bestHour) + " and "
                + hour(bestHour + 2) + ". Try to schedule hard tasks then.", Tone.POSITIVE));
        }

        // 2. distraction trend vs the previous week
        int thisWeek = activityDao.secondsByCategory(userId, weekStart, weekEnd).get(Category.DISTRACTING);
        int lastWeek = activityDao.secondsByCategory(userId, weekStart.minusDays(7), weekStart).get(Category.DISTRACTING);
        if (lastWeek >= 10 * 60 && thisWeek > 0) {
            int change = Math.round(100f * (thisWeek - lastWeek) / lastWeek);
            if (change >= 15) {
                out.add(new Insight("fth-trending-up", "Distracting apps are up " + change + "% compared with last week.", Tone.WARNING));
            } else if (change <= -15) {
                out.add(new Insight("fth-trending-down", "Distracting time is down " + (-change) + "% from last week. Nice!", Tone.POSITIVE));
            }
        }

        // 3. biggest distraction this week
        activityDao.topApps(userId, weekStart, weekEnd, 20).stream()
            .filter(a -> a.category() == Category.DISTRACTING && a.seconds() >= 20 * 60)
            .findFirst()
            .ifPresent(a -> out.add(new Insight("fth-alert-octagon", com.ekagra.model.ActivityLog.displayName(a.appName())
                + " was your biggest distraction this week (" + Formats.seconds(a.seconds()) + ").", Tone.WARNING)));

        // 4. study target hit rate
        int target = student.getDailyTargetMin();
        long hitDays = week.stream().filter(d -> d.focusMinutes() >= target).count();
        int totalFocus = week.stream().mapToInt(DaySummary::focusMinutes).sum();
        if (totalFocus > 0) {
            out.add(new Insight("fth-target", "You reached your " + Formats.minutes(target) + " study target on "
                + hitDays + " of the last " + week.size() + " days.", hitDays >= 4 ? Tone.POSITIVE : Tone.NEUTRAL));
        }

        // 5. best day of the week
        week.stream().filter(d -> d.focusMinutes() > 0).max(Comparator.comparingInt(DaySummary::focusMinutes))
            .filter(d -> week.stream().filter(x -> x.focusMinutes() > 0).count() >= 3)
            .ifPresent(d -> out.add(new Insight("fth-award", "Your best focus day was " + d.date().getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " with " + Formats.minutes(d.focusMinutes()) + ".", Tone.POSITIVE)));

        // 6. overdue work
        long overdue = taskDao.findByUser(userId).stream().filter(t -> t.isOverdue(LocalDateTime.now())).count();
        if (overdue > 0) {
            out.add(new Insight("fth-alert-circle", overdue + (overdue == 1 ? " task is" : " tasks are")
                + " overdue. Finishing one today will lift your score.", Tone.WARNING));
        }

        // 7. score trend (this half of the week vs the first half)
        if (week.size() >= 6) {
            double recent = week.subList(week.size() - 3, week.size()).stream().mapToInt(d -> d.score().total()).average().orElse(0);
            double earlier = week.subList(0, 3).stream().mapToInt(d -> d.score().total()).average().orElse(0);
            if (earlier > 0 && recent - earlier >= 10) {
                out.add(new Insight("fth-zap", "Your score is climbing: up " + Math.round(recent - earlier)
                    + " points over the last few days.", Tone.POSITIVE));
            }
        }

        if (out.isEmpty()) {
            out.add(new Insight("fth-info", "Keep using Ekagra for a few days and personalised insights will appear here.", Tone.NEUTRAL));
        }
        return out;
    }

    private static String hour(int h) {
        int hh = h % 24;
        if (hh == 0) {
            return "12 AM";
        }
        if (hh == 12) {
            return "12 PM";
        }
        return hh < 12 ? hh + " AM" : (hh - 12) + " PM";
    }

    /** Greeting for the dashboard header. */
    public static String greeting(LocalDateTime now) {
        int h = now.getHour();
        if (h < 5) {
            return "Burning the midnight oil";
        }
        if (h < 12) {
            return "Good morning";
        }
        if (h < 17) {
            return "Good afternoon";
        }
        return "Good evening";
    }

    public static boolean isWeekend(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
