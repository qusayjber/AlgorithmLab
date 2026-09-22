package app.ui.graph;

import app.icons.Icons;
import app.theme.Theme;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.*;

public class GraphView extends BorderPane {

    // ======================= Model =======================
    public static final class GNode {
        final int id;
        double x, y;
        boolean visited;
        final Circle circle;
        final Label label;
        final StackPane view;
        double dragStartX, dragStartY, startLayoutX, startLayoutY;

        GNode(int id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;

            circle = new Circle(24);
            circle.setFill(Theme.panel3());
            circle.setStroke(Theme.border());
            circle.setStrokeWidth(2);
            circle.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.30)));

            label = new Label(nameOf(id));
            label.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 14));
            label.setTextFill(Theme.text());
            label.setMouseTransparent(true);

            view = new StackPane(circle, label);
            view.setPrefSize(48, 48);
            view.setMinSize(48, 48);
            view.setMaxSize(48, 48);
            view.setLayoutX(x - 24);
            view.setLayoutY(y - 24);
        }
    }

    public static final class GEdge {
        final GNode a, b;
        final Line line;
        boolean traversed;
        GEdge(GNode a, GNode b) {
            this.a = a;
            this.b = b;
            line = new Line();
            line.setStroke(Theme.border());
            line.setStrokeWidth(2);
            line.setUserData("edge");
        }
    }

    private static String nameOf(int i) {
        return i < 26 ? String.valueOf((char) ('A' + i)) : "N" + i;
    }

    // ======================= State =======================
    private final Pane canvas = new Pane();
    private final List<GNode> nodes = new ArrayList<>();
    private final List<GEdge> edges = new ArrayList<>();
    private GNode selectedForEdge = null;
    private GNode startNode = null;
    private int nextId = 0;
    private Timeline timeline;
    private boolean animating = false;

    // ======================= UI =======================
    private final ComboBox<String> algoBox = new ComboBox<>();
    private final ComboBox<String> sampleBox = new ComboBox<>();
    private final Slider speedSlider = new Slider(1, 40, 14);
    private final Label startLabel = new Label("—");
    private final Label visitedLabel = new Label("0");
    private final Label frontierLabel = new Label("∅");
    private final Label orderLabel = new Label("—");
    private final Label hintLabel = new Label(
            "Drag = move  ·  Click A → B = connect  ·  Ctrl+click = set start  ·  Right-click node/edge = delete  ·  Right-click empty = add node");

    public GraphView() {
        setPadding(new Insets(20));
        setStyle("-fx-background-color: " + web(Theme.bg()) + ";");

        // -------- Header --------
        Label title = new Label("GRAPH TRAVERSAL");
        title.getStyleClass().add("h1");
        Label sub = new Label("BFS & DFS on an interactive graph — drag, connect, and watch it traverse");
        sub.getStyleClass().add("dim");
        VBox header = new VBox(2, title, sub);

        // -------- Controls row 1 --------
        algoBox.getItems().addAll("BFS", "DFS");
        algoBox.getSelectionModel().selectFirst();
        algoBox.getStyleClass().add("field");
        algoBox.setPrefWidth(96);
        algoBox.setMinWidth(96);

        speedSlider.setPrefWidth(130);
        speedSlider.setMinWidth(130);

        Button run = new Button("Run", Icons.get("play", 14));
        run.getStyleClass().addAll("btn", "btn-primary");
        run.setOnAction(e -> runTraversal());

        Button reset = new Button("Reset Colors", Icons.get("reset", 14));
        reset.getStyleClass().add("btn");
        reset.setOnAction(e -> resetColors());

        Button clear = new Button("Clear");
        clear.getStyleClass().add("btn");
        clear.setOnAction(e -> clearGraph());

        Button addNode = new Button("Add Node", Icons.get("plus", 14));
        addNode.getStyleClass().add("btn");
        addNode.setOnAction(e -> addNode());

        HBox controls = new HBox(10,
                new Label("Algorithm"), algoBox, sep(),
                new Label("Start"), startLabel, sep(),
                new Label("Speed"), speedSlider, sep(),
                run, reset, clear, addNode);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(12, 14, 12, 14));
        controls.setStyle(panelStyle());

        // -------- Controls row 2 (presets + hint) --------
        sampleBox.getItems().addAll("Tree", "Cycle", "Grid", "Path");
        sampleBox.getStyleClass().add("field");
        sampleBox.setPrefWidth(120);
        sampleBox.setMinWidth(120);
        sampleBox.getSelectionModel().selectFirst();
        sampleBox.setOnAction(e -> buildSample(sampleBox.getValue()));

        hintLabel.getStyleClass().add("dim");
        hintLabel.setStyle("-fx-font-size: 11px;");
        hintLabel.setWrapText(true);
        HBox.setHgrow(hintLabel, Priority.ALWAYS);

        HBox controls2 = new HBox(10,
                new Label("Preset"), sampleBox, sep(), hintLabel);
        controls2.setAlignment(Pos.CENTER_LEFT);
        controls2.setPadding(new Insets(10, 14, 10, 14));
        controls2.setStyle(panelStyle());

        // -------- Canvas --------
        canvas.setStyle("-fx-background-color: " + web(Theme.panel()) + "; " +
                "-fx-background-radius: 14; " +
                "-fx-border-color: " + web(Theme.border()) + "; " +
                "-fx-border-radius: 14; -fx-border-width: 1;");
        canvas.setMinHeight(440);

        canvas.setOnMouseClicked(e -> {
            if (e.getTarget() != canvas) return;
            if (e.getButton() == MouseButton.SECONDARY) {
                addNodeAt(e.getX(), e.getY());
            } else if (e.getButton() == MouseButton.PRIMARY && selectedForEdge != null) {
                paintNode(selectedForEdge, selectedForEdge == startNode ? NodeState.START : NodeState.DEFAULT);
                selectedForEdge = null;
            }
        });

        canvas.widthProperty().addListener((o, ov, nv) -> {
            if (ov.doubleValue() == 0 && nv.doubleValue() > 0 && nodes.isEmpty()) {
                Platform.runLater(this::buildInitialSample);
            }
        });

        VBox.setVgrow(canvas, Priority.ALWAYS);

        // -------- Side panel --------
        VBox stats = statPanel("TRAVERSAL",
                statRow("Visited", visitedLabel),
                statRow("Frontier", frontierLabel));
        stats.setPrefWidth(320);
        stats.setMinWidth(280);

        VBox orderPanel = new VBox(6);
        orderPanel.getStyleClass().add("panel");
        Label oh = new Label("VISIT ORDER");
        oh.getStyleClass().add("section-label");
        oh.setPadding(new Insets(0));
        orderLabel.getStyleClass().add("mono");
        orderLabel.setWrapText(true);
        orderLabel.setMaxWidth(Double.MAX_VALUE);
        orderLabel.setMinHeight(80);
        ScrollPane orderScroll = new ScrollPane(orderLabel);
        orderScroll.setFitToWidth(true);
        orderScroll.getStyleClass().add("scroll-pane");
        orderScroll.setPrefHeight(150);
        orderPanel.getChildren().addAll(oh, orderScroll);
        VBox.setVgrow(orderPanel, Priority.ALWAYS);

        VBox side = new VBox(12, stats, orderPanel);
        side.setPrefWidth(320);
        side.setMinWidth(280);

        VBox center = new VBox(12, header, controls, controls2, canvas);
        VBox.setVgrow(canvas, Priority.ALWAYS);
        HBox.setHgrow(center, Priority.ALWAYS);

        HBox body = new HBox(16, center, side);
        setCenter(body);

        Platform.runLater(this::buildInitialSample);
    }

    private void buildInitialSample() {
        if (!nodes.isEmpty()) return;
        buildSample("Tree");
    }

    // ======================= Layout helpers =======================
    private String panelStyle() {
        return "-fx-background-color: " + web(Theme.panel()) + "; -fx-background-radius: 12; " +
                "-fx-border-color: " + web(Theme.border()) + "; -fx-border-radius: 12; -fx-border-width: 1;";
    }

    private Region sep() {
        Region r = new Region();
        r.setPrefWidth(1);
        r.setMinWidth(1);
        r.setPrefHeight(20);
        r.setBackground(new Background(new BackgroundFill(Theme.border(), CornerRadii.EMPTY, Insets.EMPTY)));
        return r;
    }

    private VBox statPanel(String title, Node... rows) {
        VBox v = new VBox(8);
        v.getStyleClass().add("panel");
        Label h = new Label(title);
        h.getStyleClass().add("section-label");
        h.setPadding(new Insets(0));
        v.getChildren().add(h);
        v.getChildren().addAll(rows);
        return v;
    }

    private HBox statRow(String k, Label v) {
        HBox h = new HBox(8);
        h.setAlignment(Pos.CENTER_LEFT);
        Label kl = new Label(k);
        kl.getStyleClass().add("dim");
        kl.setMinWidth(80);
        kl.setPrefWidth(80);
        v.getStyleClass().add("mono");
        v.setWrapText(true);
        v.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(v, Priority.ALWAYS);
        h.getChildren().addAll(kl, v);
        return h;
    }

    // ======================= Sample graphs =======================
    private void buildSample(String kind) {
        clearGraph();
        double w = Math.max(700, canvas.getWidth());
        double h = Math.max(440, canvas.getHeight());

        switch (kind) {
            case "Tree" -> {
                addNodeAt(w * 0.50, h * 0.14);
                addNodeAt(w * 0.25, h * 0.40);
                addNodeAt(w * 0.75, h * 0.40);
                addNodeAt(w * 0.12, h * 0.72);
                addNodeAt(w * 0.38, h * 0.72);
                addNodeAt(w * 0.62, h * 0.72);
                addNodeAt(w * 0.88, h * 0.72);
                addEdge(nodes.get(0), nodes.get(1));
                addEdge(nodes.get(0), nodes.get(2));
                addEdge(nodes.get(1), nodes.get(3));
                addEdge(nodes.get(1), nodes.get(4));
                addEdge(nodes.get(2), nodes.get(5));
                addEdge(nodes.get(2), nodes.get(6));
            }
            case "Cycle" -> {
                int n = 7;
                for (int i = 0; i < n; i++) {
                    double a = 2 * Math.PI * i / n - Math.PI / 2;
                    addNodeAt(w / 2 + Math.cos(a) * w * 0.30, h / 2 + Math.sin(a) * h * 0.32);
                }
                for (int i = 0; i < n; i++) addEdge(nodes.get(i), nodes.get((i + 1) % n));
                addEdge(nodes.get(0), nodes.get(3));
                addEdge(nodes.get(1), nodes.get(5));
            }
            case "Grid" -> {
                int cols = 5, rows = 4;
                for (int r = 0; r < rows; r++)
                    for (int c = 0; c < cols; c++)
                        addNodeAt(w * (0.15 + c * 0.175), h * (0.18 + r * 0.22));
                for (int r = 0; r < rows; r++)
                    for (int c = 0; c < cols; c++) {
                        GNode me = nodes.get(r * cols + c);
                        if (c + 1 < cols) addEdge(me, nodes.get(r * cols + c + 1));
                        if (r + 1 < rows) addEdge(me, nodes.get((r + 1) * cols + c));
                    }
            }
            case "Path" -> {
                int n = 6;
                for (int i = 0; i < n; i++)
                    addNodeAt(w * (0.12 + i * 0.15), h * (i % 2 == 0 ? 0.35 : 0.60));
                for (int i = 0; i < n - 1; i++) addEdge(nodes.get(i), nodes.get(i + 1));
            }
        }
    }

    private void clearGraph() {
        stopAnimation();
        canvas.getChildren().clear();
        nodes.clear();
        edges.clear();
        selectedForEdge = null;
        startNode = null;
        nextId = 0;
        visitedLabel.setText("0");
        frontierLabel.setText("∅");
        orderLabel.setText("—");
        startLabel.setText("—");
    }

    // ======================= Node / edge ops =======================
    private void addNode() {
        double w = Math.max(700, canvas.getWidth());
        double h = Math.max(440, canvas.getHeight());
        addNodeAt(w * (0.2 + Math.random() * 0.6), h * (0.2 + Math.random() * 0.6));
    }

    private void addNodeAt(double x, double y) {
        GNode n = new GNode(nextId++, x, y);
        nodes.add(n);

        n.circle.setOnMousePressed(e -> {
            n.dragStartX = e.getSceneX();
            n.dragStartY = e.getSceneY();
            n.startLayoutX = n.view.getLayoutX();
            n.startLayoutY = n.view.getLayoutY();
        });

        n.circle.setOnMouseDragged(e -> {
            if (animating) return;
            double dx = e.getSceneX() - n.dragStartX;
            double dy = e.getSceneY() - n.dragStartY;
            double nx = n.startLayoutX + dx;
            double ny = n.startLayoutY + dy;
            n.view.setLayoutX(nx);
            n.view.setLayoutY(ny);
            n.x = nx + 24;
            n.y = ny + 24;
            redrawEdges();
        });

        n.circle.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                deleteNode(n);
                e.consume();
                return;
            }
            if (e.getButton() != MouseButton.PRIMARY || animating) return;

            if (e.isControlDown()) {
                setStartNode(n);
                e.consume();
                return;
            }

            if (selectedForEdge == null) {
                selectedForEdge = n;
                paintNode(n, NodeState.SELECTED);
            } else if (selectedForEdge == n) {
                paintNode(n, n == startNode ? NodeState.START : NodeState.DEFAULT);
                selectedForEdge = null;
            } else {
                addEdge(selectedForEdge, n);
                GNode prev = selectedForEdge;
                selectedForEdge = null;
                paintNode(prev, prev == startNode ? NodeState.START : NodeState.DEFAULT);
                paintNode(n, n == startNode ? NodeState.START : NodeState.DEFAULT);
            }
            e.consume();
        });

        canvas.getChildren().add(n.view);
        if (startNode == null) setStartNode(n);
        else paintNode(n, NodeState.DEFAULT);
    }

    private void deleteNode(GNode n) {
        Iterator<GEdge> it = edges.iterator();
        while (it.hasNext()) {
            GEdge e = it.next();
            if (e.a == n || e.b == n) {
                canvas.getChildren().remove(e.line);
                it.remove();
            }
        }
        canvas.getChildren().remove(n.view);
        nodes.remove(n);
        if (selectedForEdge == n) selectedForEdge = null;
        if (startNode == n) {
            startNode = nodes.isEmpty() ? null : nodes.get(0);
        }
        redrawEdges();
        refreshStartLabel();
        if (startNode != null) paintNode(startNode, NodeState.START);
    }

    private void addEdge(GNode a, GNode b) {
        if (a == b) return;
        for (GEdge e : edges) {
            if ((e.a == a && e.b == b) || (e.a == b && e.b == a)) return;
        }
        GEdge e = new GEdge(a, b);

        e.line.setOnMouseClicked(ev -> {
            if (ev.getButton() == MouseButton.SECONDARY) {
                edges.remove(e);
                canvas.getChildren().remove(e.line);
                ev.consume();
            }
        });

        edges.add(e);
        canvas.getChildren().add(0, e.line);
        redrawEdges();
    }

    private void redrawEdges() {
        for (GEdge e : edges) {
            e.line.setStartX(e.a.x);
            e.line.setStartY(e.a.y);
            e.line.setEndX(e.b.x);
            e.line.setEndY(e.b.y);
        }
    }

    private void setStartNode(GNode n) {
        if (startNode != null && startNode != n) paintNode(startNode, NodeState.DEFAULT);
        startNode = n;
        paintNode(n, NodeState.START);
        refreshStartLabel();
    }

    private void refreshStartLabel() {
        if (startNode == null) {
            startLabel.setText("—");
            return;
        }
        startLabel.setText(nameOf(startNode.id));
    }

    // ======================= Node painting =======================
    private enum NodeState { DEFAULT, SELECTED, START, VISITED, CURRENT }

    private void paintNode(GNode n, NodeState st) {
        switch (st) {
            case DEFAULT -> {
                n.circle.setFill(Theme.panel3());
                n.circle.setStroke(Theme.border());
                n.circle.setStrokeWidth(2);
                n.label.setTextFill(Theme.text());
            }
            case SELECTED -> {
                n.circle.setFill(Theme.panel2());
                n.circle.setStroke(Theme.accent());
                n.circle.setStrokeWidth(3);
                n.label.setTextFill(Theme.accent());
            }
            case START -> {
                n.circle.setFill(Theme.panel3());
                n.circle.setStroke(Theme.success());
                n.circle.setStrokeWidth(3);
                n.label.setTextFill(Theme.success());
            }
            case VISITED -> {
                n.circle.setFill(Theme.accent().deriveColor(0, 1, 1, 0.30));
                n.circle.setStroke(Theme.accent());
                n.circle.setStrokeWidth(2);
                n.label.setTextFill(Theme.text());
            }
            case CURRENT -> {
                n.circle.setFill(Theme.accent());
                n.circle.setStroke(Theme.accent2());
                n.circle.setStrokeWidth(3);
                n.label.setTextFill(Color.WHITE);
            }
        }
    }

    private void resetColors() {
        stopAnimation();
        for (GNode n : nodes) {
            n.visited = false;
            paintNode(n, n == startNode ? NodeState.START : NodeState.DEFAULT);
        }
        for (GEdge e : edges) {
            e.traversed = false;
            e.line.setStroke(Theme.border());
            e.line.setStrokeWidth(2);
        }
        visitedLabel.setText("0");
        frontierLabel.setText("∅");
        orderLabel.setText("—");
    }

    // ======================= Traversal =======================
    private void runTraversal() {
        if (nodes.isEmpty() || animating) return;
        stopAnimation();

        for (GNode n : nodes) n.visited = false;
        for (GEdge e : edges) {
            e.traversed = false;
            e.line.setStroke(Theme.border());
            e.line.setStrokeWidth(2);
        }
        for (GNode n : nodes) paintNode(n, n == startNode ? NodeState.START : NodeState.DEFAULT);

        boolean bfs = algoBox.getValue().equals("BFS");
        int n = nodes.size();
        int startIdx = Math.max(0, nodes.indexOf(startNode));

        // adjacency with edge reference for highlighting
        @SuppressWarnings("unchecked")
        List<int[]>[] adj = new List[n];
        for (int i = 0; i < n; i++) adj[i] = new ArrayList<>();
        for (GEdge e : edges) {
            int a = nodes.indexOf(e.a);
            int b = nodes.indexOf(e.b);
            if (a < 0 || b < 0) continue;
            adj[a].add(new int[]{b, edges.indexOf(e)});
            adj[b].add(new int[]{a, edges.indexOf(e)});
        }

        // produce steps
        record Step(int node, int viaEdge, List<Integer> frontierSnapshot, List<Integer> orderSnapshot) {}
        List<Step> steps = new ArrayList<>();
        boolean[] seen = new boolean[n];
        Deque<Integer> work = new ArrayDeque<>();
        Map<Integer, Integer> parentEdge = new HashMap<>();

        seen[startIdx] = true;
        work.add(startIdx);
        parentEdge.put(startIdx, -1);

        while (!work.isEmpty()) {
            int u = bfs ? work.pollFirst() : work.pollLast();
            steps.add(new Step(u, parentEdge.getOrDefault(u, -1),
                    new ArrayList<>(work), orderToList(steps, u)));

            List<int[]> nbrs = new ArrayList<>(adj[u]);
            nbrs.sort(Comparator.comparingInt(x -> x[0]));
            for (int[] v : nbrs) {
                if (!seen[v[0]]) {
                    seen[v[0]] = true;
                    parentEdge.put(v[0], v[1]);
                    work.add(v[0]);
                }
            }
        }

        // animate
        animating = true;
        final int[] idx = {0};
        double perStep = Math.max(80, 900 - speedSlider.getValue() * 20);

        timeline = new Timeline(new KeyFrame(Duration.millis(perStep), e -> {
            if (idx[0] >= steps.size()) {
                stopAnimation();
                finishAnimation();
                return;
            }
            Step s = steps.get(idx[0]++);

            // paint current
            GNode gn = nodes.get(s.node());
            gn.visited = true;
            paintNode(gn, NodeState.CURRENT);

            // pulse
            ScaleTransition st = new ScaleTransition(Duration.millis(180), gn.circle);
            st.setFromX(1); st.setFromY(1);
            st.setToX(1.25); st.setToY(1.25);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();

            // after a short delay, set as VISITED
            PauseTransition hold = new PauseTransition(Duration.millis(perStep * 0.55));
            hold.setOnFinished(x -> {
                if (!animating && gn != startNode) paintNode(gn, NodeState.VISITED);
                else if (gn == startNode) paintNode(gn, NodeState.START);
                else paintNode(gn, NodeState.VISITED);
            });
            hold.play();

            // highlight the edge used to reach this node
            if (s.viaEdge() >= 0) {
                GEdge ed = edges.get(s.viaEdge());
                ed.traversed = true;
                ed.line.setStroke(Theme.accent());
                ed.line.setStrokeWidth(3);
            }

            // update stats
            visitedLabel.setText(String.valueOf(idx[0]));
            frontierLabel.setText(joinNames(s.frontierSnapshot()));
            orderLabel.setText(joinNames(s.orderSnapshot()));
            app.ApplicationController.setOperations(idx[0]);
        }));

        timeline.setCycleCount(steps.size() + 1);
        timeline.setOnFinished(e -> { stopAnimation(); finishAnimation(); });
        timeline.play();
    }

    private List<Integer> orderToList(List<?> steps, int newId) {
        // Build order snapshot from steps so far + newId
        List<Integer> out = new ArrayList<>();
        for (Object s : steps) {
            try {
                int v = (int) s.getClass().getMethod("node").invoke(s);
                out.add(v);
            } catch (Exception ignored) {}
        }
        out.add(newId);
        return out;
    }

    private void finishAnimation() {
        for (GNode n : nodes) {
            if (n == startNode) paintNode(n, NodeState.START);
            else if (n.visited) paintNode(n, NodeState.VISITED);
            else paintNode(n, NodeState.DEFAULT);
        }
    }

    private void stopAnimation() {
        if (timeline != null) { timeline.stop(); timeline = null; }
        animating = false;
    }

    private String joinNames(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return "∅";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(nameOf(ids.get(i)));
        }
        return sb.toString();
    }

    private String web(Color c) {
        return String.format("#%02x%02x%02x",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }
}