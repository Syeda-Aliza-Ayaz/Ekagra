package com.ekagra.service;

import com.ekagra.dao.ActivityDao;
import com.ekagra.dao.GoalDao;
import com.ekagra.dao.HabitDao;
import com.ekagra.dao.SessionDao;
import com.ekagra.dao.TaskDao;
import com.ekagra.model.Category;
import com.ekagra.model.Goal;
import com.ekagra.model.Habit;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/** Goals with live progress (FR-27) and habits with streaks (FR-28). */
public class GoalService {

    public static final String[] HABIT_ICONS = {
        "fth-book-open", "fth-droplet", "fth-activity", "fth-moon", "fth-sun", "fth-feather", "fth-music", "fth-edit-3"
    };

    private final int userId;
    private final GoalDao goalDao = new GoalDao();
    private final HabitDao habitDao = new HabitDao();
    private final SessionDao sessionDao = new SessionDao();
    private final TaskDao taskDao = new TaskDao();
    private final ActivityDao activityDao = new ActivityDao();

    public GoalService(int userId) {
        this.userId = userId;
    }

    /** How far along a goal is right now. */
    public record GoalProgress(Goal goal, int current, int target) {
        public double fraction() {
            return target == 0 ? 0 : Math.min(1.0, (double) current / target);
        }

        /** For normal goals: reached the target. For limit goals: still under it. */
        public boolean isAchieved() {
            return goal.getType().isLimit() ? current <= target : current >= target;
        }

        public boolean isOverLimit() {
            return goal.getType().isLimit() && current > target;
        }
    }

    // ---------- goals ----------

    /** First-time students get three sensible goals so the screen isn't empty. */
    public void ensureDefaultGoals(int dailyStudyTargetMin) {
        if (goalDao.countAll(userId) == 0) {
            goalDao.insert(new Goal(userId, Goal.Type.STUDY_TIME, Goal.Period.DAILY, dailyStudyTargetMin));
            goalDao.insert(new Goal(userId, Goal.Type.MAX_DISTRACTING, Goal.Period.DAILY, 60));
            goalDao.insert(new Goal(userId, Goal.Type.TASKS_DONE, Goal.Period.WEEKLY, 5));
        }
    }

    public List<GoalProgress> goalsWithProgress() {
        return goalDao.findActive(userId).stream().map(this::progressOf).toList();
    }

    public List<GoalProgress> goalsWithProgress(LocalDate day) {
        return goalDao.findActive(userId).stream().map(g -> progressOf(g, day)).toList();
    }

    public Goal addGoal(Goal.Type type, Goal.Period period, int target) {
        if (type == null || period == null) {
            throw new ValidationException("Choose what to measure and how often.");
        }
        if (target <= 0) {
            throw new ValidationException("The target must be more than zero.");
        }
        Goal g = new Goal(userId, type, period, target);
        goalDao.insert(g);
        return g;
    }

    public void deleteGoal(Goal g) {
        goalDao.delete(g.getId());
    }

    public GoalProgress progressOf(Goal g) {
        return progressOf(g, LocalDate.now());
    }

    /** Progress as it stood at the end of {@code today} (used for history in Analytics). */
    public GoalProgress progressOf(Goal g, LocalDate today) {
        LocalDateTime from = g.getPeriod() == Goal.Period.DAILY
            ? today.atStartOfDay()
            : today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        int current = switch (g.getType()) {
            case STUDY_TIME -> sessionDao.focusStats(userId, from, to).focusMinutes();
            case FOCUS_SESSIONS -> sessionDao.focusStats(userId, from, to).completedSessions();
            case TASKS_DONE -> taskDao.countCompletedBetween(userId, from, to);
            case MAX_DISTRACTING -> activityDao.secondsByCategory(userId, from, to).get(Category.DISTRACTING) / 60;
            case MAX_SCREEN_TIME -> activityDao.totalSeconds(userId, from, to) / 60;
        };
        return new GoalProgress(g, current, g.getTargetValue());
    }

    // ---------- habits ----------

    public List<Habit> habits() {
        return habitDao.findActive(userId);
    }

    public Habit addHabit(String title, String icon) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Give the habit a name, e.g. \"Read 20 minutes\".");
        }
        if (title.trim().length() > 60) {
            throw new ValidationException("Keep the habit name under 60 characters.");
        }
        Habit h = new Habit(userId, title.trim(), icon == null ? HABIT_ICONS[0] : icon);
        habitDao.insert(h);
        return h;
    }

    public void deleteHabit(Habit h) {
        habitDao.delete(h.getId());
    }

    /** Ticks / unticks a day (only today and the past 6 days can be changed). */
    public void toggleHabit(Habit h, LocalDate day) {
        LocalDate today = LocalDate.now();
        if (day.isAfter(today) || day.isBefore(today.minusDays(6))) {
            throw new ValidationException("You can only update the last 7 days.");
        }
        if (h.isDoneOn(day)) {
            habitDao.uncheck(h.getId(), day);
            h.removeCheck(day);
        } else {
            habitDao.check(h.getId(), day);
            h.addCheck(day);
            com.ekagra.pattern.observer.AppEventBus.publish(new com.ekagra.pattern.observer.AppEvent.HabitChecked(userId, h));
        }
    }
}
