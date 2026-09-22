package app.ui.recursion;

import app.icons.Icons;
import app.theme.Theme;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.*;

/**
 * RECURSION EXPLORER
 *
 * Interactive visualization of recursive call trees.
 *
 * Layout:
 *   ┌─────────────────────────────────────────────┐
 *   │  HEADER                                     │
 *   │  CONTROLS                                   │
 *   ├──────────────────────────┬──────────────────┤
 *   │  CALL TREE               │  INFORMATION     │
 *   │  (scrolls independently) │  (scrolls indep.)│
 *   ├──────────────────────────┴──────────────────┤
 *   │  STATUS BAR                                 │
 *   └─────────────────────────────────────────────┘
 *
 * No outer page scroll — each region owns its own scroll area.
 */
public class RecursionView extends BorderPane {

    // ============================================================
    // Model
    // ============================================================

    static final class RNode {
        final int id;
        final String args;
        final int depth;
        final RNode parent;
        final List<RNode> children = new ArrayList<>();
        Long value;
        double x, y;
        int subtreeWidth = 1;

        RNode(int id, String args, int depth, RNode parent) {
            this.id = id;
            this.args = args;
            this.depth = depth;
            this.parent = parent;
        }
    }

    record Event(boolean isReturn, RNode node, long value) {}

    // ============================================================
    // Constants
    // ============================================================

    private static final double LEVEL_H = 84;
    private static final double NODE_R  = 23;
    private static final double MIN_CANVAS_WIDTH  = 620;
    private static final double MIN_CANVAS_HEIGHT = 380;
    private static final int MAX_FIBONACCI_N = 12;

    // ============================================================
    // State
    // ============================================================

    private RNode root;
    private final List<Event> events = new ArrayList<>();
    private int eventIdx = -1;
    private final Deque<RNode> active = new ArrayDeque<>();
    private Timeline animation;
    private boolean playing;
    private int nextId;
    private long callCount;
    private long elapsedNanos;
    private final Map<Long, Long> memo = new HashMap<>();
    private int cacheHits;
    private double slotW = 74;

    // Playback timer
    private long playbackAccumulatedNanos;
    private long playbackTickStartNanos;

    // ============================================================
    // Visualization
    // ============================================================

    private final Canvas canvas = new Canvas(MIN_CANVAS_WIDTH, MIN_CANVAS_HEIGHT);
    private final StackPane canvasHolder = new StackPane();
    private final ScrollPane treeScroll = new ScrollPane();

    // ============================================================
    // Information widgets
    // ============================================================

    private final VBox stackBox = new VBox(4);
    private final Label resultLabel       = new Label("—");
    private final Label callsLabel        = new Label("0");
    private final Label depthLabel        = new Label("0");
    private final Label hitsLabel         = new Label("0");
    private final Label timeLabel         = new Label("0.00 ms");
    private final Label nodesLabel        = new Label("0");
    private final Label complexityLabel   = new Label("—");
    private final Label currentCallLabel  = new Label("—");
    private final Label statusLabel       = new Label("Ready");
    private final Label playbackTimeLabel = new Label("0.000 s");
    private final Label progressTextLabel = new Label("0 / 0 events");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final TextArea log = new TextArea();

    // ============================================================
    // Controls
    // ============================================================

    private final ComboBox<String> algoBox = new ComboBox<>();
    private final Spinner<Integer> nSpinner = new Spinner<>(1, 18, 6);
    private final Slider speedSlider = new Slider(1, 40, 12);
    private final ToggleButton memoizeBtn = new ToggleButton("Memoize");
    private final Button playBtn  = new Button("Play",  Icons.get("play", 14));
    private final Button pauseBtn = new Button("Pause", Icons.get("pause", 14));
    private final Button stepBtn  = new Button("Step",  Icons.get("step", 14));
    private final Button resetBtn = new Button("Reset", Icons.get("reset", 14));
    private final Label speedValue = new Label("12");

    // ============================================================
    // Constructor
    // ============================================================

    public RecursionView() {
        setPadding(new Insets(16));
        setStyle("-fx-background-color: " + web(Theme.bg()) + ";");

        setTop(buildTopSection());
        setCenter(buildWorkspace());
        setBottom(buildStatusBar());

        // Wiring
        algoBox.getItems().setAll(
                "Factorial", "Fibonacci", "Sum", "Power", "GCD", "Binary Search");
        algoBox.getSelectionModel().selectFirst();
        algoBox.getSelectionModel().selectedItemProperty()
                .addListener((o, ov, nv) -> adjustSpinnerForAlgorithm());
        nSpinner.valueProperty().addListener((o, ov, nv) -> resetAndBuild());
        memoizeBtn.setOnAction(e -> resetAndBuild());
        playBtn.setOnAction(e -> play());
        pauseBtn.setOnAction(e -> stop());
        stepBtn.setOnAction(e -> stepOnce());
        resetBtn.setOnAction(e -> resetAndBuild());
        speedSlider.valueProperty().addListener((o, ov, nv) ->
                speedValue.setText(String.format("%.0f", nv.doubleValue())));

        adjustSpinnerForAlgorithm();
    }

    // ============================================================
    // Top section: header + controls (fixed height)
    // ============================================================

    private Region buildTopSection() {
        VBox box = new VBox(12, buildHeader(), buildControls());
        box.setPadding(new Insets(0, 0, 12, 0));
        return box;
    }

    private Region buildHeader() {
        VBox header = new VBox(4);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("RECURSION EXPLORER");
        title.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 26));

        Label badge = new Label("  ALGORITHM LAB");
        badge.getStyleClass().add("section-label");
        badge.setPadding(new Insets(2, 8, 2, 8));
        badge.setStyle(
                "-fx-background-color: " + web(Theme.accent().deriveColor(0, 1, 1, 0.15)) + ";" +
                "-fx-background-radius: 6;");
        badge.setTextFill(Theme.accent());

        titleRow.getChildren().addAll(title, badge);

        Label subtitle = new Label(
                "Watch recursive calls expand, return, and build the final result.");
        subtitle.getStyleClass().add("dim");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 12px;");

        Label flow = new Label("CALL  ▸  DESCEND  ▸  RETURN  ▸  RESULT");
        flow.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        flow.setTextFill(Theme.accent());

        header.getChildren().addAll(titleRow, subtitle, flow);
        return header;
    }

    private Region buildControls() {
        VBox wrap = new VBox(8);
        wrap.setPadding(new Insets(12));
        wrap.setStyle(panelStyle());

        algoBox.getStyleClass().add("field");
        algoBox.setPrefWidth(160);
        algoBox.setMinWidth(140);

        nSpinner.setPrefWidth(90);
        nSpinner.setMinWidth(90);

        memoizeBtn.getStyleClass().add("toggle-btn");

        for (Button b : List.of(playBtn, pauseBtn, stepBtn, resetBtn)) {
            b.getStyleClass().add("btn");
            b.setGraphicTextGap(6);
        }
        playBtn.getStyleClass().add("btn-primary");

        speedSlider.setPrefWidth(150);
        speedSlider.setMinWidth(100);
        speedValue.getStyleClass().add("chip");
        speedValue.setMinWidth(40);
        speedValue.setAlignment(Pos.CENTER);

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox functionGroup = labeled("FUNCTION", algoBox);
        VBox inputGroup    = labeled("INPUT",    nSpinner);

        Label speedLabel = new Label("Speed");
        speedLabel.getStyleClass().add("dim");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(
                functionGroup,
                inputGroup,
                memoizeBtn,
                separator(),
                playBtn, pauseBtn, stepBtn, resetBtn,
                spacer,
                speedLabel, speedSlider, speedValue);

        wrap.getChildren().add(row);
        return wrap;
    }

    // ============================================================
    // Workspace: viz (left) + info (right), fixed layout
    // ============================================================

    private Region buildWorkspace() {
        VBox vizCard  = buildVisualizationCard();
        VBox infoCard = buildInformationCard();

        infoCard.setPrefWidth(360);
        infoCard.setMinWidth(300);
        infoCard.setMaxWidth(420);

        HBox.setHgrow(vizCard, Priority.ALWAYS);

        HBox workspace = new HBox(14, vizCard, infoCard);
        VBox.setVgrow(workspace, Priority.ALWAYS);
        return workspace;
    }

    // ---------- Left card: call tree ----------

    private VBox buildVisualizationCard() {
        VBox card = new VBox(0);
        card.setStyle(panelStyle());

        // Header
        VBox headerBox = new VBox(3);
        headerBox.setPadding(new Insets(12, 12, 8, 12));

        Label heading = new Label("CALL TREE");
        heading.getStyleClass().add("section-label");

        Label subtitle = new Label(
                "Active calls are highlighted · returned calls show their values");
        subtitle.getStyleClass().add("dim");
        subtitle.setStyle("-fx-font-size: 11px;");
        subtitle.setWrapText(true);

        headerBox.getChildren().addAll(heading, subtitle);

        // Scrollable canvas
        canvasHolder.setAlignment(Pos.TOP_LEFT);
        canvasHolder.setPadding(new Insets(20));
        canvasHolder.setStyle(
                "-fx-background-color: " + web(Theme.panel()) + ";" +
                "-fx-background-radius: 10;");
        canvasHolder.getChildren().setAll(canvas);

        treeScroll.setContent(canvasHolder);
        treeScroll.setPannable(true);
        treeScroll.setFitToWidth(false);
        treeScroll.setFitToHeight(false);
        treeScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        treeScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        treeScroll.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;");
        VBox.setVgrow(treeScroll, Priority.ALWAYS);

        // Give the card a sensible minimum so it never collapses
        card.setMinHeight(400);

        // Padding around the scroll area
        StackPane scrollWrap = new StackPane(treeScroll);
        scrollWrap.setPadding(new Insets(0, 12, 12, 12));
        VBox.setVgrow(scrollWrap, Priority.ALWAYS);

        card.getChildren().addAll(headerBox, scrollWrap);
        return card;
    }

    // ---------- Right card: information (internally scrollable) ----------

    private VBox buildInformationCard() {
        VBox card = new VBox(0);
        card.setStyle(panelStyle());

        // Fixed header
        Label title = new Label("INFORMATION");
        title.getStyleClass().add("section-label");
        title.setPadding(new Insets(12, 12, 8, 12));

        // Scrollable content
        VBox content = new VBox(10);
        content.setPadding(new Insets(0, 12, 12, 12));

        // ---- Return value ----
        VBox resultCard = subCard();
        resultLabel.setFont(Font.font("Consolas", FontWeight.EXTRA_BOLD, 22));
        resultLabel.setTextFill(Theme.success());
        resultCard.getChildren().addAll(
                subTitle("RETURN VALUE"), resultLabel);

        // ---- Current call ----
        VBox currentCard = subCard();
        currentCallLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        currentCallLabel.setWrapText(true);
        currentCard.getChildren().addAll(
                subTitle("CURRENT CALL"), currentCallLabel);

        // ---- Playback time ----
        VBox timerCard = subCard();
        HBox timerRow = new HBox(10);
        timerRow.setAlignment(Pos.CENTER_LEFT);
        playbackTimeLabel.setFont(Font.font("Consolas", FontWeight.EXTRA_BOLD, 18));
        playbackTimeLabel.setTextFill(Theme.accent());
        Region timerSpacer = new Region();
        HBox.setHgrow(timerSpacer, Priority.ALWAYS);
        progressTextLabel.getStyleClass().add("mono-dim");
        progressTextLabel.setStyle("-fx-font-size: 10px;");
        timerRow.getChildren().addAll(playbackTimeLabel, timerSpacer, progressTextLabel);

        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: " + web(Theme.accent()) + ";");

        timerCard.getChildren().addAll(
                subTitle("PLAYBACK TIME"), timerRow, progressBar);

        // ---- Statistics ----
        VBox statsCard = subCard();
        statsCard.getChildren().add(subTitle("STATISTICS"));
        statsCard.getChildren().addAll(
                statRow("Total Calls", callsLabel),
                statRow("Max Depth",   depthLabel),
                statRow("Tree Nodes",  nodesLabel),
                statRow("Cache Hits",  hitsLabel),
                statRow("Build Time",  timeLabel),
                statRow("Complexity",  complexityLabel));

        // ---- Call stack ----
        VBox stackCard = subCard();
        stackCard.getChildren().add(subTitle("CALL STACK"));
        ScrollPane stackScroll = new ScrollPane(stackBox);
        stackScroll.setFitToWidth(true);
        stackScroll.setPrefHeight(180);
        stackScroll.setMinHeight(120);
        stackScroll.setMaxHeight(240);
        stackScroll.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;");
        stackCard.getChildren().add(stackScroll);

        // ---- Trace log ----
        VBox logCard = subCard();
        logCard.getChildren().add(subTitle("TRACE LOG"));
        log.setEditable(false);
        log.setWrapText(false);
        log.setPrefRowCount(7);
        log.setMinHeight(110);
        log.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");
        logCard.getChildren().add(log);

        content.getChildren().addAll(
                resultCard, currentCard, timerCard,
                statsCard, stackCard, logCard);

        ScrollPane contentScroll = new ScrollPane(content);
        contentScroll.setFitToWidth(true);
        contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        contentScroll.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;");
        VBox.setVgrow(contentScroll, Priority.ALWAYS);

        card.getChildren().addAll(title, contentScroll);
        return card;
    }

    // ============================================================
    // Status bar
    // ============================================================

    private Region buildStatusBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 14, 8, 14));
        bar.setStyle(panelStyle());

        Circle dot = new Circle(4, Theme.accent());
        statusLabel.getStyleClass().add("dim");

        HBox statusBox = new HBox(8, dot, statusLabel);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label shortcut = new Label("STEP · PLAY · PAUSE · RESET");
        shortcut.getStyleClass().add("dim");
        shortcut.setStyle("-fx-font-size: 10px;");

        bar.getChildren().addAll(statusBox, spacer, shortcut);
        HBox.setMargin(bar, new Insets(12, 0, 0, 0));
        return bar;
    }

    // ============================================================
    // Algorithm settings
    // ============================================================

    private void adjustSpinnerForAlgorithm() {
        String algorithm = algoBox.getValue();
        if (algorithm == null) return;

        switch (algorithm) {
            case "Fibonacci" -> {
                nSpinner.setValueFactory(new SpinnerValueFactory
                        .IntegerSpinnerValueFactory(1, MAX_FIBONACCI_N, 6));
                memoizeBtn.setDisable(false);
            }
            case "Binary Search" -> {
                nSpinner.setValueFactory(new SpinnerValueFactory
                        .IntegerSpinnerValueFactory(6, 32, 12));
                memoizeBtn.setDisable(true);
                memoizeBtn.setSelected(false);
            }
            case "GCD" -> {
                nSpinner.setValueFactory(new SpinnerValueFactory
                        .IntegerSpinnerValueFactory(10, 400, 120));
                memoizeBtn.setDisable(true);
                memoizeBtn.setSelected(false);
            }
            default -> {
                nSpinner.setValueFactory(new SpinnerValueFactory
                        .IntegerSpinnerValueFactory(1, 18, 6));
                memoizeBtn.setDisable(true);
                memoizeBtn.setSelected(false);
            }
        }
        resetAndBuild();
    }

    // ============================================================
    // Build recursion model
    // ============================================================

    private void resetAndBuild() {
        stop();
        events.clear();
        active.clear();
        memo.clear();

        eventIdx = -1;
        nextId = 0;
        callCount = 0;
        cacheHits = 0;

        stackBox.getChildren().clear();
        log.clear();
        resultLabel.setText("—");
        currentCallLabel.setText("—");
        callsLabel.setText("0");
        depthLabel.setText("0");
        nodesLabel.setText("0");
        hitsLabel.setText("0");
        timeLabel.setText("0.00 ms");
        complexityLabel.setText("—");
        playbackTimeLabel.setText("0.000 s");
        progressTextLabel.setText("0 / 0 events");
        progressBar.setProgress(0);
        statusLabel.setText("Building recursion tree…");

        String algorithm = algoBox.getValue();
        int n = nSpinner.getValue();
        long start = System.nanoTime();

        switch (algorithm) {
            case "Factorial" -> {
                root = new RNode(nextId++, "fact(" + n + ")", 0, null);
                buildFactorial(root, n);
            }
            case "Fibonacci" -> {
                root = new RNode(nextId++, "fib(" + n + ")", 0, null);
                buildFib(root, n);
            }
            case "Sum" -> {
                root = new RNode(nextId++, "sum(" + n + ")", 0, null);
                buildSum(root, n);
            }
            case "Power" -> {
                root = new RNode(nextId++, "pow(2," + n + ")", 0, null);
                buildPower(root, 2, n);
            }
            case "GCD" -> {
                int b = Math.max(1, n / 3);
                root = new RNode(nextId++, "gcd(" + n + "," + b + ")", 0, null);
                buildGcd(root, n, b);
            }
            case "Binary Search" -> {
                int size = Math.max(6, n);
                int[] array = new int[size];
                for (int i = 0; i < size; i++) array[i] = i * 3 + 2;
                int target = array[size - 1];
                root = new RNode(nextId++, "BS(0," + (size - 1) + ")", 0, null);
                buildBinarySearch(root, array, 0, size - 1, target);
            }
            default -> root = new RNode(nextId++, "?", 0, null);
        }

        elapsedNanos = System.nanoTime() - start;

        computeWidths(root);
        slotW = slotWidthFor(root.subtreeWidth);
        layout(root, 0, 0);
        updateCanvasSize();
        rebuildStackPanel();
        draw();

        callsLabel.setText(String.valueOf(callCount));
        depthLabel.setText(String.valueOf(maxDepth(root)));
        nodesLabel.setText(String.valueOf(nextId));
        hitsLabel.setText(String.valueOf(cacheHits));
        timeLabel.setText(String.format("%.3f ms", elapsedNanos / 1e6));
        complexityLabel.setText(complexityFor(algorithm));
        progressTextLabel.setText("0 / " + events.size() + " events");
        statusLabel.setText("Ready · " + nextId + " nodes · " + events.size() + " events");
    }

    private String complexityFor(String algorithm) {
        return switch (algorithm) {
            case "Factorial"     -> "O(n)";
            case "Fibonacci"     -> memoizeBtn.isSelected() ? "O(n) memoized" : "O(2ⁿ)";
            case "Sum"           -> "O(n)";
            case "Power"         -> "O(log n)";
            case "GCD"           -> "O(log n)";
            case "Binary Search" -> "O(log n)";
            default -> "—";
        };
    }

    // ============================================================
    // Recursive algorithms
    // ============================================================

    private long buildFactorial(RNode node, int n) {
        callCount++;
        events.add(new Event(false, node, 0));
        long result;
        if (n <= 1) result = 1;
        else {
            RNode child = new RNode(nextId++, "fact(" + (n - 1) + ")", node.depth + 1, node);
            node.children.add(child);
            result = n * buildFactorial(child, n - 1);
        }
        node.value = result;
        events.add(new Event(true, node, result));
        return result;
    }

    private long buildFib(RNode node, int n) {
        callCount++;
        events.add(new Event(false, node, 0));

        if (memoizeBtn.isSelected() && n > 1) {
            Long cached = memo.get((long) n);
            if (cached != null) {
                cacheHits++;
                node.value = cached;
                events.add(new Event(true, node, cached));
                return cached;
            }
        }

        long result;
        if (n <= 1) result = n;
        else {
            RNode left  = new RNode(nextId++, "fib(" + (n - 1) + ")", node.depth + 1, node);
            RNode right = new RNode(nextId++, "fib(" + (n - 2) + ")", node.depth + 1, node);
            node.children.add(left);
            node.children.add(right);
            long a = buildFib(left, n - 1);
            long b = buildFib(right, n - 2);
            result = a + b;
        }
        node.value = result;
        if (memoizeBtn.isSelected()) memo.put((long) n, result);
        events.add(new Event(true, node, result));
        return result;
    }

    private long buildSum(RNode node, int n) {
        callCount++;
        events.add(new Event(false, node, 0));
        long result;
        if (n <= 0) result = 0;
        else {
            RNode child = new RNode(nextId++, "sum(" + (n - 1) + ")", node.depth + 1, node);
            node.children.add(child);
            result = n + buildSum(child, n - 1);
        }
        node.value = result;
        events.add(new Event(true, node, result));
        return result;
    }

    private long buildPower(RNode node, long base, long exp) {
        callCount++;
        events.add(new Event(false, node, 0));
        long result;
        if (exp == 0) result = 1;
        else if (exp % 2 == 0) {
            RNode child = new RNode(nextId++,
                    "pow(" + base + "," + (exp / 2) + ")", node.depth + 1, node);
            node.children.add(child);
            long half = buildPower(child, base, exp / 2);
            result = half * half;
        } else {
            RNode child = new RNode(nextId++,
                    "pow(" + base + "," + (exp - 1) + ")", node.depth + 1, node);
            node.children.add(child);
            result = base * buildPower(child, base, exp - 1);
        }
        node.value = result;
        events.add(new Event(true, node, result));
        return result;
    }

    private long buildGcd(RNode node, long a, long b) {
        callCount++;
        events.add(new Event(false, node, 0));
        long result;
        if (b == 0) result = a;
        else {
            RNode child = new RNode(nextId++,
                    "gcd(" + b + "," + (a % b) + ")", node.depth + 1, node);
            node.children.add(child);
            result = buildGcd(child, b, a % b);
        }
        node.value = result;
        events.add(new Event(true, node, result));
        return result;
    }

    private long buildBinarySearch(RNode node, int[] array, int lo, int hi, int target) {
        callCount++;
        events.add(new Event(false, node, 0));
        long result;
        if (lo > hi) result = -1;
        else {
            int mid = (lo + hi) / 2;
            if (array[mid] == target) result = mid;
            else if (array[mid] < target) {
                RNode child = new RNode(nextId++,
                        "BS(" + (mid + 1) + "," + hi + ")", node.depth + 1, node);
                node.children.add(child);
                result = buildBinarySearch(child, array, mid + 1, hi, target);
            } else {
                RNode child = new RNode(nextId++,
                        "BS(" + lo + "," + (mid - 1) + ")", node.depth + 1, node);
                node.children.add(child);
                result = buildBinarySearch(child, array, lo, mid - 1, target);
            }
        }
        node.value = result;
        events.add(new Event(true, node, result));
        return result;
    }

    // ============================================================
    // Layout
    // ============================================================

    private int computeWidths(RNode node) {
        if (node == null) return 0;
        if (node.children.isEmpty()) {
            node.subtreeWidth = 1;
            return 1;
        }
        int total = 0;
        for (RNode child : node.children) total += computeWidths(child);
        node.subtreeWidth = Math.max(1, total);
        return node.subtreeWidth;
    }

    private double slotWidthFor(int leafCount) {
        double width = 2200.0 / Math.max(1, leafCount);
        return Math.max(38, Math.min(92, width));
    }

    private void layout(RNode node, double leftSlot, int depth) {
        if (node == null) return;
        node.y = depth * LEVEL_H + 48;
        if (node.children.isEmpty()) {
            node.x = (leftSlot + 0.5) * slotW;
            return;
        }
        double cursor = leftSlot;
        for (RNode child : node.children) {
            layout(child, cursor, depth + 1);
            cursor += child.subtreeWidth;
        }
        double first = node.children.get(0).x;
        double last  = node.children.get(node.children.size() - 1).x;
        node.x = (first + last) / 2.0;
    }

    private void updateCanvasSize() {
        if (root == null) {
            canvas.setWidth(MIN_CANVAS_WIDTH);
            canvas.setHeight(MIN_CANVAS_HEIGHT);
            return;
        }

        double width = 0, height = 0;
        Deque<RNode> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            RNode n = stack.pop();
            width  = Math.max(width,  n.x + slotW + 70);
            height = Math.max(height, n.y + LEVEL_H + 30);
            for (RNode c : n.children) stack.push(c);
        }

        width  = Math.max(MIN_CANVAS_WIDTH,  width);
        height = Math.max(MIN_CANVAS_HEIGHT, height);

        canvas.setWidth(width);
        canvas.setHeight(height);
    }

    // ============================================================
    // Rendering
    // ============================================================

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        g.clearRect(0, 0, w, h);
        if (root == null) return;

        g.setFill(Theme.panel());
        g.fillRect(0, 0, w, h);

        // Edges
        Deque<RNode> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            RNode n = stack.pop();
            for (RNode c : n.children) {
                boolean lit = active.contains(n) || active.contains(c) || c.value != null;
                g.setStroke(lit ? Theme.accent().deriveColor(0, 1, 1, 0.65) : Theme.border());
                g.setLineWidth(lit ? 2.5 : 1.6);
                g.strokeLine(n.x, n.y + NODE_R, c.x, c.y - NODE_R);
                stack.push(c);
            }
        }

        // Nodes
        stack.clear();
        stack.push(root);
        while (!stack.isEmpty()) {
            RNode n = stack.pop();
            drawNode(g, n);
            for (RNode c : n.children) stack.push(c);
        }
    }

    private void drawNode(GraphicsContext g, RNode node) {
        boolean activeNode = active.contains(node);
        boolean returned   = node.value != null;

        Color fill, stroke;
        if (activeNode) {
            fill = Theme.accent();
            stroke = Theme.accent2();
        } else if (returned) {
            fill = Theme.success().deriveColor(0, 1, 1, 0.28);
            stroke = Theme.success();
        } else {
            fill = Theme.panel2().deriveColor(0, 1, 1, 0.60);
            stroke = Theme.border();
        }

        if (activeNode) {
            g.setFill(Theme.accent().deriveColor(0, 1, 1, 0.12));
            g.fillOval(node.x - NODE_R - 7, node.y - NODE_R - 7,
                    NODE_R * 2 + 14, NODE_R * 2 + 14);
        }

        g.setFill(fill);
        g.fillOval(node.x - NODE_R, node.y - NODE_R, NODE_R * 2, NODE_R * 2);
        g.setStroke(stroke);
        g.setLineWidth(activeNode ? 3 : 1.8);
        g.strokeOval(node.x - NODE_R, node.y - NODE_R, NODE_R * 2, NODE_R * 2);

        g.setFill(activeNode ? Color.WHITE : Theme.text());
        g.setFont(Font.font("System", FontWeight.BOLD, 10));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText(shorten(node.args), node.x, node.y + 3.5);

        if (returned) {
            String v = String.valueOf(node.value);
            double bw = Math.max(26, v.length() * 7.2 + 12);
            double bx = node.x + NODE_R - 4;
            double by = node.y - NODE_R - 2;
            g.setFill(Theme.success());
            g.fillRoundRect(bx, by - 12, bw, 17, 6, 6);
            g.setFill(Color.WHITE);
            g.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText(v, bx + 5, by);
        }
    }

    private String shorten(String value) {
        if (value == null) return "";
        if (value.length() <= 9) return value;
        int p = value.indexOf('(');
        if (p <= 0) return value.substring(0, Math.min(8, value.length()));
        String head = value.substring(0, p);
        String tail = value.substring(p);
        if (head.length() > 3) head = head.substring(0, 3);
        if (tail.length() > 7) tail = tail.substring(0, 6) + ")";
        return head + tail;
    }

    // ============================================================
    // Playback
    // ============================================================

    private void play() {
        if (events.isEmpty()) return;
        if (eventIdx >= events.size() - 1) resetPlayback();
        if (animation != null) return;

        playing = true;
        playBtn.setText("Playing");
        playbackTickStartNanos = System.nanoTime();

        animation = new Timeline(new KeyFrame(Duration.millis(40), e -> tick()));
        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();

        statusLabel.setText("Playing recursion trace…");
    }

    private void stop() {
        playing = false;

        if (playbackTickStartNanos > 0) {
            playbackAccumulatedNanos += System.nanoTime() - playbackTickStartNanos;
            playbackTickStartNanos = 0;
        }

        if (animation != null) {
            animation.stop();
            animation = null;
        }
        playBtn.setText("Play");

        if (eventIdx >= 0 && eventIdx < events.size()) {
            statusLabel.setText("Paused · event " + (eventIdx + 1) + " / " + events.size());
        }
    }

    private void resetPlayback() {
        stop();
        eventIdx = -1;
        active.clear();
        clearValues(root);
        stackBox.getChildren().clear();
        log.clear();
        resultLabel.setText("—");
        currentCallLabel.setText("—");

        playbackAccumulatedNanos = 0;
        playbackTickStartNanos = 0;
        playbackTimeLabel.setText("0.000 s");
        progressTextLabel.setText("0 / " + events.size() + " events");
        progressBar.setProgress(0);

        rebuildStackPanel();
        draw();
        statusLabel.setText("Playback reset");
    }

    private void clearValues(RNode node) {
        if (node == null) return;
        node.value = null;
        for (RNode c : node.children) clearValues(c);
    }

    private void stepOnce() {
        if (events.isEmpty()) return;
        if (eventIdx >= events.size() - 1) { resetPlayback(); return; }
        applyNext();
        updatePlaybackInfo();
        draw();
    }

    private void tick() {
        if (events.isEmpty()) { stop(); return; }

        int batch = Math.max(1, (int) Math.ceil(speedSlider.getValue() / 7.0));
        boolean finished = false;

        for (int i = 0; i < batch; i++) {
            if (eventIdx >= events.size() - 1) { finished = true; break; }
            applyNext();
        }

        updatePlaybackInfo();
        draw();

        if (finished) {
            stop();
            statusLabel.setText("Completed · " + events.size() + " events");
        }
    }

    private void updatePlaybackInfo() {
        long elapsed = playbackAccumulatedNanos;
        if (playbackTickStartNanos > 0) {
            elapsed += System.nanoTime() - playbackTickStartNanos;
        }
        playbackTimeLabel.setText(String.format("%.3f s", elapsed / 1e9));

        int done = Math.max(0, eventIdx + 1);
        int total = events.size();
        int remaining = Math.max(0, total - done);
        progressTextLabel.setText(done + " / " + total + "  ·  " + remaining + " left");
        progressBar.setProgress(total == 0 ? 0 : done / (double) total);
    }

    // ============================================================
    // Apply event
    // ============================================================

    private void applyNext() {
        eventIdx++;
        Event event = events.get(eventIdx);
        RNode node = event.node();

        if (!event.isReturn()) {
            active.push(node);
            currentCallLabel.setText(node.args + "  · depth " + node.depth);
            log.appendText(indent(node.depth) + "▸ call   " + node.args + "\n");
        } else {
            node.value = event.value();
            active.remove(node);
            currentCallLabel.setText(node.args + " → " + node.value);
            log.appendText(indent(node.depth) + "◂ return " + node.args
                    + " → " + node.value + "\n");
            if (node == root) resultLabel.setText(String.valueOf(node.value));
        }

        rebuildStackPanel();
        log.setScrollTop(Double.MAX_VALUE);
    }

    // ============================================================
    // Call stack
    // ============================================================

    private void rebuildStackPanel() {
        stackBox.getChildren().clear();

        if (active.isEmpty()) {
            Label empty = new Label("Stack empty");
            empty.getStyleClass().add("dim");
            empty.setPadding(new Insets(10));
            stackBox.getChildren().add(empty);
            return;
        }

        List<RNode> stack = new ArrayList<>(active);
        for (int i = 0; i < stack.size(); i++) {
            RNode node = stack.get(i);
            boolean top = i == 0;

            HBox row = new HBox(9);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 10, 6, 10));

            String bg = web(top ? Theme.panel3() : Theme.panel2());
            String style = "-fx-background-color: " + bg + "; -fx-background-radius: 8;";
            if (top) {
                style += "-fx-border-color: " + web(Theme.accent())
                        + "; -fx-border-radius: 8; -fx-border-width: 1.4;";
            }
            row.setStyle(style);

            Label position = new Label("[" + (stack.size() - 1 - i) + "]");
            position.getStyleClass().add("mono-dim");
            position.setMinWidth(36);

            Label name = new Label(node.args);
            name.getStyleClass().add("mono");
            if (top) name.setTextFill(Theme.accent());

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label depth = new Label("d=" + node.depth);
            depth.getStyleClass().add("chip");

            row.getChildren().addAll(position, name, spacer, depth);
            stackBox.getChildren().add(row);
        }
    }

    // ============================================================
    // Helpers
    // ============================================================

    private long maxDepth(RNode node) {
        if (node == null) return 0;
        long m = node.depth;
        for (RNode c : node.children) m = Math.max(m, maxDepth(c));
        return m;
    }

    private String indent(int depth) {
        return "  ".repeat(Math.min(depth, 14));
    }

    private VBox subCard() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(10));
        box.setStyle(
                "-fx-background-color: " + web(Theme.panel2()) + ";" +
                "-fx-background-radius: 10;");
        return box;
    }

    private Label subTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-label");
        label.setPadding(new Insets(0, 0, 2, 0));
        return label;
    }

    private VBox labeled(String label, Node field) {
        Label l = new Label(label);
        l.getStyleClass().add("section-label");
        VBox box = new VBox(4, l, field);
        return box;
    }

    private HBox statRow(String name, Label value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label key = new Label(name);
        key.getStyleClass().add("dim");
        key.setMinWidth(110);
        key.setPrefWidth(110);
        value.getStyleClass().add("mono");
        row.getChildren().addAll(key, value);
        return row;
    }

    private Region separator() {
        Region region = new Region();
        region.setPrefWidth(1);
        region.setMinWidth(1);
        region.setPrefHeight(24);
        region.setBackground(new Background(new BackgroundFill(
                Theme.border(), CornerRadii.EMPTY, Insets.EMPTY)));
        return region;
    }

    private String panelStyle() {
        return "-fx-background-color: " + web(Theme.panel()) + ";" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: " + web(Theme.border()) + ";" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;";
    }

    private String web(Color color) {
        return String.format("#%02x%02x%02x",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}