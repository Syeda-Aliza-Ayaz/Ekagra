package com.ekagra.ui;

import com.ekagra.model.Goal;
import com.ekagra.service.GoalService;
import com.ekagra.service.ValidationException;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** "New goal" dialog with a live preview of the goal sentence. */
public class GoalDialog extends Dialog<Goal> {

    private final ComboBox<Goal.Type> typeBox = new ComboBox<>();
    private final ComboBox<Goal.Period> periodBox = new ComboBox<>();
    private final Spinner<Integer> targetSpinner = new Spinner<>();
    private final Label unitLabel = new Label();
    private final Label preview = new Label();
    private final Label error = new Label();
    private Goal created;

    public GoalDialog(GoalService service) {
        setTitle("New goal");
        setHeaderText(null);
        Ui.style(this);

        typeBox.getItems().setAll(Goal.Type.values());
        typeBox.setValue(Goal.Type.STUDY_TIME);
        typeBox.setMaxWidth(Double.MAX_VALUE);
        periodBox.getItems().setAll(Goal.Period.values());
        periodBox.setValue(Goal.Period.DAILY);
        periodBox.setMaxWidth(Double.MAX_VALUE);
        targetSpinner.setEditable(true);
        targetSpinner.setPrefWidth(160);
        Ui.spinner(targetSpinner);
        preview.getStyleClass().add("goal-preview");
        preview.setWrapText(true);
        Messages.hide(error);

        typeBox.valueProperty().addListener((o, a, b) -> resetTarget());
        targetSpinner.valueProperty().addListener((o, a, b) -> updatePreview());
        periodBox.valueProperty().addListener((o, a, b) -> updatePreview());
        resetTarget();

        HBox targetRow = new HBox(10, targetSpinner, unitLabel);
        targetRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox form = new VBox(8,
            Ui.label("What do you want to measure?", "field-label"), typeBox,
            Ui.label("How often?", "field-label"), periodBox,
            Ui.label("Target", "field-label"), targetRow,
            preview, error);
        form.setPadding(new Insets(0, 2, 4, 2));
        form.setPrefWidth(420);
        getDialogPane().setContent(form);

        ButtonType add = new ButtonType("Add goal", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(add, ButtonType.CANCEL);
        Button addButton = (Button) getDialogPane().lookupButton(add);
        addButton.getStyleClass().add("accent");
        addButton.addEventFilter(ActionEvent.ACTION, e -> {
            try {
                created = service.addGoal(typeBox.getValue(), periodBox.getValue(), targetSpinner.getValue());
            } catch (ValidationException ex) {
                Messages.error(error, ex.getMessage());
                e.consume();
            }
        });
        setResultConverter(bt -> bt == add ? created : null);
    }

    private void resetTarget() {
        Goal.Type t = typeBox.getValue();
        boolean minutes = t.getUnit().equals("min");
        int initial = switch (t) {
            case STUDY_TIME -> 120;
            case FOCUS_SESSIONS -> 4;
            case TASKS_DONE -> 3;
            case MAX_DISTRACTING -> 60;
            case MAX_SCREEN_TIME -> 300;
        };
        targetSpinner.setValueFactory(minutes
            ? new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 1440, initial, 15)
            : new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, initial, 1));
        unitLabel.setText(minutes ? "minutes" : t.getUnit());
        updatePreview();
    }

    private void updatePreview() {
        if (typeBox.getValue() != null && periodBox.getValue() != null && targetSpinner.getValue() != null) {
            preview.setText(Goal.defaultTitle(typeBox.getValue(), periodBox.getValue(), targetSpinner.getValue()));
        }
    }
}
