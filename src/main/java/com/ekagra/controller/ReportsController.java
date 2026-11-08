package com.ekagra.controller;

import com.ekagra.app.Navigator;
import com.ekagra.model.Report;
import com.ekagra.model.Student;
import com.ekagra.pattern.factory.ExporterFactory.Format;
import com.ekagra.service.ReportService;
import com.ekagra.service.SessionContext;
import com.ekagra.ui.Messages;
import com.ekagra.ui.Ui;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/** Reports & exports screen (FR-36, FR-37). */
public class ReportsController {

    @FXML private ToggleButton thisWeek;
    @FXML private ToggleButton lastWeek;
    @FXML private Label periodLabel;
    @FXML private FlowPane previewStats;
    @FXML private CheckBox openAfter;
    @FXML private VBox exportRows;
    @FXML private Label resultMessage;
    @FXML private HBox resultActions;
    @FXML private Button openFileButton;

    private Student student;
    private ReportService reports;
    private Report weekly;
    private LocalDate monday;
    private Path lastFile;

    @FXML
    private void initialize() {
        student = SessionContext.getInstance().getStudent();
        reports = new ReportService(student);

        ToggleGroup g = new ToggleGroup();
        thisWeek.setToggleGroup(g);
        lastWeek.setToggleGroup(g);
        g.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null) {
                g.selectToggle(old);
            } else {
                loadWeek();
            }
        });
        loadWeek();

        exportRow("fth-check-square", "Tasks", "All tasks with status, deadlines and time spent",
            () -> reports.tasksData(), "ekagra-tasks");
        exportRow("fth-clock", "Focus sessions", "Every focus session and break, last 30 days",
            () -> reports.sessionsData(30), "ekagra-focus-sessions");
        exportRow("fth-monitor", "Activity log", "Apps and websites used, last 30 days",
            () -> reports.activityData(30), "ekagra-activity");
    }

    private void loadWeek() {
        LocalDate thisMonday = ReportService.weekStart(LocalDate.now());
        monday = lastWeek.isSelected() ? thisMonday.minusWeeks(1) : thisMonday;
        LocalDate end = lastWeek.isSelected() ? monday.plusDays(6) : LocalDate.now();
        periodLabel.setText(monday.format(DateTimeFormatter.ofPattern("EEE d MMM")) + "  to  "
            + end.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy")));
        weekly = reports.weekly(monday);
        previewStats.getChildren().clear();
        weekly.sections().get(0).stats().forEach(s -> {
            VBox box = new VBox(2, Ui.label(s.label(), "muted", "small"), Ui.label(s.value(), "preview-value"));
            box.getStyleClass().add("preview-stat");
            previewStats.getChildren().add(box);
        });
    }

    private void exportRow(String icon, String title, String desc, Supplier<Report> data, String fileBase) {
        StackPane ic = new StackPane(new FontIcon(icon));
        ic.getStyleClass().add("goal-icon");
        Label d = Ui.label(desc, "muted", "small");
        d.setWrapText(true);
        VBox text = new VBox(2, Ui.label(title, "app-name"), d);
        text.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);
        Button b = new Button("Export");
        b.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        b.setGraphic(new FontIcon("fth-download"));
        b.setOnAction(e -> save(data.get(), Format.CSV, fileBase + "-" + LocalDate.now()));
        HBox row = new HBox(12, ic, text, b);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-row");
        exportRows.getChildren().add(row);
    }

    @FXML
    private void onWeeklyPdf() {
        save(weekly, Format.PDF, "ekagra-weekly-report-" + monday);
    }

    @FXML
    private void onWeeklyCsv() {
        save(weekly, Format.CSV, "ekagra-weekly-report-" + monday);
    }

    private void save(Report report, Format format, String baseName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save " + report.title());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(format.getDescription(), format.getPattern()));
        chooser.setInitialFileName(baseName + "." + format.name().toLowerCase());
        File dir = defaultFolder();
        if (dir != null) {
            chooser.setInitialDirectory(dir);
        }
        File file = chooser.showSaveDialog(Navigator.getStage());
        if (file == null) {
            return;
        }
        // generating runs in the background so the window never freezes (the first PDF builds a font cache)
        Messages.success(resultMessage, "Generating " + file.getName() + "…");
        resultActions.setVisible(false);
        resultActions.setManaged(false);
        Ui.background(() -> {
            reports.write(report, format, file.toPath());
            return file.toPath();
        }, path -> {
            lastFile = path;
            Messages.success(resultMessage, "Saved " + file.getName() + " to " + file.getParent());
            resultActions.setVisible(true);
            resultActions.setManaged(true);
            if (openAfter.isSelected() && format == Format.PDF) {
                onOpenFile();
            }
        }, err -> Messages.error(resultMessage, "Couldn't save the file: " + err.getMessage()));
    }

    /** Documents/Ekagra Reports, created if needed. */
    private static File defaultFolder() {
        try {
            Path p = Path.of(System.getProperty("user.home"), "Documents", "Ekagra Reports");
            Files.createDirectories(p);
            return p.toFile();
        } catch (IOException | SecurityException e) {
            return null;
        }
    }

    @FXML
    private void onOpenFile() {
        open(lastFile == null ? null : lastFile.toFile());
    }

    @FXML
    private void onOpenFolder() {
        open(lastFile == null ? null : lastFile.getParent().toFile());
    }

    private void open(File f) {
        if (f == null) {
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(f);
            }
        } catch (IOException e) {
            Messages.error(resultMessage, "Couldn't open " + f.getName() + ": " + e.getMessage());
        }
    }
}
