package com.ekagra.ui.tour;

import com.ekagra.app.Navigator;
import com.ekagra.app.Screen;
import com.ekagra.controller.MainLayoutController;
import com.ekagra.model.Role;
import com.ekagra.service.AmbientSoundService;
import com.ekagra.service.SessionContext;
import com.ekagra.ui.Animations;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

/**
 * State machine managing the interactive keyboard-driven viva demo tour.
 * Advance through the entire app lifecycle with a single press of [Enter].
 */
public final class DemoTourManager {

    private static final DemoTourManager INSTANCE = new DemoTourManager();

    private List<TourStep> steps = List.of();
    private int currentIndex = 0;
    private boolean active = false;
    private javafx.event.EventHandler<KeyEvent> keyFilter;
    private MainLayoutController layoutController;

    private DemoTourManager() {
    }

    public static DemoTourManager get() {
        return INSTANCE;
    }

    static List<TourStep> stepsFor(Role role) {
        if (role == Role.MENTOR) {
            return List.of(
                new TourStep(
                    Screen.MENTOR,
                    "1. Student Overview",
                    "Review linked students' recent scores, focus time, overdue tasks, and follow-up alerts."
                ),
                new TourStep(
                    Screen.RULES,
                    "2. App Rules",
                    "Review the app and website categories used by Focus Guard."
                ),
                new TourStep(
                    Screen.NOTIFICATIONS,
                    "3. Notifications",
                    "Check student messages, reminders, and other account updates."
                ),
                new TourStep(
                    Screen.SETTINGS,
                    "4. Settings & Rahnuma",
                    "Personalize the workspace and ask Rahnuma for a linked-student progress summary."
                )
            );
        }
        return List.of(
            new TourStep(
                Screen.DASHBOARD,
                "1. Dashboard & Productivity Score",
                "Real-time daily focus score (0-100) calculated from deep study sessions, task output, and distraction resistance."
            ),

            new TourStep(
                Screen.FOCUS,
                "2. Pomodoro Focus Timer & Ambient Sound",
                "Configurable focus sessions with built-in ambient audio (Rain, Stream, Alpha waves) to sustain flow state.",
                () -> AmbientSoundService.get().play(AmbientSoundService.SoundType.RAIN)
            ),

            new TourStep(
                Screen.TASKS,
                "3. Coursework Tasks & Habit Tracking",
                "Organize assignments by subject and deadline, track Pomodoro velocity, and build consistent daily habits."
            ),

            new TourStep(
                Screen.WELLBEING,
                "4. Digital Wellbeing & 20-20-20 Eye Breaks",
                "Prevent academic burnout with daily screen-time limits, eye strain reminders, and mood/sleep logs."
            ),

            new TourStep(
                Screen.RULES,
                "5. Focus Guard & Distraction Boundaries",
                "Monitors active window titles and soft-nudges students away from blacklisted distracting apps during study."
            ),

            new TourStep(
                Screen.ANALYTICS,
                "6. Deep Analytics & Viva Reports",
                "Visualizes 7-day focus intensity, hourly activity distributions, category shares, and PDF export for supervisors."
            ),

            new TourStep(
                Screen.SETTINGS,
                "7. Themes, Personalization & Rahnuma (رہنما)",
                "Personalize workspace with 6 curated aesthetic palettes, custom accent color picker, and the Rahnuma study mentor."
            )
        );
    }

    public void start(MainLayoutController controller) {
        this.layoutController = controller;
        var user = SessionContext.getInstance().getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("The guided tour requires a signed-in user");
        }
        this.steps = stepsFor(user.getRole());
        this.currentIndex = 0;
        this.active = true;

        attachKeyListener();
        showCurrentStep();
    }

    public void stop() {
        if (!active) return;
        this.active = false;
        detachKeyListener();

        if (layoutController != null) {
            StackPane container = layoutController.getTourContainer();
            if (container != null) {
                container.getChildren().clear();
                container.setVisible(false);
                container.setManaged(false);
            }
        }
        AmbientSoundService.get().pause();
    }

    public void next() {
        if (!active) return;
        if (currentIndex < steps.size() - 1) {
            currentIndex++;
            showCurrentStep();
        } else {
            stop();
        }
    }

    public void previous() {
        if (!active) return;
        if (currentIndex > 0) {
            currentIndex--;
            showCurrentStep();
        }
    }

    private void showCurrentStep() {
        TourStep step = steps.get(currentIndex);
        Navigator.goTo(step.screen());

        if (step.action() != null) {
            try {
                step.action().run();
            } catch (Exception ignored) {
            }
        }

        renderTourBar(step);
    }

    private void renderTourBar(TourStep step) {
        if (layoutController == null) return;
        StackPane container = layoutController.getTourContainer();
        if (container == null) return;

        container.getChildren().clear();
        container.setVisible(true);
        container.setManaged(true);
        container.setPadding(new Insets(10, 24, 16, 24));

        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("tour-bar");

        Label badge = new Label("Step " + (currentIndex + 1) + " of " + steps.size());
        badge.getStyleClass().add("tour-key-badge");

        VBox textBox = new VBox(2);
        Label title = new Label(step.title());
        title.getStyleClass().addAll("score-part-name");
        Label desc = new Label(step.description());
        desc.getStyleClass().addAll("muted", "small");
        textBox.getChildren().addAll(title, desc);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        // Keyboard navigation hints
        Label enterHint = new Label("[Enter ↵] " + (currentIndex == steps.size() - 1 ? "Finish" : "Next"));
        enterHint.getStyleClass().add("tour-key-badge");

        Label escHint = new Label("[Esc] Exit");
        escHint.getStyleClass().addAll("muted", "small");

        Button prevBtn = new Button("", new FontIcon("fth-chevron-left"));
        prevBtn.getStyleClass().add("icon-button");
        prevBtn.setDisable(currentIndex == 0);
        prevBtn.setOnAction(e -> previous());

        Button nextBtn = new Button("", new FontIcon(currentIndex == steps.size() - 1 ? "fth-check" : "fth-chevron-right"));
        nextBtn.getStyleClass().addAll("accent", "small");
        nextBtn.setOnAction(e -> next());

        Button exitBtn = new Button("", new FontIcon("fth-x"));
        exitBtn.getStyleClass().add("icon-button");
        exitBtn.setOnAction(e -> stop());

        bar.getChildren().addAll(badge, textBox, enterHint, escHint, prevBtn, nextBtn, exitBtn);
        container.getChildren().add(bar);
        Animations.appear(bar, 60);
    }

    private void attachKeyListener() {
        Scene scene = Navigator.getStage().getScene();
        if (scene == null) return;

        detachKeyListener();
        keyFilter = event -> {
            if (!active) return;
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                next();
                event.consume();
            } else if (event.getCode() == KeyCode.BACK_SPACE || event.getCode() == KeyCode.LEFT) {
                previous();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                stop();
                event.consume();
            }
        };
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
    }

    private void detachKeyListener() {
        Scene scene = Navigator.getStage().getScene();
        if (scene != null && keyFilter != null) {
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
            keyFilter = null;
        }
    }

    public boolean isActive() {
        return active;
    }
}
