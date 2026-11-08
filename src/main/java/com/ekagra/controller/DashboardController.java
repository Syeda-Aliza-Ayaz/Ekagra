package com.ekagra.controller;

import com.ekagra.app.Navigator;
import com.ekagra.app.Screen;
import com.ekagra.model.Category;
import com.ekagra.model.Student;
import com.ekagra.model.Task;
import com.ekagra.pattern.strategy.ScoreStrategy;
import com.ekagra.service.AnalyticsService;
import com.ekagra.service.AnalyticsService.DaySummary;
import com.ekagra.service.AnalyticsService.Insight;
import com.ekagra.service.GoalService;
import com.ekagra.service.SessionContext;
import com.ekagra.service.TaskService;
import com.ekagra.ui.Animations;
import com.ekagra.ui.Charts;
import com.ekagra.ui.TaskEditorDialog;
import com.ekagra.ui.TimerRing;
import com.ekagra.ui.Ui;
import com.ekagra.util.Formats;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/** Home screen for students (FR-29). */
public class DashboardController {

    @FXML private Label greeting;
    @FXML private Label subline;
    @FXML private Label strategyChip;
    @FXML private StackPane scoreRingHolder;
    @FXML private VBox scoreParts;
    @FXML private Label scoreDelta;
    @FXML private GridPane tiles;
    @FXML private StackPane weekChartHolder;
    @FXML private VBox insightList;
    @FXML private VBox upNextList;
    @FXML private VBox goalList;

    private final TimerRing scoreRing = new TimerRing(160);
    private Student student;
    private AnalyticsService analytics;

    @FXML
    private void initialize() {
        student = SessionContext.getInstance().getStudent();
        analytics = new AnalyticsService(student);
        new GoalService(student.getId()).ensureDefaultGoals(student.getDailyTargetMin());
        scoreRingHolder.getChildren().add(scoreRing);

        greeting.setText(AnalyticsService.greeting(LocalDateTime.now()) + ", " + student.getFirstName());

        List<DaySummary> week = analytics.lastDays(7);
        DaySummary today = week.get(week.size() - 1);
        DaySummary yesterday = week.get(week.size() - 2);

        subline.setText(motivation(today));
        renderScore(today, yesterday);
        renderTiles(today);
        renderWeekChart(week);
        renderInsights(analytics.insights(week));
        renderUpNext();
        renderGoals();
    }

    private String motivation(DaySummary today) {
        int left = student.getDailyTargetMin() - today.focusMinutes();
        if (today.focusMinutes() == 0) {
            return "Ready when you are. One focus session is a great start.";
        }
        if (left > 0) {
            return Formats.minutes(left) + " of focus to go to hit today's target.";
        }
        return "Daily target reached. Remember to take a proper break.";
    }

    // ---------- score ----------

    private void renderScore(DaySummary today, DaySummary yesterday) {
        ScoreStrategy s = analytics.getStrategy();
        strategyChip.setText(s.getName());
        strategyChip.getStyleClass().setAll("label", "chip", "chip-subject");
        strategyChip.setMinWidth(Region.USE_PREF_SIZE);

        int score = today.score().total();
        scoreRing.update(String.valueOf(score), word(score), score / 100.0, false);
        Animations.pulse(scoreRing);

        scoreParts.getChildren().clear();
        today.score().parts().stream()
            .sorted(Comparator.comparingDouble(ScoreStrategy.Part::weight).reversed())
            .limit(4)
            .forEach(p -> {
                Label name = Ui.label(p.name(), "score-part-name");
                Label pct = Ui.label(Math.round(p.value() * 100) + "%", "score-part-value");
                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);
                ProgressBar bar = new ProgressBar(p.value());
                bar.setMaxWidth(Double.MAX_VALUE);
                bar.getStyleClass().add("small-bar");
                scoreParts.getChildren().add(new VBox(3, new HBox(6, name, sp, pct), bar));
            });

        int delta = yesterday.hasData() ? score - yesterday.score().total() : 0;
        scoreDelta.setText(!yesterday.hasData() ? "No data from yesterday to compare"
            : delta == 0 ? "Same as yesterday"
            : (delta > 0 ? "▲ " + delta : "▼ " + (-delta)) + " points vs yesterday");
        scoreDelta.getStyleClass().removeAll("delta-up", "delta-down");
        if (delta != 0) {
            scoreDelta.getStyleClass().add(delta > 0 ? "delta-up" : "delta-down");
        }
    }

    static String word(int s) {
        if (s >= 80) {
            return "Excellent";
        }
        if (s >= 60) {
            return "Good";
        }
        if (s >= 40) {
            return "Fair";
        }
        return "Getting started";
    }

    // ---------- tiles ----------

    private void renderTiles(DaySummary today) {
        tiles.getChildren().clear();
        tiles.getColumnConstraints().clear();
        for (int i = 0; i < 2; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(50);
            tiles.getColumnConstraints().add(c);
        }
        int target = student.getDailyTargetMin();
        VBox focus = tile("fth-clock", "Focus today", today.focusMinutes(), Formats::minutes,
            "of " + Formats.minutes(target) + " target", Math.min(1.0, (double) today.focusMinutes() / target));
        int screenMin = today.screenSeconds() / 60;
        int distractMin = today.secondsByCategory().get(Category.DISTRACTING) / 60;
        VBox screen = tile("fth-monitor", "Screen time", screenMin, Formats::minutes,
            Formats.minutes(distractMin) + " distracting", -1);
        VBox tasks = tile("fth-check-square", "Tasks done today", today.tasksDone(), String::valueOf,
            today.completedSessions() + (today.completedSessions() == 1 ? " focus session" : " focus sessions"), -1);
        int streak = analytics.focusStreak();
        VBox streakTile = tile("fth-zap", "Focus streak", streak, v -> v + (v == 1 ? " day" : " days"),
            streak > 0 ? "Keep it going!" : "Do one session to start", -1);
        streakTile.getStyleClass().add("tile-streak");
        tiles.add(focus, 0, 0);
        tiles.add(screen, 1, 0);
        tiles.add(tasks, 0, 1);
        tiles.add(streakTile, 1, 1);
        Animations.stagger(tiles, 4);
    }

    private VBox tile(String icon, String caption, int value, java.util.function.IntFunction<String> fmt,
                      String sub, double progress) {
        FontIcon fi = new FontIcon(icon);
        fi.getStyleClass().add("stat-icon");
        Label cap = Ui.label(caption, "muted", "small");
        HBox top = new HBox(8, fi, cap);
        top.setAlignment(Pos.CENTER_LEFT);
        Label v = Ui.label(fmt.apply(value), "stat-value");
        Animations.countUp(v, value, fmt);
        Label s = Ui.label(sub, "muted", "small");
        VBox box = new VBox(4, top, v, s);
        if (progress >= 0) {
            ProgressBar bar = new ProgressBar(progress);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add("small-bar");
            box.getChildren().add(bar);
        }
        box.getStyleClass().addAll("card", "stat-tile");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    // ---------- week chart ----------

    private void renderWeekChart(List<DaySummary> week) {
        List<Charts.Point> points = week.stream()
            .map(d -> new Charts.Point(d.dayLabel(), d.score().total(),
                d.date().format(DateTimeFormatter.ofPattern("EEE d MMM"))
                    + (d.hasData() ? "\nScore " + d.score().total() + " · focus " + Formats.minutes(d.focusMinutes())
                    : "\nNo activity")))
            .toList();
        weekChartHolder.getChildren().setAll(Charts.line(points, 100, "score-chart"));
    }

    // ---------- insights ----------

    private void renderInsights(List<Insight> insights) {
        insightList.getChildren().clear();
        insights.stream().limit(3).forEach(i -> insightList.getChildren().add(insightRow(i)));
        Animations.stagger(insightList, 3);
    }

    static HBox insightRow(Insight i) {
        StackPane icon = new StackPane(new FontIcon(i.icon()));
        icon.getStyleClass().addAll("insight-icon", "tone-" + i.tone().name().toLowerCase());
        Label text = Ui.label(i.text(), "insight-text");
        text.setWrapText(true);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox row = new HBox(12, icon, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("insight-row");
        return row;
    }

    // ---------- up next ----------

    private void renderUpNext() {
        upNextList.getChildren().clear();
        List<Task> next = analytics.upNext(4);
        if (next.isEmpty()) {
            upNextList.getChildren().add(Ui.label("Nothing pending. Enjoy the free time!", "muted"));
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Task t : next) {
            Label title = Ui.label(t.getTitle(), "app-name");
            title.setMinWidth(0);
            Label when = Ui.label(Formats.deadline(t.getDeadline(), now), "small");
            when.getStyleClass().add(t.isOverdue(now) ? "text-danger" : t.isDueSoon(now) ? "text-warning" : "muted");
            VBox text = new VBox(2, title, when);
            HBox.setHgrow(text, Priority.ALWAYS);
            HBox row = new HBox(10, Ui.chip(t.getPriority().getLabel(), t.getPriority().styleClass()), text,
                Ui.iconButton("fth-play-circle", "Focus on this task",
                    () -> Navigator.goTo(Screen.FOCUS, FocusController.class, c -> c.focusOn(t))));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("list-row");
            upNextList.getChildren().add(row);
        }
    }

    // ---------- goals ----------

    private void renderGoals() {
        goalList.getChildren().clear();
        var goals = new GoalService(student.getId()).goalsWithProgress().stream()
            .filter(p -> p.goal().getPeriod() == com.ekagra.model.Goal.Period.DAILY).toList();
        if (goals.isEmpty()) {
            goalList.getChildren().add(Ui.label("No daily goals. Add one on the Goals screen.", "muted"));
        }
        for (var p : goals) {
            Label name = Ui.label(p.goal().getTitle(), "score-part-name");
            name.setMinWidth(0);
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            String unit = p.goal().getType().getUnit();
            String amounts = unit.equals("min") ? Formats.minutes(p.current()) + " / " + Formats.minutes(p.target())
                : p.current() + " / " + p.target();
            Label value = Ui.label(amounts, "muted", "small");
            value.setMinWidth(Region.USE_PREF_SIZE);
            ProgressBar bar = new ProgressBar(p.fraction());
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add("goal-bar");
            if (p.isOverLimit()) {
                bar.getStyleClass().add("over");
            } else if (p.isAchieved() && !p.goal().getType().isLimit()) {
                bar.getStyleClass().add("done");
            }
            goalList.getChildren().add(new VBox(4, new HBox(6, name, sp, value), bar));
        }
    }

    // ---------- actions ----------

    @FXML
    private void onStartFocus() {
        Navigator.goTo(Screen.FOCUS);
    }

    @FXML
    private void onNewTask() {
        TaskService ts = new TaskService();
        new TaskEditorDialog(null, student.getId(), ts.getSubjects(ts.getTasks(student.getId())), ts).showAndWait();
        Navigator.refreshIfShowing(Screen.DASHBOARD);
    }

    @FXML private void onOpenAnalytics() { Navigator.goTo(Screen.ANALYTICS); }
    @FXML private void onOpenTasks() { Navigator.goTo(Screen.TASKS); }
    @FXML private void onOpenGoals() { Navigator.goTo(Screen.GOALS); }
}
