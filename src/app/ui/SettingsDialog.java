package app.ui;

import app.theme.Theme;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SettingsDialog {
    private final Stage parent;

    public SettingsDialog(Stage parent) { this.parent = parent; }

    public void show() {
        Stage dlg = new Stage();
        dlg.initOwner(parent);
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Settings — Algorithm Lab");

        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("app-bg");

        Label title = new Label("Settings");
        title.getStyleClass().add("h1");
        Label sub = new Label("Personalize the Algorithm Lab experience");
        sub.getStyleClass().add("dim");
        VBox head = new VBox(2, title, sub);

        root.getChildren().addAll(
                head,
                section("Appearance",
                        switchRow("Dark Mode", Theme.isDark(), v -> {
                            if (v != Theme.isDark()) Theme.toggle();
                        }),
                        switchRow("Particle Effects", true, v -> {}),
                        switchRow("Reduced Motion", false, v -> {})
                ),
                section("Visualization",
                        switchRow("Show Labels", true, v -> {}),
                        switchRow("Show Complexity", true, v -> {}),
                        switchRow("Show Code Panel", true, v -> {}),
                        switchRow("Show Recursion View", true, v -> {}),
                        switchRow("Show Metrics", true, v -> {})
                ),
                section("Performance",
                        labeled("Animation FPS", slider(30, 144, 60)),
                        labeled("Animation Intensity", slider(0, 100, 70))
                )
        );

        Button close = new Button("Close");
        close.getStyleClass().addAll("btn", "btn-primary");
        close.setOnAction(e -> dlg.close());
        HBox actions = new HBox(close);
        actions.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().add(actions);

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("scroll-pane");

        Scene scene = new Scene(sp, 480, 620);
        Theme.register(scene);
        dlg.setScene(scene);
        dlg.showAndWait();
    }

    /** Card-style section with a title and content rows. */
    private VBox section(String name, Node... children) {
        VBox box = new VBox(6);
        box.getStyleClass().add("panel");
        Label l = new Label(name);
        l.getStyleClass().add("h3");
        l.setPadding(new Insets(0, 0, 4, 0));
        box.getChildren().add(l);
        box.getChildren().addAll(children);
        return box;
    }

    /** A modern row: [label ......... toggle-switch] */
    private HBox switchRow(String label, boolean init, java.util.function.Consumer<Boolean> onChange) {
        Label l = new Label(label);
        l.getStyleClass().add("nav-text");

        StackPane toggle = buildToggle(init, onChange);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(10, l, spacer, toggle);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 4, 8, 4));
        return row;
    }

    /** The actual animated toggle switch. */
    private StackPane buildToggle(boolean init, java.util.function.Consumer<Boolean> onChange) {
        Region track = new Region();
        track.setPrefSize(42, 24);
        track.setMinSize(42, 24);
        track.setMaxSize(42, 24);

        Circle thumb = new Circle(9);
        thumb.setMouseTransparent(true);

        StackPane toggle = new StackPane(track, thumb);
        toggle.setPrefSize(42, 24);
        toggle.setMinSize(42, 24);
        toggle.setMaxSize(42, 24);
        toggle.setStyle("-fx-cursor: hand;");

        final boolean[] state = { init };

        Runnable paint = () -> {
            String trackColor = state[0] ? web(Theme.accent()) : web(Theme.panel3());
            track.setStyle(
                    "-fx-background-color: " + trackColor + ";" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-color: " + web(Theme.border()) + ";" +
                    "-fx-border-radius: 12;" +
                    "-fx-border-width: 1;");
            thumb.setFill(state[0] ? Color.WHITE : Theme.textDim());
            TranslateTransition t = new TranslateTransition(Duration.millis(160), thumb);
            t.setToX(state[0] ? 9 : -9);
            t.play();
        };
        paint.run();

        toggle.setOnMouseClicked(e -> {
            state[0] = !state[0];
            paint.run();
            onChange.accept(state[0]);
        });

        return toggle;
    }

    private VBox labeled(String label, Node control) {
        VBox v = new VBox(4);
        Label l = new Label(label);
        l.getStyleClass().add("dim");
        v.getChildren().addAll(l, control);
        v.setPadding(new Insets(6, 0, 6, 0));
        return v;
    }

    private Slider slider(double min, double max, double init) {
        Slider s = new Slider(min, max, init);
        s.setShowTickLabels(true);
        s.setShowTickMarks(true);
        s.setBlockIncrement((max - min) / 10);
        return s;
    }

    private String web(Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }
}