package com.ekagra.model;

import java.time.LocalDate;

/** Daily mood and stress check-in (FR-24). Both use a 1..5 scale. */
public class MoodEntry {

    public static final String[] MOOD_LABELS = {"", "Awful", "Low", "Okay", "Good", "Great"};
    public static final String[] STRESS_LABELS = {"", "Calm", "Relaxed", "Some stress", "Stressed", "Overwhelmed"};

    private int id;
    private int userId;
    private LocalDate date;
    private int mood;
    private int stress;
    private String note;

    public MoodEntry() {
    }

    public MoodEntry(int userId, LocalDate date, int mood, int stress, String note) {
        this.userId = userId;
        this.date = date;
        this.mood = mood;
        this.stress = stress;
        this.note = note;
    }

    public String getMoodLabel() {
        return MOOD_LABELS[mood];
    }

    public String getStressLabel() {
        return STRESS_LABELS[stress];
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getMood() { return mood; }
    public void setMood(int mood) { this.mood = mood; }

    public int getStress() { return stress; }
    public void setStress(int stress) { this.stress = stress; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
