package com.ekagra.model;

/** A checklist item inside a {@link Task}. Exists only as part of its task (composition). */
public class SubTask {

    private int id;
    private int taskId;
    private String title;
    private boolean done;

    public SubTask() {
    }

    public SubTask(String title, boolean done) {
        this.title = title;
        this.done = done;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
}
