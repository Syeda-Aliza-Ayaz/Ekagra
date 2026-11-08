package com.ekagra.controller;

import com.ekagra.app.Navigator;
import com.ekagra.model.Mentor;
import com.ekagra.model.Student;
import com.ekagra.model.User;
import com.ekagra.service.AuthService;
import com.ekagra.service.ProfileService;
import com.ekagra.service.SessionContext;
import com.ekagra.service.ValidationException;
import com.ekagra.ui.Avatar;
import com.ekagra.ui.Messages;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Profile, mentor link and password change (FR-02, FR-03, FR-04). */
public class ProfileController {

    @FXML private StackPane avatarHolder;
    @FXML private Label headerName;
    @FXML private Label headerEmail;
    @FXML private Label headerRole;
    @FXML private TextField nameField;
    @FXML private VBox studentFields;
    @FXML private TextField universityField;
    @FXML private Spinner<Integer> semesterSpinner;
    @FXML private Spinner<Integer> targetSpinner;
    @FXML private HBox swatchBox;
    @FXML private Label profileMessage;
    @FXML private VBox mentorCard;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordMessage;

    private final ProfileService profileService = new ProfileService();
    private final AuthService authService = new AuthService();
    private final Avatar avatar = new Avatar(64);
    private User user;
    private String selectedColor;

    @FXML
    private void initialize() {
        user = SessionContext.getInstance().getCurrentUser();
        selectedColor = user.getAvatarColor();
        avatarHolder.getChildren().add(avatar);

        nameField.setText(user.getFullName());
        semesterSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 1));
        targetSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 960, 240, 15));
        com.ekagra.ui.Ui.spinner(semesterSpinner);
        com.ekagra.ui.Ui.spinner(targetSpinner);

        if (user instanceof Student s) {
            universityField.setText(s.getUniversity());
            semesterSpinner.getValueFactory().setValue(Math.max(1, s.getSemester()));
            targetSpinner.getValueFactory().setValue(s.getDailyTargetMin());
        } else {
            studentFields.setVisible(false);
            studentFields.setManaged(false);
        }

        buildSwatches();
        refreshHeader();
        buildMentorCard();
    }

    private void refreshHeader() {
        avatar.set(user.getInitials(), selectedColor);
        headerName.setText(user.getFullName());
        headerEmail.setText(user.getEmail());
        headerRole.setText(user.getSubtitle());
    }

    private void buildSwatches() {
        swatchBox.getChildren().clear();
        for (String color : AuthService.AVATAR_COLORS) {
            Region swatch = new Region();
            swatch.getStyleClass().add("swatch");
            swatch.setStyle("-fx-background-color: " + color + ";");
            if (color.equalsIgnoreCase(selectedColor)) {
                swatch.getStyleClass().add("selected");
            }
            swatch.setOnMouseClicked(e -> {
                selectedColor = color;
                buildSwatches();
                avatar.set(user.getInitials(), selectedColor);
            });
            swatchBox.getChildren().add(swatch);
        }
    }

    private void buildMentorCard() {
        mentorCard.getChildren().clear();
        if (user instanceof Mentor m) {
            Label title = new Label("Your mentor code");
            title.getStyleClass().add("h3");
            Label hint = new Label("Share this code with your students. They enter it when signing up or on their profile.");
            hint.getStyleClass().add("muted");
            hint.setWrapText(true);
            Label code = new Label(m.getInviteCode());
            code.getStyleClass().add("code-label");
            Button copy = new Button("Copy");
            copy.setOnAction(e -> {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(m.getInviteCode());
                Clipboard.getSystemClipboard().setContent(cc);
                copy.setText("Copied!");
            });
            int count = profileService.getStudentsOf(m).size();
            Label students = new Label(count + (count == 1 ? " student linked" : " students linked"));
            students.getStyleClass().add("muted");
            HBox row = new HBox(12, code, copy, students);
            row.setAlignment(Pos.CENTER_LEFT);
            mentorCard.getChildren().addAll(title, hint, row);
        } else if (user instanceof Student s) {
            Label title = new Label("Mentor");
            title.getStyleClass().add("h3");
            mentorCard.getChildren().add(title);
            Label message = new Label();
            Messages.hide(message);

            if (s.getMentorId() != null) {
                String mentorName = profileService.findUser(s.getMentorId()).map(User::getFullName).orElse("Unknown");
                Label linked = new Label("You are linked to " + mentorName + ". They can see your progress summary.");
                linked.setWrapText(true);
                Button unlink = new Button("Unlink");
                unlink.getStyleClass().add("danger");
                unlink.setOnAction(e -> {
                    profileService.unlinkMentor(s);
                    buildMentorCard();
                });
                mentorCard.getChildren().addAll(linked, new HBox(unlink));
            } else {
                Label hint = new Label("Link to a mentor so they can follow your progress and send encouragement.");
                hint.getStyleClass().add("muted");
                hint.setWrapText(true);
                TextField codeField = new TextField();
                codeField.setPromptText("e.g. EK-7KQ2M");
                codeField.setPrefWidth(200);
                Button link = new Button("Link mentor");
                link.getStyleClass().add("accent");
                link.setOnAction(e -> {
                    try {
                        profileService.linkMentor(s, codeField.getText());
                        buildMentorCard();
                    } catch (ValidationException ex) {
                        Messages.error(message, ex.getMessage());
                    }
                });
                HBox row = new HBox(10, codeField, link);
                mentorCard.getChildren().addAll(hint, row, message);
            }
        }
    }

    @FXML
    private void onSaveProfile() {
        try {
            profileService.updateProfile(user, nameField.getText(), selectedColor,
                universityField.getText(), semesterSpinner.getValue(), targetSpinner.getValue());
            refreshHeader();
            Navigator.refreshUserCard();
            Messages.success(profileMessage, "Profile saved.");
        } catch (ValidationException e) {
            Messages.error(profileMessage, e.getMessage());
        }
    }

    @FXML
    private void onChangePassword() {
        try {
            authService.changePassword(user, currentPasswordField.getText(),
                newPasswordField.getText(), confirmPasswordField.getText());
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            Messages.success(passwordMessage, "Password updated.");
        } catch (ValidationException e) {
            Messages.error(passwordMessage, e.getMessage());
        }
    }
}
