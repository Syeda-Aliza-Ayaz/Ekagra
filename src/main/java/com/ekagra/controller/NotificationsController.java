package com.ekagra.controller;

import com.ekagra.model.Notification;
import com.ekagra.service.NotificationService;
import com.ekagra.service.SessionContext;
import com.ekagra.ui.Animations;
import com.ekagra.ui.Ui;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Notification centre (FR-35). Opening it marks everything as read (after showing what was new). */
public class NotificationsController implements Disposable {

    @FXML private ToggleButton allToggle;
    @FXML private ToggleButton unreadToggle;
    @FXML private Label summary;
    @FXML private VBox list;

    private int userId;
    private List<Notification> items;

    @FXML
    private void initialize() {
        userId = SessionContext.getInstance().getCurrentUser().getId();
        ToggleGroup g = new ToggleGroup();
        allToggle.setToggleGroup(g);
        unreadToggle.setToggleGroup(g);
        g.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null) {
                g.selectToggle(old);
            } else {
                render();
            }
        });
        items = NotificationService.recent(userId, 100);
        render();
        Animations.stagger(list, 10);
    }

    /** Leaving the screen marks the shown notifications as read. */
    @Override
    public void dispose() {
        NotificationService.markAllRead(userId);
    }

    private void render() {
        long unread = items.stream().filter(n -> !n.isRead()).count();
        summary.setText(items.isEmpty() ? "" : unread + " unread of " + items.size());
        list.getChildren().clear();
        List<Notification> shown = unreadToggle.isSelected() ? items.stream().filter(n -> !n.isRead()).toList() : items;
        if (shown.isEmpty()) {
            list.getChildren().add(Ui.emptyState("fth-bell", unreadToggle.isSelected() ? "All caught up" : "No notifications yet",
                "Reminders, alerts, badges and mentor messages will appear here."));
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : shown) {
            StackPane icon = new StackPane(new FontIcon(n.getType().getIcon()));
            icon.getStyleClass().addAll("insight-icon", "notif-" + n.getType().name().toLowerCase());
            Label title = Ui.label(n.getTitle(), "notif-title");
            Label msg = Ui.label(n.getMessage(), "muted");
            msg.setWrapText(true);
            VBox text = new VBox(2, title, msg);
            HBox.setHgrow(text, Priority.ALWAYS);
            Label when = Ui.label(ago(n.getCreatedAt(), now), "muted", "small");
            when.setMinWidth(Region.USE_PREF_SIZE);
            HBox row = new HBox(12, icon, text, when);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().addAll("card", "notif-row");
            if (!n.isRead()) {
                row.getStyleClass().add("unread");
                Region dot = new Region();
                dot.getStyleClass().add("unread-dot");
                row.getChildren().add(dot);
            }
            list.getChildren().add(row);
        }
    }

    static String ago(LocalDateTime t, LocalDateTime now) {
        Duration d = Duration.between(t, now);
        if (d.toMinutes() < 1) {
            return "just now";
        }
        if (d.toHours() < 1) {
            return d.toMinutes() + " min ago";
        }
        if (d.toDays() < 1) {
            return d.toHours() + " h ago";
        }
        if (d.toDays() < 7) {
            return d.toDays() == 1 ? "yesterday" : d.toDays() + " days ago";
        }
        return t.format(DateTimeFormatter.ofPattern("d MMM"));
    }

    @FXML
    private void onMarkAllRead() {
        NotificationService.markAllRead(userId);
        items = NotificationService.recent(userId, 100);
        render();
    }

    @FXML
    private void onClearAll() {
        if (Ui.confirm("Clear all notifications?", "This removes your whole notification history.", "Clear")) {
            NotificationService.clearAll(userId);
            items = List.of();
            render();
        }
    }
}
