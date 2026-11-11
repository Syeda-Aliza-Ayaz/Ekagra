package com.ekagra.service;

import com.ekagra.app.Screen;
import com.ekagra.dao.ActivityDao;
import com.ekagra.dao.MoodDao;
import com.ekagra.dao.SleepDao;
import com.ekagra.dao.TaskDao;
import com.ekagra.model.Category;
import com.ekagra.model.MoodEntry;
import com.ekagra.model.SleepEntry;
import com.ekagra.model.Student;
import com.ekagra.model.Task;
import com.ekagra.model.TaskStatus;
import com.ekagra.service.AnalyticsService.DaySummary;
import com.ekagra.util.Formats;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Intelligent study companion & focus mentor — "Rahnuma" (Urdu: رہنما).
 * Completely grounded in the student's real SQLite activity, habits, and wellbeing.
 * Evaluates heuristically with 100% offline reliability for academic vivas and presentations.
 */
public class RahnumaService {

    public record RahnumaResponse(String text, String actionLabel, Screen targetScreen) {
        public RahnumaResponse(String text) {
            this(text, null, null);
        }
    }

    private final Student student;
    private final AnalyticsService analytics;
    private final ActivityDao activityDao;
    private final TaskDao taskDao;
    private final SleepDao sleepDao;
    private final MoodDao moodDao;

    public RahnumaService(Student student) {
        this.student = student;
        this.analytics = new AnalyticsService(student);
        this.activityDao = new ActivityDao();
        this.taskDao = new TaskDao();
        this.sleepDao = new SleepDao();
        this.moodDao = new MoodDao();
    }

    public String getGreeting() {
        String name = student != null ? student.getFirstName() : "student";
        return "Salam " + name + "! I am Rahnuma (رہنما), your study guide and focus companion. "
            + "I observe your focus, habits, and wellbeing so you can navigate your studies with clarity and intention. "
            + "How can I guide your study session right now?";
    }

    /** 1. Deep analysis of today\'s focus, score, and rhythm. */
    public RahnumaResponse analyzeToday() {
        List<DaySummary> week = analytics.lastDays(7);
        DaySummary today = week.get(week.size() - 1);
        DaySummary yesterday = week.get(week.size() - 2);

        int score = today.score().total();
        int delta = score - yesterday.score().total();
        String deltaStr = (delta >= 0 ? "+" : "") + delta;

        int focusMin = today.focusMinutes();
        int targetMin = student.getDailyTargetMin();
        int remaining = Math.max(0, targetMin - focusMin);

        StringBuilder sb = new StringBuilder();
        sb.append("📊 Today\'s Focus Analysis:\n\n");
        sb.append("• Productivity Score: ").append(score).append("/100 (").append(deltaStr).append(" vs yesterday)\n");
        sb.append("• Deep Focus Logged: ").append(focusMin).append(" min / ").append(targetMin).append(" min target\n");
        sb.append("• Pomodoro Blocks Completed: ").append(today.completedSessions()).append(" sessions\n");
        sb.append("• Tasks Finished Today: ").append(today.tasksDone()).append("\n\n");

        if (focusMin >= targetMin) {
            sb.append("✨ Mashallah, you have reached your daily focus target! Take a mindful break or wrap up gently.");
            return new RahnumaResponse(sb.toString(), "View Analytics", Screen.ANALYTICS);
        } else {
            int blocksNeeded = (int) Math.ceil(remaining / 25.0);
            sb.append("💡 You need ").append(remaining).append(" more minutes to reach your goal (~")
              .append(blocksNeeded).append(" standard 25m Pomodoro block").append(blocksNeeded > 1 ? "s" : "").append("). Let\'s do one now!");
            return new RahnumaResponse(sb.toString(), "Start Pomodoro Session", Screen.FOCUS);
        }
    }

    /** 2. Audits distracting apps tracked today and suggests boundaries. */
    public RahnumaResponse distractionAudit() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<ActivityDao.AppUsage> topApps = activityDao.topApps(student.getId(), startOfDay, now, 10);
        List<ActivityDao.AppUsage> distracting = topApps.stream()
            .filter(a -> a.category() == Category.DISTRACTING)
            .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("🛡️ Distraction & Focus Guard Audit:\n\n");

        if (distracting.isEmpty()) {
            sb.append("Excellent discipline! No major distracting applications have pulled you off track today. Keep your attention protected.");
            return new RahnumaResponse(sb.toString(), "Review Focus Rules", Screen.RULES);
        }

        int totalDistractingSec = distracting.stream().mapToInt(ActivityDao.AppUsage::seconds).sum();
        int totalDistractingMin = totalDistractingSec / 60;

        sb.append("Tracked distracting screen time today: ").append(totalDistractingMin).append(" minutes.\n\n");
        sb.append("Top attention leaks:\n");
        for (int i = 0; i < Math.min(3, distracting.size()); i++) {
            ActivityDao.AppUsage a = distracting.get(i);
            sb.append(" • ").append(a.appName()).append(": ").append(Formats.seconds(a.seconds())).append("\n");
        }

        sb.append("\n💡 Recommendation: Add these apps to your Distraction Guard blocklist or enable 20-minute app limits.");
        return new RahnumaResponse(sb.toString(), "Configure Rules", Screen.RULES);
    }

    /** 3. Inspects deadlines and plans realistic focus blocks. */
    public RahnumaResponse planUpcomingTasks() {
        List<Task> tasks = taskDao.findByUser(student.getId());
        List<Task> active = tasks.stream()
            .filter(t -> t.getStatus() != TaskStatus.DONE)
            .sorted(Comparator.comparing(Task::getPriority).reversed())
            .toList();

        if (active.isEmpty()) {
            return new RahnumaResponse("🎉 You have no pending tasks on your list! Take time to reflect, organize future goals, or rest.", "Add New Task", Screen.TASKS);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📝 Task Prioritization & Study Plan:\n\n");
        sb.append("You have ").append(active.size()).append(" active task(s). Here is the recommended priority order:\n\n");

        for (int i = 0; i < Math.min(3, active.size()); i++) {
            Task t = active.get(i);
            int est = t.getEstimatedMin() > 0 ? t.getEstimatedMin() : 30;
            int blocks = (int) Math.ceil(est / 25.0);
            sb.append(i + 1).append(". **").append(t.getTitle()).append("** [").append(t.getPriority()).append("]\n");
            sb.append("   Estimated: ").append(est).append(" min (~").append(blocks).append(" Pomodoro blocks)\n");
            if (t.getDeadline() != null) {
                sb.append("   Deadline: ").append(t.getDeadline().format(Formats.DATE_TIME)).append("\n");
            }
        }

        Task top = active.get(0);
        sb.append("\n💡 Action: Focus exclusively on \"").append(top.getTitle()).append("\" first before touching anything else.");
        return new RahnumaResponse(sb.toString(), "Open Tasks", Screen.TASKS);
    }

    /** 4. Evaluates mood, sleep, and digital wellbeing. */
    public RahnumaResponse reflectWellbeing() {
        List<DaySummary> week = analytics.lastDays(7);
        DaySummary today = week.get(week.size() - 1);

        LocalDate todayDate = LocalDate.now();
        Optional<SleepEntry> sleepOpt = sleepDao.findByDate(student.getId(), todayDate);
        Optional<MoodEntry> moodOpt = moodDao.findByDate(student.getId(), todayDate);

        StringBuilder sb = new StringBuilder();
        sb.append("🌿 Digital Wellbeing & Energy Reflection:\n\n");

        if (sleepOpt.isPresent()) {
            double sleep = sleepOpt.get().getHours();
            sb.append("• Sleep Logged: ").append(String.format("%.1f", sleep)).append(" hours\n");
            if (sleep < 6.5) {
                sb.append("  ⚠️ Sleep deficit detected. Cognitive endurance drops significantly under 7h of rest. Avoid high-stress cramming tonight.\n");
            } else {
                sb.append("  ✓ Good restorative rest logged. Your brain is primed for analytical thinking.\n");
            }
        } else {
            sb.append("• Sleep: Not logged yet today.\n");
        }

        if (moodOpt.isPresent()) {
            MoodEntry m = moodOpt.get();
            sb.append("• Mood Rating: ").append(m.getMood()).append("/5 (").append(m.getMoodLabel()).append(")\n");
            sb.append("• Stress Level: ").append(m.getStress()).append("/5 (").append(m.getStressLabel()).append(")\n");
        } else {
            sb.append("• Mood & Stress: Not logged yet today.\n");
        }

        int screenMin = today.screenSeconds() / 60;
        sb.append("• Screen Time: ").append(screenMin).append(" min today\n\n");
        sb.append("💡 Gentle Reminder: Practice the 20-20-20 eye rest rule: every 20 minutes, gaze at something 20 feet away for 20 seconds.");

        return new RahnumaResponse(sb.toString(), "Log Mood & Sleep", Screen.WELLBEING);
    }

    /** Conversational responder for user write-in queries. */
    public RahnumaResponse answerQuery(String query) {
        if (query == null || query.isBlank()) {
            return new RahnumaResponse("I am here to guide you! You can ask me to analyze your day, audit distractions, or plan upcoming deadlines.");
        }

        String q = query.toLowerCase();
        if (q.contains("score") || q.contains("analyze") || q.contains("today") || q.contains("progress")) {
            return analyzeToday();
        } else if (q.contains("distract") || q.contains("waste") || q.contains("youtube") || q.contains("social") || q.contains("game")) {
            return distractionAudit();
        } else if (q.contains("task") || q.contains("deadline") || q.contains("plan") || q.contains("exam") || q.contains("study")) {
            return planUpcomingTasks();
        } else if (q.contains("sleep") || q.contains("mood") || q.contains("tired") || q.contains("wellbeing") || q.contains("break")) {
            return reflectWellbeing();
        } else if (q.contains("pomodoro") || q.contains("timer") || q.contains("focus")) {
            return new RahnumaResponse("A standard Pomodoro consists of 25 minutes of single-task immersion followed by a 5-minute break. After 4 sessions, take an extended 15-minute rest.", "Open Focus Timer", Screen.FOCUS);
        } else if (q.contains("theme") || q.contains("color") || q.contains("appearance")) {
            return new RahnumaResponse("You can fully customize Ekagra\'s color palette or pick your custom accent color in Settings.", "Open Settings", Screen.SETTINGS);
        }

        return new RahnumaResponse("That is a thoughtful inquiry. The core philosophy of Ekagra is \"one-pointed focus\": eliminating competing stimuli and giving your full attention to one meaningful task at a time. What task would you like to dedicate your next 25 minutes to?", "Start Focus", Screen.FOCUS);
    }
}
