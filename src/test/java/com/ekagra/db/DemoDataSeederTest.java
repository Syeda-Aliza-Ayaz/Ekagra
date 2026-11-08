package com.ekagra.db;

import com.ekagra.dao.ActivityDao;
import com.ekagra.dao.SessionDao;
import com.ekagra.dao.TaskDao;
import com.ekagra.dao.UserDao;
import com.ekagra.model.Category;
import com.ekagra.model.Mentor;
import com.ekagra.model.Student;
import com.ekagra.service.AnalyticsService;
import com.ekagra.service.AuthService;
import com.ekagra.service.GamificationService;
import com.ekagra.service.MentorService;
import com.ekagra.service.SessionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DemoDataSeederTest {

    @BeforeEach
    void setUp() {
        DatabaseManager.shutdown();
        DatabaseManager.configure(DatabaseManager.IN_MEMORY);
    }

    @AfterEach
    void tearDown() {
        SessionContext.getInstance().clear();
        DatabaseManager.shutdown();
    }

    @Test
    void seedsRealisticConnectedData() {
        Student ayesha = new DemoDataSeeder().seed();
        assertTrue(DemoDataSeeder.exists());

        // can log in with the published password
        assertNotNull(new AuthService().login(DemoDataSeeder.STUDENT_EMAIL, DemoDataSeeder.PASSWORD));
        assertNotNull(new AuthService().login(DemoDataSeeder.MENTOR_EMAIL, DemoDataSeeder.PASSWORD));

        LocalDateTime from = LocalDate.now().minusDays(13).atStartOfDay();
        LocalDateTime to = LocalDate.now().plusDays(1).atStartOfDay();
        assertTrue(new SessionDao().focusStats(ayesha.getId(), from, to).completedSessions() >= 15);
        var cats = new ActivityDao().secondsByCategory(ayesha.getId(), from, to);
        assertTrue(cats.get(Category.PRODUCTIVE) > cats.get(Category.DISTRACTING), "Ayesha is mostly productive");
        assertTrue(new TaskDao().findByUser(ayesha.getId()).size() >= 10);
        assertTrue(ayesha.getXp() > 500, "enough XP to be above level 1");

        // nothing is in the future
        assertTrue(new SessionDao().findRecent(ayesha.getId(), 1).get(0).getEndedAt().isBefore(LocalDateTime.now().plusSeconds(1)));

        // badges and insights are populated
        assertTrue(new GamificationService(ayesha).badgeStatuses().stream().filter(GamificationService.BadgeStatus::isEarned).count() >= 3);
        var analytics = new AnalyticsService(ayesha);
        assertTrue(analytics.insights(analytics.lastDays(7)).size() >= 3);

        // mentor sees 3 students, Bilal is at risk
        Mentor mentor = (Mentor) new UserDao().findByEmail(DemoDataSeeder.MENTOR_EMAIL).orElseThrow();
        var overview = new MentorService(mentor).overview();
        assertEquals(3, overview.size());
        assertEquals("Bilal Raza", overview.get(0).student().getFullName(), "at-risk students are listed first");
        assertTrue(overview.get(0).isAtRisk());
    }

    @Test
    void reseedingReplacesInsteadOfDuplicating() {
        new DemoDataSeeder().seed();
        Student again = new DemoDataSeeder().seed();
        Mentor mentor = (Mentor) new UserDao().findByEmail(DemoDataSeeder.MENTOR_EMAIL).orElseThrow();
        assertEquals(3, new UserDao().findStudentsOfMentor(mentor.getId()).size());
        assertEquals(13, new TaskDao().findByUser(again.getId()).size());
    }
}
