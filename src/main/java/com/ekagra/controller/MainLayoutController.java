package com.ekagra.controller;

import com.ekagra.app.Navigator;
import com.ekagra.app.Screen;
import com.ekagra.app.ThemeManager;
import com.ekagra.model.FocusSession;
import com.ekagra.model.SessionType;
import com.ekagra.model.User;
import com.ekagra.pattern.observer.TimerListener;
import com.ekagra.pattern.state.PomodoroTimer;
import com.ekagra.service.FocusService;
import com.ekagra.service.TrackingService;
import com.ekagra.pattern.observer.ActivityListener;
import com.ekagra.service.SessionContext;
import com.ekagra.ui.Avatar;
import com.ekagra.ui.LogoMark;
import com.ekagra.ui.RatingDialog;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

/** The app frame: sidebar navigation, top bar and the content area that screens are placed into. */
public class MainLayoutController implements TimerListener, Disposable {

    @FXML private HBox brandBox;
    @FXML private VBox navBox;
    @FXML private StackPane avatarHolder;
    @FXML private Label userNameLabel;
    @FXML private Label userSubtitleLabel;
    @FXML private Label pageTitle;
    @FXML private Label dateLabel;
    @FXML private Button themeButton;
    @FXML private Button timerChip;
    @FXML private Button trackingChip;
    @FXML private Label bellBadge;
    private final Runnable bellUpdater = () -> javafx.application.Platform.runLater(this::updateBell);

    /** Observer for the top-bar tracking indicator. */
    private final ActivityListener trackingIndicator = new ActivityListener() {
        @Override
        public void onIdleChanged(boolean idle) {
            updateTrackingChip();
        }

        @Override
        public void onTrackingChanged(boolean active) {
            updateTrackingChip();
        }
    };
    @FXML private StackPane content;
    @FXML private StackPane tourContainer;

    private final Map<Screen, Button> navButtons = new EnumMap<>(Screen.class);
    private final Avatar avatar = new Avatar(36);

    @FXML
    private void initialize() {
        brandBox.getChildren().add(0, new LogoMark(30));
        avatarHolder.getChildren().add(avatar);
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")));
        buildNavigation(SessionContext.getInstance().getCurrentUser());
        refreshUserCard();
        updateThemeIcon();
        com.ekagra.service.NotificationService.addChangeListener(bellUpdater);
        updateBell();
        if (SessionContext.getInstance().isStudent()) {
            FocusService.get().getTimer().addListener(this);
            TrackingService.get().addListener(trackingIndicator);
            updateTrackingChip();
        }
    }

    private void updateBell() {
        var user = SessionContext.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        int unread = com.ekagra.service.NotificationService.unreadCount(user.getId());
        boolean changed = !bellBadge.getText().equals(String.valueOf(unread));
        bellBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
        bellBadge.setVisible(unread > 0);
        if (changed && unread > 0) {
            com.ekagra.ui.Animations.pulse(bellBadge);
        }
    }

    @Override
    public void dispose() {
        com.ekagra.service.NotificationService.removeChangeListener(bellUpdater);
        if (FocusService.isStarted()) {
            FocusService.get().getTimer().removeListener(this);
        }
        if (TrackingService.isStarted()) {
            TrackingService.get().removeListener(trackingIndicator);
        }
        com.ekagra.ui.tour.DemoTourManager.get().stop();
    }

    private void updateTrackingChip() {
        TrackingService t = TrackingService.get();
        trackingChip.setVisible(true);
        trackingChip.setManaged(true);
        trackingChip.getStyleClass().removeAll("live", "paused", "idle");
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(4);
        dot.getStyleClass().add("tracking-dot");
        trackingChip.setGraphic(dot);
        if (!t.isAvailable()) {
            trackingChip.setText("Tracking unavailable");
            trackingChip.getStyleClass().add("paused");
        } else if (t.isPaused()) {
            trackingChip.setText("Tracking paused");
            trackingChip.getStyleClass().add("paused");
        } else if (t.isIdle()) {
            trackingChip.setText("Away");
            trackingChip.getStyleClass().add("idle");
        } else {
            trackingChip.setText("Tracking");
            trackingChip.getStyleClass().add("live");
        }
    }

    @FXML
    private void onOpenActivity() {
        Navigator.goTo(Screen.ACTIVITY);
    }

    // ---------- mini timer in the top bar (Observer) ----------

    @Override
    public void onTick(PomodoroTimer timer) {
        updateTimerChip(timer);
    }

    @Override
    public void onStateChanged(PomodoroTimer timer) {
        updateTimerChip(timer);
    }

    @Override
    public void onSessionFinished(PomodoroTimer timer, FocusSession session) {
        if (!session.isCompleted()) {
            return;
        }
        java.awt.Toolkit.getDefaultToolkit().beep();
        if (session.getType() == SessionType.FOCUS) {
            if (session.getTaskId() != null && timer.getTask() != null) {
                session.setTaskTitle(timer.getTask().getTitle());
            }
            // dialogs can't open during an animation tick, so defer to the next UI pulse
            Platform.runLater(() -> new RatingDialog(session).showAndWait().ifPresent(rating -> {
                FocusService.get().rateSession(session, rating);
                Navigator.refreshIfShowing(Screen.FOCUS);
            }));
        }
    }

    private void updateTimerChip(PomodoroTimer timer) {
        boolean show = timer.isActive();
        timerChip.setVisible(show);
        timerChip.setManaged(show);
        if (show) {
            String label = timer.getState().isPaused() ? "Paused" : timer.getCurrentType().getLabel();
            timerChip.setText(label + "  " + timer.getFormattedRemaining());
            timerChip.getStyleClass().remove("break");
            if (timer.getCurrentType().isBreak()) {
                timerChip.getStyleClass().add("break");
            }
        }
    }

    @FXML
    private void onOpenFocus() {
        Navigator.goTo(Screen.FOCUS);
    }

    private void buildNavigation(User user) {
        String lastGroup = null;
        for (Screen screen : Screen.navFor(user.getRole())) {
            if (!screen.getGroup().equals(lastGroup)) {
                Label groupLabel = new Label(screen.getGroup().toUpperCase());
                groupLabel.getStyleClass().add("nav-group-label");
                navBox.getChildren().add(groupLabel);
                lastGroup = screen.getGroup();
            }
            Button button = new Button(screen.getTitle(), new FontIcon(screen.getIcon()));
            button.getStyleClass().add("nav-button");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(e -> Navigator.goTo(screen));
            navButtons.put(screen, button);
            navBox.getChildren().add(button);
        }
    }

    public void show(Screen screen, Parent view) {
        pageTitle.setText(screen.getTitle());
        content.getChildren().setAll(view);
        navButtons.forEach((s, b) -> b.getStyleClass().remove("active"));
        Button active = navButtons.get(screen);
        if (active != null) {
            active.getStyleClass().add("active");
        }
    }

    public void refreshUserCard() {
        User user = SessionContext.getInstance().getCurrentUser();
        avatar.setUser(user);
        userNameLabel.setText(user.getFullName());
        userSubtitleLabel.setText(user.getSubtitle());
    }

    private void updateThemeIcon() {
        themeButton.setGraphic(new FontIcon(ThemeManager.isDark() ? "fth-sun" : "fth-moon"));
    }

    @FXML
    private void onToggleTheme() {
        Navigator.toggleTheme();
        updateThemeIcon();
    }

    @FXML
    private void onOpenProfile() {
        Navigator.goTo(Screen.PROFILE);
    }

    @FXML
    private void onOpenNotifications() {
        Navigator.goTo(Screen.NOTIFICATIONS);
    }

    @FXML
    private void onLogout() {
        Navigator.logout();
    }

    public StackPane getTourContainer() {
        return tourContainer;
    }

    @FXML
    private void onOpenRahnuma() {
        com.ekagra.ui.RahnumaDialog.show();
    }

    @FXML
    private void onStartTour() {
        com.ekagra.ui.tour.DemoTourManager.get().start(this);
    }
}


