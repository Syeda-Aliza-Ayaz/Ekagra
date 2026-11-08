package com.ekagra.controller;

import com.ekagra.dao.ActivityDao;
import com.ekagra.model.ActivityLog;
import com.ekagra.model.MoodEntry;
import com.ekagra.model.ScreenLimit;
import com.ekagra.model.SleepEntry;
import com.ekagra.service.SessionContext;
import com.ekagra.service.TrackingService;
import com.ekagra.service.ValidationException;
import com.ekagra.service.WellbeingService;
import com.ekagra.service.WellbeingService.ScorePart;
import com.ekagra.service.WellbeingService.WellbeingScore;
import com.ekagra.ui.Animations;
import com.ekagra.ui.Messages;
import com.ekagra.ui.TimerRing;
import com.ekagra.ui.Ui;
import com.ekagra.util.Formats;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Wellbeing screen (FR-21 .. FR-26). */
public class WellbeingController implements Disposable {

    private static final String[] MOOD_ICONS = {"", "fth-cloud-rain", "fth-cloud", "fth-minus-circle", "fth-smile", "fth-sun"};
    private static final String[] STRESS_ICONS = {"", "fth-feather", "fth-wind", "fth-activity", "fth-zap", "fth-alert-octagon"};
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE");

    @FXML private StackPane scoreRingHolder;
    @FXML private VBox scoreParts;
    @FXML private Label moodStatus;
    @FXML private HBox moodButtons;
    @FXML private HBox stressButtons;
    @FXML private TextField moodNote;
    @FXML private Label moodMessage;
    @FXML private Label screenUsed;
    @FXML private ProgressBar screenBar;
    @FXML private Spinner<Integer> dailyLimitSpinner;
    @FXML private VBox appLimitList;
    @FXML private ComboBox<String> appLimitName;
    @FXML private Spinner<Integer> appLimitSpinner;
    @FXML private Label limitMessage;
    @FXML private Label continuousValue;
    @FXML private Spinner<Integer> breakSpinner;
    @FXML private CheckBox eyeRestCheck;
    @FXML private CheckBox remindersCheck;
    @FXML private Label breakHint;
    @FXML private Label sleepStatus;
    @FXML private ComboBox<String> bedtimeBox;
    @FXML private ComboBox<String> wakeBox;
    @FXML private Label sleepMessage;
    @FXML private StackPane sleepChartHolder;
    @FXML private StackPane moodChartHolder;

    private final ToggleGroup moodGroup = new ToggleGroup();
    private final ToggleGroup stressGroup = new ToggleGroup();
    private final TimerRing scoreRing = new TimerRing(150);
    private WellbeingService service;
    private Timeline refresher;

    @FXML
    private void initialize() {
        service = new WellbeingService(SessionContext.getInstance().getCurrentUser().getId());
        scoreRingHolder.getChildren().add(scoreRing);

        buildScaleButtons(moodButtons, moodGroup, MoodEntry.MOOD_LABELS, MOOD_ICONS);
        buildScaleButtons(stressButtons, stressGroup, MoodEntry.STRESS_LABELS, STRESS_ICONS);

        dailyLimitSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 1440, service.getDailyLimitMin(), 15));
        appLimitSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 600, 30, 5));
        breakSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 240, service.getBreakEveryMin(), 5));
        for (Spinner<Integer> s : List.of(dailyLimitSpinner, appLimitSpinner, breakSpinner)) {
            Ui.spinner(s);
        }
        eyeRestCheck.setSelected(service.isEyeRestEnabled());
        remindersCheck.setSelected(service.areRemindersEnabled());

        for (ComboBox<String> box : List.of(bedtimeBox, wakeBox)) {
            for (int h = 0; h < 24; h++) {
                box.getItems().addAll(String.format("%02d:00", h), String.format("%02d:30", h));
            }
        }
        bedtimeBox.setValue("23:30");
        wakeBox.setValue("07:00");

        loadMood();
        loadSleep();
        refreshScreenTime();
        renderAppLimits();
        renderMoodChart();
        renderScore(true);
        updateContinuous();

        refresher = new Timeline(new KeyFrame(Duration.seconds(20), e -> {
            refreshScreenTime();
            renderAppLimits();
            updateContinuous();
            renderScore(false);
        }));
        refresher.setCycleCount(Animation.INDEFINITE);
        refresher.play();
    }

    @Override
    public void dispose() {
        refresher.stop();
    }

    // ---------- score ----------

    private void renderScore(boolean animate) {
        WellbeingScore score = service.todaysScore();
        scoreParts.getChildren().clear();
        if (!score.hasData()) {
            scoreRing.update("–", "No data yet", 0, false);
            Label hint = Ui.label("Log last night's sleep and today's mood to see your score.", "muted");
            hint.setWrapText(true);
            scoreParts.getChildren().add(hint);
            return;
        }
        scoreRing.update(String.valueOf(score.total()), scoreWord(score.total()), score.total() / 100.0, false);
        if (animate) {
            Animations.pulse(scoreRing);
        }
        for (ScorePart p : score.parts()) {
            Label name = Ui.label(p.name(), "score-part-name");
            Label value = Ui.label(String.valueOf(p.score()), "score-part-value");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            ProgressBar bar = new ProgressBar(p.score() / 100.0);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add("small-bar");
            VBox row = new VBox(3, new HBox(6, name, spacer, value), bar);
            Tooltip.install(row, new Tooltip(p.detail()));
            scoreParts.getChildren().add(row);
        }
    }

    private static String scoreWord(int s) {
        if (s >= 80) {
            return "Thriving";
        }
        if (s >= 60) {
            return "Balanced";
        }
        if (s >= 40) {
            return "Needs care";
        }
        return "Rest up";
    }

    // ---------- mood ----------

    private void buildScaleButtons(HBox box, ToggleGroup group, String[] labels, String[] icons) {
        for (int i = 1; i <= 5; i++) {
            ToggleButton b = new ToggleButton(labels[i], new FontIcon(icons[i]));
            b.setUserData(i);
            b.setToggleGroup(group);
            b.getStyleClass().add("scale-button");
            b.setContentDisplay(javafx.scene.control.ContentDisplay.TOP);
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, Priority.ALWAYS);
            box.getChildren().add(b);
        }
    }

    private void loadMood() {
        service.todaysMood().ifPresentOrElse(m -> {
            select(moodGroup, m.getMood());
            select(stressGroup, m.getStress());
            moodNote.setText(m.getNote());
            moodStatus.setText("Checked in today");
        }, () -> moodStatus.setText("Not checked in yet"));
    }

    private static void select(ToggleGroup group, int value) {
        group.getToggles().stream().filter(t -> (int) t.getUserData() == value).findFirst().ifPresent(group::selectToggle);
    }

    @FXML
    private void onSaveMood() {
        try {
            int mood = moodGroup.getSelectedToggle() == null ? 0 : (int) moodGroup.getSelectedToggle().getUserData();
            int stress = stressGroup.getSelectedToggle() == null ? 0 : (int) stressGroup.getSelectedToggle().getUserData();
            service.saveMood(mood, stress, moodNote.getText());
            Messages.success(moodMessage, "Thanks for checking in.");
            moodStatus.setText("Checked in today");
            renderMoodChart();
            renderScore(true);
        } catch (ValidationException e) {
            Messages.error(moodMessage, e.getMessage());
        }
    }

    private void renderMoodChart() {
        LocalDate today = LocalDate.now();
        Map<LocalDate, MoodEntry> byDay = service.moodHistory(7).stream()
            .collect(Collectors.toMap(MoodEntry::getDate, Function.identity()));
        moodChartHolder.getChildren().clear();
        if (byDay.isEmpty()) {
            moodChartHolder.getChildren().add(Ui.emptyState("fth-smile", "No check-ins yet",
                "Save a check-in above and your trend will appear here."));
            return;
        }
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(0, 5.5, 1);
        y.setMinorTickVisible(false);
        y.setTickLabelFormatter(new NumberAxis.DefaultFormatter(y) {
            @Override
            public String toString(Number n) {
                return n.doubleValue() % 1 == 0 && n.intValue() >= 1 && n.intValue() <= 5 ? String.valueOf(n.intValue()) : "";
            }
        });
        LineChart<String, Number> chart = new LineChart<>(x, y);
        chart.setAnimated(false);
        chart.setVerticalGridLinesVisible(false);
        chart.getStyleClass().addAll("ek-chart", "mood-chart");
        XYChart.Series<String, Number> mood = new XYChart.Series<>();
        mood.setName("Mood");
        XYChart.Series<String, Number> stress = new XYChart.Series<>();
        stress.setName("Stress");
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String label = i == 0 ? "Today" : d.format(DAY);
            MoodEntry m = byDay.get(d);
            if (m != null) {
                mood.getData().add(point(label, m.getMood(), label + ": mood " + m.getMoodLabel()));
                stress.getData().add(point(label, m.getStress(), label + ": stress " + m.getStressLabel()));
            }
        }
        // keep all 7 days on the axis even when some are missing
        x.setCategories(javafx.collections.FXCollections.observableArrayList(
            java.util.stream.IntStream.rangeClosed(0, 6).mapToObj(i -> i == 6 ? "Today" : today.minusDays(6L - i).format(DAY)).toList()));
        chart.getData().addAll(List.of(mood, stress));
        moodChartHolder.getChildren().add(chart);
    }

    private static XYChart.Data<String, Number> point(String x, int y, String tip) {
        XYChart.Data<String, Number> d = new XYChart.Data<>(x, y);
        d.nodeProperty().addListener((o, a, n) -> {
            if (n != null) {
                Tooltip.install(n, new Tooltip(tip));
            }
        });
        return d;
    }

    // ---------- sleep ----------

    private void loadSleep() {
        service.lastNight().ifPresentOrElse(s -> {
            bedtimeBox.setValue(s.getBedtime().format(Formats.TIME));
            wakeBox.setValue(s.getWakeTime().format(Formats.TIME));
            sleepStatus.setText(String.format("Last night: %.1f h", s.getHours()));
        }, () -> sleepStatus.setText("Last night not logged"));
        renderSleepChart();
    }

    @FXML
    private void onSaveSleep() {
        try {
            SleepEntry s = service.saveSleep(parseTime(bedtimeBox), parseTime(wakeBox));
            Messages.success(sleepMessage, String.format("Logged %.1f hours.", s.getHours()));
            sleepStatus.setText(String.format("Last night: %.1f h", s.getHours()));
            renderSleepChart();
            renderScore(true);
        } catch (ValidationException e) {
            Messages.error(sleepMessage, e.getMessage());
        }
    }

    private static LocalTime parseTime(ComboBox<String> box) {
        try {
            return LocalTime.parse(box.getEditor().getText().trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Times must look like 23:30.");
        }
    }

    private void renderSleepChart() {
        LocalDate today = LocalDate.now();
        Map<LocalDate, SleepEntry> byDay = service.sleepHistory(7).stream()
            .collect(Collectors.toMap(SleepEntry::getDate, Function.identity()));
        sleepChartHolder.getChildren().clear();
        if (byDay.isEmpty()) {
            sleepChartHolder.getChildren().add(Ui.emptyState("fth-moon", "No sleep logged",
                "Log when you went to bed and woke up."));
            return;
        }
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(0, 12, 2);
        y.setLabel("hours");
        y.setMinorTickVisible(false);
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setAnimated(false);
        chart.setLegendVisible(false);           // one series: the title names it
        chart.setVerticalGridLinesVisible(false);
        chart.setCategoryGap(18);
        chart.getStyleClass().addAll("ek-chart", "sleep-chart");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String label = i == 0 ? "Today" : d.format(DAY);
            SleepEntry s = byDay.get(d);
            double h = s == null ? 0 : s.getHours();
            XYChart.Data<String, Number> data = new XYChart.Data<>(label, h);
            String tip = s == null ? label + ": not logged"
                : String.format("%s: %.1f h (%s–%s)", label, h, s.getBedtime().format(Formats.TIME), s.getWakeTime().format(Formats.TIME));
            data.nodeProperty().addListener((o, a, n) -> {
                if (n != null) {
                    Tooltip.install(n, new Tooltip(tip));
                }
            });
            series.getData().add(data);
        }
        chart.getData().add(series);
        sleepChartHolder.getChildren().add(chart);
    }

    // ---------- screen time & limits ----------

    private void refreshScreenTime() {
        int usedMin = service.screenSecondsToday() / 60;
        int limit = service.getDailyLimitMin();
        double f = Math.min(1.0, (double) usedMin / limit);
        screenUsed.setText(Formats.minutes(usedMin) + " of " + Formats.minutes(limit));
        screenBar.setProgress(f);
        screenBar.getStyleClass().removeAll("warn", "over");
        if (usedMin >= limit) {
            screenBar.getStyleClass().add("over");
        } else if (usedMin >= limit * 0.8) {
            screenBar.getStyleClass().add("warn");
        }
    }

    @FXML
    private void onSaveDailyLimit() {
        try {
            service.setDailyLimitMin(dailyLimitSpinner.getValue());
            refreshScreenTime();
            renderScore(false);
            Messages.success(limitMessage, "Daily limit saved.");
        } catch (ValidationException e) {
            Messages.error(limitMessage, e.getMessage());
        }
    }

    private void renderAppLimits() {
        appLimitList.getChildren().clear();
        List<ScreenLimit> limits = service.appLimits();
        if (limits.isEmpty()) {
            appLimitList.getChildren().add(Ui.label("No app limits yet. Add one below, e.g. Instagram 30 min.", "muted", "small"));
        }
        for (ScreenLimit l : limits) {
            int used = service.secondsOnAppToday(l.getAppName()) / 60;
            Label name = Ui.label(l.getAppName(), "app-name");
            Label value = Ui.label(Formats.minutes(used) + " / " + Formats.minutes(l.getLimitMin()), "muted", "small");
            ProgressBar bar = new ProgressBar(Math.min(1.0, (double) used / l.getLimitMin()));
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().addAll("small-bar", "limit-bar");
            if (used >= l.getLimitMin()) {
                bar.getStyleClass().add("over");
            } else if (used >= l.getLimitMin() * 0.8) {
                bar.getStyleClass().add("warn");
            }
            HBox.setHgrow(bar, Priority.ALWAYS);
            name.setMinWidth(110);
            HBox row = new HBox(10, name, bar, value, Ui.iconButton("fth-trash-2", "Remove limit", () -> {
                service.removeAppLimit(l);
                renderAppLimits();
            }));
            row.setAlignment(Pos.CENTER_LEFT);
            appLimitList.getChildren().add(row);
        }
        // suggestions: today's most used apps
        if (appLimitName.getItems().isEmpty() && TrackingService.isStarted()) {
            for (ActivityDao.AppUsage a : TrackingService.get().topApps(LocalDate.now(), 8)) {
                appLimitName.getItems().add(ActivityLog.displayName(a.appName()));
            }
            appLimitName.getItems().addAll("Instagram", "YouTube", "TikTok", "Netflix");
        }
    }

    @FXML
    private void onAddAppLimit() {
        try {
            service.addAppLimit(appLimitName.getEditor().getText(), appLimitSpinner.getValue());
            appLimitName.getEditor().clear();
            Messages.hide(limitMessage);
            renderAppLimits();
        } catch (ValidationException e) {
            Messages.error(limitMessage, e.getMessage());
        }
    }

    // ---------- breaks ----------

    private void updateContinuous() {
        int mins = TrackingService.isStarted() ? TrackingService.get().getWellbeingMonitor().getContinuousMinutes() : 0;
        continuousValue.setText(Formats.minutes(mins));
    }

    @FXML
    private void onSaveBreaks() {
        try {
            service.setBreakEveryMin(breakSpinner.getValue());
            service.setEyeRestEnabled(eyeRestCheck.isSelected());
            service.setRemindersEnabled(remindersCheck.isSelected());
            breakHint.setText("Saved");
        } catch (ValidationException e) {
            breakHint.setText(e.getMessage());
        }
    }
}
