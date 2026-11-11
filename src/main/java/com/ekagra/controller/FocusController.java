package com.ekagra.controller;

import com.ekagra.model.FocusSession;
import com.ekagra.service.AmbientSoundService;
import javafx.scene.control.Slider;
import com.ekagra.model.SessionType;
import com.ekagra.model.Task;
import com.ekagra.pattern.observer.TimerListener;
import com.ekagra.pattern.state.PomodoroTimer;
import com.ekagra.pattern.state.TimerConfig;
import com.ekagra.service.FocusService;
import com.ekagra.service.SessionContext;
import com.ekagra.service.TaskService;
import com.ekagra.ui.TimerRing;
import com.ekagra.ui.Ui;
import com.ekagra.util.Formats;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Focus Timer screen (FR-10 .. FR-14). Observes the shared timer while visible. */
public class FocusController implements TimerListener, Disposable {

    @FXML private ToggleButton focusMode;
    @FXML private ToggleButton shortMode;
    @FXML private ToggleButton longMode;
    @FXML private StackPane ringHolder;
    @FXML private HBox cycleDots;
    @FXML private Button mainButton;
    @FXML private Button stopButton;
    @FXML private Button skipButton;
    @FXML private ComboBox<Task> taskCombo;
    @FXML private Label distractionLabel;
    @FXML private HBox todayStats;
    @FXML private Spinner<Integer> focusSpinner;
    @FXML private Spinner<Integer> shortSpinner;
    @FXML private Spinner<Integer> longSpinner;
    @FXML private Spinner<Integer> cycleSpinner;
    @FXML private CheckBox autoBreakCheck;
    @FXML private Label configHint;
    @FXML private ComboBox<AmbientSoundService.SoundType> soundCombo;
    @FXML private Button soundPlayBtn;
    @FXML private FontIcon soundPlayIcon;
    @FXML private Button soundMuteBtn;
    @FXML private FontIcon soundMuteIcon;
    @FXML private Slider soundVolumeSlider;

    private final TimerRing ring = new TimerRing(220);
    private final TaskService taskService = new TaskService();
    private FocusService focusService;
    private PomodoroTimer timer;
    private boolean updatingTaskCombo;

    @FXML
    private void initialize() {
        focusService = FocusService.get();
        timer = focusService.getTimer();
        try {
            var zenUrl = getClass().getResource("/com/ekagra/images/focus_forest.jpg");
            if (zenUrl != null) {
                javafx.scene.image.ImageView zen = new javafx.scene.image.ImageView(new javafx.scene.image.Image(zenUrl.toExternalForm()));
                zen.setFitWidth(210);
                zen.setFitHeight(210);
                zen.setOpacity(0.08);
                zen.setMouseTransparent(true);
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(105, 105, 105);
                zen.setClip(clip);
                ringHolder.getChildren().add(zen);
            }
        } catch (Exception ignored) {
        }
        ringHolder.getChildren().add(ring);

        ToggleGroup modes = new ToggleGroup();
        for (ToggleButton b : List.of(focusMode, shortMode, longMode)) {
            b.setToggleGroup(modes);
        }
        focusMode.setOnAction(e -> timer.selectType(SessionType.FOCUS));
        shortMode.setOnAction(e -> timer.selectType(SessionType.SHORT_BREAK));
        longMode.setOnAction(e -> timer.selectType(SessionType.LONG_BREAK));

        stopButton.setTooltip(new Tooltip("Stop and save"));
        skipButton.setTooltip(new Tooltip("Skip"));

        setupTaskCombo();
        setupConfig();
        setupSoundControls();

        timer.addListener(this);
        refreshTimer();
        refreshStats();
    }

    @Override
    public void dispose() {
        timer.removeListener(this);
        AmbientSoundService.get().pause();
    }

    /** Called from the Tasks screen's "start focus" button. */
    public void focusOn(Task task) {
        if (timer.isActive()) {
            return;   // don't hijack a running session
        }
        timer.selectType(SessionType.FOCUS);
        timer.setTask(task);
        selectTaskInCombo(task);
        timer.start();
    }

    // ---------- Observer callbacks ----------

    @Override
    public void onTick(PomodoroTimer t) {
        refreshRing();
    }

    @Override
    public void onStateChanged(PomodoroTimer t) {
        refreshTimer();
        if (t.getState().isRunning() && t.getCurrentType() == SessionType.FOCUS) {
            AmbientSoundService.get().play();
            updateSoundUi();
        } else if (t.getState().isPaused() || t.getCurrentType().isBreak()) {
            AmbientSoundService.get().pause();
            updateSoundUi();
        }
    }

    @Override
    public void onSessionFinished(PomodoroTimer t, FocusSession session) {
        refreshStats();
        if (session.getType() == SessionType.FOCUS) {
            setupTaskCombo();     // spent time on the task changed
        }
    }

    // ---------- rendering ----------

    private void refreshRing() {
        boolean isBreak = timer.getCurrentType().isBreak();
        String caption = timer.getState().isPaused() ? "Paused"
            : timer.isActive() ? timer.getState().getName() : timer.getCurrentType().getLabel();
        ring.update(timer.getFormattedRemaining(), caption, timer.getProgress(), isBreak);
    }

    private void refreshTimer() {
        refreshRing();

        var state = timer.getState();
        boolean active = timer.isActive();
        if (state.isRunning()) {
            mainButton.setText("Pause");
            mainButton.setGraphic(new FontIcon("fth-pause"));
        } else if (state.isPaused()) {
            mainButton.setText("Resume");
            mainButton.setGraphic(new FontIcon("fth-play"));
        } else {
            mainButton.setText(timer.getCurrentType().isBreak() ? "Start break" : "Start focus");
            mainButton.setGraphic(new FontIcon("fth-play"));
        }
        stopButton.setDisable(!active);
        skipButton.setDisable(!active);

        focusMode.setSelected(timer.getCurrentType() == SessionType.FOCUS);
        shortMode.setSelected(timer.getCurrentType() == SessionType.SHORT_BREAK);
        longMode.setSelected(timer.getCurrentType() == SessionType.LONG_BREAK);
        focusMode.setDisable(active);
        shortMode.setDisable(active);
        longMode.setDisable(active);

        // task can be chosen before starting; it's locked while a session runs
        taskCombo.setDisable(active);

        renderCycleDots();

        FocusSession current = timer.getCurrentSession();
        int distractions = current == null ? 0 : current.getDistractions();
        boolean show = current != null && current.getType() == SessionType.FOCUS;
        distractionLabel.setText(distractions == 0 ? "No distractions so far, keep going!"
            : distractions + (distractions == 1 ? " distraction" : " distractions") + " this session");
        distractionLabel.getStyleClass().remove("has-distractions");
        if (distractions > 0) {
            distractionLabel.getStyleClass().add("has-distractions");
        }
        distractionLabel.setVisible(show);   // stays managed so the layout doesn't jump
    }

    private void renderCycleDots() {
        cycleDots.getChildren().clear();
        int total = timer.getConfig().sessionsBeforeLongBreak();
        int done = timer.getCyclePosition();
        if (done == 0 && timer.getCompletedFocusCount() > 0 && timer.getCurrentType() == SessionType.LONG_BREAK) {
            done = total;   // a full cycle just finished
        }
        for (int i = 0; i < total; i++) {
            Region dot = new Region();
            dot.getStyleClass().add("cycle-dot");
            if (i < done) {
                dot.getStyleClass().add("filled");
            }
            cycleDots.getChildren().add(dot);
        }
        Label text = Ui.label(timer.getCompletedFocusCount() + " done this sitting", "muted", "small");
        HBox.setMargin(text, new javafx.geometry.Insets(0, 0, 0, 8));
        cycleDots.getChildren().add(text);
    }

    private void refreshStats() {
        var stats = focusService.getTodayStats();
        todayStats.getChildren().setAll(
            miniStat(Formats.minutes(stats.focusMinutes()), "Focused"),
            miniStat(String.valueOf(stats.completedSessions()), "Sessions"),
            miniStat(String.valueOf(stats.distractions()), "Distractions"));
        renderHistory();
    }

    private VBox miniStat(String value, String caption) {
        VBox box = new VBox(2, Ui.label(value, "mini-stat-value"), Ui.label(caption, "muted", "small"));
        box.getStyleClass().add("mini-stat");
        HBox.setHgrow(box, javafx.scene.layout.Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    @FXML private VBox historyList;

    private void renderHistory() {
        historyList.getChildren().clear();
        List<FocusSession> recent = focusService.getRecentSessions(12);
        if (recent.isEmpty()) {
            historyList.getChildren().add(Ui.label("No sessions yet. Start your first focus session!", "muted"));
            return;
        }
        LocalDate today = LocalDate.now();
        for (FocusSession s : recent) {
            FontIcon icon = new FontIcon(s.getType() == SessionType.FOCUS ? "fth-target" : "fth-coffee");
            icon.getStyleClass().add(s.getType() == SessionType.FOCUS ? "history-icon-focus" : "history-icon-break");

            String what = s.getType() == SessionType.FOCUS
                ? (s.getTaskTitle() != null ? s.getTaskTitle() : "Focus session")
                : s.getType().getLabel();
            Label title = Ui.label(what, "history-title");
            String day = s.getStartedAt().toLocalDate().equals(today) ? "Today"
                : s.getStartedAt().format(Formats.DAY_SHORT);
            Label when = Ui.label(day + " " + s.getStartedAt().format(Formats.TIME)
                + " Ã‚Â· " + Formats.seconds(s.getActualSec())
                + (s.isCompleted() ? "" : " (stopped)"), "muted", "small");
            VBox text = new VBox(1, title, when);
            HBox.setHgrow(text, javafx.scene.layout.Priority.ALWAYS);

            HBox row = new HBox(10, icon, text);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("history-row");
            if (s.getFocusRating() != null) {
                row.getChildren().add(Ui.label("Ã¢Ëœâ€¦".repeat(s.getFocusRating()), "rating-stars"));
            }
            if (s.getDistractions() > 0) {
                Label d = new Label(String.valueOf(s.getDistractions()), new FontIcon("fth-alert-triangle"));
                d.getStyleClass().add("history-distractions");
                d.setTooltip(new Tooltip(s.getDistractions() + " distractions"));
                row.getChildren().add(d);
            }
            historyList.getChildren().add(row);
        }
    }

    // ---------- task selection ----------

    private void setupTaskCombo() {
        updatingTaskCombo = true;
        int uid = SessionContext.getInstance().getCurrentUser().getId();
        List<Task> items = new ArrayList<>();
        items.add(null);
        items.addAll(taskService.getOpenTasks(uid));
        taskCombo.setItems(FXCollections.observableArrayList(items));
        Ui.nullableCombo(taskCombo, "No specific task", t -> t.getTitle()
            + (t.getSubject() != null ? "  Ã‚Â·  " + t.getSubject() : "")
            + (t.getSpentMin() > 0 ? "  Ã‚Â·  " + Formats.minutes(t.getSpentMin()) + " so far" : ""));
        selectTaskInCombo(timer.getTask());
        updatingTaskCombo = false;

        taskCombo.setOnAction(e -> {
            if (!updatingTaskCombo) {
                timer.setTask(taskCombo.getValue());
            }
        });
    }

    private void selectTaskInCombo(Task task) {
        updatingTaskCombo = true;
        if (task == null) {
            taskCombo.getSelectionModel().selectFirst();
        } else {
            taskCombo.getItems().stream()
                .filter(t -> t != null && t.getId() == task.getId())
                .findFirst()
                .ifPresentOrElse(taskCombo::setValue, () -> {
                    taskCombo.getItems().add(task);
                    taskCombo.setValue(task);
                });
        }
        updatingTaskCombo = false;
    }

    // ---------- buttons ----------

    @FXML
    private void onMain() {
        var state = timer.getState();
        if (state.isRunning()) {
            timer.pause();
        } else if (state.isPaused()) {
            timer.resume();
        } else {
            timer.start();
        }
    }

    @FXML
    private void onStop() {
        timer.stop();
    }

    @FXML
    private void onSkip() {
        timer.skip();
    }

    // ---------- config ----------

    private void setupConfig() {
        TimerConfig c = timer.getConfig();
        focusSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 180, c.focusMin(), 5));
        shortSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, c.shortBreakMin()));
        longSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 90, c.longBreakMin(), 5));
        cycleSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, c.sessionsBeforeLongBreak()));
        for (Spinner<Integer> s : List.of(focusSpinner, shortSpinner, longSpinner, cycleSpinner)) {
            Ui.spinner(s);
        }
        autoBreakCheck.setSelected(c.autoStartBreaks());
        configHint.setText("");
    }

    private void applyPreset(int focus, int shortBreak, int longBreak) {
        focusSpinner.getValueFactory().setValue(focus);
        shortSpinner.getValueFactory().setValue(shortBreak);
        longSpinner.getValueFactory().setValue(longBreak);
        onSaveConfig();
    }

    @FXML private void onPresetClassic() { applyPreset(25, 5, 15); }
    @FXML private void onPresetDeep()    { applyPreset(50, 10, 20); }
    @FXML private void onPresetDemo()    { applyPreset(1, 1, 2); }

    @FXML
    private void onSaveConfig() {
        TimerConfig config = new TimerConfig(focusSpinner.getValue(), shortSpinner.getValue(), longSpinner.getValue(),
            cycleSpinner.getValue(), autoBreakCheck.isSelected());
        focusService.saveConfig(config);
        configHint.setText(timer.isActive() ? "Applies from the next session" : "Saved");
    }

    // ---------- Ambient Sound Controls (FR-Sound) ----------

    private void setupSoundControls() {
        if (soundCombo == null) return;
        soundCombo.setItems(FXCollections.observableArrayList(AmbientSoundService.SoundType.values()));
        soundCombo.setValue(AmbientSoundService.get().getCurrentType());
        soundCombo.valueProperty().addListener((o, old, now) -> {
            if (now != null) {
                AmbientSoundService.get().play(now);
                updateSoundUi();
            }
        });

        if (soundVolumeSlider != null) {
            soundVolumeSlider.setValue(AmbientSoundService.get().getVolume());
            soundVolumeSlider.valueProperty().addListener((o, old, now) -> {
                AmbientSoundService.get().setVolume(now.doubleValue());
            });
        }
        updateSoundUi();
    }

    @FXML
    private void onToggleSoundPlay() {
        AmbientSoundService.get().togglePlay();
        updateSoundUi();
    }

    @FXML
    private void onToggleSoundMute() {
        AmbientSoundService.get().toggleMute();
        updateSoundUi();
    }

    private void updateSoundUi() {
        if (soundPlayIcon == null || soundMuteIcon == null) return;
        AmbientSoundService s = AmbientSoundService.get();
        soundPlayIcon.setIconLiteral(s.isPlaying() ? "fth-pause" : "fth-play");
        soundMuteIcon.setIconLiteral(s.isMuted() ? "fth-volume-x" : "fth-volume-2");
        if (soundPlayBtn != null) {
            soundPlayBtn.setTooltip(new Tooltip(s.isPlaying() ? "Pause ambient sound" : "Play ambient sound"));
        }
        if (soundMuteBtn != null) {
            soundMuteBtn.setTooltip(new Tooltip(s.isMuted() ? "Unmute" : "Mute"));
        }
    }

}
