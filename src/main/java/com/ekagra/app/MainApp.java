package com.ekagra.app;

import com.ekagra.db.DatabaseManager;
import com.ekagra.ui.LogoMark;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainApp extends Application {

    public static final String APP_NAME = "Ekagra";

    @Override
    public void start(Stage stage) {
        DatabaseManager.getInstance();          // creates data/ekagra.db + tables on first run
        ThemeManager.apply(false);

        stage.setTitle(APP_NAME);
        stage.getIcons().addAll(LogoMark.icon(16), LogoMark.icon(32), LogoMark.icon(64), LogoMark.icon(128));
        Navigator.init(stage);
        Navigator.showLogin();
        fitToScreen(stage);
        stage.show();
    }

    /** Sizes the window to the usable screen area so nothing is cut off on small/scaled displays. */
    private static void fitToScreen(Stage stage) {
        Rectangle2D area = Screen.getPrimary().getVisualBounds();
        stage.setMinWidth(Math.min(960, area.getWidth()));
        stage.setMinHeight(Math.min(600, area.getHeight()));
        if (area.getWidth() < 1500 || area.getHeight() < 900) {
            stage.setX(area.getMinX());
            stage.setY(area.getMinY());
            stage.setWidth(area.getWidth());
            stage.setHeight(area.getHeight());
            stage.setMaximized(true);
        } else {
            stage.setWidth(1400);
            stage.setHeight(880);
            stage.centerOnScreen();
        }
    }

    @Override
    public void stop() {
        Navigator.shutdown();
        DatabaseManager.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
