package app.ui;

import app.icons.Icons;
import app.theme.Theme;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;

public class Sidebar extends VBox {
    private final Map<String, HBox> items = new LinkedHashMap<>();
    private final VBox navContainer = new VBox(2);
    private final MainWindow window;
    private final VBox scrollContent = new VBox(4);
    private StackPane contentHost;
    private boolean collapsed = false;
    private final Label collapsedToggle = new Label("›");
    private String current = "dashboard";

    public Sidebar(MainWindow window) {
        this.window = window;
        getStyleClass().add("sidebar");
        setPrefWidth(232);
        setMinWidth(232);
        setSpacing(0);

        addSection("MAIN");
        addItem("dashboard", "Dashboard", "dashboard");
        addSection("ALGORITHMS");
        addItem("hanoi", "Tower of Hanoi", "hanoi");
        addItem("sorting", "Sorting Lab", "sorting");
        addItem("searching", "Searching Lab", "search");
        addItem("graph", "Graph Traversal", "graph");
        addItem("pathfinding", "Pathfinding", "path");
        addItem("recursion", "Recursion Explorer", "recursion");
        addItem("ds", "Data Structures", "ds");
        addSection("LAB TOOLS");
        addItem("compare", "Comparison", "compare");
        addItem("metrics", "Performance", "metrics");
        addItem("playground", "Playground", "playground");

        ScrollPane sp = new ScrollPane(scrollContent);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.getStyleClass().add("scroll-pane");
        VBox.setVgrow(sp, Priority.ALWAYS);
        getChildren().add(sp);

        Button collapseBtn = new Button();
        collapseBtn.setGraphic(Icons.get("chevron", 14));
        collapseBtn.getStyleClass().add("icon-btn");
        collapseBtn.setOnAction(e -> toggleCollapse());
        HBox bottom = new HBox(collapseBtn);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(6, 0, 0, 0));
        getChildren().add(bottom);
    }

    public void setContentHost(StackPane host) { this.contentHost = host; }

    private void addSection(String title) {
        Label l = new Label(title);
        l.getStyleClass().add("section-label");
        scrollContent.getChildren().add(l);
    }

    private void addItem(String key, String label, String icon) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("nav-item");
        row.setPadding(new Insets(8, 12, 8, 12));

        Node ic = Icons.get(icon, 16);
        Label text = new Label(label);
        text.getStyleClass().add("nav-text");

        row.getChildren().addAll(ic, text);
        row.setOnMouseClicked(e -> select(key));
        row.setOnMouseEntered(e -> {
            if (!key.equals(current)) row.setTranslateX(2);
        });
        row.setOnMouseExited(e -> row.setTranslateX(0));

        scrollContent.getChildren().add(row);
        items.put(key, row);
    }

    public void select(String key) {
        if (!items.containsKey(key)) return;
        current = key;
        for (Map.Entry<String, HBox> e : items.entrySet()) {
            e.getValue().getStyleClass().remove("active");
            if (e.getKey().equals(key)) e.getValue().getStyleClass().add("active");
        }
        if (contentHost == null) return;
        Node view = buildView(key);
        window.showView(view);
    }

    public String current() { return current; }

    private Node buildView(String key) {
        switch (key) {
            case "dashboard":   return window.dashboard();
            case "hanoi":       return new app.ui.hanoi.HanoiView();
            case "sorting":     return new app.ui.sorting.SortingView();
            case "searching":   return new app.ui.searching.SearchingView();
            case "graph":       return new app.ui.graph.GraphView();
            case "pathfinding": return new app.ui.pathfinding.PathfindingView();
            case "recursion":   return new app.ui.recursion.RecursionView();
            case "ds":          return new app.ui.datastructures.DSView();
            case "compare":     return new app.ui.sorting.ComparisonView();
            case "metrics":     return new app.ui.MetricsView();
            case "playground":  return new app.ui.recursion.RecursionView();
            default:            return window.dashboard();
        }
    }

    private void toggleCollapse() {
        collapsed = !collapsed;
        double target = collapsed ? 64 : 232;
        Timeline t = new Timeline(new KeyFrame(Duration.millis(220),
                new KeyValue(prefWidthProperty(), target, Interpolator.EASE_BOTH),
                new KeyValue(minWidthProperty(), target, Interpolator.EASE_BOTH)));
        t.play();
        for (HBox r : items.values()) {
            for (Node n : r.getChildren()) {
                if (n instanceof Label) n.setVisible(!collapsed);
            }
        }
    }
}