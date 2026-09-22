package app.ui;

import app.ApplicationController;
import app.icons.Icons;
import app.theme.Theme;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainWindow {
    private final Stage stage;
    private final BorderPane root = new BorderPane();
    private final StackPane contentHolder = new StackPane();
    private final Sidebar sidebar;
    private final TopBar topbar;
    private final StatusBar statusbar;
    private final DashboardView dashboard;
    private final Label statusLabel;
    private final Label opsLabel;
    private final Label complexityLabel;
    private final StackPane overlayHost = new StackPane();

    public MainWindow(Stage stage) {
        this.stage = stage;
        this.topbar = new TopBar(this);
        this.statusbar = new StatusBar();
        this.dashboard = new DashboardView(this);
        this.sidebar = new Sidebar(this);

        this.statusLabel = statusbar.status();
        this.opsLabel = statusbar.operations();
        this.complexityLabel = statusbar.complexity();

        // ----- Header -----
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("topbar");

        StackPane logoBox = new StackPane();
        Polygon diamond = new Polygon(0, -11, 11, 0, 0, 11, -11, 0);
        diamond.setFill(Theme.accent());
        diamond.setStroke(Theme.accent2());
        diamond.setStrokeWidth(1.5);
        Circle dot = new Circle(3, Theme.panel());
        logoBox.getChildren().addAll(diamond, dot);
        logoBox.setPrefSize(26, 26);

        VBox brandText = new VBox(-2);
        Label t1 = new Label("ALGORITHM LAB");
        t1.getStyleClass().add("brand-title");
        Label t2 = new Label("Interactive Algorithm Visualization");
        t2.getStyleClass().add("brand-sub");
        brandText.getChildren().addAll(t1, t2);

        HBox brand = new HBox(10, logoBox, brandText);
        brand.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // >>> FIXED: use the TopBar's search box (with dropdown) <<<
        Region searchBox = topbar.buildSearchBox();

        Button themeBtn = iconButton("theme", "Toggle theme (Ctrl+D)");
        themeBtn.setOnAction(e -> Theme.toggle());

        Button settingsBtn = iconButton("settings", "Settings");
        settingsBtn.setOnAction(e -> new SettingsDialog(stage).show());

        HBox actions = new HBox(6, searchBox, themeBtn, settingsBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(brand, spacer, actions);

        // ----- Content -----
        contentHolder.setPadding(javafx.geometry.Insets.EMPTY);
        StackPane.setAlignment(contentHolder, Pos.TOP_LEFT);
        sidebar.setContentHost(contentHolder);

        HBox body = new HBox(sidebar, contentHolder);
        HBox.setHgrow(contentHolder, Priority.ALWAYS);

        BorderPane main = new BorderPane();
        main.setTop(header);
        main.setCenter(body);
        main.setBottom(statusbar);

        overlayHost.getChildren().add(main);
        overlayHost.getChildren().add(buildSplash());

        root.setCenter(overlayHost);
        ApplicationController.setWindow(this);

        Scene scene = new Scene(root, 1280, 800);
        Theme.register(scene);
        stage.setScene(scene);
        stage.setTitle("Algorithm Lab — Interactive Algorithm Visualization");
        stage.setMinWidth(1000);
        stage.setMinHeight(650);

        installShortcuts(scene);
        sidebar.select("dashboard");
    }

    private Node buildSplash() {
        StackPane splash = new StackPane();
        splash.setStyle("-fx-background-color: " + toRgba(Theme.bg()));
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        StackPane logoBox = new StackPane();
        Polygon diamond = new Polygon(0, -32, 32, 0, 0, 32, -32, 0);
        diamond.setFill(Theme.accent());
        diamond.setStroke(Theme.accent2());
        diamond.setStrokeWidth(2.5);
        Circle dot = new Circle(6, Theme.accent2());
        logoBox.getChildren().addAll(diamond, dot);
        logoBox.setPrefSize(80, 80);

        Label title = new Label("ALGORITHM LAB");
        title.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 34));
        title.setTextFill(Theme.text());
        Label sub = new Label("Interactive Algorithm Visualization Laboratory");
        sub.setFont(Font.font("System", 13));
        sub.setTextFill(Theme.textDim());
        box.getChildren().addAll(logoBox, title, sub);
        splash.getChildren().add(box);

        FadeTransition in = new FadeTransition(Duration.millis(320), splash);
        in.setFromValue(0); in.setToValue(1);
        PauseTransition hold = new PauseTransition(Duration.millis(900));
        FadeTransition out = new FadeTransition(Duration.millis(400), splash);
        out.setFromValue(1); out.setToValue(0);
        SequentialTransition seq = new SequentialTransition(in, hold, out);
        seq.setOnFinished(e -> overlayHost.getChildren().remove(splash));
        seq.play();
        return splash;
    }

    private static String toRgba(Color c) {
        return String.format("rgba(%d,%d,%d,%.3f)",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255), c.getOpacity());
    }

    private Button iconButton(String icon, String tooltip) {
        Button b = new Button();
        b.setGraphic(Icons.get(icon, 18));
        b.getStyleClass().add("icon-btn");
        b.setTooltip(new Tooltip(tooltip));
        return b;
    }

    private void installShortcuts(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN),
                Theme::toggle);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.K, KeyCombination.CONTROL_DOWN),
                () -> topbar.focusSearch());
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN),
                () -> new SettingsDialog(stage).show());
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN),
                () -> sidebar.select("dashboard"));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.CONTROL_DOWN),
                () -> sidebar.select("hanoi"));
    }

    public void navigate(String key) { sidebar.select(key); }

    public void showView(Node view) {
        contentHolder.getChildren().setAll(view);
        FadeTransition ft = new FadeTransition(Duration.millis(220), view);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }

    public DashboardView dashboard() { return dashboard; }
    public Sidebar sidebar() { return sidebar; }

    public void setStatus(String s) { statusLabel.setText(s); }
    public void setOperations(long n) { opsLabel.setText("Operations: " + n); }
    public void setComplexity(String s) { complexityLabel.setText("Complexity: " + s); }

    public void show() { stage.show(); }
}