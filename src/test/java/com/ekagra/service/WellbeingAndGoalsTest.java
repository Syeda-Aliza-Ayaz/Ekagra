package com.ekagra.service;

import com.ekagra.dao.ActivityDao;
import com.ekagra.dao.SessionDao;
import com.ekagra.db.DatabaseManager;
import com.ekagra.model.ActivityLog;
import com.ekagra.model.Category;
import com.ekagra.model.FocusSession;
import com.ekagra.model.Goal;
import com.ekagra.model.Habit;
import com.ekagra.model.MoodEntry;
import com.ekagra.model.Role;
import com.ekagra.model.SessionType;
import com.ekagra.model.SleepEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class WellbeingAndGoalsTest {

    private int userId;

    @BeforeEach
    void setUp() {
        DatabaseManager.shutdown();
        DatabaseManager.configure(DatabaseManager.IN_MEMORY);
        userId = new AuthService().register(new AuthService.SignupRequest(Role.STUDENT, "Well Being", "wb@uni.edu",
            "secret12", "secret12", null, 1, null)).getId();
    }

    @AfterEach
    void tearDown() {
        DatabaseManager.shutdown();
    }

    // ---------- pure score calculation ----------

    @Test
    void perfectDayScoresHigh() {
        SleepEntry sleep = new SleepEntry(userId, LocalDate.now(), LocalTime.of(23, 0), LocalTime.of(7, 0)); // 8 h
        MoodEntry mood = new MoodEntry(userId, LocalDate.now(), 5, 1, null);                                  // great, calm
        var score = WellbeingService.computeScore(sleep, mood, 3 * 3600, 0, 360);                              // 3 h of 6 h
        assertEquals(100, score.total());
        assertEquals(4, score.parts().size());
    }

    @Test
    void badDayScoresLow() {
        SleepEntry sleep = new SleepEntry(userId, LocalDate.now(), LocalTime.of(3, 0), LocalTime.of(7, 0));   // 4 h
        MoodEntry mood = new MoodEntry(userId, LocalDate.now(), 1, 5, null);
        var score = WellbeingService.computeScore(sleep, mood, 9 * 3600, 5 * 3600, 360);                       // 150 % limit
        assertTrue(score.total() <= 5, "was " + score.total());
    }

    @Test
    void missingPartsAreSkippedNotCountedAsZero() {
        MoodEntry mood = new MoodEntry(userId, LocalDate.now(), 4, 2, null);   // (75 + 75) / 2
        var score = WellbeingService.computeScore(null, mood, 0, 0, 360);
        assertEquals(1, score.parts().size());
        assertEquals(75, score.total());
        assertFalse(WellbeingService.computeScore(null, null, 0, 0, 360).hasData());
    }

    @Test
    void sleepAcrossMidnightIsComputedCorrectly() {
        assertEquals(450, new SleepEntry(userId, LocalDate.now(), LocalTime.of(23, 30), LocalTime.of(7, 0)).getDurationMin());
        assertEquals(360, new SleepEntry(userId, LocalDate.now(), LocalTime.of(1, 0), LocalTime.of(7, 0)).getDurationMin());
    }

    // ---------- persistence ----------

    @Test
    void moodCheckInIsOnePerDay() {
        WellbeingService w = new WellbeingService(userId);
        w.saveMood(2, 4, "exam tomorrow");
        w.saveMood(4, 2, "feeling better");
        assertEquals(1, w.moodHistory(7).size());
        assertEquals(4, w.todaysMood().orElseThrow().getMood());
        assertThrows(ValidationException.class, () -> w.saveMood(0, 3, null));
    }

    @Test
    void appLimitsValidateAndMatchTitles() {
        WellbeingService w = new WellbeingService(userId);
        w.addAppLimit("Instagram", 30);
        assertThrows(ValidationException.class, () -> w.addAppLimit("instagram", 20), "duplicate");
        assertThrows(ValidationException.class, () -> w.addAppLimit("", 20));

        ActivityLog a = new ActivityLog(userId, "chrome.exe", "Reels • Instagram", Category.DISTRACTING,
            LocalDate.now().atTime(9, 0));
        a.extendTo(LocalDate.now().atTime(9, 40));
        new ActivityDao().insert(a);
        assertEquals(40 * 60, w.secondsOnAppToday("Instagram"));
    }

    // ---------- habits ----------

    @Test
    void habitStreaks() {
        Habit h = new Habit(userId, "Read", "fth-book-open");
        LocalDate today = LocalDate.of(2026, 11, 7);
        h.addCheck(today.minusDays(1));
        h.addCheck(today.minusDays(2));
        h.addCheck(today.minusDays(3));
        assertEquals(3, h.currentStreak(today), "today not ticked yet still keeps yesterday's streak");
        h.addCheck(today);
        assertEquals(4, h.currentStreak(today));
        h.addCheck(today.minusDays(10));
        assertEquals(4, h.bestStreak());
        assertEquals(0, new Habit().currentStreak(today));
    }

    @Test
    void habitToggleOnlyForLastWeek() {
        GoalService g = new GoalService(userId);
        Habit h = g.addHabit("Drink water", null);
        g.toggleHabit(h, LocalDate.now());
        assertTrue(g.habits().get(0).isDoneOn(LocalDate.now()));
        g.toggleHabit(h, LocalDate.now());
        assertFalse(g.habits().get(0).isDoneOn(LocalDate.now()));
        assertThrows(ValidationException.class, () -> g.toggleHabit(h, LocalDate.now().minusDays(10)));
        assertThrows(ValidationException.class, () -> g.toggleHabit(h, LocalDate.now().plusDays(1)));
    }

    // ---------- goals ----------

    @Test
    void goalProgressUsesRealData() {
        GoalService g = new GoalService(userId);
        g.ensureDefaultGoals(90);
        assertEquals(3, g.goalsWithProgress().size());
        g.ensureDefaultGoals(90);
        assertEquals(3, g.goalsWithProgress().size(), "defaults are only created once");

        LocalDateTime start = LocalDate.now().atTime(8, 0);
        FocusSession s = new FocusSession(userId, null, SessionType.FOCUS, 50, start);
        s.setActualSec(50 * 60);
        s.setCompleted(true);
        s.setEndedAt(start.plusMinutes(50));
        new SessionDao().insert(s);

        ActivityLog yt = new ActivityLog(userId, "chrome.exe", "YouTube", Category.DISTRACTING, start.plusHours(2));
        yt.extendTo(start.plusHours(2).plusMinutes(75));
        new ActivityDao().insert(yt);

        var progress = g.goalsWithProgress();
        var study = progress.stream().filter(p -> p.goal().getType() == Goal.Type.STUDY_TIME).findFirst().orElseThrow();
        assertEquals(50, study.current());
        assertFalse(study.isAchieved());

        var distract = progress.stream().filter(p -> p.goal().getType() == Goal.Type.MAX_DISTRACTING).findFirst().orElseThrow();
        assertEquals(75, distract.current());
        assertTrue(distract.isOverLimit(), "75 min is over the 60 min limit");

        assertThrows(ValidationException.class, () -> g.addGoal(Goal.Type.TASKS_DONE, Goal.Period.DAILY, 0));
    }
}
