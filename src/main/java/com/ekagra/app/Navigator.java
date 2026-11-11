package com.ekagra.app;

import com.ekagra.controller.Disposable;
import com.ekagra.controller.MainLayoutController;
import com.ekagra.model.Student;
import com.ekagra.service.DeadlineWatcher;
import com.ekagra.service.FocusService;
import com.ekagra.service.GamificationService;
import com.ekagra.service.NotificationService;
import com.ekagra.service.TrackingService;
import com.ekagra.ui.DesktopToast;
import com.ekagra.service.SessionContext;
import com.ekagra.service.SettingsService;
import com.ekagra.ui.Animations;
import com.ekagra.ui.PlaceholderView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

/** Central place that swaps screens. Controllers call Navigator instead of loading FXML themselves. */
public final class Navigator {

    private static final String VIEW_PATH = "/com/ekagra/view/";
    private static final String CSS = "/com/ekagra/css/app.css";

    private static Stage stage;
    private static Scene scene;
    private static MainLayoutController mainLayout;
    private static Screen current;
    private static Object currentController;

    private Navigator() {
    }

    public static void init(Stage primaryStage) {
        stage = primaryStage;
        scene = new Scene(new StackPane());
        scene.getStylesheets().add(resource(CSS).toExternalForm());
        Animations.installPressFeedback(scene);
        stage.setScene(scene);
    }

    public static void showLogin() {
        setRoot(load("Login.fxml").root());
        mainLayout = null;
    }

    public static void showSignup() {
        setRoot(load("Signup.fxml").root());
    }

    /** Called after a successful login. */
    public static void showMain() {
        var user = SessionContext.getInstance().getCurrentUser();
        ThemeManager.initFromUser(user.getId());

        Loaded<MainLayoutController> loaded = load("MainLayout.fxml");
        mainLayout = loaded.controller();
        setRoot(loaded.root());
        goTo(Screen.homeFor(user));
        if (SessionContext.getInstance().isStudent()) {
            startStudentServices(SessionContext.getInstance().getStudent());
        }
    }

    private static GamificationService gamification;
    private static DeadlineWatcher deadlineWatcher;

    /** Background helpers that run while a student is logged in. */
    private static void startStudentServices(Student student) {
        TrackingService.get();                       // activity tracking (background thread)
        gamification = new GamificationService(student);
        gamification.setNotifier(list -> {
            NotificationService.notifyAll(list);
            refreshUserCard();                       // level / XP shown in the sidebar
        });
        gamification.start();
        java.util.List<com.ekagra.model.Notification> catchUp = gamification.checkBadges();
        if (!catchUp.isEmpty()) {
            NotificationService.notifyAll(catchUp);
        }
        deadlineWatcher = new DeadlineWatcher(student.getId());
        deadlineWatcher.start();
    }

    private static void stopStudentServices() {
        if (gamification != null) {
            gamification.stop();
            gamification = null;
        }
        if (deadlineWatcher != null) {
            deadlineWatcher.stop();
            deadlineWatcher = null;
        }
    }

    /** Restores and focuses the main window (used by desktop notifications). */
    public static void bringToFront() {
        stage.setIconified(false);
        stage.toFront();
        stage.requestFocus();
    }

    public static void goTo(Screen screen) {
        if (mainLayout == null) {
            return;
        }
        disposeCurrent();
        Parent view;
        if (screen.getFxml() == null) {
            view = PlaceholderView.create(screen);
            currentController = null;
        } else {
            Loaded<Object> loaded = load(screen.getFxml());
            view = loaded.root();
            currentController = loaded.controller();
        }
        current = screen;
        mainLayout.show(screen, view);
        Animations.smoothScrollAll(view);
        Animations.pageEnter(view);
    }

    /** Reloads the current screen (e.g. after theme change or profile update). */
    public static void refresh() {
        if (mainLayout != null) {
            mainLayout.refreshUserCard();
            goTo(current);
        }
    }

    public static void refreshUserCard() {
        if (mainLayout != null) {
            mainLayout.refreshUserCard();
        }
    }

    public static void toggleTheme() {
        ThemeManager.apply(!ThemeManager.isDark());
        ThemeManager.decorate(scene.getRoot());
        var user = SessionContext.getInstance().getCurrentUser();
        if (user != null) {
            new SettingsService().set(user.getId(), SettingsService.THEME, ThemeManager.isDark() ? "dark" : "light");
        }
    }

    public static void refreshTheme() {
        if (scene != null && scene.getRoot() != null) {
            ThemeManager.decorate(scene.getRoot());
        }
    }

    /** Opens a screen and lets its controller receive an argument (e.g. a task to focus on). */
    public static <C> void goTo(Screen screen, Class<C> controllerType, java.util.function.Consumer<C> setup) {
        goTo(screen);
        if (controllerType.isInstance(currentController)) {
            setup.accept(controllerType.cast(currentController));
        }
    }

    public static void logout() {
        disposeCurrent();
        if (mainLayout != null) {
            mainLayout.dispose();
        }
        stopStudentServices();
        TrackingService.reset();
        FocusService.reset();
        DesktopToast.shutdown();
        SessionContext.getInstance().clear();
        ThemeManager.apply(false);
        showLogin();
    }

    public static void shutdown() {
        stopStudentServices();
        TrackingService.reset();
        FocusService.reset();
        DesktopToast.shutdown();
    }

    private static void disposeCurrent() {
        if (currentController instanceof Disposable d) {
            d.dispose();
        }
        currentController = null;
    }

    public static Stage getStage() {
        return stage;
    }

    public static MainLayoutController getMainLayoutController() {
        return mainLayout;
    }

    public static String stylesheet() {
        return resource(CSS).toExternalForm();
    }

    public static Screen getCurrentScreen() {
        return current;
    }

    /** Reloads a screen only if it is the one currently shown. */
    public static void refreshIfShowing(Screen screen) {
        if (current == screen) {
            goTo(screen);
        }
    }

    private static void setRoot(Parent root) {
        ThemeManager.decorate(root);
        scene.setRoot(root);
        Animations.smoothScrollAll(root);
    }

    public record Loaded<C>(Parent root, C controller) {
    }

    public static <C> Loaded<C> load(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(resource(VIEW_PATH + fxml));
            Parent root = loader.load();
            return new Loaded<>(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load view " + fxml, e);
        }
    }

    private static URL resource(String path) {
        URL url = Navigator.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("Missing resource: " + path);
        }
        return url;
    }
}
