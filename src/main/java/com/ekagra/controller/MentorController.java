package com.ekagra.controller;

import com.ekagra.app.Navigator;
import com.ekagra.dao.ActivityDao;
import com.ekagra.model.ActivityLog;
import com.ekagra.model.Category;
import com.ekagra.model.Mentor;
import com.ekagra.model.Student;
import com.ekagra.model.Task;
import com.ekagra.pattern.factory.ExporterFactory;
import com.ekagra.service.AnalyticsService.DaySummary;
import com.ekagra.service.MentorService;
import com.ekagra.service.MentorService.StudentOverview;
import com.ekagra.service.ReportService;
import com.ekagra.service.SessionContext;
import com.ekagra.service.ValidationException;
import com.ekagra.ui.Animations;
import com.ekagra.ui.Avatar;
import com.ekagra.ui.Charts;
import com.ekagra.ui.Messages;
import com.ekagra.ui.Ui;
import com.ekagra.util.Formats;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** "My Students" screen for mentors (FR-38, FR-39). */
public class MentorController {

    @FXML private StackPane root;

    private Mentor mentor;
    private MentorService service;

    @FXML
    private void initialize() {
        mentor = (Mentor) SessionContext.getInstance().getCurrentUser();
        service = new MentorService(mentor);
        showList();
    }

    // ---------- list ----------

    private void showList() {
        List<StudentOverview> students = service.overview();
        long atRisk = students.stream().filter(StudentOverview::isAtRisk).count();

        // invite code card
        Label code = Ui.label(mentor.getInviteCode(), "code-label");
        Button copy = new Button("Copy code", new FontIcon("fth-copy"));
        copy.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(mentor.getInviteCode());
            Clipboard.getSystemClipboard().setContent(cc);
            copy.setText("Copied!");
        });
        VBox codeText = new VBox(2, Ui.label("Your mentor code", "h3"),
            Ui.label("Students enter this when signing up or on their Profile to link to you.", "muted", "small"));
        HBox.setHgrow(codeText, Priority.ALWAYS);
        HBox codeCard = new HBox(14, codeText, code, copy);
        codeCard.setAlignment(Pos.CENTER_LEFT);
        codeCard.getStyleClass().add("card");

        int avg = (int) Math.round(students.stream().filter(s -> s.week().stream().anyMatch(DaySummary::hasData))
            .mapToInt(StudentOverview::avgScore).average().orElse(0));
        HBox tiles = new HBox(14,
            Ui.statTile(students.size(), "Linked students", "fth-users"),
            Ui.statTile((int) atRisk, "Need attention", "fth-alert-triangle"),
            Ui.statTile(avg, "Average score (7 days)", "fth-bar-chart-2"));
        tiles.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        if (atRisk > 0) {
            tiles.getChildren().get(1).getStyleClass().add("stat-danger");
        }

        VBox list = new VBox(10);
        if (students.isEmpty()) {
            list.getChildren().add(Ui.emptyState("fth-users", "No students yet",
                "Share your mentor code. Students who link to you will appear here."));
        }
        for (StudentOverview o : students) {
            list.getChildren().add(studentCard(o));
        }
        Animations.stagger(list, 10);

        VBox page = new VBox(14, codeCard, tiles, Ui.label("Students", "h3"), list);
        root.getChildren().setAll(page);
    }

    private HBox studentCard(StudentOverview o) {
        Student s = o.student();
        Avatar avatar = new Avatar(s, 44);
        Label name = Ui.label(s.getFullName(), "app-name");
        Label meta = Ui.label(s.getSubtitle() + (s.getUniversity() == null || s.getUniversity().isBlank() ? "" : " · " + s.getUniversity()),
            "muted", "small");
        VBox who = new VBox(2, name, meta);
        HBox.setHgrow(who, Priority.ALWAYS);

        VBox score = metric(String.valueOf(o.avgScore()), "Avg score");
        VBox focus = metric(Formats.minutes(o.focusMinutes()), "Focus (7d)");
        VBox overdue = metric(String.valueOf(o.overdue()), "Overdue");
        VBox last = metric(o.lastActive() == null ? "Never" : NotificationsController.ago(o.lastActive(), LocalDateTime.now()),
            "Last active");

        FlowPane flags = new FlowPane(6, 6);
        flags.setPrefWrapLength(180);
        if (o.isAtRisk()) {
            o.riskReasons().forEach(r -> flags.getChildren().add(Ui.chip(r, "chip-urgent")));
        } else {
            flags.getChildren().add(Ui.chip("On track", "chip-progress"));
        }
        flags.setPrefWidth(190);

        HBox card = new HBox(18, avatar, who, score, focus, overdue, last, flags, new FontIcon("fth-chevron-right"));
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().addAll("card", "task-card", o.isAtRisk() ? "priority-urgent" : "priority-low");
        card.setOnMouseClicked(e -> showDetail(o));
        return card;
    }

    private static VBox metric(String value, String caption) {
        VBox v = new VBox(1, Ui.label(value, "metric-value"), Ui.label(caption, "muted", "small"));
        v.setMinWidth(84);
        v.setAlignment(Pos.CENTER_LEFT);
        return v;
    }

    // ---------- detail ----------

    private void showDetail(StudentOverview o) {
        Student s = o.student();
        Button back = new Button("All students", new FontIcon("fth-arrow-left"));
        back.setOnAction(e -> showList());
        Avatar avatar = new Avatar(s, 52);
        VBox who = new VBox(2, Ui.label(s.getFullName(), "h2"), Ui.label(s.getEmail() + " · " + s.getSubtitle(), "muted"));
        HBox.setHgrow(who, Priority.ALWAYS);
        Button pdf = new Button("Weekly report PDF", new FontIcon("fth-file-text"));
        pdf.setOnAction(e -> downloadReport(s));
        HBox header = new HBox(14, avatar, who, pdf);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("card");

        HBox tiles = new HBox(14,
            Ui.statTile(o.avgScore(), "Avg score (7 days)", "fth-bar-chart-2"),
            Ui.statTile(Formats.minutes(o.focusMinutes()), "Focus time (7 days)", "fth-clock"),
            Ui.statTile(o.overdue(), "Overdue tasks", "fth-alert-circle"),
            Ui.statTile(s.getLevel(), "Level", "fth-award"));
        tiles.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        // charts: score and focus per day (read-only view of the student's data)
        List<Charts.Point> scorePts = o.week().stream().map(d -> new Charts.Point(d.dayLabel(), d.score().total(),
            d.dayLabel() + ": " + (d.hasData() ? "score " + d.score().total() : "no activity"))).toList();
        VBox scoreCard = chartCard("Score, last 7 days", Charts.line(scorePts, 100, "score-chart"));
        List<Charts.Point> focusPts = o.week().stream().map(d -> new Charts.Point(d.dayLabel(), d.focusMinutes(),
            d.dayLabel() + ": " + Formats.minutes(d.focusMinutes()))).toList();
        VBox focusCard = chartCard("Focus minutes per day", Charts.bar(focusPts, "min", "focus-chart"));
        HBox charts = new HBox(14, scoreCard, focusCard);
        charts.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        // overdue tasks + distracting apps
        VBox overdueBox = new VBox(8, Ui.label("Overdue tasks", "h3"));
        List<Task> overdue = service.overdueTasks(s);
        if (overdue.isEmpty()) {
            overdueBox.getChildren().add(Ui.label("Nothing overdue.", "muted"));
        }
        LocalDateTime now = LocalDateTime.now();
        overdue.forEach(t -> overdueBox.getChildren().add(new HBox(8,
            Ui.chip(t.getPriority().getLabel(), t.getPriority().styleClass()),
            Ui.label(t.getTitle() + "  ·  " + Formats.deadline(t.getDeadline(), now), "score-part-name"))));
        overdueBox.getStyleClass().add("card");

        VBox appsBox = new VBox(8, Ui.label("Most used apps (7 days)", "h3"));
        List<ActivityDao.AppUsage> apps = service.topApps(s);
        if (apps.isEmpty()) {
            appsBox.getChildren().add(Ui.label("No tracked activity.", "muted"));
        }
        for (ActivityDao.AppUsage a : apps) {
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            HBox row = new HBox(8, Ui.categoryDot(a.category()), Ui.label(ActivityLog.displayName(a.appName()), "score-part-name"),
                sp, Ui.label(Formats.seconds(a.seconds()) + (a.category() == Category.DISTRACTING ? " · distracting" : ""), "muted", "small"));
            row.setAlignment(Pos.CENTER_LEFT);
            appsBox.getChildren().add(row);
        }
        appsBox.getStyleClass().add("card");
        HBox lists = new HBox(14, overdueBox, appsBox);
        lists.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        // note
        TextArea note = new TextArea();
        note.setPromptText("Write an encouraging note or a reminder, e.g. \"Great focus this week! Don't forget the OOAD report.\"");
        note.setPrefRowCount(3);
        note.setWrapText(true);
        Label msg = new Label();
        Messages.hide(msg);
        Button send = new Button("Send note", new FontIcon("fth-send"));
        send.getStyleClass().add("accent");
        send.setOnAction(e -> {
            try {
                service.sendNote(s, note.getText());
                note.clear();
                Messages.success(msg, "Sent. " + s.getFirstName() + " will see it in their notifications.");
            } catch (ValidationException ex) {
                Messages.error(msg, ex.getMessage());
            }
        });
        VBox noteBox = new VBox(10, Ui.label("Send a note to " + s.getFirstName(), "h3"), note, new HBox(send), msg);
        noteBox.getStyleClass().add("card");

        VBox page = new VBox(14, new HBox(back), header, tiles, charts, lists, noteBox);
        root.getChildren().setAll(page);
        Animations.pageEnter(page);
    }

    private static VBox chartCard(String title, javafx.scene.Node chart) {
        StackPane holder = new StackPane(chart);
        holder.setPrefHeight(210);
        VBox card = new VBox(6, Ui.label(title, "h3"), holder);
        card.getStyleClass().add("card");
        card.setMinWidth(360);
        return card;
    }

    private void downloadReport(Student s) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save weekly report");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF document", "*.pdf"));
        chooser.setInitialFileName("ekagra-" + s.getFirstName().toLowerCase() + "-week-" + LocalDate.now() + ".pdf");
        File f = chooser.showSaveDialog(Navigator.getStage());
        if (f == null) {
            return;
        }
        ReportService rs = new ReportService(s);
        var report = rs.weekly(ReportService.weekStart(LocalDate.now()));
        Ui.background(() -> {
            rs.write(report, ExporterFactory.Format.PDF, f.toPath());
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(f);
            }
            return f;
        }, ok -> { }, err -> Ui.confirm("Couldn't save report", err.getMessage(), "OK"));
    }
}
