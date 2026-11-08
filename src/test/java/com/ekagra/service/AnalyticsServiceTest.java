package com.ekagra.service;

import com.ekagra.dao.ActivityDao;
import com.ekagra.dao.SessionDao;
import com.ekagra.db.DatabaseManager;
import com.ekagra.model.ActivityLog;
import com.ekagra.model.Category;
import com.ekagra.model.FocusSession;
import com.ekagra.model.Role;
import com.ekagra.model.SessionType;
import com.ekagra.model.Student;
import com.ekagra.pattern.strategy.StudyFocusedStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsServiceTest {

    private Student student;

    @BeforeEach
    void setUp() {
        DatabaseManager.shutdown();
        DatabaseManager.configure(DatabaseManager.IN_MEMORY);
        student = (Student) new AuthService().register(new AuthService.SignupRequest(Role.STUDENT, "Ana Lytics",
            "an@uni.edu", "secret12", "secret12", null, 1, null));
    }

    @AfterEach
    void tearDown() {
        DatabaseManager.shutdown();
    }

    private void focus(LocalDate day, int minutes) {
        LocalDateTime start = day.atTime(9, 0);
        FocusSession s = new FocusSession(student.getId(), null, SessionType.FOCUS, minutes, start);
        s.setActualSec(minutes * 60);
        s.setCompleted(true);
        s.setEndedAt(start.plusMinutes(minutes));
        new SessionDao().insert(s);
    }

    private void activity(LocalDate day, int hour, Category c, int minutes) {
        ActivityLog a = new ActivityLog(student.getId(), "app.exe", "x", c, day.atTime(hour, 0));
        a.extendTo(day.atTime(hour, 0).plusMinutes(minutes));
        new ActivityDao().insert(a);
    }

    @Test
    void lastDaysReturnsOneSummaryPerDayEndingToday() {
        AnalyticsService a = new AnalyticsService(student);
        var week = a.lastDays(7);
        assertEquals(7, week.size());
        assertEquals(LocalDate.now(), week.get(6).date());
        assertEquals("Today", week.get(6).dayLabel());
    }

    @Test
    void streakCountsConsecutiveFocusDays() {
        LocalDate today = LocalDate.now();
        focus(today.minusDays(1), 25);
        focus(today.minusDays(2), 25);
        focus(today.minusDays(4), 25);   // gap on day -3
        AnalyticsService a = new AnalyticsService(student);
        assertEquals(2, a.focusStreak(), "today has nothing yet, streak counts from yesterday");
        focus(today, 25);
        assertEquals(3, a.focusStreak());
    }

    @Test
    void scoreReflectsDataAndStrategyIsRemembered() {
        LocalDate today = LocalDate.now();
        focus(today, student.getDailyTargetMin());
        activity(today, 10, Category.PRODUCTIVE, 120);
        AnalyticsService a = new AnalyticsService(student);
        int balanced = a.summaryFor(today).score().total();
        assertTrue(balanced >= 60, "full focus target + all productive should score well, was " + balanced);

        a.setStrategy(new StudyFocusedStrategy());
        assertEquals("STUDY", new AnalyticsService(student).getStrategy().getId(), "choice is saved in settings");
    }

    @Test
    void insightsFindMostProductiveHours() {
        LocalDate today = LocalDate.now();
        for (int d = 0; d < 3; d++) {
            activity(today.minusDays(d), 9, Category.PRODUCTIVE, 50);
            activity(today.minusDays(d), 10, Category.PRODUCTIVE, 50);
            activity(today.minusDays(d), 20, Category.DISTRACTING, 30);
        }
        AnalyticsService a = new AnalyticsService(student);
        var insights = a.insights(a.lastDays(7));
        assertTrue(insights.stream().anyMatch(i -> i.text().contains("9 AM and 11 AM")),
            "expected a most-productive-hours insight, got " + insights);
    }
}
