package com.ekagra.service;

import com.ekagra.dao.ActivityDao;
import com.ekagra.dao.BadgeDao;
import com.ekagra.dao.HabitDao;
import com.ekagra.dao.MoodDao;
import com.ekagra.dao.SessionDao;
import com.ekagra.dao.TaskDao;
import com.ekagra.dao.UserDao;
import com.ekagra.model.Badge;
import com.ekagra.model.Category;
import com.ekagra.model.Habit;
import com.ekagra.model.Notification;
import com.ekagra.model.Student;
import com.ekagra.pattern.factory.NotificationFactory;
import com.ekagra.pattern.observer.AppEvent;
import com.ekagra.pattern.observer.AppEventBus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * XP, levels and badges (FR-33, FR-34). Subscribes to the {@link AppEventBus}: whenever something
 * good happens it awards XP and checks whether a new badge was unlocked.
 */
public class GamificationService {

    // XP rules (also shown on the Achievements screen)
    public static final int XP_PER_5_FOCUS_MIN = 10;
    public static final int XP_TASK = 30;
    public static final int XP_TASK_ON_TIME_BONUS = 20;
    public static final int XP_HABIT = 10;
    public static final int XP_MOOD = 10;

    /** Badge plus how close the student is to it. */
    public record BadgeStatus(Badge badge, LocalDateTime earnedAt, int current, int target) {
        public boolean isEarned() {
            return earnedAt != null;
        }

        public double fraction() {
            return isEarned() ? 1.0 : target == 0 ? 0 : Math.min(1.0, (double) current / target);
        }
    }

    private final Student student;
    private final UserDao userDao = new UserDao();
    private final BadgeDao badgeDao = new BadgeDao();
    private final SessionDao sessionDao = new SessionDao();
    private final TaskDao taskDao = new TaskDao();
    private final HabitDao habitDao = new HabitDao();
    private final MoodDao moodDao = new MoodDao();
    private final ActivityDao activityDao = new ActivityDao();
    private final Consumer<AppEvent> subscriber = this::onEvent;
    private Consumer<List<Notification>> notifier = list -> list.forEach(NotificationService::notify);

    public GamificationService(Student student) {
        this.student = student;
    }

    /** Lets tests capture notifications instead of showing pop-ups. */
    public void setNotifier(Consumer<List<Notification>> notifier) {
        this.notifier = notifier;
    }

    public void start() {
        AppEventBus.subscribe(subscriber);
    }

    public void stop() {
        AppEventBus.unsubscribe(subscriber);
    }

    // ---------- events ----------

    void onEvent(AppEvent e) {
        if (e.userId() != student.getId()) {
            return;
        }
        int xp = switch (e) {
            case AppEvent.FocusCompleted f -> Math.max(XP_PER_5_FOCUS_MIN, f.session().getActualMinutes() / 5 * XP_PER_5_FOCUS_MIN);
            case AppEvent.TaskCompleted t -> XP_TASK + (t.task().getDeadline() != null && t.task().wasCompletedOnTime()
                ? XP_TASK_ON_TIME_BONUS : 0);
            case AppEvent.HabitChecked h -> XP_HABIT;
            case AppEvent.MoodLogged m -> XP_MOOD;
        };
        List<Notification> news = new ArrayList<>();
        addXp(xp, news);
        news.addAll(checkBadges());
        if (!news.isEmpty()) {
            notifier.accept(news);
        }
    }

    private void addXp(int amount, List<Notification> news) {
        int levelBefore = student.getLevel();
        userDao.addXp(student.getId(), amount);
        student.setXp(student.getXp() + amount);
        if (student.getLevel() > levelBefore) {
            news.add(NotificationFactory.system(student.getId(), "Level up! You're now level " + student.getLevel(),
                "Keep going: " + (Student.XP_PER_LEVEL - student.getXpIntoLevel()) + " XP to the next level."));
        }
    }

    // ---------- badges ----------

    /** Awards any badges whose condition is now met. Returns a notification per new badge. */
    public List<Notification> checkBadges() {
        List<Notification> news = new ArrayList<>();
        Map<String, LocalDateTime> earned = badgeDao.findEarned(student.getId());
        for (BadgeStatus s : evaluate(earned)) {
            if (!s.isEarned() && s.current() >= s.target() && badgeDao.award(student.getId(), s.badge().getCode())) {
                news.add(NotificationFactory.badgeEarned(student.getId(), s.badge().getName(), s.badge().getDescription()));
            }
        }
        return news;
    }

    public List<BadgeStatus> badgeStatuses() {
        return evaluate(badgeDao.findEarned(student.getId()));
    }

    private List<BadgeStatus> evaluate(Map<String, LocalDateTime> earned) {
        int uid = student.getId();
        int sessions = sessionDao.countCompletedFocus(uid);
        List<BadgeStatus> out = new ArrayList<>();
        for (Badge b : badgeDao.findAll()) {
            int[] p = switch (b.getCode()) {
                case "FIRST_FOCUS" -> new int[]{sessions, 1};
                case "FOCUS_10" -> new int[]{sessions, 10};
                case "FOCUS_50" -> new int[]{sessions, 50};
                case "STREAK_7" -> new int[]{focusStreak(), 7};
                case "DEADLINE_CRUSHER" -> new int[]{taskDao.countCompletedOnTime(uid), 5};
                case "TASK_MASTER" -> new int[]{taskDao.countCompleted(uid), 25};
                case "DIGITAL_DETOX" -> new int[]{detoxDays(), 1};
                case "EARLY_BIRD" -> new int[]{sessionDao.countCompletedFocusStartedBefore(uid, 8), 1};
                case "NIGHT_OWL" -> new int[]{sessionDao.countCompletedFocusEndedFrom(uid, 23), 1};
                case "HABIT_HERO" -> new int[]{habitDao.findActive(uid).stream()
                    .mapToInt(h -> h.currentStreak(LocalDate.now())).max().orElse(0), 7};
                case "ZEN" -> new int[]{moodStreak(), 7};
                default -> new int[]{0, 1};
            };
            out.add(new BadgeStatus(b, earned.get(b.getCode()), p[0], p[1]));
        }
        return out;
    }

    private int focusStreak() {
        LocalDate day = LocalDate.now();
        if (completedOn(day) == 0) {
            day = day.minusDays(1);
        }
        int streak = 0;
        while (streak < 365 && completedOn(day) > 0) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    private int completedOn(LocalDate d) {
        return sessionDao.focusStats(student.getId(), d.atStartOfDay(), d.plusDays(1).atStartOfDay()).completedSessions();
    }

    /** Finished days (yesterday and earlier this week) with 1 h+ screen time but under 30 min distracting. */
    private int detoxDays() {
        int count = 0;
        for (int i = 1; i <= 7; i++) {
            LocalDate d = LocalDate.now().minusDays(i);
            Map<Category, Integer> c = activityDao.secondsByCategory(student.getId(), d.atStartOfDay(), d.plusDays(1).atStartOfDay());
            int total = c.values().stream().mapToInt(Integer::intValue).sum();
            if (total >= 3600 && c.get(Category.DISTRACTING) < 30 * 60) {
                count++;
            }
        }
        return count;
    }

    private int moodStreak() {
        LocalDate day = LocalDate.now();
        if (moodDao.findByDate(student.getId(), day).isEmpty()) {
            day = day.minusDays(1);
        }
        int streak = 0;
        while (streak < 365 && moodDao.findByDate(student.getId(), day).isPresent()) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    /** Best habit streak, for the Achievements screen. */
    public int bestHabitStreak() {
        return habitDao.findActive(student.getId()).stream().mapToInt(Habit::bestStreak).max().orElse(0);
    }
}
