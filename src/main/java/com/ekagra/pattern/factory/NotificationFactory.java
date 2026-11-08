package com.ekagra.pattern.factory;

import com.ekagra.model.Notification;
import com.ekagra.model.Notification.Type;
import com.ekagra.util.Formats;

/**
 * FACTORY pattern: one place that knows how to build every kind of notification
 * (wording, type, icon). Callers just say what happened.
 */
public final class NotificationFactory {

    private NotificationFactory() {
    }

    public static Notification breakReminder(int userId, int continuousMin) {
        return new Notification(userId, Type.BREAK, "Time for a break",
            "You've been at the screen for " + Formats.minutes(continuousMin)
                + " without a pause. Stand up, stretch and drink some water.");
    }

    public static Notification eyeRest(int userId) {
        return new Notification(userId, Type.EYE_REST, "20-20-20 eye break",
            "Look at something about 20 feet (6 m) away for 20 seconds to relax your eyes.");
    }

    public static Notification screenLimitWarning(int userId, int usedMin, int limitMin) {
        int pct = Math.round(100f * usedMin / limitMin);
        return new Notification(userId, Type.SCREEN_LIMIT, "Screen time at " + pct + "%",
            "You've used " + Formats.minutes(usedMin) + " of your " + Formats.minutes(limitMin) + " daily limit.");
    }

    public static Notification screenLimitReached(int userId, int limitMin) {
        return new Notification(userId, Type.SCREEN_LIMIT, "Daily screen limit reached",
            "You've hit your " + Formats.minutes(limitMin) + " limit for today. Consider winding down.");
    }

    public static Notification appLimitReached(int userId, String app, int limitMin) {
        return new Notification(userId, Type.APP_LIMIT, app + " limit reached",
            "You've spent " + Formats.minutes(limitMin) + " on " + app + " today, which is your limit.");
    }

    public static Notification goalAchieved(int userId, String goalTitle) {
        return new Notification(userId, Type.GOAL, "Goal achieved!", goalTitle);
    }

    public static Notification deadlineSoon(int userId, String taskTitle, String when) {
        return new Notification(userId, Type.DEADLINE, "Deadline coming up", "\"" + taskTitle + "\" is " + when + ".");
    }

    public static Notification focusDistraction(int userId, String what) {
        return new Notification(userId, Type.FOCUS, "Distraction during focus",
            "You opened " + what + " during a focus session.");
    }

    public static Notification badgeEarned(int userId, String badgeName, String description) {
        return new Notification(userId, Type.ACHIEVEMENT, "Badge unlocked: " + badgeName, description);
    }

    public static Notification mentorNote(int userId, int mentorId, String mentorName, String text) {
        Notification n = new Notification(userId, Type.MENTOR, "Message from " + mentorName, text);
        n.setSenderId(mentorId);
        return n;
    }

    public static Notification system(int userId, String title, String message) {
        return new Notification(userId, Type.SYSTEM, title, message);
    }
}
