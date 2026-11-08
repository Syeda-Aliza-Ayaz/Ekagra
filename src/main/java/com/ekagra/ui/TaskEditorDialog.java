package com.ekagra.ui;

import com.ekagra.model.Priority;
import com.ekagra.model.SubTask;
import com.ekagra.model.Task;
import com.ekagra.model.TaskStatus;
import com.ekagra.service.TaskService;
import com.ekagra.service.ValidationException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Create / edit a task including its subtasks (FR-05, FR-07).
 * The task is validated and saved when the user clicks Save; on error the dialog stays open.
 */
public class TaskEditorDialog extends Dialog<Task> {

    private final Task task;
    private final TaskService taskService;

    private final TextField titleField = new TextField();
    private final TextArea descriptionArea = new TextArea();
    private final ComboBox<String> subjectBox = new ComboBox<>();
    private final ComboBox<Priority> priorityBox = new ComboBox<>();
    private final ComboBox<TaskStatus> statusBox = new ComboBox<>();
    private final DatePicker datePicker = new DatePicker();
    private final ComboBox<String> timeBox = new ComboBox<>();
    private final Spinner<Integer> estimateSpinner = new Spinner<>();
    private final VBox subtaskList = new VBox(6);
    private final TextField newSubtaskField = new TextField();
    private final Label errorLabel = new Label();
    private final List<SubtaskRow> rows = new ArrayList<>();

    private record SubtaskRow(HBox box, CheckBox done, TextField title) {
    }

    public TaskEditorDialog(Task existing, int userId, List<String> subjects, TaskService taskService) {
        this.taskService = taskService;
        boolean isNew = existing == null;
        this.task = isNew ? new Task() : existing;
        if (isNew) {
            task.setUserId(userId);
        }

        setTitle(isNew ? "New task" : "Edit task");
        setHeaderText(null);
        Ui.style(this);

        ButtonType save = new ButtonType(isNew ? "Add task" : "Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        getDialogPane().setContent(buildForm(subjects));
        getDialogPane().setPrefWidth(600);

        fill();

        Button saveButton = (Button) getDialogPane().lookupButton(save);
        saveButton.getStyleClass().add("accent");
        saveButton.addEventFilter(ActionEvent.ACTION, e -> {
            if (!trySave()) {
                e.consume();       // keep dialog open
            }
        });
        setResultConverter(bt -> bt == save ? task : null);
        Platform.runLater(titleField::requestFocus);
    }

    private ScrollPane buildForm(List<String> subjects) {
        titleField.setPromptText("e.g. OOAD assignment 2: class diagram");
        descriptionArea.setPromptText("Notes (optional)");
        descriptionArea.setPrefRowCount(2);
        descriptionArea.setWrapText(true);

        subjectBox.setEditable(true);
        subjectBox.getItems().setAll(subjects);
        subjectBox.setPromptText("e.g. OOAD");
        subjectBox.setMaxWidth(Double.MAX_VALUE);

        priorityBox.getItems().setAll(Priority.values());
        priorityBox.setMaxWidth(Double.MAX_VALUE);
        statusBox.getItems().setAll(TaskStatus.values());
        statusBox.setMaxWidth(Double.MAX_VALUE);

        datePicker.setPromptText("No deadline");
        datePicker.setMaxWidth(Double.MAX_VALUE);
        timeBox.setEditable(true);
        for (int h = 0; h < 24; h++) {
            timeBox.getItems().addAll(String.format("%02d:00", h), String.format("%02d:30", h));
        }
        timeBox.getItems().add("23:59");
        timeBox.setPrefWidth(110);

        estimateSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1200, 0, 15));
        estimateSpinner.setEditable(true);
        estimateSpinner.setPrefWidth(160);
        Ui.spinner(estimateSpinner);

        newSubtaskField.setPromptText("Add a subtask and press Enter");
        newSubtaskField.setOnAction(e -> addSubtaskFromField());
        Button addSub = new Button("Add");
        addSub.setOnAction(e -> addSubtaskFromField());
        HBox addRow = new HBox(8, newSubtaskField, addSub);
        HBox.setHgrow(newSubtaskField, javafx.scene.layout.Priority.ALWAYS);

        Messages.hide(errorLabel);

        VBox form = new VBox(10,
            field("Title", titleField),
            field("Description", descriptionArea),
            row(field("Subject", subjectBox), field("Priority", priorityBox), field("Status", statusBox)),
            row(field("Deadline", datePicker), field("Time", timeBox), field("Estimate (min)", estimateSpinner)),
            Ui.label("Subtasks", "field-label"),
            subtaskList,
            addRow,
            errorLabel);
        form.getStyleClass().add("dialog-form");

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        // fit on small (720p) screens: leave room for the title bar and buttons
        double screenH = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
        scroll.setPrefViewportHeight(Math.min(520, screenH - 220));
        return scroll;
    }

    private static VBox field(String label, javafx.scene.Node control) {
        VBox box = new VBox(6, Ui.label(label, "field-label"), control);
        HBox.setHgrow(box, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }

    private static HBox row(VBox... fields) {
        HBox box = new HBox(12, fields);
        box.setAlignment(Pos.BOTTOM_LEFT);
        return box;
    }

    private void fill() {
        titleField.setText(task.getTitle());
        descriptionArea.setText(task.getDescription());
        subjectBox.setValue(task.getSubject());
        priorityBox.setValue(task.getPriority());
        statusBox.setValue(task.getStatus());
        if (task.getDeadline() != null) {
            datePicker.setValue(task.getDeadline().toLocalDate());
            timeBox.setValue(task.getDeadline().toLocalTime().format(com.ekagra.util.Formats.TIME));
        } else {
            timeBox.setValue("23:59");
        }
        estimateSpinner.getValueFactory().setValue(task.getEstimatedMin());
        task.getSubtasks().forEach(s -> addSubtaskRow(s.getTitle(), s.isDone()));
    }

    private void addSubtaskFromField() {
        String text = newSubtaskField.getText();
        if (text != null && !text.isBlank()) {
            addSubtaskRow(text.trim(), false);
            newSubtaskField.clear();
        }
    }

    private void addSubtaskRow(String title, boolean done) {
        CheckBox check = new CheckBox();
        check.setSelected(done);
        TextField field = new TextField(title);
        HBox box = new HBox(8, check, field);
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(field, javafx.scene.layout.Priority.ALWAYS);
        SubtaskRow row = new SubtaskRow(box, check, field);
        box.getChildren().add(Ui.iconButton("fth-x", "Remove subtask", () -> {
            rows.remove(row);
            subtaskList.getChildren().remove(box);
        }));
        rows.add(row);
        subtaskList.getChildren().add(box);
    }

    /** Copies the form into the task and saves it. Returns false (and shows an error) if invalid. */
    private boolean trySave() {
        try {
            task.setTitle(titleField.getText());
            task.setDescription(descriptionArea.getText());
            task.setSubject(subjectBox.getEditor().getText());
            task.setPriority(priorityBox.getValue());
            task.setStatus(statusBox.getValue());
            task.setDeadline(readDeadline());
            task.setEstimatedMin(estimateSpinner.getValue());
            task.clearSubTasks();
            for (SubtaskRow r : rows) {
                if (!r.title().getText().isBlank()) {
                    task.addSubTask(new SubTask(r.title().getText().trim(), r.done().isSelected()));
                }
            }
            taskService.save(task);
            return true;
        } catch (ValidationException e) {
            Messages.error(errorLabel, e.getMessage());
            return false;
        }
    }

    private LocalDateTime readDeadline() {
        if (datePicker.getValue() == null) {
            // the user may have typed a date without pressing Enter
            String typed = datePicker.getEditor().getText();
            if (typed == null || typed.isBlank()) {
                return null;
            }
            try {
                datePicker.setValue(datePicker.getConverter().fromString(typed));
            } catch (DateTimeParseException e) {
                throw new ValidationException("Deadline date isn't valid.");
            }
        }
        String timeText = timeBox.getEditor().getText();
        LocalTime time;
        try {
            time = timeText == null || timeText.isBlank() ? LocalTime.of(23, 59) : LocalTime.parse(timeText.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Time must look like 14:30.");
        }
        return datePicker.getValue().atTime(time);
    }
}
