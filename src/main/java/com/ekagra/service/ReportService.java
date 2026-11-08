package com.ekagra.service;

import com.ekagra.dao.ActivityDao;
import com.ekagra.dao.MoodDao;
import com.ekagra.dao.SessionDao;
import com.ekagra.dao.SleepDao;
import com.ekagra.dao.TaskDao;
import com.ekagra.export.Exporter;
import com.ekagra.model.ActivityLog;
import com.ekagra.model.Category;
import com.ekagra.model.FocusSession;
import com.ekagra.model.MoodEntry;
import com.ekagra.model.Report;
import com.ekagra.model.SleepEntry;
import com.ekagra.model.Student;
import com.ekagra.model.Task;
import com.ekagra.pattern.builder.ReportBuilder;
import com.ekagra.pattern.factory.ExporterFactory;
import com.ekagra.service.AnalyticsService.DaySummary;
import com.ekagra.util.Formats;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Builds reports (Builder) and writes them as PDF / CSV (Factory) (FR-36, FR-37). */
public class ReportService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Map<Category, String> COLORS = Map.of(
        Category.PRODUCTIVE, "#2F8F5B", Category.NEUTRAL, "#4A86CC", Category.DISTRACTING, "#D9534F");

    private final Student student;
    private final AnalyticsService analytics;

    public ReportService(Student student) {
        this.student = student;
        this.analytics = new AnalyticsService(student);
    }

    public static LocalDate weekStart(LocalDate anyDay) {
        return anyDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public void write(Report report, ExporterFactory.Format format, Path file) throws IOException {
        Exporter exporter = ExporterFactory.create(format);
        exporter.export(report, file);
    }

    // ---------- weekly report ----------

    public Report weekly(LocalDate monday) {
        List<DaySummary> days = analytics.days(monday, 7);
        LocalDate last = days.isEmpty() ? monday : days.get(days.size() - 1).date();

        int focus = days.stream().mapToInt(DaySummary::focusMinutes).sum();
        int sessions = days.stream().mapToInt(DaySummary::completedSessions).sum();
        int screen = days.stream().mapToInt(DaySummary::screenSeconds).sum();
        int tasksDone = days.stream().mapToInt(DaySummary::tasksDone).sum();
        var active = days.stream().filter(DaySummary::hasData).toList();
        int avgScore = (int) Math.round(active.stream().mapToInt(d -> d.score().total()).average().orElse(0));

        ReportBuilder b = new ReportBuilder("Weekly report")
            .subtitle(student.getFullName() + "  ·  " + monday.format(DateTimeFormatter.ofPattern("d MMM"))
                + " – " + last.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
                + (student.getUniversity() == null || student.getUniversity().isBlank() ? "" : "  ·  " + student.getUniversity()));

        b.section("Summary")
            .stat("Productivity score (avg)", active.isEmpty() ? "-" : avgScore + " / 100")
            .stat("Focus time", Formats.minutes(focus))
            .stat("Focus sessions", String.valueOf(sessions))
            .stat("Tasks completed", String.valueOf(tasksDone))
            .stat("Screen time", Formats.seconds(screen))
            .stat("Scoring mode", analytics.getStrategy().getName())
            .stat("Days active", active.size() + " of " + days.size())
            .stat("Level", student.getLevel() + " (" + student.getXp() + " XP)");

        List<List<String>> rows = new ArrayList<>();
        for (DaySummary d : days) {
            rows.add(List.of(d.date().format(DAY), d.hasData() ? String.valueOf(d.score().total()) : "-",
                Formats.minutes(d.focusMinutes()), Formats.seconds(d.screenSeconds()),
                Formats.seconds(d.secondsByCategory().get(Category.PRODUCTIVE)),
                Formats.seconds(d.secondsByCategory().get(Category.DISTRACTING)), String.valueOf(d.tasksDone())));
        }
        b.section("Daily breakdown")
            .table(List.of("Day", "Score", "Focus", "Screen", "Productive", "Distracting", "Tasks"), rows);

        b.section("Focus time per day");
        int target = student.getDailyTargetMin();
        double maxFocus = Math.max(target, days.stream().mapToInt(DaySummary::focusMinutes).max().orElse(0));
        for (DaySummary d : days) {
            b.bar(d.date().format(DAY), d.focusMinutes(), maxFocus, Formats.minutes(d.focusMinutes()),
                d.focusMinutes() >= target ? "#2F8F5B" : "#226192");
        }
        b.paragraph("Daily target: " + Formats.minutes(target) + ". Green bars reached the target.");

        Map<Category, Integer> totals = new EnumMap<>(Category.class);
        for (Category c : Category.values()) {
            totals.put(c, days.stream().mapToInt(d -> d.secondsByCategory().get(c)).sum());
        }
        int all = Math.max(1, totals.values().stream().mapToInt(Integer::intValue).sum());
        b.section("Where the time went");
        for (Category c : Category.values()) {
            int sec = totals.get(c);
            b.bar(c.getLabel(), sec, all, Formats.seconds(sec) + " (" + Math.round(100f * sec / all) + "%)", COLORS.get(c));
        }

        List<ActivityDao.AppUsage> apps = new ActivityDao().topApps(student.getId(), monday.atStartOfDay(),
            last.plusDays(1).atStartOfDay(), 8);
        if (!apps.isEmpty()) {
            b.section("Top apps").table(List.of("App", "Category", "Time"),
                apps.stream().map(a -> List.of(ActivityLog.displayName(a.appName()), a.category().getLabel(),
                    Formats.seconds(a.seconds()))).toList());
        }

        var goals = new GoalService(student.getId()).goalsWithProgress(last);
        if (!goals.isEmpty()) {
            b.section("Goals (status at end of period)").table(List.of("Goal", "Progress", "Status"),
                goals.stream().map(p -> {
                    String unit = p.goal().getType().getUnit();
                    String prog = unit.equals("min") ? Formats.minutes(p.current()) + " / " + Formats.minutes(p.target())
                        : p.current() + " / " + p.target();
                    String status = p.isOverLimit() ? "Over limit" : p.isAchieved() ? "Achieved" : Math.round(p.fraction() * 100) + "%";
                    return List.of(p.goal().getTitle(), prog, status);
                }).toList());
        }

        List<MoodEntry> moods = new MoodDao().findBetween(student.getId(), monday, last);
        List<SleepEntry> sleeps = new SleepDao().findBetween(student.getId(), monday, last);
        b.section("Wellbeing")
            .stat("Mood check-ins", String.valueOf(moods.size()))
            .stat("Average mood", moods.isEmpty() ? "-" : String.format("%.1f / 5", moods.stream().mapToInt(MoodEntry::getMood).average().orElse(0)))
            .stat("Average stress", moods.isEmpty() ? "-" : String.format("%.1f / 5", moods.stream().mapToInt(MoodEntry::getStress).average().orElse(0)))
            .stat("Average sleep", sleeps.isEmpty() ? "-" : String.format("%.1f h", sleeps.stream().mapToDouble(SleepEntry::getHours).average().orElse(0)));

        b.section("Insights");
        analytics.insights(days).forEach(i -> b.paragraph(i.text()));
        return b.build();
    }

    // ---------- raw data (CSV) ----------

    public Report tasksData() {
        List<Task> tasks = new TaskDao().findByUser(student.getId());
        return new ReportBuilder("Tasks").section("Tasks").table(
            List.of("Title", "Subject", "Priority", "Status", "Deadline", "Estimated (min)", "Focused (min)",
                "Subtasks done", "Created", "Completed"),
            tasks.stream().map(t -> List.of(t.getTitle(), nz(t.getSubject()), t.getPriority().getLabel(),
                t.getStatus().getLabel(), fmt(t.getDeadline()), String.valueOf(t.getEstimatedMin()),
                String.valueOf(t.getSpentMin()), t.getDoneSubtaskCount() + "/" + t.getSubtasks().size(),
                fmt(t.getCreatedAt()), fmt(t.getCompletedAt()))).toList()).build();
    }

    public Report sessionsData(int days) {
        LocalDateTime from = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        List<FocusSession> sessions = new SessionDao().findBetween(student.getId(), from, LocalDate.now().plusDays(1).atStartOfDay());
        return new ReportBuilder("Focus sessions").section("Focus sessions").table(
            List.of("Started", "Ended", "Type", "Task", "Planned (min)", "Actual (min)", "Completed", "Distractions", "Rating"),
            sessions.stream().map(s -> List.of(fmt(s.getStartedAt()), fmt(s.getEndedAt()), s.getType().getLabel(),
                nz(s.getTaskTitle()), String.valueOf(s.getPlannedMin()), String.valueOf(s.getActualMinutes()),
                s.isCompleted() ? "yes" : "no", String.valueOf(s.getDistractions()),
                s.getFocusRating() == null ? "" : String.valueOf(s.getFocusRating()))).toList()).build();
    }

    public Report activityData(int days) {
        LocalDateTime from = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        List<ActivityLog> logs = new ActivityDao().findBetween(student.getId(), from, LocalDate.now().plusDays(1).atStartOfDay());
        return new ReportBuilder("Activity").section("Activity").table(
            List.of("Started", "Ended", "App", "Window title", "Category", "Seconds"),
            logs.stream().map(a -> List.of(fmt(a.getStartedAt()), fmt(a.getEndedAt()), a.getAppName(),
                nz(a.getWindowTitle()), a.getCategory().getLabel(), String.valueOf(a.getDurationSec()))).toList()).build();
    }

    private static String fmt(LocalDateTime t) {
        return t == null ? "" : t.format(ISO);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
