package com.ekagra.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * One night of sleep (FR-25). {@code date} is the morning the student woke up.
 * Bedtime may be before midnight (e.g. 23:30) or after (e.g. 01:15).
 */
public class SleepEntry {

    private int id;
    private int userId;
    private LocalDate date;
    private LocalTime bedtime;
    private LocalTime wakeTime;

    public SleepEntry() {
    }

    public SleepEntry(int userId, LocalDate date, LocalTime bedtime, LocalTime wakeTime) {
        this.userId = userId;
        this.date = date;
        this.bedtime = bedtime;
        this.wakeTime = wakeTime;
    }

    /** Minutes slept, handling bedtimes before midnight. */
    public int getDurationMin() {
        long mins = Duration.between(bedtime, wakeTime).toMinutes();
        if (mins <= 0) {
            mins += 24 * 60;
        }
        return (int) mins;
    }

    public double getHours() {
        return getDurationMin() / 60.0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getBedtime() { return bedtime; }
    public void setBedtime(LocalTime bedtime) { this.bedtime = bedtime; }

    public LocalTime getWakeTime() { return wakeTime; }
    public void setWakeTime(LocalTime wakeTime) { this.wakeTime = wakeTime; }
}
