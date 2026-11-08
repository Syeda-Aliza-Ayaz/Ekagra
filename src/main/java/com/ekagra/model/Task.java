package com.ekagra.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An assignment or to-do item.
 * COMPOSITION: a Task owns its {@link SubTask}s; deleting the task deletes them (ON DELETE CASCADE).
 */
public class Task {

    public static final Duration DUE_SOON_WINDOW = Duration.ofHours(24);

    private int id;
    private int userId;
    private String title;
    private String description;
    private String subject;
    private Priority priority = Priority.MEDIUM;
    private TaskStatus status = TaskStatus.TODO;
    private LocalDateTime deadline;
    private int estimatedMin;
    private int spentMin;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime completedAt;
    private final List<SubTask> subtasks = new ArrayList<>();

    // ---------- business logic ----------

    public boolean isDone() {
        return status == TaskStatus.DONE;
    }

    public boolean isOverdue(LocalDateTime now) {
        return !isDone() && deadline != null && deadline.isBefore(now);
    }

    public boolean isDueSoon(LocalDateTime now) {
        return !isDone() && deadline != null && !deadline.isBefore(now)
            && deadline.isBefore(now.plus(DUE_SOON_WINDOW));
    }

    /** Whether the task was finished on or before its deadline. */
    public boolean wasCompletedOnTime() {
        return isDone() && completedAt != null && (deadline == null || !completedAt.isAfter(deadline));
    }

    public void markDone(LocalDateTime when) {
        status = TaskStatus.DONE;
        completedAt = when;
    }

    public void reopen() {
        status = spentMin > 0 ? TaskStatus.IN_PROGRESS : TaskStatus.TODO;
        completedAt = null;
    }

    /** 0..1 progress; based on subtasks if any, otherwise on status. */
    public double getProgress() {
        if (isDone()) {
            return 1.0;
        }
        if (subtasks.isEmpty()) {
            return status == TaskStatus.IN_PROGRESS ? 0.5 : 0.0;
        }
        return (double) getDoneSubtaskCount() / subtasks.size();
    }

    public int getDoneSubtaskCount() {
        return (int) subtasks.stream().filter(SubTask::isDone).count();
    }

    public void addSubTask(SubTask subTask) {
        subTask.setTaskId(id);
        subtasks.add(subTask);
    }

    public void removeSubTask(SubTask subTask) {
        subtasks.remove(subTask);
    }

    public void clearSubTasks() {
        subtasks.clear();
    }

    public List<SubTask> getSubtasks() {
        return Collections.unmodifiableList(subtasks);
    }

    // ---------- getters / setters ----------

    public int getId() { return id; }
    public void setId(int id) {
        this.id = id;
        subtasks.forEach(s -> s.setTaskId(id));
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public int getEstimatedMin() { return estimatedMin; }
    public void setEstimatedMin(int estimatedMin) { this.estimatedMin = estimatedMin; }

    public int getSpentMin() { return spentMin; }
    public void setSpentMin(int spentMin) { this.spentMin = spentMin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    @Override
    public String toString() {
        return title;
    }
}
