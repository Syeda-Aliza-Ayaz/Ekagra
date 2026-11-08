package com.ekagra.tracking;

import com.ekagra.dao.ActivityDao;
import com.ekagra.dao.AppRuleDao;
import com.ekagra.db.DatabaseManager;
import com.ekagra.model.ActivityLog;
import com.ekagra.model.AppRule;
import com.ekagra.model.Category;
import com.ekagra.model.Role;
import com.ekagra.pattern.observer.ActivityListener;
import com.ekagra.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Tracker merging, idle handling and classification, with a fake "Windows" and a fake clock. */
class ActivityTrackerTest {

    /** Pretends to be Windows: the test sets what's in the foreground and how long the user's been idle. */
    static class FakeWindows implements WindowProvider {
        WindowInfo window;
        long idleMs;

        @Override
        public Optional<WindowInfo> foregroundWindow() {
            return Optional.ofNullable(window);
        }

        @Override
        public long idleMillis() {
            return idleMs;
        }
    }

    private final FakeWindows windows = new FakeWindows();
    private LocalDateTime now = LocalDate.now().atTime(10, 0);
    private ActivityTracker tracker;
    private ActivityDao dao;
    private int userId;
    private final List<ActivitySnapshot> samples = new ArrayList<>();
    private final List<Boolean> idleEvents = new ArrayList<>();

    @BeforeEach
    void setUp() {
        DatabaseManager.shutdown();
        DatabaseManager.configure(DatabaseManager.IN_MEMORY);
        userId = new AuthService().register(new AuthService.SignupRequest(Role.STUDENT, "Track Me", "tr@uni.edu",
            "secret12", "secret12", null, 1, null)).getId();
        dao = new ActivityDao();
        tracker = new ActivityTracker(userId, windows, new AppClassifier(new AppRuleDao()::findAll), dao,
            () -> now, Duration.ofMinutes(5));
        tracker.addListener(new ActivityListener() {
            @Override
            public void onSample(ActivitySnapshot s) {
                samples.add(s);
            }

            @Override
            public void onIdleChanged(boolean idle) {
                idleEvents.add(idle);
            }
        });
    }

    @AfterEach
    void tearDown() {
        DatabaseManager.shutdown();
    }

    /** Simulates n samples, 2 seconds apart. */
    private void run(int n) {
        for (int i = 0; i < n; i++) {
            tracker.sample();
            now = now.plusSeconds(2);
        }
    }

    private List<ActivityLog> logs() {
        tracker.closeSegment();
        return dao.findBetween(userId, now.toLocalDate().atStartOfDay(), now.plusDays(1));
    }

    @Test
    void defaultRulesClassifyCommonApps() {
        AppClassifier c = new AppClassifier(new AppRuleDao()::findAll);
        assertEquals(Category.DISTRACTING, c.classify("chrome.exe", "Funny cats - YouTube - Google Chrome"));
        assertEquals(Category.PRODUCTIVE, c.classify("chrome.exe", "java - Stack Overflow - Google Chrome"));
        assertEquals(Category.PRODUCTIVE, c.classify("idea64.exe", "ekagra – Main.java"));
        assertEquals(Category.DISTRACTING, c.classify("discord.exe", "Discord"));
        assertEquals(Category.NEUTRAL, c.classify("notepad.exe", "notes.txt"));
    }

    @Test
    void moreSpecificRuleWins() {
        List<AppRule> rules = List.of(
            new AppRule("chrome.exe", null, Category.NEUTRAL),
            new AppRule(null, "YouTube", Category.DISTRACTING),
            new AppRule("chrome.exe", "YouTube - OOAD lecture", Category.PRODUCTIVE));
        AppClassifier c = new AppClassifier(() -> rules);
        assertEquals(Category.PRODUCTIVE, c.classify("chrome.exe", "UML YouTube - OOAD lecture"));
        assertEquals(Category.DISTRACTING, c.classify("chrome.exe", "Music - YouTube"));
        assertEquals(Category.NEUTRAL, c.classify("chrome.exe", "Google"));
    }

    @Test
    void samplesOfSameAppAreMergedIntoOneRow() {
        windows.window = new WindowInfo("code.exe", "Main.java - VS Code");
        run(10);   // 20 seconds in VS Code
        windows.window = new WindowInfo("chrome.exe", "Cats - YouTube");
        run(5);    // 10 seconds on YouTube

        List<ActivityLog> logs = logs();
        assertEquals(2, logs.size());
        assertEquals("code.exe", logs.get(0).getAppName());
        assertEquals(Category.PRODUCTIVE, logs.get(0).getCategory());
        assertEquals(18, logs.get(0).getDurationSec());        // first sample starts the row at 0s
        assertEquals(Category.DISTRACTING, logs.get(1).getCategory());
        assertTrue(samples.get(0).switchedApp());
        assertFalse(samples.get(1).switchedApp());
        assertTrue(samples.get(10).switchedApp());
    }

    @Test
    void changingCategoryInSameBrowserStartsNewRow() {
        windows.window = new WindowInfo("chrome.exe", "LeetCode - Google Chrome");
        run(3);
        windows.window = new WindowInfo("chrome.exe", "Reels - Instagram");
        run(3);
        List<ActivityLog> logs = logs();
        assertEquals(2, logs.size());
        assertEquals(Category.PRODUCTIVE, logs.get(0).getCategory());
        assertEquals(Category.DISTRACTING, logs.get(1).getCategory());
    }

    @Test
    void idleTimeIsNotCounted() {
        windows.window = new WindowInfo("code.exe", "Main.java");
        run(5);
        windows.idleMs = Duration.ofMinutes(6).toMillis();   // user walked away
        run(30);
        assertEquals(List.of(true), idleEvents);
        windows.idleMs = 0;                                   // user is back
        run(3);
        assertEquals(List.of(true, false), idleEvents);

        List<ActivityLog> logs = logs();
        assertEquals(2, logs.size(), "a new row starts after coming back");
        int total = logs.stream().mapToInt(ActivityLog::getDurationSec).sum();
        assertTrue(total <= 14, "idle minutes must not be added, was " + total);
    }

    @Test
    void pausedTrackingRecordsNothing() {
        tracker.setPaused(true);
        windows.window = new WindowInfo("chrome.exe", "YouTube");
        run(10);
        assertTrue(logs().isEmpty());
        assertTrue(samples.isEmpty());
    }

    @Test
    void summaryQueriesAddUp() {
        windows.window = new WindowInfo("code.exe", "Main.java");
        run(31);   // 60 s productive
        windows.window = new WindowInfo("chrome.exe", "YouTube");
        run(16);   // 30 s distracting
        tracker.closeSegment();

        LocalDateTime from = now.toLocalDate().atStartOfDay();
        Map<Category, Integer> byCat = dao.secondsByCategory(userId, from, from.plusDays(1));
        assertEquals(60, byCat.get(Category.PRODUCTIVE));
        assertEquals(30, byCat.get(Category.DISTRACTING));
        assertEquals("code.exe", dao.topApps(userId, from, from.plusDays(1), 5).get(0).appName());
        assertEquals(10, dao.hourly(userId, from, from.plusDays(1)).get(0).hour());
    }
}
