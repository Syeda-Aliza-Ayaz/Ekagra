package com.ekagra.model;

import java.time.Duration;
import java.time.LocalDateTime;

/** One continuous stretch of time spent in a single app (merged from many 2-second samples). */
public class ActivityLog {

    private int id;
    private int userId;
    private String appName;
    private String windowTitle;
    private Category category;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private int durationSec;

    public ActivityLog() {
    }

    public ActivityLog(int userId, String appName, String windowTitle, Category category, LocalDateTime start) {
        this.userId = userId;
        this.appName = appName;
        this.windowTitle = windowTitle;
        this.category = category;
        this.startedAt = start;
        this.endedAt = start;
    }

    /** Extends this stretch up to the given time. */
    public void extendTo(LocalDateTime time) {
        if (time.isAfter(endedAt)) {
            endedAt = time;
            durationSec = (int) Duration.between(startedAt, endedAt).getSeconds();
        }
    }

    /** Friendly app name: "chrome.exe" -> "Chrome". */
    public String getDisplayName() {
        return displayName(appName);
    }

    public static String displayName(String process) {
        if (process == null || process.isBlank()) {
            return "Unknown";
        }
        String n = process.toLowerCase().endsWith(".exe") ? process.substring(0, process.length() - 4) : process;
        return switch (n.toLowerCase()) {
            case "chrome" -> "Chrome";
            case "msedge" -> "Microsoft Edge";
            case "firefox" -> "Firefox";
            case "code" -> "VS Code";
            case "idea64" -> "IntelliJ IDEA";
            case "winword" -> "Word";
            case "excel" -> "Excel";
            case "powerpnt" -> "PowerPoint";
            case "explorer" -> "File Explorer";
            case "java", "javaw" -> "Java app";
            default -> Character.toUpperCase(n.charAt(0)) + n.substring(1);
        };
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getWindowTitle() { return windowTitle; }
    public void setWindowTitle(String windowTitle) { this.windowTitle = windowTitle; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public int getDurationSec() { return durationSec; }
    public void setDurationSec(int durationSec) { this.durationSec = durationSec; }
}
