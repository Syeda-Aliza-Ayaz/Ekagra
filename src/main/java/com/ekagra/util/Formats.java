package com.ekagra.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Human-friendly text for durations and deadlines. */
public final class Formats {

    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DAY_SHORT = DateTimeFormatter.ofPattern("EEE d MMM");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM, HH:mm");

    private Formats() {
    }

    /** 95 -> "1h 35m", 40 -> "40m", 0 -> "0m". */
    public static String minutes(int totalMinutes) {
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        if (h == 0) {
            return m + "m";
        }
        return m == 0 ? h + "h" : h + "h " + m + "m";
    }

    /** Like seconds() but shows seconds for short stretches: "45s", "3m 05s", "1h 20m". */
    public static String secondsPrecise(int totalSeconds) {
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }
        if (totalSeconds < 3600) {
            return String.format("%dm %02ds", totalSeconds / 60, totalSeconds % 60);
        }
        return minutes(totalSeconds / 60);
    }

    public static String seconds(int totalSeconds) {
        return minutes(totalSeconds / 60);
    }

    /** "Overdue by 2 days", "Due in 3h", "Due today 23:59", "Due tomorrow 10:00", "Due Mon 9 Nov". */
    public static String deadline(LocalDateTime deadline, LocalDateTime now) {
        if (deadline == null) {
            return "No deadline";
        }
        if (deadline.isBefore(now)) {
            Duration late = Duration.between(deadline, now);
            if (late.toDays() >= 1) {
                long d = late.toDays();
                return "Overdue by " + d + (d == 1 ? " day" : " days");
            }
            if (late.toHours() >= 1) {
                return "Overdue by " + late.toHours() + "h";
            }
            return "Overdue by " + Math.max(1, late.toMinutes()) + "m";
        }
        Duration left = Duration.between(now, deadline);
        if (left.toHours() < 6) {
            return left.toHours() >= 1 ? "Due in " + left.toHours() + "h " + left.toMinutesPart() + "m"
                : "Due in " + Math.max(1, left.toMinutes()) + "m";
        }
        LocalDate day = deadline.toLocalDate();
        LocalDate today = now.toLocalDate();
        if (day.equals(today)) {
            return "Due today " + deadline.format(TIME);
        }
        if (day.equals(today.plusDays(1))) {
            return "Due tomorrow " + deadline.format(TIME);
        }
        return "Due " + deadline.format(DAY_SHORT);
    }
}
