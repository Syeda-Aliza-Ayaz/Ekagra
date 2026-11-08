package com.ekagra.db;

import com.ekagra.dao.ActivityDao;
import com.ekagra.dao.AppRuleDao;
import com.ekagra.dao.HabitDao;
import com.ekagra.dao.MoodDao;
import com.ekagra.dao.ScreenLimitDao;
import com.ekagra.dao.SessionDao;
import com.ekagra.dao.SleepDao;
import com.ekagra.dao.TaskDao;
import com.ekagra.dao.UserDao;
import com.ekagra.model.ActivityLog;
import com.ekagra.model.Category;
import com.ekagra.model.FocusSession;
import com.ekagra.model.Goal;
import com.ekagra.model.Habit;
import com.ekagra.model.Mentor;
import com.ekagra.model.MoodEntry;
import com.ekagra.model.Priority;
import com.ekagra.model.Role;
import com.ekagra.model.ScreenLimit;
import com.ekagra.model.SessionType;
import com.ekagra.model.SleepEntry;
import com.ekagra.model.Student;
import com.ekagra.model.SubTask;
import com.ekagra.model.Task;
import com.ekagra.model.TaskStatus;
import com.ekagra.pattern.factory.NotificationFactory;
import com.ekagra.service.AuthService;
import com.ekagra.service.GamificationService;
import com.ekagra.service.GoalService;
import com.ekagra.service.NotificationService;
import com.ekagra.tracking.AppClassifier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * DEMO MODE (FR-20): creates a demo mentor and three demo students with two weeks of realistic,
 * consistent data relative to today, so every chart, badge, insight and at-risk flag is populated
 * for the lab demo. Running it again wipes the old demo accounts and rebuilds them.
 *
 * <ul>
 *   <li>Ayesha Khan: the main demo student (balanced, improving).</li>
 *   <li>Hira Siddiqui: very focused.</li>
 *   <li>Bilal Raza: at risk (distracted, overdue tasks, inactive for days).</li>
 * </ul>
 */
public class DemoDataSeeder {

    public static final String PASSWORD = "demo1234";
    public static final String STUDENT_EMAIL = "demo@ekagra.app";
    public static final String MENTOR_EMAIL = "mentor.demo@ekagra.app";
    private static final String HIRA = "hira.demo@ekagra.app";
    private static final String BILAL = "bilal.demo@ekagra.app";
    private static final int DAYS = 14;

    /** How a demo student behaves. */
    private record Profile(double focus, double productiveShare, double distractingShare, int inactiveLastDays,
                           double moodBase, double sleepBase) {
    }

    // window titles the classifier will sort into categories
    private static final String[][] PRODUCTIVE = {
        {"idea64.exe", "ekagra – PomodoroTimer.java"}, {"idea64.exe", "ekagra – AnalyticsService.java"},
        {"code.exe", "notes.md - Visual Studio Code"}, {"winword.exe", "OOAD Report.docx - Word"},
        {"chrome.exe", "java - How to use JavaFX Timeline - Stack Overflow - Google Chrome"},
        {"chrome.exe", "CT-263 OOAD - Google Classroom - Google Chrome"},
        {"chrome.exe", "ekagra/ekagra: Pull requests - GitHub - Google Chrome"},
        {"msedge.exe", "Normalization (1NF, 2NF, 3NF) - GeeksforGeeks - Microsoft Edge"},
        {"powerpnt.exe", "DBMS Presentation.pptx - PowerPoint"}, {"acrord32.exe", "Linear Algebra Ch5.pdf - Adobe Reader"}
    };
    private static final String[][] NEUTRAL = {
        {"explorer.exe", "Downloads"}, {"spotify.exe", "Spotify Premium"}, {"chrome.exe", "Google Search - Google Chrome"},
        {"outlook.exe", "Inbox - Outlook"}, {"notepad.exe", "todo.txt - Notepad"}
    };
    private static final String[][] DISTRACTING = {
        {"chrome.exe", "Funny cat compilation - YouTube - Google Chrome"}, {"chrome.exe", "Instagram - Google Chrome"},
        {"discord.exe", "#general - Discord"}, {"whatsapp.exe", "WhatsApp"},
        {"chrome.exe", "r/pakistan - Reddit - Google Chrome"}, {"msedge.exe", "Netflix - Microsoft Edge"}
    };

    private final UserDao userDao = new UserDao();
    private final TaskDao taskDao = new TaskDao();
    private final SessionDao sessionDao = new SessionDao();
    private final ActivityDao activityDao = new ActivityDao();
    private final MoodDao moodDao = new MoodDao();
    private final SleepDao sleepDao = new SleepDao();
    private final HabitDao habitDao = new HabitDao();
    private final AppClassifier classifier = new AppClassifier(new AppRuleDao()::findAll);
    private final Random random = new Random(263);    // fixed seed: the same demo every time
    private LocalDateTime now;

    /** Rebuilds all demo accounts. Returns the main demo student. */
    public Student seed() {
        now = LocalDateTime.now();
        Student[] main = new Student[1];
        DatabaseManager.getInstance().inTransaction(() -> {
            for (String email : List.of(STUDENT_EMAIL, HIRA, BILAL, MENTOR_EMAIL)) {
                userDao.findByEmail(email).ifPresent(u -> userDao.delete(u.getId()));
            }
            AuthService auth = new AuthService();
            Mentor mentor = (Mentor) auth.register(new AuthService.SignupRequest(Role.MENTOR, "Dr. Sara Ahmed",
                MENTOR_EMAIL, PASSWORD, PASSWORD, null, null, null));

            Student ayesha = student(auth, "Ayesha Khan", STUDENT_EMAIL, 3, "#226192", mentor);
            Student hira = student(auth, "Hira Siddiqui", HIRA, 5, "#3A8D8C", mentor);
            Student bilal = student(auth, "Bilal Raza", BILAL, 3, "#C0574F", mentor);

            fill(ayesha, new Profile(0.9, 0.6, 0.18, 0, 3.8, 7.2), true);
            fill(hira, new Profile(1.0, 0.72, 0.08, 0, 4.2, 7.6), false);
            fill(bilal, new Profile(0.25, 0.25, 0.55, 4, 2.6, 5.8), false);

            NotificationService.save(NotificationFactory.mentorNote(ayesha.getId(), mentor.getId(), mentor.getFullName(),
                "Great consistency this week, Ayesha! Keep the OOAD report on track. You're almost there."));
            NotificationService.save(NotificationFactory.mentorNote(bilal.getId(), mentor.getId(), mentor.getFullName(),
                "Hi Bilal, I noticed a few overdue tasks. Can we meet on Monday to plan them?"));
            main[0] = ayesha;
        });
        return main[0];
    }

    private Student student(AuthService auth, String name, String email, int semester, String color, Mentor mentor) {
        Student s = (Student) auth.register(new AuthService.SignupRequest(Role.STUDENT, name, email, PASSWORD, PASSWORD,
            "NED University", semester, mentor.getInviteCode()));
        s.setAvatarColor(color);
        s.setDailyTargetMin(180);
        userDao.update(s);
        return s;
    }

    // ---------- one student's two weeks ----------

    private void fill(Student s, Profile p, boolean isMain) {
        LocalDate today = now.toLocalDate();
        List<Task> tasks = tasks(s, p, today);
        List<Habit> habits = habits(s, today);

        int xp = 0;
        for (int back = DAYS - 1; back >= 0; back--) {
            LocalDate day = today.minusDays(back);
            boolean inactive = back < p.inactiveLastDays();
            // gentle upward trend for the main student so "your score is climbing" shows up
            double trend = isMain ? 0.75 + 0.25 * (DAYS - back) / DAYS : 1.0;
            if (isMain && back == 0) {
                trend = 1.25;      // the demo opens on a good day
            }
            if (inactive) {
                continue;
            }
            xp += focusSessions(s, day, p.focus() * trend, tasks);
            activity(s, day, p, trend);
            if (random.nextDouble() < 0.85) {
                int mood = clamp((int) Math.round(p.moodBase() + random.nextGaussian() * 0.7 + (trend - 1) * 2), 1, 5);
                int stress = clamp((int) Math.round(6 - p.moodBase() + random.nextGaussian() * 0.8), 1, 5);
                moodDao.upsert(new MoodEntry(s.getId(), day, mood, stress, null));
                xp += GamificationService.XP_MOOD;
            }
            if (random.nextDouble() < 0.9) {
                double hours = p.sleepBase() + random.nextGaussian() * 0.7;
                LocalTime wake = LocalTime.of(6, 30).plusMinutes(random.nextInt(90));
                LocalTime bed = wake.minusMinutes((long) (hours * 60));
                sleepDao.upsert(new SleepEntry(s.getId(), day, bed, wake));
            }
            for (Habit h : habits) {
                double chance = h.getTitle().startsWith("Drink") ? 0.85 : h.getTitle().startsWith("Read") ? 0.7 : 0.55;
                if (random.nextDouble() < chance * (p.focus() > 0.5 ? 1 : 0.6)) {
                    habitDao.check(h.getId(), day);
                    xp += GamificationService.XP_HABIT;
                }
            }
        }
        for (Task t : tasks) {
            if (t.isDone()) {
                xp += GamificationService.XP_TASK + (t.wasCompletedOnTime() && t.getDeadline() != null
                    ? GamificationService.XP_TASK_ON_TIME_BONUS : 0);
            }
        }
        userDao.addXp(s.getId(), xp);
        s.setXp(xp);

        GoalService goals = new GoalService(s.getId());
        goals.ensureDefaultGoals(s.getDailyTargetMin());
        goals.addGoal(Goal.Type.FOCUS_SESSIONS, Goal.Period.WEEKLY, 15);
        new ScreenLimitDao().insert(new ScreenLimit(s.getId(), "Instagram", 30));

        new GamificationService(s).checkBadges().forEach(NotificationService::save);
    }

    private List<Task> tasks(Student s, Profile p, LocalDate today) {
        LocalDateTime t0 = today.atStartOfDay();
        List<Task> list = new ArrayList<>();
        // title, subject, priority, deadline offset (days), done?, done offset days (relative to deadline), est min
        Object[][] defs = {
            {"OOAD project: class diagram", "OOAD", Priority.URGENT, 1.5, false, 0, 180},
            {"OOAD project: sequence diagrams", "OOAD", Priority.HIGH, 4.0, false, 0, 150},
            {"Normalization worksheet", "DBMS", Priority.MEDIUM, -6.0, true, -1, 90},
            {"DBMS quiz 2 revision", "DBMS", Priority.HIGH, -3.0, true, 0, 120},
            {"Linear Algebra assignment 3", "Maths", Priority.HIGH, -1.0, false, 0, 120},
            {"Physics lab report 4", "Physics", Priority.MEDIUM, -9.0, true, 1, 60},
            {"Technical writing: email exercise", "Tech Writing", Priority.LOW, 2.0, false, 0, 45},
            {"Read chapter 6: Transactions", "DBMS", Priority.MEDIUM, 6.0, false, 0, 60},
            {"Prepare viva notes", "OOAD", Priority.HIGH, 9.0, false, 0, 120},
            {"Pay hostel dues", null, Priority.LOW, -4.0, true, -2, 15},
            {"Linear Algebra assignment 2", "Maths", Priority.MEDIUM, -11.0, true, -1, 120},
            {"Physics problem set 5", "Physics", Priority.MEDIUM, -2.0, p.focus() > 0.5, 0, 90},
            {"Submit DBMS lab 6", "DBMS", Priority.HIGH, 0.0, p.focus() > 0.5, 0, 60},
        };
        for (Object[] d : defs) {
            Task t = new Task();
            t.setUserId(s.getId());
            t.setTitle((String) d[0]);
            t.setSubject((String) d[1]);
            t.setPriority((Priority) d[2]);
            t.setDeadline(t0.plusMinutes((long) ((Double) d[3] * 24 * 60)).withHour(23).withMinute(59));
            t.setEstimatedMin((Integer) d[6]);
            t.setCreatedAt(t0.minusDays(DAYS));
            boolean done = (Boolean) d[4] && (p.focus() > 0.4 || random.nextBoolean());
            if (done) {
                LocalDateTime finished = t.getDeadline().minusHours(6).plusDays((Integer) d[5]);
                if (finished.isAfter(now.minusMinutes(30))) {
                    finished = now.minusMinutes(30);      // never completed in the future
                }
                t.markDone(finished);
                t.setSpentMin((int) ((Integer) d[6] * (0.7 + random.nextDouble() * 0.5)));
            } else if ((Double) d[3] < 5) {
                t.setStatus(TaskStatus.IN_PROGRESS);
                t.setSpentMin((int) ((Integer) d[6] * random.nextDouble() * 0.6 * p.focus()));
            }
            if (((String) d[0]).startsWith("OOAD project: class")) {
                t.addSubTask(new SubTask("List all classes and attributes", true));
                t.addSubTask(new SubTask("Add relationships (inheritance, composition)", true));
                t.addSubTask(new SubTask("Mark design patterns", p.focus() > 0.7));
                t.addSubTask(new SubTask("Review with group", false));
            }
            if (((String) d[0]).startsWith("Prepare viva")) {
                t.addSubTask(new SubTask("Patterns: Singleton, Observer, Strategy, State, Factory, Builder", false));
                t.addSubTask(new SubTask("Practice demo flow", false));
            }
            taskDao.insert(t);
            list.add(t);
        }
        return list;
    }

    private List<Habit> habits(Student s, LocalDate today) {
        List<Habit> list = new ArrayList<>();
        String[][] defs = {{"Drink 8 glasses of water", "fth-droplet"}, {"Read 20 minutes", "fth-book-open"},
            {"No phone after 11 PM", "fth-moon"}};
        for (String[] d : defs) {
            Habit h = new Habit(s.getId(), d[0], d[1]);
            h.setCreatedAt(today.minusDays(DAYS).atTime(9, 0));
            habitDao.insert(h);
            list.add(h);
        }
        return list;
    }

    /** Returns XP earned. */
    private int focusSessions(Student s, LocalDate day, double intensity, List<Task> tasks) {
        int count = (int) Math.round(intensity * (2 + random.nextInt(4)));
        boolean consistent = intensity >= 0.7;      // strong students focus every day (keeps the streak)
        if (consistent) {
            count = Math.max(2, count);
        }
        int xp = 0;
        LocalDateTime t = day.atTime(8, 0).plusMinutes(random.nextInt(90));
        List<Task> open = tasks;
        for (int i = 0; i < count; i++) {
            int planned = random.nextDouble() < 0.7 ? 25 : 50;
            boolean completed = (consistent && i == 0) || random.nextDouble() < 0.82;
            int actualMin = completed ? planned : 5 + random.nextInt(planned - 5);
            if (t.plusMinutes(actualMin).isAfter(now)) {
                break;     // never create sessions in the future
            }
            Task task = random.nextDouble() < 0.25 ? null : open.get(random.nextInt(open.size()));
            FocusSession f = new FocusSession(s.getId(), task == null ? null : task.getId(), SessionType.FOCUS, planned, t);
            f.setEndedAt(t.plusMinutes(actualMin));
            f.setActualSec(actualMin * 60);
            f.setCompleted(completed);
            f.setDistractions(random.nextDouble() < 0.6 ? 0 : 1 + random.nextInt(2));
            f.setFocusRating(completed && random.nextDouble() < 0.7 ? 3 + random.nextInt(3) : null);
            sessionDao.insert(f);
            if (completed) {
                xp += Math.max(GamificationService.XP_PER_5_FOCUS_MIN, actualMin / 5 * GamificationService.XP_PER_5_FOCUS_MIN);
                // short break after a completed focus session
                FocusSession b = new FocusSession(s.getId(), null, SessionType.SHORT_BREAK, 5, f.getEndedAt());
                b.setEndedAt(f.getEndedAt().plusMinutes(5));
                b.setActualSec(300);
                b.setCompleted(true);
                sessionDao.insert(b);
            }
            t = t.plusMinutes(actualMin + 15 + random.nextInt(150));
            if (t.getHour() >= 23) {
                break;
            }
        }
        return xp;
    }

    private void activity(Student s, LocalDate day, Profile p, double trend) {
        LocalDateTime t = day.atTime(9, 0).plusMinutes(random.nextInt(45));
        LocalDateTime end = day.atTime(22, 30).plusMinutes(random.nextInt(60));
        if (end.isAfter(now)) {
            end = now.minusMinutes(2);
        }
        double productive = Math.min(0.9, p.productiveShare() * trend);
        double distracting = Math.max(0.03, p.distractingShare() / trend);
        while (t.isBefore(end)) {
            // breaks away from the computer (meals, class)
            if (random.nextDouble() < 0.12) {
                t = t.plusMinutes(30 + random.nextInt(90));
                continue;
            }
            double r = random.nextDouble();
            String[] app = r < productive ? pick(PRODUCTIVE) : r < productive + distracting ? pick(DISTRACTING) : pick(NEUTRAL);
            int minutes = 4 + random.nextInt(r < productive ? 40 : 22);
            LocalDateTime stop = t.plusMinutes(minutes);
            if (stop.isAfter(end)) {
                stop = end;
            }
            if (!stop.isAfter(t)) {
                break;
            }
            Category c = classifier.classify(app[0], app[1]);
            ActivityLog log = new ActivityLog(s.getId(), app[0], app[1], c, t);
            log.extendTo(stop);
            activityDao.insert(log);
            t = stop.plusSeconds(random.nextInt(60));
        }
    }

    private String[] pick(String[][] options) {
        return options[random.nextInt(options.length)];
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** True if the demo accounts already exist. */
    public static boolean exists() {
        return new UserDao().emailExists(STUDENT_EMAIL);
    }
}
