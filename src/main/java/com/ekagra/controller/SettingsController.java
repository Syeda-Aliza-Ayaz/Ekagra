package com.ekagra.controller;

import com.ekagra.app.MainApp;
import com.ekagra.app.Navigator;
import com.ekagra.app.Screen;
import com.ekagra.app.ThemeManager;
import com.ekagra.dao.BaseDao;
import com.ekagra.model.User;
import com.ekagra.pattern.strategy.ScoreStrategy;
import com.ekagra.service.AnalyticsService;
import com.ekagra.service.SessionContext;
import com.ekagra.service.SettingsService;
import com.ekagra.service.TrackingService;
import com.ekagra.ui.Ui;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/** Settings (FR-41). Every change is saved immediately. */
public class SettingsController {

    @FXML private VBox root;

    private final SettingsService settings = new SettingsService();
    private User user;
    private int uid;

    @FXML
    private void initialize() {
        user = SessionContext.getInstance().getCurrentUser();
        uid = user.getId();

        root.getChildren().add(section("fth-sun", "Appearance", "How Ekagra looks.", themeRow()));

        if (SessionContext.getInstance().isStudent()) {
            root.getChildren().add(section("fth-bar-chart-2", "Productivity score",
                "Which formula the Dashboard and Analytics use.", strategyRow()));
            root.getChildren().add(section("fth-crosshair", "Focus", "Pomodoro lengths are set on the Focus Timer screen.",
                toggle("Focus Guard", "Warn me when I open a distracting app during a focus session.",
                    SettingsService.FOCUS_GUARD, true),
                linkRow("Timer lengths", "Open Focus Timer", Screen.FOCUS)));
            root.getChildren().add(section("fth-monitor", "Activity tracking",
                "Only app names and window titles are stored, never keystrokes or screenshots.",
                toggle("Start tracking when I log in", "Turn off to start every session paused.",
                    SettingsService.AUTO_TRACK, true),
                idleRow(),
                linkRow("Pause tracking now", "Open Activity Monitor", Screen.ACTIVITY)));
            root.getChildren().add(section("fth-heart", "Wellbeing reminders", "Break and eye-rest reminders, screen-time limits.",
                toggle("Wellbeing reminders", "Break reminders, eye breaks and limit warnings.", SettingsService.REMINDERS, true),
                toggle("20-20-20 eye breaks", "Every 20 minutes, look 20 feet away for 20 seconds.", SettingsService.EYE_REST, true),
                linkRow("Limits and break interval", "Open Wellbeing", Screen.WELLBEING)));
        }

        root.getChildren().add(section("fth-bell", "Notifications", "Desktop pop-ups appear in the bottom-right corner.",
            toggle("Desktop pop-ups", "Show reminders and alerts on top of other apps. They're always saved in Notifications.",
                SettingsService.POPUPS, true)));

        Button profile = new Button("Edit profile", new FontIcon("fth-user"));
        profile.setOnAction(e -> Navigator.goTo(Screen.PROFILE));
        Button logout = new Button("Log out", new FontIcon("fth-log-out"));
        logout.setOnAction(e -> Navigator.logout());
        HBox account = new HBox(10, profile, logout);
        root.getChildren().add(section("fth-user", "Account", user.getFullName() + " · " + user.getEmail(), account));

        if (SessionContext.getInstance().isStudent()) {
            Button wipe = new Button("Delete my activity history", new FontIcon("fth-trash-2"));
            wipe.getStyleClass().add("danger");
            Label done = Ui.label("", "muted", "small");
            wipe.setOnAction(e -> {
                if (Ui.confirm("Delete activity history?",
                    "All tracked app/website usage will be removed permanently. Tasks, focus sessions and goals are kept.",
                    "Delete")) {
                    new BaseDao() {
                        int wipe() {
                            return update("DELETE FROM activity_logs WHERE user_id = ?", uid);
                        }
                    }.wipe();
                    done.setText("Activity history deleted.");
                }
            });
            HBox dataRow = new HBox(12, wipe, done);
            dataRow.setAlignment(Pos.CENTER_LEFT);
            root.getChildren().add(section("fth-database", "Your data", "Stored only on this computer in data/ekagra.db.", dataRow));
        }

        root.getChildren().add(section("fth-info", "About", MainApp.APP_NAME + " 1.0  ·  CT-263 Object-Oriented Analysis & Design",
            Ui.label("Student Productivity & Digital Wellbeing Monitoring System. Java 21, JavaFX, SQLite.", "muted", "small")));
    }

    // ---------- building blocks ----------

    private static VBox section(String icon, String title, String subtitle, Node... rows) {
        StackPane ic = new StackPane(new FontIcon(icon));
        ic.getStyleClass().add("goal-icon");
        VBox text = new VBox(2, Ui.label(title, "h3"), Ui.label(subtitle, "muted", "small"));
        HBox head = new HBox(12, ic, text);
        head.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(12, head);
        for (Node r : rows) {
            VBox.setMargin(r, new javafx.geometry.Insets(0, 0, 0, 48));
            box.getChildren().add(r);
        }
        box.getStyleClass().add("card");
        return box;
    }

    private HBox toggle(String title, String help, String key, boolean def) {
        CheckBox cb = new CheckBox();
        cb.setSelected(settings.getBoolean(uid, key, def));
        cb.selectedProperty().addListener((o, a, b) -> settings.set(uid, key, b));
        return row(title, help, cb);
    }

    private static HBox row(String title, String help, Node control) {
        VBox text = new VBox(2, Ui.label(title, "score-part-name"), Ui.label(help, "muted", "small"));
        ((Label) text.getChildren().get(1)).setWrapText(true);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox row = new HBox(16, text, control);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox linkRow(String title, String button, Screen target) {
        Button b = new Button(button, new FontIcon("fth-arrow-right"));
        b.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        b.setOnAction(e -> Navigator.goTo(target));
        return row(title, "", b);
    }

    private HBox themeRow() {
        ToggleButton light = new ToggleButton("Light", new FontIcon("fth-sun"));
        ToggleButton dark = new ToggleButton("Dark", new FontIcon("fth-moon"));
        light.getStyleClass().add("left-pill");
        dark.getStyleClass().add("right-pill");
        ToggleGroup g = new ToggleGroup();
        light.setToggleGroup(g);
        dark.setToggleGroup(g);
        (ThemeManager.isDark() ? dark : light).setSelected(true);
        g.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null) {
                g.selectToggle(old);
            } else if ((now == dark) != ThemeManager.isDark()) {
                Navigator.toggleTheme();
            }
        });
        return row("Theme", "Dark mode is easier on the eyes at night.", new HBox(light, dark));
    }

    private HBox strategyRow() {
        AnalyticsService a = new AnalyticsService(SessionContext.getInstance().getStudent());
        ComboBox<ScoreStrategy> box = new ComboBox<>(FXCollections.observableArrayList(ScoreStrategy.all()));
        box.setCellFactory(lv -> strategyCell());
        box.setButtonCell(strategyCell());
        box.getItems().stream().filter(s -> s.getId().equals(a.getStrategy().getId())).findFirst().ifPresent(box::setValue);
        box.setPrefWidth(200);
        Label help = Ui.label(a.getStrategy().getDescription(), "muted", "small");
        box.valueProperty().addListener((o, x, s) -> {
            if (s != null) {
                a.setStrategy(s);
                help.setText(s.getDescription());
            }
        });
        HBox r = row("Scoring mode", "", box);
        ((VBox) r.getChildren().get(0)).getChildren().set(1, help);
        help.setWrapText(true);
        return r;
    }

    private static ListCell<ScoreStrategy> strategyCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ScoreStrategy s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : s.getName());
            }
        };
    }

    private HBox idleRow() {
        Spinner<Integer> sp = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30,
            settings.getInt(uid, SettingsService.IDLE_THRESHOLD_MIN, 5)));
        sp.setEditable(true);
        sp.setPrefWidth(140);
        Ui.spinner(sp);
        sp.valueProperty().addListener((o, a, b) -> {
            settings.set(uid, SettingsService.IDLE_THRESHOLD_MIN, b);
            if (TrackingService.isStarted()) {
                TrackingService.get().setIdleMinutes(b);
            }
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.NEVER);
        return row("Count me as away after (min)", "No keyboard or mouse input for this long pauses screen-time counting.", sp);
    }
}
