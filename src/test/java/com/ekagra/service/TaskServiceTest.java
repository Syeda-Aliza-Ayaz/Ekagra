package com.ekagra.service;

import com.ekagra.dao.SessionDao;
import com.ekagra.dao.TaskDao;
import com.ekagra.db.DatabaseManager;
import com.ekagra.model.FocusSession;
import com.ekagra.model.Priority;
import com.ekagra.model.Role;
import com.ekagra.model.SessionType;
import com.ekagra.model.SubTask;
import com.ekagra.model.Task;
import com.ekagra.model.TaskStatus;
import com.ekagra.service.TaskService.SortBy;
import com.ekagra.service.TaskService.TaskQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {

    private TaskService service;
    private int userId;

    @BeforeEach
    void setUp() {
        DatabaseManager.shutdown();
        DatabaseManager.configure(DatabaseManager.IN_MEMORY);
        userId = new AuthService().register(new AuthService.SignupRequest(Role.STUDENT, "Test User", "t@uni.edu",
            "secret12", "secret12", null, 1, null)).getId();
        service = new TaskService();
    }

    @AfterEach
    void tearDown() {
        DatabaseManager.shutdown();
    }

    private Task task(String title, Priority p, LocalDateTime deadline, String subject) {
        Task t = new Task();
        t.setUserId(userId);
        t.setTitle(title);
        t.setPriority(p);
        t.setDeadline(deadline);
        t.setSubject(subject);
        return service.save(t);
    }

    @Test
    void saveAndLoadWithSubtasks() {
        Task t = new Task();
        t.setUserId(userId);
        t.setTitle("  Class diagram  ");
        t.addSubTask(new SubTask("Identify classes", true));
        t.addSubTask(new SubTask("Draw relationships", false));
        service.save(t);

        Task loaded = new TaskDao().findById(t.getId()).orElseThrow();
        assertEquals("Class diagram", loaded.getTitle());
        assertEquals(2, loaded.getSubtasks().size());
        assertEquals(0.5, loaded.getProgress(), 1e-9);
    }

    @Test
    void deletingTaskDeletesSubtasks() {
        Task t = new Task();
        t.setUserId(userId);
        t.setTitle("Report");
        t.addSubTask(new SubTask("Intro", false));
        service.save(t);
        service.delete(t);
        assertTrue(service.getTasks(userId).isEmpty());
        assertEquals(0, new TaskDao() {
            long count() {
                return queryLong("SELECT COUNT(*) FROM subtasks");
            }
        }.count());
    }

    @Test
    void emptyTitleIsRejected() {
        Task t = new Task();
        t.setUserId(userId);
        t.setTitle("   ");
        assertThrows(ValidationException.class, () -> service.save(t));
    }

    @Test
    void overdueAndDueSoonAreDetected() {
        LocalDateTime now = LocalDateTime.now();
        task("Late", Priority.HIGH, now.minusHours(3), null);
        task("Soon", Priority.LOW, now.plusHours(5), null);
        task("Later", Priority.LOW, now.plusDays(5), null);
        Task done = task("Done late", Priority.LOW, now.minusDays(1), null);
        service.toggleDone(done);

        var summary = service.deadlineSummary(service.getTasks(userId), now);
        assertEquals(1, summary.overdue());
        assertEquals(1, summary.dueSoon());
    }

    @Test
    void filterAndSort() {
        LocalDateTime base = LocalDate.now().plusDays(1).atTime(10, 0);
        task("Physics lab", Priority.LOW, base.plusDays(2), "Physics");
        task("OOAD project", Priority.URGENT, base.plusDays(5), "OOAD");
        task("OOAD quiz", Priority.MEDIUM, base, "OOAD");
        List<Task> all = service.getTasks(userId);

        List<Task> bySubject = service.filter(all, new TaskQuery(null, null, null, "ooad", SortBy.DEADLINE));
        assertEquals(List.of("OOAD quiz", "OOAD project"), bySubject.stream().map(Task::getTitle).toList());

        List<Task> byPriority = service.filter(all, new TaskQuery(null, null, null, null, SortBy.PRIORITY));
        assertEquals("OOAD project", byPriority.get(0).getTitle());

        List<Task> search = service.filter(all, new TaskQuery("lab", null, null, null, SortBy.TITLE));
        assertEquals(1, search.size());

        assertEquals(List.of("OOAD", "Physics"), service.getSubjects(all));
    }

    @Test
    void toggleDoneSetsCompletionAndFiresHook() {
        int[] fired = {0};
        service.setOnTaskCompleted(t -> fired[0]++);
        Task t = task("Essay", Priority.MEDIUM, null, null);
        service.toggleDone(t);
        Task loaded = new TaskDao().findById(t.getId()).orElseThrow();
        assertEquals(TaskStatus.DONE, loaded.getStatus());
        assertNotNull(loaded.getCompletedAt());
        assertEquals(1, fired[0]);

        service.toggleDone(loaded);
        assertEquals(TaskStatus.TODO, new TaskDao().findById(t.getId()).orElseThrow().getStatus());
    }

    @Test
    void focusTimeIsAddedToTaskAndCounted() {
        Task t = task("Revise", Priority.MEDIUM, null, null);
        new TaskDao().addSpentMinutes(t.getId(), 25);
        Task loaded = new TaskDao().findById(t.getId()).orElseThrow();
        assertEquals(25, loaded.getSpentMin());
        assertEquals(TaskStatus.IN_PROGRESS, loaded.getStatus());

        SessionDao sessions = new SessionDao();
        LocalDateTime start = LocalDate.now().atTime(9, 0);
        FocusSession s = new FocusSession(userId, t.getId(), SessionType.FOCUS, 25, start);
        s.setEndedAt(start.plusMinutes(25));
        s.setActualSec(1500);
        s.setCompleted(true);
        s.setDistractions(2);
        sessions.insert(s);

        var stats = sessions.focusStats(userId, LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay());
        assertEquals(25, stats.focusMinutes());
        assertEquals(1, stats.completedSessions());
        assertEquals(2, stats.distractions());
        assertEquals("Revise", sessions.findRecent(userId, 5).get(0).getTaskTitle());
    }
}
