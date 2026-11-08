package com.ekagra.controller;

import com.ekagra.app.Navigator;
import com.ekagra.db.DemoDataSeeder;
import com.ekagra.service.AuthService;
import com.ekagra.service.ValidationException;
import com.ekagra.ui.Messages;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private javafx.scene.control.Button demoStudentButton;
    @FXML private javafx.scene.control.Button demoMentorButton;
    @FXML private Label demoStatus;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        Platform.runLater(emailField::requestFocus);
    }

    @FXML
    private void onLogin() {
        try {
            authService.login(emailField.getText(), passwordField.getText());
            Navigator.showMain();
        } catch (ValidationException e) {
            Messages.error(errorLabel, e.getMessage());
            passwordField.clear();
        }
    }

    @FXML
    private void onDemoStudent() {
        startDemo(DemoDataSeeder.STUDENT_EMAIL);
    }

    @FXML
    private void onDemoMentor() {
        startDemo(DemoDataSeeder.MENTOR_EMAIL);
    }

    /** Builds fresh demo data in the background (FR-20), then logs in. */
    private void startDemo(String email) {
        demoStudentButton.setDisable(true);
        demoMentorButton.setDisable(true);
        demoStatus.setText("Preparing two weeks of demo data…");
        com.ekagra.ui.Ui.background(() -> new DemoDataSeeder().seed(), student -> {
            demoStatus.setText("Demo accounts use the password " + DemoDataSeeder.PASSWORD + ".");
            authService.login(email, DemoDataSeeder.PASSWORD);
            Navigator.showMain();
        }, err -> {
            demoStudentButton.setDisable(false);
            demoMentorButton.setDisable(false);
            demoStatus.setText("Couldn't create demo data: " + err.getMessage());
        });
    }

    @FXML
    private void onGoToSignup() {
        Navigator.showSignup();
    }
}
