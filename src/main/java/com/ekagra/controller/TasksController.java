package com.ekagra.controller;

import com.ekagra.app.Navigator;
import com.ekagra.app.Screen;
import com.ekagra.model.Priority;
import com.ekagra.model.Task;
import com.ekagra.model.TaskStatus;
import com.ekagra.service.SessionContext;
import com.ekagra.service.TaskService;
import com.ekagra.service.TaskService.SortBy;
import com.ekagra.service.TaskService.TaskQuery;
import com.ekagra.ui.Animations;
import com.ekagra.ui.TaskEditorDialog;
import com.ekagra.ui.Ui;
import com.ekagra.util.Formats;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** Tasks screen: list with filters/sort, deadline alerts and a weekly calendar (FR-05 .. FR-09). */
public class TasksController {

    private static final javafx.scene.layout.Priority GROW = javafx.scene.layout.Priority.ALWAYS;

    @FXML private HBox statsRow;
    @FXML private HBox alertBanner;
    @FXML private Label alertText;
    @FXML private TextField searchField;
    @FXML private ComboBox<TaskStatus> statusFilter;
    @FXML private ComboBox<Priority> priorityFilter;
    @FXML private ComboBox<String> subjectFilter;
    @FXML private ComboBox<SortBy> sortBox;
    @FXML private ToggleButton listToggle;
    @FXML private ToggleButton weekToggle;
    @FXML private ScrollPane listView;
    @FXML private VBox taskList;
    @FXML private VBox weekView;
    @FXML private Label weekLabel;
    @FXML private GridPane weekGrid;

    private final TaskService taskService = new TaskService();
    private int userId;
    private List<Task> allTasks = new ArrayList<>();
    private LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

    @FXML
    private void initialize() {
        userId = SessionContext.getInstance().getCurrentUser().getId();

        statusFilter.setItems(FXCollections.observableArrayList(null, TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE));
        Ui.nullableCombo(statusFilter, "All statuses", TaskStatus::getLabel);
        statusFilter.getSelectionModel().selectFirst();

        priorityFilter.setItems(FXCollections.observableArrayList(null, Priority.URGENT, Priority.HIGH, Priority.MEDIUM, Priority.LOW));
        Ui.nullableCombo(priorityFilter, "All priorities", Priority::getLabel);
        priorityFilter.getSelectionModel().selectFirst();

        Ui.nullableCombo(subjectFilter, "All subjects", s -> s);

        sortBox.getItems().setAll(SortBy.values());
        sortBox.setValue(SortBy.DEADLINE);

        searchField.textProperty().addListener((o, a, b) -> renderList());
        statusFilter.valueProperty().addListener((o, a, b) -> renderList());
        priorityFilter.valueProperty().addListener((o, a, b) -> renderList());
        subjectFilter.valueProperty().addListener((o, a, b) -> renderList());
        sortBox.valueProperty().addListener((o, a, b) -> renderList());

        ToggleGroup views = new ToggleGroup();
        listToggle.setToggleGroup(views);
        weekToggle.setToggleGroup(views);
        views.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null) {
                views.selectToggle(old);
                return;
            }
            boolean week = now == weekToggle;
            weekView.setVisible(week);
            listView.setVisible(!week);
            if (week) {
                renderWeek();
            }
        });

        reload();
    }

    // ---------- data ----------

    private void reload() {
        allTasks = taskService.getTasks(userId);

        String selectedSubject = subjectFilter.getValue();
        List<String> subjects = new ArrayList<>();
        subjects.add(null);
        subjects.addAll(taskService.getSubjects(allTasks));
        subjectFilter.setItems(FXCollections.observableArrayList(subjects));
        subjectFilter.setValue(subjects.contains(selectedSubject) ? selectedSubject : null);

        renderStats();
        renderList();
        Animations.stagger(taskList, 10);
        if (weekView.isVisible()) {
            renderWeek();
        }
    }

    private void renderStats() {
        LocalDateTime now = LocalDateTime.now();
        long open = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
        long progress = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long done = allTasks.stream().filter(Task::isDone).count();
        var summary = taskService.deadlineSummary(allTasks, now);

        VBox overdueTile = Ui.statTile(summary.overdue(), "Overdue", "fth-alert-circle");
        if (summary.overdue() > 0) {
            overdueTile.getStyleClass().add("stat-danger");
        }
        statsRow.getChildren().setAll(
            Ui.statTile((int) open, "To do", "fth-circle"),
            Ui.statTile((int) progress, "In progress", "fth-loader"),
            Ui.statTile((int) done, "Completed", "fth-check-circle"),
            overdueTile);
        statsRow.getChildren().forEach(n -> HBox.setHgrow(n, GROW));

        if (summary.hasAlerts()) {
            List<String> parts = new ArrayList<>();
            if (summary.overdue() > 0) {
                parts.add(summary.overdue() + (summary.overdue() == 1 ? " task is overdue" : " tasks are overdue"));
            }
            if (summary.dueSoon() > 0) {
                parts.add(summary.dueSoon() + " due in the next 24 hours");
            }
            alertText.setText(String.join("  ·  ", parts));
            alertBanner.getStyleClass().remove("alert-danger");
            if (summary.overdue() > 0) {
                alertBanner.getStyleClass().add("alert-danger");
            }
        }
        alertBanner.setVisible(summary.hasAlerts());
        alertBanner.setManaged(summary.hasAlerts());
    }

    // ---------- list view ----------

    private void renderList() {
        TaskQuery query = new TaskQuery(searchField.getText(), statusFilter.getValue(), priorityFilter.getValue(),
            subjectFilter.getValue(), sortBox.getValue());
        List<Task> shown = taskService.filter(allTasks, query);

        taskList.getChildren().clear();
        if (allTasks.isEmpty()) {
            taskList.getChildren().add(Ui.emptyState("fth-check-square", "No tasks yet",
                "Add your first assignment or to-do with the \"New task\" button."));
            return;
        }
        if (shown.isEmpty()) {
            taskList.getChildren().add(Ui.emptyState("fth-search", "Nothing matches",
                "Try clearing the search or filters."));
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        shown.forEach(t -> taskList.getChildren().add(taskCard(t, now)));
    }

    private HBox taskCard(Task task, LocalDateTime now) {
        CheckBox done = new CheckBox();
        done.setSelected(task.isDone());
        done.getStyleClass().add("task-check");
        done.setOnAction(e -> {
            taskService.toggleDone(task);
            reload();
        });

        Label title = Ui.label(task.getTitle(), "task-title");
        if (task.isDone()) {
            title.getStyleClass().add("done");
        }

        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.getChildren().add(Ui.chip(task.getPriority().getLabel(), task.getPriority().styleClass()));
        if (task.getSubject() != null) {
            meta.getChildren().add(Ui.chip(task.getSubject(), "chip-subject"));
        }
        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            meta.getChildren().add(Ui.chip("In progress", "chip-progress"));
        }
        if (task.getDeadline() != null) {
            Label deadline = new Label(Formats.deadline(task.getDeadline(), now), new FontIcon("fth-calendar"));
            deadline.getStyleClass().add("task-meta");
            if (task.isOverdue(now)) {
                deadline.getStyleClass().add("overdue");
            } else if (task.isDueSoon(now)) {
                deadline.getStyleClass().add("due-soon");
            } else if (task.isDone()) {
                deadline.setText(task.wasCompletedOnTime() ? "Finished on time" : "Finished late");
            }
            meta.getChildren().add(deadline);
        }
        if (task.getEstimatedMin() > 0 || task.getSpentMin() > 0) {
            String time = Formats.minutes(task.getSpentMin())
                + (task.getEstimatedMin() > 0 ? " / " + Formats.minutes(task.getEstimatedMin()) : " focused");
            Label timeLabel = new Label(time, new FontIcon("fth-clock"));
            timeLabel.getStyleClass().add("task-meta");
            meta.getChildren().add(timeLabel);
        }

        VBox main = new VBox(8, title, meta);
        if (!task.getSubtasks().isEmpty()) {
            ProgressBar bar = new ProgressBar(task.getProgress());
            bar.setMaxWidth(260);
            bar.getStyleClass().add("small-bar");
            Label count = Ui.label(task.getDoneSubtaskCount() + "/" + task.getSubtasks().size() + " subtasks",
                "muted", "small");
            HBox progressRow = new HBox(10, bar, count);
            progressRow.setAlignment(Pos.CENTER_LEFT);
            main.getChildren().add(progressRow);
        }
        HBox.setHgrow(main, GROW);

        HBox actions = new HBox(2);
        actions.setAlignment(Pos.CENTER);
        if (!task.isDone()) {
            actions.getChildren().add(Ui.iconButton("fth-play-circle", "Start a focus session on this task",
                () -> Navigator.goTo(Screen.FOCUS, FocusController.class, c -> c.focusOn(task))));
        }
        actions.getChildren().addAll(
            Ui.iconButton("fth-edit-2", "Edit", () -> openEditor(task)),
            Ui.iconButton("fth-trash-2", "Delete", () -> deleteTask(task)));

        HBox card = new HBox(14, done, main, actions);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().addAll("card", "task-card", "priority-" + task.getPriority().name().toLowerCase());
        if (task.isOverdue(now)) {
            card.getStyleClass().add("task-overdue");
        }
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                openEditor(task);
            }
        });
        return card;
    }

    // ---------- week view ----------

    private void renderWeek() {
        LocalDate weekEnd = weekStart.plusDays(6);
        weekLabel.setText(weekStart.format(DateTimeFormatter.ofPattern("d MMM")) + " – "
            + weekEnd.format(DateTimeFormatter.ofPattern("d MMM yyyy")));

        weekGrid.getChildren().clear();
        weekGrid.getColumnConstraints().clear();
        weekGrid.getRowConstraints().clear();
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            weekGrid.getColumnConstraints().add(cc);
        }
        RowConstraints rc = new RowConstraints();
        rc.setVgrow(GROW);
        rc.setFillHeight(true);
        weekGrid.getRowConstraints().add(rc);

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            Label dayName = Ui.label(day.format(DateTimeFormatter.ofPattern("EEE")).toUpperCase(), "week-day-name");
            Label dayNum = Ui.label(String.valueOf(day.getDayOfMonth()), "week-day-num");
            VBox column = new VBox(8, new VBox(0, dayName, dayNum));
            column.getStyleClass().add("week-column");
            if (day.equals(today)) {
                column.getStyleClass().add("today");
            }
            allTasks.stream()
                .filter(t -> t.getDeadline() != null && t.getDeadline().toLocalDate().equals(day))
                .sorted((a, b) -> a.getDeadline().compareTo(b.getDeadline()))
                .forEach(t -> column.getChildren().add(weekChip(t, now)));
            weekGrid.add(column, i, 0);
        }
    }

    private VBox weekChip(Task task, LocalDateTime now) {
        Label title = Ui.label(task.getTitle(), "week-task-title");
        title.setWrapText(true);
        Label time = Ui.label(task.getDeadline().format(Formats.TIME)
            + (task.getSubject() != null ? " · " + task.getSubject() : ""), "small", "muted");
        VBox chip = new VBox(2, title, time);
        chip.getStyleClass().addAll("week-task", "priority-" + task.getPriority().name().toLowerCase());
        if (task.isDone()) {
            chip.getStyleClass().add("done");
        } else if (task.isOverdue(now)) {
            chip.getStyleClass().add("task-overdue");
        }
        chip.setOnMouseClicked(e -> openEditor(task));
        return chip;
    }

    @FXML
    private void onPrevWeek() {
        weekStart = weekStart.minusWeeks(1);
        renderWeek();
    }

    @FXML
    private void onNextWeek() {
        weekStart = weekStart.plusWeeks(1);
        renderWeek();
    }

    @FXML
    private void onThisWeek() {
        weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        renderWeek();
    }

    // ---------- actions ----------

    @FXML
    private void onNewTask() {
        openEditor(null);
    }

    private void openEditor(Task task) {
        new TaskEditorDialog(task, userId, taskService.getSubjects(allTasks), taskService).showAndWait();
        reload();   // reload either way: cancelled edits must not leave stale values in memory
    }

    private void deleteTask(Task task) {
        if (Ui.confirm("Delete task?", "\"" + task.getTitle() + "\" and its subtasks will be removed.", "Delete")) {
            taskService.delete(task);
            reload();
        }
    }
}
