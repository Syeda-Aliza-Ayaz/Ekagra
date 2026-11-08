package com.ekagra.ui;

import com.ekagra.model.FocusSession;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

/** "How focused were you?" 1-5 stars after a completed focus session (FR-14). */
public class RatingDialog extends Dialog<Integer> {

    private static final String[] LABELS = {"", "Very distracted", "Distracted", "Okay", "Focused", "Deep focus"};
    private int rating;

    public RatingDialog(FocusSession session) {
        setTitle("Session complete");
        Ui.style(this);

        Label title = Ui.label("Nice work! 🎉", "h2");
        int mins = session.getActualMinutes();
        Label sub = Ui.label("You focused for " + mins + (mins == 1 ? " minute" : " minutes")
            + (session.getTaskTitle() != null ? " on " + session.getTaskTitle() : "")
            + ". How focused did you feel?", "muted");
        sub.setWrapText(true);
        Label hint = Ui.label(" ", "small", "muted");

        List<Button> stars = new ArrayList<>();
        HBox starRow = new HBox(6);
        starRow.setAlignment(Pos.CENTER);
        for (int i = 1; i <= 5; i++) {
            int value = i;
            Button star = new Button(null, new FontIcon("fth-star"));
            star.getStyleClass().addAll("icon-button", "star");
            star.setOnAction(e -> {
                rating = value;
                for (int j = 0; j < stars.size(); j++) {
                    stars.get(j).getStyleClass().remove("on");
                    if (j < value) {
                        stars.get(j).getStyleClass().add("on");
                    }
                }
                hint.setText(LABELS[value]);
            });
            stars.add(star);
            starRow.getChildren().add(star);
        }

        VBox content = new VBox(12, title, sub, starRow, hint);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new javafx.geometry.Insets(6, 8, 6, 8));
        content.setPrefWidth(380);
        getDialogPane().setContent(content);

        ButtonType save = new ButtonType("Save rating", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType skip = new ButtonType("Skip", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(save, skip);
        getDialogPane().lookupButton(save).getStyleClass().add("accent");
        setResultConverter(bt -> bt == save && rating > 0 ? rating : null);
    }
}
