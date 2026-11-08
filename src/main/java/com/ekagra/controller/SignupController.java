package com.ekagra.controller;

import com.ekagra.app.Navigator;
import com.ekagra.model.Role;
import com.ekagra.service.AuthService;
import com.ekagra.service.SessionContext;
import com.ekagra.service.ValidationException;
import com.ekagra.ui.Messages;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

public class SignupController {

    @FXML private ToggleButton studentToggle;
    @FXML private ToggleButton mentorToggle;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private VBox studentFields;
    @FXML private TextField universityField;
    @FXML private Spinner<Integer> semesterSpinner;
    @FXML private TextField mentorCodeField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        ToggleGroup group = new ToggleGroup();
        studentToggle.setToggleGroup(group);
        mentorToggle.setToggleGroup(group);
        // never allow "nothing selected"
        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) {
                group.selectToggle(old);
            }
        });
        studentFields.visibleProperty().bind(studentToggle.selectedProperty());
        studentFields.managedProperty().bind(studentToggle.selectedProperty());

        semesterSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 1));
        com.ekagra.ui.Ui.spinner(semesterSpinner);
    }

    @FXML
    private void onSignup() {
        Role role = mentorToggle.isSelected() ? Role.MENTOR : Role.STUDENT;
        var request = new AuthService.SignupRequest(
            role,
            nameField.getText(),
            emailField.getText(),
            passwordField.getText(),
            confirmField.getText(),
            universityField.getText(),
            semesterSpinner.getValue(),
            mentorCodeField.getText());
        try {
            authService.register(request);
            authService.login(emailField.getText(), passwordField.getText());
            if (SessionContext.getInstance().isLoggedIn()) {
                Navigator.showMain();
            }
        } catch (ValidationException e) {
            Messages.error(errorLabel, e.getMessage());
        }
    }

    @FXML
    private void onGoToLogin() {
        Navigator.showLogin();
    }
}
