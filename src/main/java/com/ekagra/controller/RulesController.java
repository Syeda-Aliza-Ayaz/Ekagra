package com.ekagra.controller;

import com.ekagra.model.AppRule;
import com.ekagra.model.Category;
import com.ekagra.service.RuleService;
import com.ekagra.service.SessionContext;
import com.ekagra.service.ValidationException;
import com.ekagra.ui.Messages;
import com.ekagra.ui.Ui;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;

/** App Rules screen for mentors (FR-40). */
public class RulesController {

    @FXML private Label ruleCount;
    @FXML private TextField search;
    @FXML private ComboBox<Category> categoryFilter;
    @FXML private VBox ruleList;
    @FXML private TextField newProcess;
    @FXML private TextField newKeyword;
    @FXML private ComboBox<Category> newCategory;
    @FXML private Label addMessage;
    @FXML private TextField testProcess;
    @FXML private TextField testTitle;
    @FXML private Label testResult;

    private RuleService service;

    @FXML
    private void initialize() {
        service = new RuleService(SessionContext.getInstance().getCurrentUser());
        categoryFilter.setItems(FXCollections.observableArrayList(null, Category.PRODUCTIVE, Category.NEUTRAL, Category.DISTRACTING));
        Ui.nullableCombo(categoryFilter, "All categories", Category::getLabel);
        categoryFilter.getSelectionModel().selectFirst();
        newCategory.getItems().setAll(Category.values());
        newCategory.setCellFactory(lv -> categoryCell());
        newCategory.setButtonCell(categoryCell());
        newCategory.setValue(Category.DISTRACTING);

        search.textProperty().addListener((o, a, b) -> render());
        categoryFilter.valueProperty().addListener((o, a, b) -> render());
        render();
    }

    static ListCell<Category> categoryCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Category c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getLabel());
                setGraphic(empty || c == null ? null : Ui.categoryDot(c));
            }
        };
    }

    private void render() {
        List<AppRule> all = service.all();
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        Category cat = categoryFilter.getValue();
        List<AppRule> shown = all.stream()
            .filter(r -> cat == null || r.getCategory() == cat)
            .filter(r -> q.isEmpty() || (r.getProcessName() != null && r.getProcessName().contains(q))
                || (r.getTitleKeyword() != null && r.getTitleKeyword().toLowerCase(Locale.ROOT).contains(q)))
            .toList();
        ruleCount.setText(shown.size() + " of " + all.size() + " rules");
        ruleList.getChildren().clear();
        for (AppRule r : shown) {
            Label app = Ui.label(r.getProcessName() == null ? "Any app" : r.getProcessName(),
                r.getProcessName() == null ? "muted" : "app-name");
            app.setPrefWidth(170);
            app.setMinWidth(170);
            Label kw = Ui.label(r.getTitleKeyword() == null ? "Any title" : "\"" + r.getTitleKeyword() + "\"",
                r.getTitleKeyword() == null ? "muted" : "score-part-name");
            kw.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(kw, Priority.ALWAYS);
            ComboBox<Category> c = new ComboBox<>(FXCollections.observableArrayList(Category.values()));
            c.setCellFactory(lv -> categoryCell());
            c.setButtonCell(categoryCell());
            c.setValue(r.getCategory());
            c.setPrefWidth(170);
            c.valueProperty().addListener((o, a, b) -> {
                if (b != null && b != a) {
                    service.changeCategory(r, b);
                }
            });
            HBox row = new HBox(10, app, kw, c, Ui.iconButton("fth-trash-2", "Delete rule", () -> {
                if (Ui.confirm("Delete rule?", r.describe() + " will go back to Neutral (unless another rule matches).", "Delete")) {
                    service.delete(r);
                    render();
                }
            }));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("rule-row");
            ruleList.getChildren().add(row);
        }
    }

    @FXML
    private void onAdd() {
        try {
            service.add(newProcess.getText(), newKeyword.getText(), newCategory.getValue());
            newProcess.clear();
            newKeyword.clear();
            Messages.success(addMessage, "Rule added. Students' tracking picks it up at their next login.");
            render();
        } catch (ValidationException e) {
            Messages.error(addMessage, e.getMessage());
        }
    }

    @FXML
    private void onTest() {
        Category c = service.test(testProcess.getText(), testTitle.getText());
        testResult.setText(c.getLabel());
        testResult.setGraphic(Ui.categoryDot(c));
        testResult.getStyleClass().setAll("label", "category-pill");
    }
}
