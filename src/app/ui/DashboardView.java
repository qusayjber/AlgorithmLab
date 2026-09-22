package app.ui;

import app.icons.Icons;
import app.theme.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

public class DashboardView extends ScrollPane {
    public DashboardView(MainWindow window) {
        setFitToWidth(true);
        getStyleClass().add("scroll-pane");

        VBox root = new VBox(24);
        root.setPadding(new Insets(32, 36, 32, 36));
        root.getStyleClass().add("app-bg");

        Label hero = new Label("ALGORITHM LAB");
        hero.getStyleClass().add("h1");

        Label sub = new Label("Understand algorithms by watching them work.");
        sub.getStyleClass().add("dim");
        sub.setStyle("-fx-font-size: 14px;");

        VBox intro = new VBox(6, hero, sub);
        root.getChildren().add(intro);

        FlowPane cards = new FlowPane(18, 18);
        cards.setPrefWrapLength(1200);

        cards.getChildren().addAll(
            card("Hanoi", "Tower of Hanoi", "Recursive puzzle solver", "O(2ⁿ) time", "O(n) space", "hanoi", window),
            card("Sorting", "Sorting Laboratory", "Six classic sorting algorithms", "O(n²) – O(n log n)", "O(n) space", "sorting", window),
            card("Search", "Searching Laboratory", "Linear and binary search", "O(log n) best case", "O(1) space", "searching", window),
            card("Graph", "Graph Traversal", "BFS & DFS on interactive graphs", "O(V + E)", "O(V) space", "graph", window),
            card("Path", "Pathfinding", "BFS • Dijkstra • A*", "O(E log V)", "O(V) space", "pathfinding", window),
            card("Recursion", "Recursion Explorer", "Watch the stack unfold", "varies", "O(depth)", "recursion", window),
            card("Ds", "Data Structure Playground", "Stacks, queues, trees, heaps", "varies", "O(n)", "ds", window),
            card("Compare", "Algorithm Comparison", "Race algorithms side-by-side", "compare", "compare", "compare", window)
        );

        root.getChildren().add(cards);
        setContent(root);
    }

    private Node card(String key, String title, String subtitle, String time, String space, String icon, MainWindow window) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(300);
        card.setMinHeight(180);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Node ic = Icons.get(icon.toLowerCase().equals("compare") ? "compare" :
                icon.toLowerCase().equals("searching") ? "search" :
                icon.toLowerCase().equals("pathfinding") ? "path" :
                icon.toLowerCase().equals("recursion") ? "recursion" :
                icon.toLowerCase().equals("graph") ? "graph" :
                icon.toLowerCase().equals("sorting") ? "sorting" :
                icon.toLowerCase().equals("ds") ? "ds" : "hanoi", 20);
        Label t = new Label(title);
        t.getStyleClass().add("h2");
        header.getChildren().addAll(ic, t);

        Label desc = new Label(subtitle);
        desc.getStyleClass().add("dim");
        desc.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox meta = new HBox(8);
        Label timeChip = new Label("Time " + time);
        timeChip.getStyleClass().addAll("chip", "chip-accent");
        Label spaceChip = new Label("Space " + space);
        spaceChip.getStyleClass().add("chip");
        meta.getChildren().addAll(timeChip, spaceChip);

        Label launch = new Label("Launch Visualization  →");
        launch.setStyle("-fx-text-fill: " + toWeb(Theme.accent()) + "; -fx-font-weight: 700;");

        card.getChildren().addAll(header, desc, spacer, meta, launch);
        card.setOnMouseClicked(e -> window.navigate(mapKey(key)));
        return card;
    }

    private String mapKey(String icon) {
        switch (icon) {
            case "Search": return "searching";
            case "Graph": return "graph";
            case "Path": return "pathfinding";
            case "Recursion": return "recursion";
            case "Ds": return "ds";
            case "Compare": return "compare";
            case "Sorting": return "sorting";
            default: return "hanoi";
        }
    }

    private String toWeb(javafx.scene.paint.Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }
}