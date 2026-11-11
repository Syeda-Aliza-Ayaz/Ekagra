package com.ekagra.ui;

import com.ekagra.app.Navigator;
import com.ekagra.app.ThemeManager;
import com.ekagra.model.Student;
import com.ekagra.service.RahnumaService;
import com.ekagra.service.SessionContext;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Slide-in / Modal conversational interface for "Rahnuma" (رہنما).
 */
public class RahnumaDialog {

    private final Stage stage;
    private final RahnumaService rahnumaService;
    private final VBox messageList;
    private final ScrollPane scrollPane;
    private final TextField inputField;

    public RahnumaDialog() {
        Student s = SessionContext.getInstance().getStudent();
        this.rahnumaService = new RahnumaService(s);

        stage = new Stage();
        stage.initOwner(Navigator.getStage());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UTILITY);
        stage.setTitle("Rahnuma (رہنما) — Study Guide & Mentor");

        double windowH = Navigator.getStage() != null && Navigator.getStage().getHeight() > 0
            ? Navigator.getStage().getHeight() : 600;
        double targetH = Math.min(500, Math.max(380, windowH * 0.72));

        BorderPane root = new BorderPane();
        root.setPrefSize(540, targetH);
        root.setMaxHeight(targetH);
        root.getStyleClass().add("card");

        // ---------- Header ----------
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 14, 20));

        FontIcon avatarIcon = new FontIcon("fth-compass");
        avatarIcon.setIconSize(24);
        avatarIcon.getStyleClass().add("brand-feature-icon");

        VBox titleBox = new VBox(2);
        Label title = new Label("Rahnuma (رہنما)");
        title.getStyleClass().addAll("h2");
        Label subtitle = new Label("Your data-grounded study guide & focus mentor");
        subtitle.getStyleClass().addAll("muted", "small");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("", new FontIcon("fth-x"));
        closeBtn.getStyleClass().add("icon-button");
        closeBtn.setOnAction(e -> stage.close());

        header.getChildren().addAll(avatarIcon, titleBox, spacer, closeBtn);
        root.setTop(header);

        // ---------- Quick Action Chips ----------
        HBox chipBox = new HBox(8);
        chipBox.setPadding(new Insets(0, 20, 10, 20));
        chipBox.setAlignment(Pos.CENTER_LEFT);

        Button chipScore = new Button("📊 Score Analysis");
        chipScore.getStyleClass().add("rafiq-action-chip");
        chipScore.setOnAction(e -> triggerAction("Analyze Today's Score", rahnumaService::analyzeToday));

        Button chipDistract = new Button("🛡️ Distraction Audit");
        chipDistract.getStyleClass().add("rafiq-action-chip");
        chipDistract.setOnAction(e -> triggerAction("Audit Distractions", rahnumaService::distractionAudit));

        Button chipTasks = new Button("📝 Plan Tasks");
        chipTasks.getStyleClass().add("rafiq-action-chip");
        chipTasks.setOnAction(e -> triggerAction("Plan Upcoming Deadlines", rahnumaService::planUpcomingTasks));

        Button chipWellbeing = new Button("🌿 Wellbeing");
        chipWellbeing.getStyleClass().add("rafiq-action-chip");
        chipWellbeing.setOnAction(e -> triggerAction("Reflect on Energy & Wellbeing", rahnumaService::reflectWellbeing));

        chipBox.getChildren().addAll(chipScore, chipDistract, chipTasks, chipWellbeing);

        // ---------- Message Area ----------
        messageList = new VBox(12);
        messageList.setPadding(new Insets(14, 20, 14, 20));

        scrollPane = new ScrollPane(messageList);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("sidebar-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox centerBox = new VBox(chipBox, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        VBox.setVgrow(centerBox, Priority.ALWAYS);
        root.setCenter(centerBox);

        // Initial Greeting
        addBotMessage(new RahnumaService.RahnumaResponse(rahnumaService.getGreeting()));

        // ---------- Bottom Input Bar ----------
        HBox inputBar = new HBox(10);
        inputBar.setPadding(new Insets(12, 20, 16, 20));
        inputBar.setAlignment(Pos.CENTER_LEFT);

        inputField = new TextField();
        inputField.setPromptText("Ask Rahnuma about your study habits, score or tasks...");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.setOnAction(e -> onSend());

        Button sendBtn = new Button("Send", new FontIcon("fth-send"));
        sendBtn.getStyleClass().addAll("accent", "auth-button");
        sendBtn.setOnAction(e -> onSend());

        inputBar.getChildren().addAll(inputField, sendBtn);
        root.setBottom(inputBar);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(Navigator.stylesheet());
        ThemeManager.decorate(root);

        stage.setScene(scene);
        Animations.installPressFeedback(scene);
    }

    public static void show() {
        RahnumaDialog dialog = new RahnumaDialog();
        dialog.stage.centerOnScreen();
        dialog.stage.show();
    }

    private void onSend() {
        String text = inputField.getText();
        if (text == null || text.isBlank()) return;
        inputField.clear();

        addUserMessage(text);
        Platform.runLater(() -> {
            RahnumaService.RahnumaResponse resp = rahnumaService.answerQuery(text);
            addBotMessage(resp);
        });
    }

    private void triggerAction(String userLabel, java.util.function.Supplier<RahnumaService.RahnumaResponse> action) {
        addUserMessage(userLabel);
        Platform.runLater(() -> {
            RahnumaService.RahnumaResponse resp = action.get();
            addBotMessage(resp);
        });
    }

    private void addUserMessage(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.getStyleClass().add("rafiq-bubble-user");
        HBox row = new HBox(l);
        row.setAlignment(Pos.CENTER_RIGHT);
        messageList.getChildren().add(row);
        scrollToBottom();
    }

    private void addBotMessage(RahnumaService.RahnumaResponse resp) {
        VBox box = new VBox(8);
        box.getStyleClass().add("rafiq-bubble-bot");

        Label text = new Label(resp.text());
        text.setWrapText(true);
        text.getStyleClass().add("score-part-name");
        box.getChildren().add(text);

        if (resp.actionLabel() != null && resp.targetScreen() != null) {
            Button actionBtn = new Button(resp.actionLabel(), new FontIcon("fth-arrow-right"));
            actionBtn.getStyleClass().addAll("accent", "small");
            actionBtn.setOnAction(e -> {
                stage.close();
                Navigator.goTo(resp.targetScreen());
            });
            box.getChildren().add(actionBtn);
        }

        HBox row = new HBox(box);
        row.setAlignment(Pos.CENTER_LEFT);
        messageList.getChildren().add(row);
        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }
}
