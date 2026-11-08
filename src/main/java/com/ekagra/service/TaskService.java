package com.ekagra.service;

import com.ekagra.dao.TaskDao;
import com.ekagra.model.Priority;
import com.ekagra.model.SubTask;
import com.ekagra.model.Task;
import com.ekagra.model.TaskStatus;
import com.ekagra.pattern.observer.AppEvent;
import com.ekagra.pattern.observer.AppEventBus;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/** Task management rules: validation, filtering/sorting and deadline alerts (FR-05 .. FR-09). */
public class TaskService {

    public static final int MAX_TITLE_LENGTH = 120;

    public enum SortBy {
        DEADLINE("Deadline"), PRIORITY("Priority"), CREATED("Newest"), TITLE("Title");

        private final String label;

        SortBy(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Filter + sort options chosen on the Tasks screen. Null fields mean "any". */
    public record TaskQuery(String search, TaskStatus status, Priority priority, String subject, SortBy sortBy) {
        public static TaskQuery all() {
            return new TaskQuery(null, null, null, null, SortBy.DEADLINE);
        }
    }

    public record DeadlineSummary(int overdue, int dueSoon) {
        public boolean hasAlerts() {
            return overdue > 0 || dueSoon > 0;
        }
    }

    private final TaskDao taskDao;
    private Consumer<Task> onTaskCompleted = t -> { };

    public TaskService() {
        this(new TaskDao());
    }

    public TaskService(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    /** Hook so other modules (e.g. gamification) can react when a task is completed. */
    public void setOnTaskCompleted(Consumer<Task> listener) {
        this.onTaskCompleted = listener == null ? t -> { } : listener;
    }

    public List<Task> getTasks(int userId) {
        return taskDao.findByUser(userId);
    }

    public List<Task> getOpenTasks(int userId) {
        return taskDao.findByUser(userId).stream()
            .filter(t -> !t.isDone())
            .sorted(deadlineComparator())
            .toList();
    }

    public Task save(Task task) {
        validate(task);
        task.setTitle(task.getTitle().trim());
        task.setSubject(blankToNull(task.getSubject()));
        task.setDescription(blankToNull(task.getDescription()));
        if (task.isDone() && task.getCompletedAt() == null) {
            task.setCompletedAt(LocalDateTime.now());
        } else if (!task.isDone()) {
            task.setCompletedAt(null);
        }
        if (task.getId() == 0) {
            taskDao.insert(task);
        } else {
            taskDao.update(task);
        }
        return task;
    }

    public void delete(Task task) {
        taskDao.delete(task.getId());
    }

    /** Ticks a task as done or reopens it. */
    public void toggleDone(Task task) {
        if (task.isDone()) {
            task.reopen();
            taskDao.updateStatus(task);
        } else {
            task.markDone(LocalDateTime.now());
            taskDao.updateStatus(task);
            onTaskCompleted.accept(task);
            AppEventBus.publish(new AppEvent.TaskCompleted(task.getUserId(), task));
        }
    }

    public void setSubtaskDone(SubTask subTask, boolean done) {
        subTask.setDone(done);
        taskDao.setSubtaskDone(subTask.getId(), done);
    }

    public List<Task> filter(List<Task> tasks, TaskQuery q) {
        String search = q.search() == null ? "" : q.search().trim().toLowerCase(Locale.ROOT);
        return tasks.stream()
            .filter(t -> search.isEmpty()
                || t.getTitle().toLowerCase(Locale.ROOT).contains(search)
                || (t.getSubject() != null && t.getSubject().toLowerCase(Locale.ROOT).contains(search))
                || (t.getDescription() != null && t.getDescription().toLowerCase(Locale.ROOT).contains(search)))
            .filter(t -> q.status() == null || t.getStatus() == q.status())
            .filter(t -> q.priority() == null || t.getPriority() == q.priority())
            .filter(t -> q.subject() == null || q.subject().equalsIgnoreCase(t.getSubject()))
            .sorted(comparatorFor(q.sortBy() == null ? SortBy.DEADLINE : q.sortBy()))
            .toList();
    }

    public List<String> getSubjects(List<Task> tasks) {
        return tasks.stream()
            .map(Task::getSubject)
            .filter(Objects::nonNull)
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    public DeadlineSummary deadlineSummary(List<Task> tasks, LocalDateTime now) {
        int overdue = (int) tasks.stream().filter(t -> t.isOverdue(now)).count();
        int soon = (int) tasks.stream().filter(t -> t.isDueSoon(now)).count();
        return new DeadlineSummary(overdue, soon);
    }

    private Comparator<Task> comparatorFor(SortBy sortBy) {
        // done tasks always sink to the bottom
        Comparator<Task> doneLast = Comparator.comparing(Task::isDone);
        Comparator<Task> main = switch (sortBy) {
            case DEADLINE -> deadlineComparator();
            case PRIORITY -> Comparator.comparing((Task t) -> t.getPriority().getWeight()).reversed()
                .thenComparing(deadlineComparator());
            case CREATED -> Comparator.comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            case TITLE -> Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);
        };
        return doneLast.thenComparing(main);
    }

    private static Comparator<Task> deadlineComparator() {
        return Comparator.comparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing((Task t) -> t.getPriority().getWeight(), Comparator.reverseOrder());
    }

    private void validate(Task task) {
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new ValidationException("Please give the task a title.");
        }
        if (task.getTitle().trim().length() > MAX_TITLE_LENGTH) {
            throw new ValidationException("Title is too long (max " + MAX_TITLE_LENGTH + " characters).");
        }
        if (task.getEstimatedMin() < 0) {
            throw new ValidationException("Estimated time can't be negative.");
        }
        if (task.getPriority() == null || task.getStatus() == null) {
            throw new ValidationException("Please choose a priority and status.");
        }
        for (SubTask s : task.getSubtasks()) {
            if (s.getTitle() == null || s.getTitle().isBlank()) {
                throw new ValidationException("Subtasks can't be empty.");
            }
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
