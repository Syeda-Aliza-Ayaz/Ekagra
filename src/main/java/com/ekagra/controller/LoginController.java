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
    @FXML private javafx.scene.control.Button demoTourButton;
    @FXML private Label demoStatus;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        Platform.runLater(emailField::requestFocus);
        if (demoTourButton != null) {
            demoTourButton.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    onStartInteractiveTour();
                }
            });
        }
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
        demoStatus.setText("Preparing two weeks of demo dataâ€¦");
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
    private void onStartInteractiveTour() {
        emailField.setText(DemoDataSeeder.STUDENT_EMAIL);
        passwordField.setText(DemoDataSeeder.PASSWORD);
        if (demoTourButton != null) demoTourButton.setDisable(true);
        demoStudentButton.setDisable(true);
        demoMentorButton.setDisable(true);
        demoStatus.setText("Preparing live demo data for tour…");

        com.ekagra.ui.Ui.background(() -> new DemoDataSeeder().seed(), student -> {
            authService.login(DemoDataSeeder.STUDENT_EMAIL, DemoDataSeeder.PASSWORD);
            Navigator.showMain();
            Platform.runLater(() -> {
                var layout = Navigator.getMainLayoutController();
                if (layout != null) {
                    com.ekagra.ui.tour.DemoTourManager.get().start(layout);
                }
            });
        }, err -> {
            if (demoTourButton != null) demoTourButton.setDisable(false);
            demoStudentButton.setDisable(false);
            demoMentorButton.setDisable(false);
            demoStatus.setText("Couldn't start demo tour: " + err.getMessage());
        });
    }

    @FXML
    private void onGoToSignup() {
        Navigator.showSignup();
    }
}

