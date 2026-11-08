package com.ekagra.service;

import com.ekagra.dao.NotificationDao;
import com.ekagra.dao.SessionDao;
import com.ekagra.db.DatabaseManager;
import com.ekagra.model.Category;
import com.ekagra.model.FocusSession;
import com.ekagra.model.Mentor;
import com.ekagra.model.Notification;
import com.ekagra.model.Priority;
import com.ekagra.model.Report;
import com.ekagra.model.Role;
import com.ekagra.model.SessionType;
import com.ekagra.model.Student;
import com.ekagra.model.Task;
import com.ekagra.pattern.builder.ReportBuilder;
import com.ekagra.pattern.factory.ExporterFactory;
import com.ekagra.pattern.factory.NotificationFactory;
import com.ekagra.pattern.observer.AppEvent;
import com.ekagra.pattern.observer.AppEventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Phase6Test {

    private Student student;
    private Mentor mentor;
    private final List<Notification> shown = new ArrayList<>();

    @BeforeEach
    void setUp() {
        DatabaseManager.shutdown();
        DatabaseManager.configure(DatabaseManager.IN_MEMORY);
        AuthService auth = new AuthService();
        mentor = (Mentor) auth.register(new AuthService.SignupRequest(Role.MENTOR, "Sir Mentor", "m@uni.edu",
            "secret12", "secret12", null, null, null));
        student = (Student) auth.register(new AuthService.SignupRequest(Role.STUDENT, "Stu Dent", "s@uni.edu",
            "secret12", "secret12", "NED", 3, mentor.getInviteCode()));
    }

    @AfterEach
    void tearDown() {
        AppEventBus.clear();
        DatabaseManager.shutdown();
    }

    private FocusSession focus(LocalDateTime start, int minutes) {
        FocusSession s = new FocusSession(student.getId(), null, SessionType.FOCUS, minutes, start);
        s.setActualSec(minutes * 60);
        s.setCompleted(true);
        s.setEndedAt(start.plusMinutes(minutes));
        new SessionDao().insert(s);
        return s;
    }

    // ---------- gamification ----------

    @Test
    void focusEventGivesXpAndFirstBadge() {
        GamificationService g = new GamificationService(student);
        g.setNotifier(shown::addAll);
        g.start();

        FocusSession s = focus(LocalDate.now().atTime(10, 0), 25);
        AppEventBus.publish(new AppEvent.FocusCompleted(student.getId(), s));

        assertEquals(50, student.getXp(), "25 min = 5 x 10 XP");
        assertTrue(shown.stream().anyMatch(n -> n.getTitle().contains("First Focus")));
        assertTrue(g.badgeStatuses().stream().filter(b -> b.badge().getCode().equals("FIRST_FOCUS")).findFirst()
            .orElseThrow().isEarned());

        shown.clear();
        AppEventBus.publish(new AppEvent.FocusCompleted(student.getId(), s));
        assertTrue(shown.stream().noneMatch(n -> n.getTitle().contains("First Focus")), "badges are only awarded once");
    }

    @Test
    void onTimeTaskEarnsBonusAndEventsOfOtherUsersAreIgnored() {
        GamificationService g = new GamificationService(student);
        g.setNotifier(shown::addAll);
        g.start();
        Task t = new Task();
        t.setUserId(student.getId());
        t.setTitle("Report");
        t.setPriority(Priority.HIGH);
        t.setDeadline(LocalDateTime.now().plusDays(1));
        new TaskService().save(t);
        new TaskService().toggleDone(t);            // publishes TaskCompleted
        assertEquals(GamificationService.XP_TASK + GamificationService.XP_TASK_ON_TIME_BONUS, student.getXp());

        AppEventBus.publish(new AppEvent.MoodLogged(mentor.getId(), null));
        assertEquals(50, student.getXp(), "someone else's event gives no XP");
    }

    @Test
    void levelUpAfter500Xp() {
        GamificationService g = new GamificationService(student);
        g.setNotifier(shown::addAll);
        g.start();
        FocusSession big = focus(LocalDate.now().atTime(9, 0), 260);   // 52 * 10 = 520 XP
        AppEventBus.publish(new AppEvent.FocusCompleted(student.getId(), big));
        assertEquals(2, student.getLevel());
        assertTrue(shown.stream().anyMatch(n -> n.getTitle().startsWith("Level up")));
    }

    @Test
    void earlyBirdBadge() {
        focus(LocalDate.now().atTime(6, 30), 25);
        GamificationService g = new GamificationService(student);
        List<Notification> news = g.checkBadges();
        assertTrue(news.stream().anyMatch(n -> n.getTitle().contains("Early Bird")));
    }

    // ---------- reports ----------

    @Test
    void builderCollectsSectionsInOrder() {
        Report r = new ReportBuilder("Test").subtitle("sub")
            .section("A").stat("x", "1").paragraph("hello")
            .section("B").table(List.of("h1", "h2"), List.of(List.of("a", "b")))
            .build();
        assertEquals(2, r.sections().size());
        assertEquals("A", r.sections().get(0).heading());
        assertEquals(1, r.sections().get(0).stats().size());
        assertNotNull(r.sections().get(1).table());
        assertThrows(IllegalStateException.class, () -> new ReportBuilder("x").build());
        assertThrows(IllegalStateException.class, () -> new ReportBuilder("x").stat("a", "b"));
    }

    @Test
    void weeklyReportExportsToPdfAndCsv(@TempDir Path dir) throws Exception {
        focus(LocalDate.now().atTime(10, 0), 50);
        ReportService rs = new ReportService(student);
        Report weekly = rs.weekly(ReportService.weekStart(LocalDate.now()));
        assertTrue(weekly.sections().size() >= 5);

        Path pdf = dir.resolve("w.pdf");
        rs.write(weekly, ExporterFactory.Format.PDF, pdf);
        byte[] head = Files.readAllBytes(pdf);
        assertTrue(head.length > 1000);
        assertEquals("%PDF", new String(head, 0, 4, StandardCharsets.US_ASCII));

        Path csv = dir.resolve("w.csv");
        rs.write(weekly, ExporterFactory.Format.CSV, csv);
        String text = Files.readString(csv, StandardCharsets.UTF_8);
        assertTrue(text.contains("Daily breakdown"));
        assertTrue(text.contains("Focus time"));
    }

    @Test
    void csvEscapesCommasAndQuotes(@TempDir Path dir) throws Exception {
        Task t = new Task();
        t.setUserId(student.getId());
        t.setTitle("Read \"Clean Code\", ch. 2");
        new TaskService().save(t);
        Path csv = dir.resolve("tasks.csv");
        new ReportService(student).write(new ReportService(student).tasksData(), ExporterFactory.Format.CSV, csv);
        String text = Files.readString(csv, StandardCharsets.UTF_8);
        assertTrue(text.contains("\"Read \"\"Clean Code\"\", ch. 2\""), text);
        assertTrue(text.startsWith("﻿Title,Subject"), "single-table export is a plain CSV with a header row");
    }

    // ---------- rules ----------

    @Test
    void onlyMentorsCanEditRulesAndTestUsesThem() {
        assertThrows(ValidationException.class, () -> new RuleService(student).add("game", null, Category.DISTRACTING));
        RuleService rs = new RuleService(mentor);
        rs.add("Game", null, Category.DISTRACTING);
        assertEquals(Category.DISTRACTING, rs.test("game.exe", "Level 3"));
        assertThrows(ValidationException.class, () -> rs.add("game.exe", null, Category.NEUTRAL), "duplicate");
        assertThrows(ValidationException.class, () -> rs.add(" ", "", Category.NEUTRAL), "empty");
        rs.add("chrome", "OOAD lecture", Category.PRODUCTIVE);
        assertEquals(Category.PRODUCTIVE, rs.test("chrome", "OOAD lecture 4 - YouTube"), "specific rule beats YouTube");
    }

    // ---------- mentor ----------

    @Test
    void mentorSeesLinkedStudentFlaggedAndCanSendNote() {
        Task late = new Task();
        late.setUserId(student.getId());
        late.setTitle("Late 1");
        late.setDeadline(LocalDateTime.now().minusDays(2));
        new TaskService().save(late);
        Task late2 = new Task();
        late2.setUserId(student.getId());
        late2.setTitle("Late 2");
        late2.setDeadline(LocalDateTime.now().minusDays(1));
        new TaskService().save(late2);

        MentorService ms = new MentorService(mentor);
        var overview = ms.overview();
        assertEquals(1, overview.size());
        assertTrue(overview.get(0).isAtRisk());
        assertTrue(overview.get(0).riskReasons().stream().anyMatch(r -> r.contains("overdue")));
        assertTrue(overview.get(0).riskReasons().stream().anyMatch(r -> r.contains("Never active")));

        ms.sendNote(student, "Keep going!");
        var inbox = NotificationService.recent(student.getId(), 5);
        assertEquals(Notification.Type.MENTOR, inbox.get(0).getType());
        assertEquals(mentor.getId(), inbox.get(0).getSenderId());
        assertThrows(ValidationException.class, () -> ms.sendNote(student, "  "));
    }

    @Test
    void deadlineDuplicateCheck() {
        NotificationDao dao = new NotificationDao();
        NotificationService.save(NotificationFactory.deadlineSoon(student.getId(), "Essay", "due in 3h"));
        assertTrue(dao.existsToday(student.getId(), Notification.Type.DEADLINE, "\"Essay\""));
        assertFalse(dao.existsToday(student.getId(), Notification.Type.DEADLINE, "\"Other\""));
        assertEquals(1, NotificationService.unreadCount(student.getId()));
        NotificationService.markAllRead(student.getId());
        assertEquals(0, NotificationService.unreadCount(student.getId()));
    }
}
