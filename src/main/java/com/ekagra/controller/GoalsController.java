package com.ekagra.controller;

import com.ekagra.model.Goal;
import com.ekagra.model.Habit;
import com.ekagra.model.Student;
import com.ekagra.service.GoalService;
import com.ekagra.service.GoalService.GoalProgress;
import com.ekagra.service.SessionContext;
import com.ekagra.service.ValidationException;
import com.ekagra.ui.Animations;
import com.ekagra.ui.GoalDialog;
import com.ekagra.ui.Messages;
import com.ekagra.ui.Ui;
import com.ekagra.util.Formats;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Goals & Habits screen (FR-27, FR-28). */
public class GoalsController {

    @FXML private Label goalSummary;
    @FXML private VBox goalList;
    @FXML private ComboBox<String> habitIcon;
    @FXML private TextField habitTitle;
    @FXML private Label habitMessage;
    @FXML private VBox habitList;

    private GoalService service;
    private boolean firstRender = true;

    @FXML
    private void initialize() {
        Student student = SessionContext.getInstance().getStudent();
        service = new GoalService(student.getId());
        service.ensureDefaultGoals(student.getDailyTargetMin());

        habitIcon.getItems().setAll(GoalService.HABIT_ICONS);
        habitIcon.setCellFactory(lv -> iconCell());
        habitIcon.setButtonCell(iconCell());
        habitIcon.getSelectionModel().selectFirst();

        renderGoals();
        renderHabits();
        firstRender = false;
    }

    private static ListCell<String> iconCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : new FontIcon(item));
                setText(null);
            }
        };
    }

    // ---------- goals ----------

    private void renderGoals() {
        List<GoalProgress> goals = service.goalsWithProgress();
        long achieved = goals.stream().filter(GoalProgress::isAchieved).count();
        goalSummary.setText(goals.isEmpty() ? "Set a target to work towards"
            : achieved + " of " + goals.size() + " on track right now");
        goalList.getChildren().clear();
        if (goals.isEmpty()) {
            goalList.getChildren().add(Ui.emptyState("fth-target", "No goals yet", "Add a daily or weekly goal."));
        }
        for (GoalProgress p : goals) {
            goalList.getChildren().add(goalRow(p));
        }
        if (firstRender) {
            Animations.stagger(goalList, 8);
        }
    }

    private VBox goalRow(GoalProgress p) {
        Goal g = p.goal();
        StackPane icon = new StackPane(new FontIcon(g.getType().getIcon()));
        icon.getStyleClass().add("goal-icon");

        Label title = Ui.label(g.getTitle(), "goal-title");
        title.setWrapText(true);
        title.setMinWidth(0);
        HBox.setHgrow(title, Priority.SOMETIMES);
        Label period = Ui.chip(g.getPeriod().getLabel(), "chip-subject");

        Label status;
        if (p.isOverLimit()) {
            status = Ui.chip("Over limit", "chip-urgent");
        } else if (p.isAchieved()) {
            status = Ui.chip(g.getType().isLimit() ? "Under limit" : "Achieved", "chip-progress");
            status.setGraphic(new FontIcon("fth-check"));
        } else {
            status = Ui.chip(Math.round(p.fraction() * 100) + "%", "chip-medium");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button delete = Ui.iconButton("fth-trash-2", "Delete goal", () -> {
            if (Ui.confirm("Delete goal?", "\"" + g.getTitle() + "\" will be removed.", "Delete")) {
                service.deleteGoal(g);
                renderGoals();
            }
        });
        HBox top = new HBox(10, icon, title, spacer, period, status, delete);
        top.setAlignment(Pos.CENTER_LEFT);

        ProgressBar bar = new ProgressBar(p.fraction());
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("goal-bar");
        if (p.isOverLimit()) {
            bar.getStyleClass().add("over");
        } else if (p.isAchieved() && !g.getType().isLimit()) {
            bar.getStyleClass().add("done");
        }
        HBox.setHgrow(bar, Priority.ALWAYS);
        String unit = g.getType().getUnit();
        String amounts = unit.equals("min")
            ? Formats.minutes(p.current()) + " / " + Formats.minutes(p.target())
            : p.current() + " / " + p.target() + " " + unit;
        Label numbers = Ui.label(amounts, "muted", "small");
        numbers.setMinWidth(90);
        numbers.setAlignment(Pos.CENTER_RIGHT);
        HBox bottom = new HBox(12, bar, numbers);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new javafx.geometry.Insets(0, 0, 0, 46));

        VBox row = new VBox(8, top, bottom);
        row.getStyleClass().add("goal-row");
        return row;
    }

    @FXML
    private void onAddGoal() {
        new GoalDialog(service).showAndWait().ifPresent(g -> renderGoals());
    }

    // ---------- habits ----------

    private void renderHabits() {
        habitList.getChildren().clear();
        List<Habit> habits = service.habits();
        if (habits.isEmpty()) {
            habitList.getChildren().add(Ui.emptyState("fth-repeat", "No habits yet",
                "Small daily habits add up, like drinking water or reading 20 minutes."));
        }
        LocalDate today = LocalDate.now();
        for (Habit h : habits) {
            habitList.getChildren().add(habitRow(h, today));
        }
        if (firstRender) {
            Animations.stagger(habitList, 8);
        }
    }

    private VBox habitRow(Habit h, LocalDate today) {
        StackPane icon = new StackPane(new FontIcon(h.getIcon()));
        icon.getStyleClass().add("goal-icon");
        Label title = Ui.label(h.getTitle(), "goal-title");
        int streak = h.currentStreak(today);
        Label streakLabel = new Label(streak + (streak == 1 ? " day" : " days"), new FontIcon("fth-zap"));
        streakLabel.getStyleClass().add(streak > 0 ? "streak-on" : "streak-off");
        streakLabel.setTooltip(new Tooltip("Current streak · best " + h.bestStreak()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button delete = Ui.iconButton("fth-trash-2", "Delete habit", () -> {
            if (Ui.confirm("Delete habit?", "\"" + h.getTitle() + "\" and its history will be removed.", "Delete")) {
                service.deleteHabit(h);
                renderHabits();
            }
        });
        HBox top = new HBox(10, icon, title, spacer, streakLabel, delete);
        top.setAlignment(Pos.CENTER_LEFT);

        HBox days = new HBox(6);
        days.setPadding(new javafx.geometry.Insets(0, 0, 0, 46));
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            Button dayButton = new Button(i == 0 ? "Today" : d.format(DateTimeFormatter.ofPattern("EEE")));
            dayButton.getStyleClass().add("habit-day");
            if (h.isDoneOn(d)) {
                dayButton.getStyleClass().add("done");
                dayButton.setGraphic(new FontIcon("fth-check"));
            }
            dayButton.setTooltip(new Tooltip(d.format(DateTimeFormatter.ofPattern("EEEE d MMM"))));
            dayButton.setOnAction(e -> {
                service.toggleHabit(h, d);
                renderHabits();
            });
            days.getChildren().add(dayButton);
        }
        VBox row = new VBox(8, top, days);
        row.getStyleClass().add("goal-row");
        return row;
    }

    @FXML
    private void onAddHabit() {
        try {
            service.addHabit(habitTitle.getText(), habitIcon.getValue());
            habitTitle.clear();
            Messages.hide(habitMessage);
            renderHabits();
        } catch (ValidationException e) {
            Messages.error(habitMessage, e.getMessage());
        }
    }
}
