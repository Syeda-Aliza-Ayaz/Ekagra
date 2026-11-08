package com.ekagra.controller;

import com.ekagra.dao.ActivityDao;
import com.ekagra.model.ActivityLog;
import com.ekagra.model.Category;
import com.ekagra.model.Student;
import com.ekagra.pattern.strategy.ScoreStrategy;
import com.ekagra.service.AnalyticsService;
import com.ekagra.service.AnalyticsService.DaySummary;
import com.ekagra.service.SessionContext;
import com.ekagra.ui.Animations;
import com.ekagra.ui.Charts;
import com.ekagra.ui.Ui;
import com.ekagra.util.Formats;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Analytics screen (FR-30, FR-31, FR-32). Switching "Scoring mode" swaps the Strategy live. */
public class AnalyticsController {

    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("d MMM");

    @FXML private ToggleButton range7;
    @FXML private ToggleButton range14;
    @FXML private ToggleButton range30;
    @FXML private ComboBox<ScoreStrategy> strategyBox;
    @FXML private Label strategyDescription;
    @FXML private Label avgScore;
    @FXML private StackPane scoreChartHolder;
    @FXML private Label todayTotal;
    @FXML private VBox breakdownList;
    @FXML private StackPane categoryChartHolder;
    @FXML private Label periodTotal;
    @FXML private VBox shareList;
    @FXML private Label focusTotal;
    @FXML private Label focusTargetNote;
    @FXML private StackPane focusChartHolder;
    @FXML private VBox topAppList;
    @FXML private VBox insightList;

    private AnalyticsService analytics;
    private Student student;
    private int days = 7;

    @FXML
    private void initialize() {
        student = SessionContext.getInstance().getStudent();
        analytics = new AnalyticsService(student);

        ToggleGroup range = new ToggleGroup();
        for (ToggleButton b : List.of(range7, range14, range30)) {
            b.setToggleGroup(range);
        }
        range.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null) {
                range.selectToggle(old);
                return;
            }
            days = now == range7 ? 7 : now == range14 ? 14 : 30;
            render();
        });

        strategyBox.getItems().setAll(ScoreStrategy.all());
        strategyBox.setCellFactory(lv -> strategyCell());
        strategyBox.setButtonCell(strategyCell());
        strategyBox.getItems().stream().filter(s -> s.getId().equals(analytics.getStrategy().getId()))
            .findFirst().ifPresent(strategyBox::setValue);
        strategyBox.valueProperty().addListener((o, a, s) -> {
            if (s != null) {
                analytics.setStrategy(s);     // Strategy pattern: same data, different formula
                render();
            }
        });
        render();
    }

    private static ListCell<ScoreStrategy> strategyCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ScoreStrategy item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        };
    }

    private void render() {
        strategyDescription.setText(analytics.getStrategy().getName() + ": " + analytics.getStrategy().getDescription());
        List<DaySummary> period = analytics.lastDays(days);
        renderScoreChart(period);
        renderBreakdown(period.get(period.size() - 1));
        renderCategoryChart(period);
        renderShares(period);
        renderFocusChart(period);
        renderTopApps();
        insightList.getChildren().clear();
        analytics.insights(period.subList(Math.max(0, period.size() - 7), period.size()))
            .forEach(i -> insightList.getChildren().add(DashboardController.insightRow(i)));
        Animations.stagger(insightList, 8);
    }

    private String label(DaySummary d) {
        return days <= 7 ? d.dayLabel() : d.date().format(SHORT);
    }

    // ---------- charts ----------

    private void renderScoreChart(List<DaySummary> period) {
        List<Charts.Point> points = period.stream().map(d -> new Charts.Point(label(d), d.score().total(),
            d.date().format(DateTimeFormatter.ofPattern("EEE d MMM"))
                + (d.hasData() ? "\nScore " + d.score().total() : "\nNo activity"))).toList();
        scoreChartHolder.getChildren().setAll(Charts.line(points, 100, "score-chart"));
        var active = period.stream().filter(DaySummary::hasData).toList();
        double avg = active.stream().mapToInt(d -> d.score().total()).average().orElse(0);
        avgScore.setText(active.isEmpty() ? "No data yet"
            : "Average " + Math.round(avg) + " over " + active.size() + (active.size() == 1 ? " active day" : " active days"));
    }

    private void renderBreakdown(DaySummary today) {
        todayTotal.setText("Today: " + today.score().total() + " / 100 with the "
            + analytics.getStrategy().getName() + " formula");
        breakdownList.getChildren().clear();
        for (ScoreStrategy.Part p : today.score().parts()) {
            Label name = Ui.label(p.name(), "score-part-name");
            Label weight = Ui.chip(Math.round(p.weight() * 100) + "% weight", "chip-subject");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Label points = Ui.label("+" + p.points(), "score-part-value");
            HBox top = new HBox(8, name, weight, sp, points);
            top.setAlignment(Pos.CENTER_LEFT);
            ProgressBar bar = new ProgressBar(p.value());
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add("small-bar");
            VBox row = new VBox(4, top, bar);
            Tooltip.install(row, new Tooltip(p.name() + " is at " + Math.round(p.value() * 100)
                + "% and counts for " + Math.round(p.weight() * 100) + "% of the score = " + p.points() + " points"));
            breakdownList.getChildren().add(row);
        }
        Animations.stagger(breakdownList, 6);
    }

    private void renderCategoryChart(List<DaySummary> period) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        y.setLabel("h");
        y.setMinorTickVisible(false);
        StackedBarChart<String, Number> chart = new StackedBarChart<>(x, y);
        chart.setAnimated(false);
        chart.setVerticalGridLinesVisible(false);
        chart.setCategoryGap(Math.max(6, 40 - days));
        chart.getStyleClass().add("ek-chart");
        for (Category c : Category.values()) {
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            s.setName(c.getLabel());
            for (DaySummary d : period) {
                int sec = d.secondsByCategory().get(c);
                XYChart.Data<String, Number> data = new XYChart.Data<>(label(d), sec / 3600.0);
                Charts.tooltip(data, d.date().format(DateTimeFormatter.ofPattern("EEE d MMM")) + "\n"
                    + c.getLabel() + ": " + Formats.seconds(sec));
                s.getData().add(data);
            }
            chart.getData().add(s);
        }
        categoryChartHolder.getChildren().setAll(chart);
    }

    private void renderShares(List<DaySummary> period) {
        Map<Category, Integer> totals = new EnumMap<>(Category.class);
        for (Category c : Category.values()) {
            totals.put(c, period.stream().mapToInt(d -> d.secondsByCategory().get(c)).sum());
        }
        int all = totals.values().stream().mapToInt(Integer::intValue).sum();
        periodTotal.setText(Formats.seconds(all) + " tracked in the last " + days + " days");
        shareList.getChildren().clear();
        for (Category c : Category.values()) {
            int sec = totals.get(c);
            int pct = all == 0 ? 0 : Math.round(100f * sec / all);
            Label name = new Label(c.getLabel(), Ui.categoryDot(c));
            name.getStyleClass().add("legend-name");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Label value = Ui.label(Formats.seconds(sec) + " · " + pct + "%", "muted", "small");
            StackPane track = new StackPane();
            track.getStyleClass().add("app-bar-track");
            Region fill = new Region();
            fill.getStyleClass().addAll("app-bar-fill", c.styleClass());
            fill.maxWidthProperty().bind(track.widthProperty().multiply(all == 0 ? 0 : (double) sec / all));
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            track.getChildren().add(fill);
            shareList.getChildren().add(new VBox(6, new HBox(8, name, sp, value), track));
        }
    }

    private void renderFocusChart(List<DaySummary> period) {
        int target = student.getDailyTargetMin();
        List<Charts.Point> points = period.stream().map(d -> new Charts.Point(label(d), d.focusMinutes(),
            d.date().format(DateTimeFormatter.ofPattern("EEE d MMM")) + "\n" + Formats.minutes(d.focusMinutes())
                + " focus · " + d.completedSessions() + " sessions"
                + (d.focusMinutes() >= target ? "\nTarget reached" : ""))).toList();
        focusChartHolder.getChildren().setAll(Charts.bar(points, "min", "focus-chart"));
        int total = period.stream().mapToInt(DaySummary::focusMinutes).sum();
        long hit = period.stream().filter(d -> d.focusMinutes() >= target).count();
        focusTotal.setText(Formats.minutes(total) + " total");
        focusTargetNote.setText("Daily target " + Formats.minutes(target) + ": reached on " + hit + " of " + days + " days");
    }

    private void renderTopApps() {
        topAppList.getChildren().clear();
        List<ActivityDao.AppUsage> apps = analytics.topApps(days, 6);
        if (apps.isEmpty()) {
            topAppList.getChildren().add(Ui.label("No activity tracked in this period yet.", "muted"));
            return;
        }
        int max = apps.get(0).seconds();
        for (ActivityDao.AppUsage a : apps) {
            Label name = Ui.label(ActivityLog.displayName(a.appName()), "app-name");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            HBox top = new HBox(8, Ui.categoryDot(a.category()), name, sp, Ui.label(Formats.seconds(a.seconds()), "muted", "small"));
            top.setAlignment(Pos.CENTER_LEFT);
            StackPane track = new StackPane();
            track.getStyleClass().add("app-bar-track");
            Region fill = new Region();
            fill.getStyleClass().addAll("app-bar-fill", a.category().styleClass());
            fill.maxWidthProperty().bind(track.widthProperty().multiply(Math.max(0.03, (double) a.seconds() / max)));
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            track.getChildren().add(fill);
            topAppList.getChildren().add(new VBox(5, top, track));
        }
    }
}
