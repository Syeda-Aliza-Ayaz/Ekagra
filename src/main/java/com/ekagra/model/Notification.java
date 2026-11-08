package com.ekagra.model;

import java.time.LocalDateTime;

/** A message for the user: reminders, alerts, achievements, mentor notes (FR-35). */
public class Notification {

    public enum Type {
        BREAK("fth-coffee"),
        EYE_REST("fth-eye"),
        SCREEN_LIMIT("fth-monitor"),
        APP_LIMIT("fth-slash"),
        DEADLINE("fth-calendar"),
        FOCUS("fth-crosshair"),
        GOAL("fth-target"),
        ACHIEVEMENT("fth-award"),
        MENTOR("fth-message-circle"),
        SYSTEM("fth-info");

        private final String icon;

        Type(String icon) {
            this.icon = icon;
        }

        public String getIcon() {
            return icon;
        }
    }

    private int id;
    private int userId;
    private Integer senderId;
    private Type type;
    private String title;
    private String message;
    private boolean read;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification() {
    }

    public Notification(int userId, Type type, String title, String message) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getSenderId() { return senderId; }
    public void setSenderId(Integer senderId) { this.senderId = senderId; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
