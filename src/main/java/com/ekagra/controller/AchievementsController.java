package com.ekagra.controller;

import com.ekagra.model.Student;
import com.ekagra.service.GamificationService;
import com.ekagra.service.GamificationService.BadgeStatus;
import com.ekagra.service.SessionContext;
import com.ekagra.ui.Animations;
import com.ekagra.ui.TimerRing;
import com.ekagra.ui.Ui;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Achievements screen (FR-33, FR-34): level, XP and the badge collection. */
public class AchievementsController {

    @FXML private StackPane levelRingHolder;
    @FXML private Label levelTitle;
    @FXML private Label levelSub;
    @FXML private ProgressBar xpBar;
    @FXML private Label totalXp;
    @FXML private Label badgeCount;
    @FXML private Label habitBest;
    @FXML private VBox xpRules;
    @FXML private Label badgeSummary;
    @FXML private FlowPane badgeGrid;

    @FXML
    private void initialize() {
        Student s = SessionContext.getInstance().getStudent();
        GamificationService game = new GamificationService(s);
        List<BadgeStatus> badges = game.badgeStatuses();
        long earned = badges.stream().filter(BadgeStatus::isEarned).count();

        TimerRing ring = new TimerRing(130);
        double into = (double) s.getXpIntoLevel() / Student.XP_PER_LEVEL;
        ring.update(String.valueOf(s.getLevel()), "Level", into, false);
        levelRingHolder.getChildren().add(ring);

        levelTitle.setText(levelName(s.getLevel()));
        levelSub.setText((Student.XP_PER_LEVEL - s.getXpIntoLevel()) + " XP to level " + (s.getLevel() + 1));
        xpBar.setProgress(into);
        totalXp.setText(s.getXp() + " XP total");
        Animations.countUp(totalXp, s.getXp(), v -> v + " XP total");
        badgeCount.setText(earned + " / " + badges.size() + " badges");
        habitBest.setText("Best habit streak: " + game.bestHabitStreak() + " days");

        rule("fth-clock", "Focus session", "+" + GamificationService.XP_PER_5_FOCUS_MIN + " XP per 5 min focused");
        rule("fth-check-square", "Complete a task", "+" + GamificationService.XP_TASK + " XP");
        rule("fth-calendar", "...before its deadline", "+" + GamificationService.XP_TASK_ON_TIME_BONUS + " bonus");
        rule("fth-repeat", "Tick a habit", "+" + GamificationService.XP_HABIT + " XP");
        rule("fth-smile", "Daily mood check-in", "+" + GamificationService.XP_MOOD + " XP");

        badgeSummary.setText(earned == 0 ? "Complete your first focus session to unlock one!"
            : earned + " unlocked, " + (badges.size() - earned) + " to go");
        for (BadgeStatus b : badges) {
            badgeGrid.getChildren().add(badgeCard(b));
        }
        Animations.stagger(badgeGrid, 12);
    }

    private static String levelName(int level) {
        String[] names = {"Beginner", "Learner", "Focused", "Dedicated", "Disciplined", "Scholar", "Master", "Sage"};
        return "Level " + level + " · " + names[Math.min(names.length - 1, level - 1)];
    }

    private void rule(String icon, String what, String xp) {
        FontIcon fi = new FontIcon(icon);
        fi.getStyleClass().add("stat-icon");
        Label w = Ui.label(what, "score-part-name");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label x = Ui.chip(xp, "chip-progress");
        HBox row = new HBox(10, fi, w, sp, x);
        row.setAlignment(Pos.CENTER_LEFT);
        xpRules.getChildren().add(row);
    }

    private VBox badgeCard(BadgeStatus s) {
        StackPane icon = new StackPane(new FontIcon(s.badge().getIcon()));
        icon.getStyleClass().add("badge-icon");
        Label name = Ui.label(s.badge().getName(), "badge-name");
        Label desc = Ui.label(s.badge().getDescription(), "muted", "small");
        desc.setWrapText(true);
        VBox card = new VBox(8, icon, name, desc);
        card.getStyleClass().addAll("badge-card", s.isEarned() ? "earned" : "locked");
        card.setPrefWidth(200);
        card.setAlignment(Pos.TOP_CENTER);
        desc.setAlignment(Pos.CENTER);
        desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        if (s.isEarned()) {
            card.getChildren().add(Ui.chip("Earned " + s.earnedAt().format(DateTimeFormatter.ofPattern("d MMM")), "chip-progress"));
        } else {
            ProgressBar bar = new ProgressBar(s.fraction());
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add("small-bar");
            Label progress = Ui.label(Math.min(s.current(), s.target()) + " / " + s.target(), "muted", "small");
            card.getChildren().addAll(bar, progress);
            Tooltip.install(card, new Tooltip("Locked: " + s.badge().getDescription()));
        }
        return card;
    }
}
