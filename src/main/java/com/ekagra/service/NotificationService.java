package com.ekagra.service;

import com.ekagra.dao.NotificationDao;
import com.ekagra.model.Notification;
import com.ekagra.ui.DesktopToast;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Saves notifications (for the notification centre) and optionally shows them as a desktop toast (FR-35).
 * Listeners (e.g. the bell badge in the top bar) are told when the unread count changes.
 */
public final class NotificationService {

    private static final NotificationDao DAO = new NotificationDao();
    private static final List<Runnable> CHANGE_LISTENERS = new CopyOnWriteArrayList<>();

    private NotificationService() {
    }

    /** Saves and shows a desktop toast. Must be called on the UI thread. */
    public static Notification notify(Notification n, DesktopToast.Action... actions) {
        save(n);
        if (!popupsEnabled(n.getUserId())) {
            return n;
        }
        DesktopToast.show(kindFor(n.getType()), n.getTitle(), n.getMessage(), Duration.ofSeconds(8), actions);
        return n;
    }

    /** Several at once (e.g. two badges): all are saved, one combined pop-up is shown. */
    public static void notifyAll(List<Notification> list) {
        if (list.isEmpty()) {
            return;
        }
        if (list.size() == 1) {
            notify(list.get(0));
            return;
        }
        list.forEach(NotificationService::save);
        DesktopToast.show(DesktopToast.Kind.SUCCESS, list.size() + " new updates",
            String.join("\n", list.stream().map(Notification::getTitle).toList()), Duration.ofSeconds(9));
    }

    /** Saves without a pop-up (e.g. messages that arrive while the recipient isn't logged in). */
    public static Notification save(Notification n) {
        DAO.insert(n);
        CHANGE_LISTENERS.forEach(Runnable::run);
        return n;
    }

    public static List<Notification> recent(int userId, int limit) {
        return DAO.findRecent(userId, limit);
    }

    public static int unreadCount(int userId) {
        return DAO.countUnread(userId);
    }

    public static void markAllRead(int userId) {
        DAO.markAllRead(userId);
        CHANGE_LISTENERS.forEach(Runnable::run);
    }

    public static void clearAll(int userId) {
        DAO.deleteAll(userId);
        CHANGE_LISTENERS.forEach(Runnable::run);
    }

    public static void addChangeListener(Runnable r) {
        CHANGE_LISTENERS.add(r);
    }

    public static void removeChangeListener(Runnable r) {
        CHANGE_LISTENERS.remove(r);
    }

    private static boolean popupsEnabled(int userId) {
        return new SettingsService().getBoolean(userId, SettingsService.POPUPS, true);
    }

    private static DesktopToast.Kind kindFor(Notification.Type type) {
        return switch (type) {
            case SCREEN_LIMIT, APP_LIMIT, FOCUS, DEADLINE -> DesktopToast.Kind.WARNING;
            case GOAL, ACHIEVEMENT -> DesktopToast.Kind.SUCCESS;
            default -> DesktopToast.Kind.INFO;
        };
    }
}
