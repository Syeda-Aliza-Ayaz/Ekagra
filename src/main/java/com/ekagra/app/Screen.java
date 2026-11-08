package com.ekagra.app;

import com.ekagra.model.Role;
import com.ekagra.model.User;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Every screen reachable from the sidebar, with its icon, FXML file and who may see it. */
public enum Screen {

    DASHBOARD("Dashboard", "Overview", "fth-home", "Dashboard.fxml", 5,
        "Your Productivity Score, focus time, screen time and streak at a glance.", Role.STUDENT),
    ANALYTICS("Analytics", "Overview", "fth-bar-chart-2", "Analytics.fxml", 5,
        "Charts and insights about where your time goes.", Role.STUDENT),

    TASKS("Tasks", "Work", "fth-check-square", "Tasks.fxml", 2,
        "Assignments and to-dos with priorities, deadlines and subtasks.", Role.STUDENT),
    FOCUS("Focus Timer", "Work", "fth-clock", "Focus.fxml", 2,
        "Pomodoro sessions linked to your tasks, with Focus Guard.", Role.STUDENT),
    GOALS("Goals & Habits", "Work", "fth-target", "Goals.fxml", 4,
        "Daily and weekly goals plus habit streaks.", Role.STUDENT),

    ACTIVITY("Activity Monitor", "Wellbeing", "fth-monitor", "Activity.fxml", 3,
        "Live tracking of the apps and sites you use.", Role.STUDENT),
    WELLBEING("Wellbeing", "Wellbeing", "fth-heart", "Wellbeing.fxml", 4,
        "Screen-time limits, break reminders, mood and sleep.", Role.STUDENT),

    MENTOR("My Students", "Mentor", "fth-users", "Mentor.fxml", 6,
        "Your linked students, their scores and at-risk alerts.", Role.MENTOR),
    RULES("App Rules", "Mentor", "fth-sliders", "Rules.fxml", 6,
        "Decide which apps and sites count as productive or distracting.", Role.MENTOR),

    ACHIEVEMENTS("Achievements", "You", "fth-award", "Achievements.fxml", 6,
        "XP, levels and badges you've earned.", Role.STUDENT),
    REPORTS("Reports", "You", "fth-file-text", "Reports.fxml", 6,
        "Weekly PDF reports and CSV exports.", Role.STUDENT),
    NOTIFICATIONS("Notifications", "You", "fth-bell", "Notifications.fxml", 6,
        "Reminders, alerts and messages.", Role.STUDENT, Role.MENTOR),
    SETTINGS("Settings", "You", "fth-settings", "Settings.fxml", 6,
        "Theme, timer lengths, limits and tracking preferences.", Role.STUDENT, Role.MENTOR),

    /** Not shown in the sidebar list; opened from the user card. */
    PROFILE("Profile", null, "fth-user", "Profile.fxml", 1,
        "Your account details.", Role.STUDENT, Role.MENTOR);

    private final String title;
    private final String group;
    private final String icon;
    private final String fxml;
    private final int phase;
    private final String description;
    private final Set<Role> roles;

    Screen(String title, String group, String icon, String fxml, int phase, String description, Role... roles) {
        this.title = title;
        this.group = group;
        this.icon = icon;
        this.fxml = fxml;
        this.phase = phase;
        this.description = description;
        this.roles = EnumSet.copyOf(Arrays.asList(roles));
    }

    public static Screen homeFor(User user) {
        return user.getRole() == Role.MENTOR ? MENTOR : DASHBOARD;
    }

    /** Sidebar items for a role, in display order. */
    public static List<Screen> navFor(Role role) {
        return Arrays.stream(values())
            .filter(s -> s.group != null && s.roles.contains(role))
            .toList();
    }

    public String getTitle() { return title; }
    public String getGroup() { return group; }
    public String getIcon() { return icon; }
    public String getFxml() { return fxml; }
    public int getPhase() { return phase; }
    public String getDescription() { return description; }
}
