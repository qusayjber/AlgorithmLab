package app.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import app.theme.Theme;

public class StatusBar extends HBox {
    private final Label status = new Label("Engine Ready");
    private final Label operations = new Label("Operations: 0");
    private final Label complexity = new Label("Complexity: O(1)");
    private final Circle indicator = new Circle(4, Theme.success());

    public StatusBar() {
        super(16);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("statusbar");
        setPadding(new Insets(6, 16, 6, 16));

        status.getStyleClass().add("dim");
        operations.getStyleClass().add("dim");
        complexity.getStyleClass().add("dim");

        HBox left = new HBox(8, indicator, status);
        left.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(left, spacer, operations, sep(), complexity);
    }

    private Region sep() {
        Region r = new Region();
        r.setPrefWidth(1); r.setPrefHeight(14);
        r.setBackground(new Background(new BackgroundFill(Theme.border(), CornerRadii.EMPTY, Insets.EMPTY)));
        return r;
    }

    public Label status() { return status; }
    public Label operations() { return operations; }
    public Label complexity() { return complexity; }
}