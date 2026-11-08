package com.ekagra.service;

import com.ekagra.app.Navigator;
import com.ekagra.app.Screen;
import com.ekagra.dao.NotificationDao;
import com.ekagra.dao.TaskDao;
import com.ekagra.model.Notification;
import com.ekagra.model.Task;
import com.ekagra.pattern.factory.NotificationFactory;
import com.ekagra.ui.DesktopToast;
import com.ekagra.util.Formats;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reminds the student about tasks due in the next 24 hours (FR-08 + FR-35).
 * Checks at login and every 15 minutes; each task is reminded at most once per day.
 */
public class DeadlineWatcher {

    private final int userId;
    private final TaskDao taskDao = new TaskDao();
    private final NotificationDao notificationDao = new NotificationDao();
    private final Timeline timeline;

    public DeadlineWatcher(int userId) {
        this.userId = userId;
        timeline = new Timeline(new KeyFrame(Duration.minutes(15), e -> check()));
        timeline.setCycleCount(Animation.INDEFINITE);
    }

    public void start() {
        check();
        timeline.play();
    }

    public void stop() {
        timeline.stop();
    }

    void check() {
        LocalDateTime now = LocalDateTime.now();
        List<Task> soon = taskDao.findByUser(userId).stream().filter(t -> t.isDueSoon(now)).toList();
        int fresh = 0;
        Notification last = null;
        for (Task t : soon) {
            Notification n = NotificationFactory.deadlineSoon(userId, t.getTitle(),
                Formats.deadline(t.getDeadline(), now).toLowerCase());
            String key = "\"" + t.getTitle() + "\"";
            if (!notificationDao.existsToday(userId, Notification.Type.DEADLINE, key)) {
                NotificationService.save(n);
                fresh++;
                last = n;
            }
        }
        if (fresh == 1) {
            DesktopToast.show(DesktopToast.Kind.WARNING, last.getTitle(), last.getMessage(), java.time.Duration.ofSeconds(8),
                new DesktopToast.Action("Open tasks", true, () -> {
                    Navigator.bringToFront();
                    Navigator.goTo(Screen.TASKS);
                }));
        } else if (fresh > 1) {
            DesktopToast.show(DesktopToast.Kind.WARNING, fresh + " deadlines coming up",
                fresh + " tasks are due in the next 24 hours.", java.time.Duration.ofSeconds(8),
                new DesktopToast.Action("Open tasks", true, () -> {
                    Navigator.bringToFront();
                    Navigator.goTo(Screen.TASKS);
                }));
        }
    }
}
