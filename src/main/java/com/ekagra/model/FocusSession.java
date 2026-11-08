package com.ekagra.model;

import java.time.LocalDateTime;

/** One Pomodoro session (focus or break), saved to history when it ends. */
public class FocusSession {

    private int id;
    private int userId;
    private Integer taskId;
    private String taskTitle;          // loaded for display only
    private SessionType type;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private int plannedMin;
    private int actualSec;
    private boolean completed;
    private int distractions;
    private Integer focusRating;       // 1..5, optional

    public FocusSession() {
    }

    public FocusSession(int userId, Integer taskId, SessionType type, int plannedMin, LocalDateTime startedAt) {
        this.userId = userId;
        this.taskId = taskId;
        this.type = type;
        this.plannedMin = plannedMin;
        this.startedAt = startedAt;
    }

    public void addDistraction() {
        distractions++;
    }

    public int getActualMinutes() {
        return Math.round(actualSec / 60f);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }

    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }

    public SessionType getType() { return type; }
    public void setType(SessionType type) { this.type = type; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public int getPlannedMin() { return plannedMin; }
    public void setPlannedMin(int plannedMin) { this.plannedMin = plannedMin; }

    public int getActualSec() { return actualSec; }
    public void setActualSec(int actualSec) { this.actualSec = actualSec; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public int getDistractions() { return distractions; }
    public void setDistractions(int distractions) { this.distractions = distractions; }

    public Integer getFocusRating() { return focusRating; }
    public void setFocusRating(Integer focusRating) { this.focusRating = focusRating; }
}
