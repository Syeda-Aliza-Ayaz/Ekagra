package com.ekagra.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/** A habit the student ticks off each day, e.g. "Read 20 minutes" (FR-28). */
public class Habit {

    private int id;
    private int userId;
    private String title;
    private String icon = "fth-check";
    private boolean active = true;
    private LocalDateTime createdAt = LocalDateTime.now();
    private final Set<LocalDate> checkedDays = new TreeSet<>();

    public Habit() {
    }

    public Habit(int userId, String title, String icon) {
        this.userId = userId;
        this.title = title;
        this.icon = icon;
    }

    public boolean isDoneOn(LocalDate day) {
        return checkedDays.contains(day);
    }

    /**
     * Consecutive days ticked, counting back from today.
     * If today isn't ticked yet the streak from yesterday still counts (the day isn't over).
     */
    public int currentStreak(LocalDate today) {
        LocalDate day = isDoneOn(today) ? today : today.minusDays(1);
        int streak = 0;
        while (isDoneOn(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    public int bestStreak() {
        int best = 0;
        int run = 0;
        LocalDate prev = null;
        for (LocalDate d : checkedDays) {
            run = (prev != null && prev.plusDays(1).equals(d)) ? run + 1 : 1;
            best = Math.max(best, run);
            prev = d;
        }
        return best;
    }

    public void addCheck(LocalDate day) {
        checkedDays.add(day);
    }

    public void removeCheck(LocalDate day) {
        checkedDays.remove(day);
    }

    public Set<LocalDate> getCheckedDays() {
        return Collections.unmodifiableSet(checkedDays);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
