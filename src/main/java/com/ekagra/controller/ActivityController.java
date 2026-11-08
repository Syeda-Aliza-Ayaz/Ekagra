package com.ekagra.controller;

import com.ekagra.dao.ActivityDao;
import com.ekagra.model.ActivityLog;
import com.ekagra.model.Category;
import com.ekagra.pattern.observer.ActivityListener;
import com.ekagra.service.TrackingService;
import com.ekagra.tracking.ActivitySnapshot;
import com.ekagra.ui.Animations;
import com.ekagra.ui.Ui;
import com.ekagra.util.Formats;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Activity Monitor screen (FR-18): live current app + how today's screen time was spent. */
public class ActivityController implements ActivityListener, Disposable {

    @FXML private Label statusLabel;
    @FXML private Button pauseButton;
    @FXML private StackPane nowIconHolder;
    @FXML private Label nowApp;
    @FXML private Label nowTitle;
    @FXML private Label nowCategory;
    @FXML private Label nowDuration;
    @FXML private HBox tiles;
    @FXML private Label balanceCaption;
    @FXML private HBox balanceBar;
    @FXML private HBox balanceLegend;
    @FXML private StackPane chartHolder;
    @FXML private VBox topApps;

    private TrackingService tracking;
    private Timeline refresher;
    private boolean firstRender = true;
    private Map<Category, Integer> totals = new EnumMap<>(Category.class);

    @FXML
    private void initialize() {
        tracking = TrackingService.get();
        tracking.addListener(this);
        nowTitle.setTextOverrun(OverrunStyle.ELLIPSIS);

        showNowPlaceholder();
        updateStatus();
        refreshSummary();

        refresher = new Timeline(new KeyFrame(Duration.seconds(15), e -> refreshSummary()));
        refresher.setCycleCount(Animation.INDEFINITE);
        refresher.play();
    }

    @Override
    public void dispose() {
        tracking.removeListener(this);
        refresher.stop();
    }

    // ---------- live (Observer callbacks, on the UI thread) ----------

    @Override
    public void onSample(ActivitySnapshot s) {
        boolean self = s.window().title() != null && s.window().title().startsWith("Ekagra")
            && s.window().processName().startsWith("java");
        nowApp.setText(self ? "Ekagra (this app)" : ActivityLog.displayName(s.window().processName()));
        nowTitle.setText(s.window().title() == null || s.window().title().isBlank()
            ? s.window().processName() : s.window().title());
        nowCategory.setText(s.category().getLabel());
        nowCategory.setGraphic(Ui.categoryDot(s.category()));
        nowCategory.getStyleClass().setAll("label", "category-pill");
        nowDuration.setText("for " + Formats.secondsPrecise(s.segment().getDurationSec()));
        setNowIcon(s.category());
        if (s.switchedApp()) {
            Animations.pulse(nowIconHolder);
        }
    }

    @Override
    public void onIdleChanged(boolean idle) {
        updateStatus();
        if (idle) {
            showNowPlaceholder();
        }
    }

    @Override
    public void onTrackingChanged(boolean active) {
        updateStatus();
        if (!active) {
            showNowPlaceholder();
        }
    }

    private void showNowPlaceholder() {
        String text;
        if (!tracking.isAvailable()) {
            text = "Activity tracking needs Windows";
        } else if (tracking.isPaused()) {
            text = "Tracking is paused";
        } else if (tracking.isIdle()) {
            text = "You're away";
        } else {
            text = "Waiting for activity…";
        }
        nowApp.setText(text);
        nowTitle.setText(tracking.isPaused() ? "Nothing is being recorded right now." : "");
        nowCategory.setText("");
        nowCategory.setGraphic(null);
        nowCategory.getStyleClass().setAll("label");
        nowDuration.setText("");
        nowIconHolder.getChildren().setAll(new FontIcon(tracking.isPaused() ? "fth-eye-off" : "fth-monitor"));
        nowIconHolder.getStyleClass().setAll("now-icon");
    }

    private void setNowIcon(Category c) {
        nowIconHolder.getChildren().setAll(new FontIcon(c.getIcon()));
        nowIconHolder.getStyleClass().setAll("now-icon", c.styleClass());
    }

    private void updateStatus() {
        statusLabel.getStyleClass().setAll("label", "status-pill");
        if (!tracking.isAvailable()) {
            statusLabel.setText("Unavailable");
        } else if (tracking.isPaused()) {
            statusLabel.setText("Paused");
            statusLabel.getStyleClass().add("status-paused");
        } else if (tracking.isIdle()) {
            statusLabel.setText("Idle");
            statusLabel.getStyleClass().add("status-idle");
        } else {
            statusLabel.setText("Live");
            statusLabel.getStyleClass().add("status-live");
        }
        boolean paused = tracking.isPaused();
        pauseButton.setText(paused ? "Resume tracking" : "Pause tracking");
        pauseButton.setGraphic(new FontIcon(paused ? "fth-play" : "fth-pause"));
        pauseButton.getStyleClass().remove("accent");
        if (paused) {
            pauseButton.getStyleClass().add("accent");
        }
    }

    @FXML
    private void onTogglePause() {
        tracking.setPaused(!tracking.isPaused());
    }

    // ---------- summary ----------

    private void refreshSummary() {
        LocalDate today = LocalDate.now();
        totals = tracking.secondsByCategory(today);
        renderTiles();
        renderBalance();
        renderChart(tracking.hourly(today));
        renderTopApps(tracking.topApps(today, 7));
        firstRender = false;
    }

    private int total() {
        return totals.values().stream().mapToInt(Integer::intValue).sum();
    }

    private void renderTiles() {
        int all = total();
        int productivePct = all == 0 ? 0 : Math.round(100f * totals.get(Category.PRODUCTIVE) / all);
        tiles.getChildren().setAll(
            tile(all / 60, Formats::minutes, "Screen time", "fth-monitor"),
            tile(productivePct, v -> v + "%", "Productive", Category.PRODUCTIVE.getIcon()),
            tile(totals.get(Category.DISTRACTING) / 60, Formats::minutes, "Distracting", Category.DISTRACTING.getIcon()));
    }

    private VBox tile(int value, java.util.function.IntFunction<String> fmt, String caption, String icon) {
        VBox t = Ui.statTile(fmt.apply(value), caption, icon);
        t.setPrefWidth(150);
        if (firstRender) {
            Animations.countUp((Label) t.getChildren().get(1), value, fmt);
        }
        return t;
    }

    private void renderBalance() {
        int all = total();
        balanceBar.getChildren().clear();
        balanceLegend.getChildren().clear();
        balanceCaption.setText(all == 0 ? "No activity recorded yet today" : Formats.seconds(all) + " tracked");
        if (all == 0) {
            Region empty = new Region();
            empty.getStyleClass().add("balance-empty");
            HBox.setHgrow(empty, Priority.ALWAYS);
            balanceBar.getChildren().add(empty);
        }
        for (Category c : Category.values()) {
            int sec = totals.get(c);
            int pct = all == 0 ? 0 : Math.round(100f * sec / all);
            if (sec > 0) {
                Region seg = new Region();
                seg.getStyleClass().addAll("balance-seg", c.styleClass());
                seg.prefWidthProperty().bind(balanceBar.widthProperty().multiply((double) sec / all));
                seg.setMinWidth(4);
                Tooltip.install(seg, new Tooltip(c.getLabel() + " · " + Formats.seconds(sec) + " (" + pct + "%)"));
                balanceBar.getChildren().add(seg);
            }
            Label name = new Label(c.getLabel(), Ui.categoryDot(c));
            name.getStyleClass().add("legend-name");
            Label value = Ui.label(Formats.seconds(sec) + "  ·  " + pct + "%", "muted", "small");
            balanceLegend.getChildren().add(new VBox(2, name, value));
        }
    }

    private void renderChart(List<ActivityDao.HourUsage> hours) {
        chartHolder.getChildren().clear();
        if (hours.isEmpty()) {
            chartHolder.getChildren().add(Ui.emptyState("fth-bar-chart-2", "No data yet",
                "Use your computer for a few minutes and the chart will fill in."));
            return;
        }
        int nowHour = LocalTime.now().getHour();
        int first = Math.min(hours.stream().mapToInt(ActivityDao.HourUsage::hour).min().orElse(nowHour),
            Math.max(0, nowHour - 5));
        int last = Math.max(nowHour, hours.stream().mapToInt(ActivityDao.HourUsage::hour).max().orElse(nowHour));

        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        y.setLabel("min");
        y.setMinorTickVisible(false);
        StackedBarChart<String, Number> chart = new StackedBarChart<>(x, y);
        chart.setAnimated(false);
        chart.setLegendVisible(true);
        chart.setVerticalGridLinesVisible(false);
        chart.setCategoryGap(Math.max(6, 40 - (last - first) * 2));
        chart.getStyleClass().add("ek-chart");

        for (Category c : Category.values()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(c.getLabel());
            for (int h = first; h <= last; h++) {
                int hour = h;
                int sec = hours.stream().filter(u -> u.hour() == hour && u.category() == c)
                    .mapToInt(ActivityDao.HourUsage::seconds).sum();
                XYChart.Data<String, Number> d = new XYChart.Data<>(hourLabel(h), sec / 60.0);
                String tip = hourLabel(h) + "–" + hourLabel(h + 1) + "\n" + c.getLabel() + ": " + Formats.seconds(sec);
                d.nodeProperty().addListener((o, a, node) -> {
                    if (node != null) {
                        Tooltip.install(node, new Tooltip(tip));
                    }
                });
                series.getData().add(d);
            }
            chart.getData().add(series);
        }
        chartHolder.getChildren().add(chart);
    }

    private static String hourLabel(int h) {
        int hh = h % 24;
        if (hh == 0) {
            return "12a";
        }
        if (hh == 12) {
            return "12p";
        }
        return hh < 12 ? hh + "a" : (hh - 12) + "p";
    }

    private void renderTopApps(List<ActivityDao.AppUsage> apps) {
        topApps.getChildren().clear();
        if (apps.isEmpty()) {
            topApps.getChildren().add(Ui.label("Nothing yet today.", "muted"));
            return;
        }
        int max = apps.get(0).seconds();
        for (ActivityDao.AppUsage a : apps) {
            Label name = Ui.label(ActivityLog.displayName(a.appName()), "app-name");
            Label time = Ui.label(Formats.seconds(a.seconds()), "muted", "small");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox top = new HBox(8, Ui.categoryDot(a.category()), name, spacer, time);
            top.setAlignment(Pos.CENTER_LEFT);

            StackPane track = new StackPane();
            track.getStyleClass().add("app-bar-track");
            Region fill = new Region();
            fill.getStyleClass().addAll("app-bar-fill", a.category().styleClass());
            fill.maxWidthProperty().bind(track.widthProperty().multiply(Math.max(0.03, (double) a.seconds() / max)));
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            track.getChildren().add(fill);

            VBox row = new VBox(5, top, track);
            Tooltip.install(row, new Tooltip(a.appName() + " · " + a.category().getLabel()));
            topApps.getChildren().add(row);
        }
        if (firstRender) {
            Animations.stagger(topApps, 8);
        }
    }
}
