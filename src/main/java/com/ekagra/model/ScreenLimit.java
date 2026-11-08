package com.ekagra.model;

/** A daily time limit for one app or site, e.g. "Instagram: 30 min" (FR-22). Matches the process name or a word in the window title. */
public class ScreenLimit {

    private int id;
    private int userId;
    private String appName;
    private int limitMin;

    public ScreenLimit() {
    }

    public ScreenLimit(int userId, String appName, int limitMin) {
        this.userId = userId;
        this.appName = appName;
        this.limitMin = limitMin;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public int getLimitMin() { return limitMin; }
    public void setLimitMin(int limitMin) { this.limitMin = limitMin; }
}
