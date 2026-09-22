package app.ui;

import app.icons.Icons;
import app.theme.Theme;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Popup;

import java.util.ArrayList;
import java.util.List;

public class TopBar {

    public record Entry(String title, String subtitle, String key, String icon) {}

    private static final List<Entry> CATALOG = List.of(
            new Entry("Tower of Hanoi",            "Recursive puzzle · O(2ⁿ)",                            "hanoi",       "hanoi"),
            new Entry("Sorting Laboratory",        "Bubble · Selection · Insertion · Merge · Quick · Heap","sorting",     "sorting"),
            new Entry("Bubble Sort",               "O(n²) comparison sort",                               "sorting",     "sorting"),
            new Entry("Selection Sort",            "In-place · O(n²)",                                    "sorting",     "sorting"),
            new Entry("Insertion Sort",            "Stable · O(n²)",                                      "sorting",     "sorting"),
            new Entry("Merge Sort",                "Divide & conquer · O(n log n)",                       "sorting",     "sorting"),
            new Entry("Quick Sort",                "Partition-based · O(n log n)",                        "sorting",     "sorting"),
            new Entry("Heap Sort",                 "Binary heap · O(n log n)",                            "sorting",     "sorting"),
            new Entry("Searching Laboratory",      "Linear and binary search",                            "searching",   "search"),
            new Entry("Binary Search",             "O(log n) ordered search",                             "searching",   "search"),
            new Entry("Linear Search",             "O(n) sequential scan",                                "searching",   "search"),
            new Entry("Graph Traversal",           "BFS & DFS · interactive graph",                       "graph",       "graph"),
            new Entry("BFS",                       "Breadth-first traversal",                             "graph",       "graph"),
            new Entry("DFS",                       "Depth-first traversal",                               "graph",       "graph"),
            new Entry("Pathfinding",               "BFS · Dijkstra · A*",                                 "pathfinding", "path"),
            new Entry("Dijkstra",                  "Shortest path",                                       "pathfinding", "path"),
            new Entry("A*",                        "Heuristic shortest path",                             "pathfinding", "path"),
            new Entry("Recursion Explorer",        "Watch the call stack",                                "recursion",   "recursion"),
            new Entry("Factorial",                 "n! via recursion",                                    "recursion",   "recursion"),
            new Entry("Fibonacci",                 "Naive exponential recursion",                         "recursion",   "recursion"),
            new Entry("Data Structure Playground", "Stack · Queue · Linked List · BST · Heap",            "ds",          "ds"),
            new Entry("Stack",                     "LIFO structure",                                      "ds",          "ds"),
            new Entry("Queue",                     "FIFO structure",                                      "ds",          "ds"),
            new Entry("Binary Search Tree",        "Ordered tree",                                        "ds",          "ds"),
            new Entry("Heap",                      "Priority structure",                                  "ds",          "ds"),
            new Entry("Algorithm Comparison",      "Run all sorts on same input",                         "compare",     "compare"),
            new Entry("Performance Monitor",       "Live runtime metrics",                                "metrics",     "metrics")
    );

    private final MainWindow window;
    private final TextField searchField = new TextField();
    private final Popup popup = new Popup();
    private final VBox results = new VBox(2);

    public TopBar(MainWindow w) {
        this.window = w;

        searchField.setPromptText("Search algorithms...  (Ctrl+K)");
        searchField.getStyleClass().add("field");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((o, ov, nv) -> onQuery(nv));
        searchField.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ESCAPE -> { searchField.clear(); popup.hide(); }
                case ENTER -> {
                    List<Entry> hits = matches(searchField.getText());
                    if (!hits.isEmpty()) {
                        window.navigate(hits.get(0).key());
                        popup.hide();
                        searchField.clear();
                    }
                }
                case DOWN -> {
                    if (!results.getChildren().isEmpty()) {
                        results.getChildren().get(0).requestFocus();
                    }
                }
                default -> { }
            }
        });

        results.setPadding(new Insets(6));
        results.setPrefWidth(440);
        stylePopup();
        popup.getContent().add(results);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
    }

    private void stylePopup() {
        results.setStyle(
                "-fx-background-color: " + web(Theme.panel()) + ";" +
                "-fx-border-color: " + web(Theme.border()) + ";" +
                "-fx-border-width: 1;" +
                "-fx-background-radius: 12;" +
                "-fx-border-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 24, 0.15, 0, 8);");
    }

    /** Returns the search field container to be placed in the window header. */
    public Region buildSearchBox() {
        StackPane holder = new StackPane(searchField);
        holder.setPrefWidth(280);
        holder.setMinWidth(200);
        return holder;
    }

    public void focusSearch() {
        searchField.requestFocus();
        searchField.selectAll();
    }

    private void onQuery(String q) {
        results.getChildren().clear();
        if (q == null || q.isBlank()) { popup.hide(); return; }

        List<Entry> hits = matches(q);
        if (hits.isEmpty()) { popup.hide(); return; }

        for (Entry e : hits) results.getChildren().add(makeRow(e));
        stylePopup();

        // Position under the field
        Bounds b = searchField.localToScreen(searchField.getBoundsInLocal());
        if (b == null) return; // scene not attached yet
        double x = b.getMinX();
        double y = b.getMaxY() + 6;
        if (popup.isShowing()) {
            popup.setX(x);
            popup.setY(y);
        } else {
            popup.show(searchField, x, y);
        }
    }

    private HBox makeRow(Entry e) {
        Node ic = Icons.get(e.icon(), 16);
        VBox text = new VBox(0);
        Label title = new Label(e.title());
        title.getStyleClass().add("h3");
        Label sub = new Label(e.subtitle());
        sub.getStyleClass().add("dim");
        sub.setStyle("-fx-font-size: 11px;");
        text.getChildren().addAll(title, sub);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label enter = new Label("↵");
        enter.getStyleClass().add("chip");

        HBox row = new HBox(10, ic, text, enter);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setStyle("-fx-background-radius: 8; -fx-cursor: hand;");
        row.setOnMouseEntered(ev -> row.setStyle(
                "-fx-background-color: " + web(Theme.panel2()) + "; -fx-background-radius: 8; -fx-cursor: hand;"));
        row.setOnMouseExited(ev -> row.setStyle(
                "-fx-background-radius: 8; -fx-cursor: hand;"));
        row.setOnMouseClicked(ev -> {
            window.navigate(e.key());
            popup.hide();
            searchField.clear();
        });
        return row;
    }

    private List<Entry> matches(String q) {
        String s = q.toLowerCase().trim();
        if (s.isEmpty()) return List.of();

        List<Entry> out = new ArrayList<>();

        // 1) title contains query
        for (Entry e : CATALOG) {
            if (e.title().toLowerCase().contains(s)) out.add(e);
        }
        // 2) fall back: query contains a known algorithm keyword
        if (out.isEmpty()) {
            for (Entry e : CATALOG) {
                String title = e.title().toLowerCase();
                String key = e.key().toLowerCase();
                for (String token : s.split("\\s+")) {
                    if (token.length() >= 2 && (title.contains(token) || key.contains(token))) {
                        out.add(e);
                        break;
                    }
                }
            }
        }
        // 3) no matches — return all so the user sees choices
        if (out.isEmpty()) out.addAll(CATALOG);

        return out.size() > 10 ? out.subList(0, 10) : out;
    }

    private String web(javafx.scene.paint.Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }
}