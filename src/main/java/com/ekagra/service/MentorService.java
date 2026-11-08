package com.ekagra.service;

import com.ekagra.dao.ActivityDao;
import com.ekagra.dao.SessionDao;
import com.ekagra.dao.TaskDao;
import com.ekagra.dao.UserDao;
import com.ekagra.model.FocusSession;
import com.ekagra.model.Mentor;
import com.ekagra.model.Student;
import com.ekagra.model.Task;
import com.ekagra.pattern.factory.NotificationFactory;
import com.ekagra.service.AnalyticsService.DaySummary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Mentor panel logic (FR-38, FR-39): overview of linked students, at-risk detection, notes. */
public class MentorService {

    /** Below this 7-day average score a student is flagged. */
    public static final int AT_RISK_SCORE = 40;
    public static final int AT_RISK_OVERDUE = 2;
    public static final int AT_RISK_INACTIVE_DAYS = 3;

    private final Mentor mentor;
    private final UserDao userDao = new UserDao();
    private final TaskDao taskDao = new TaskDao();
    private final SessionDao sessionDao = new SessionDao();
    private final ActivityDao activityDao = new ActivityDao();

    public MentorService(Mentor mentor) {
        this.mentor = mentor;
    }

    public record StudentOverview(Student student, List<DaySummary> week, int avgScore, int focusMinutes,
                                  int overdue, LocalDateTime lastActive, List<String> riskReasons) {
        public boolean isAtRisk() {
            return !riskReasons.isEmpty();
        }
    }

    public List<StudentOverview> overview() {
        List<StudentOverview> out = new ArrayList<>();
        for (Student s : userDao.findStudentsOfMentor(mentor.getId())) {
            mentor.addStudent(s);
            out.add(overviewOf(s));
        }
        // most worrying first
        out.sort((a, b) -> Boolean.compare(b.isAtRisk(), a.isAtRisk()) != 0
            ? Boolean.compare(b.isAtRisk(), a.isAtRisk()) : Integer.compare(a.avgScore(), b.avgScore()));
        return out;
    }

    public StudentOverview overviewOf(Student s) {
        List<DaySummary> week = new AnalyticsService(s).lastDays(7);
        var active = week.stream().filter(DaySummary::hasData).toList();
        int avg = (int) Math.round(active.stream().mapToInt(d -> d.score().total()).average().orElse(0));
        int focus = week.stream().mapToInt(DaySummary::focusMinutes).sum();
        LocalDateTime now = LocalDateTime.now();
        int overdue = (int) taskDao.findByUser(s.getId()).stream().filter(t -> t.isOverdue(now)).count();
        LocalDateTime last = lastActive(s.getId());

        List<String> reasons = new ArrayList<>();
        if (!active.isEmpty() && avg < AT_RISK_SCORE) {
            reasons.add("Low score (" + avg + ")");
        }
        if (overdue >= AT_RISK_OVERDUE) {
            reasons.add(overdue + " overdue tasks");
        }
        if (last == null || last.isBefore(now.minusDays(AT_RISK_INACTIVE_DAYS))) {
            reasons.add(last == null ? "Never active" : "Inactive " + java.time.Duration.between(last, now).toDays() + " days");
        }
        return new StudentOverview(s, week, avg, focus, overdue, last, reasons);
    }

    /** Most recent focus session or tracked activity. */
    public LocalDateTime lastActive(int studentId) {
        LocalDateTime a = sessionDao.findRecent(studentId, 1).stream().findFirst()
            .map(FocusSession::getStartedAt).orElse(null);
        LocalDateTime b = activityDao.findRecent(studentId, 1).stream().findFirst()
            .map(com.ekagra.model.ActivityLog::getEndedAt).orElse(null);
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    public List<Task> overdueTasks(Student s) {
        LocalDateTime now = LocalDateTime.now();
        return taskDao.findByUser(s.getId()).stream().filter(t -> t.isOverdue(now)).toList();
    }

    public List<ActivityDao.AppUsage> topApps(Student s) {
        LocalDate today = LocalDate.now();
        return activityDao.topApps(s.getId(), today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay(), 6);
    }

    /** Saves a note for the student; it appears in their notification centre. */
    public void sendNote(Student s, String text) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Write a message first.");
        }
        if (text.length() > 500) {
            throw new ValidationException("Keep notes under 500 characters.");
        }
        NotificationService.save(NotificationFactory.mentorNote(s.getId(), mentor.getId(), mentor.getFullName(), text.trim()));
    }

    public Optional<Student> find(int studentId) {
        return userDao.findById(studentId).filter(u -> u instanceof Student).map(Student.class::cast);
    }
}
