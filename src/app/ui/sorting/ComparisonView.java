package app.ui.sorting;

import app.algorithms.sorting.SortingAlgorithms;
import app.algorithms.sorting.SortingAlgorithms.Result;
import app.icons.Icons;
import app.theme.Theme;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ComparisonView extends BorderPane {

    // ==================== Data ====================
    private record Row(
            String name,
            String key,
            long comparisons,
            long swaps,
            long writes,
            double timeMs,
            long totalOps
    ) {}

    private static final int WARMUP_RUNS = 3;
    private static final int TIMED_RUNS  = 5;

    private final Spinner<Integer> sizeSpinner  = new Spinner<>(10, 200, 40);
    private final Spinner<Integer> seedSpinner  = new Spinner<>(1, 999999, 42);
    private final CheckBox randomizeSeed        = new CheckBox("Random seed");
    private final ComboBox<String> metricBox    = new ComboBox<>();

    private final VBox tableContainer = new VBox(2);
    private final Label summaryLabel  = new Label("Press Run to compare");
    private final Label statusLabel   = new Label("Ready");
    private final Button runBtn       = new Button("Run Comparison", Icons.get("play", 14));
    private final Button copyBtn      = new Button("Copy", Icons.get("code", 14));
    private final ProgressIndicator progress = new ProgressIndicator();

    private List<Row> lastRows = new ArrayList<>();
    private int[] lastInput;

    public ComparisonView() {
        setPadding(new Insets(20));
        setStyle("-fx-background-color: " + web(Theme.bg()) + ";");

        // ---------------- Header ----------------
        Label title = new Label("ALGORITHM COMPARISON");
        title.getStyleClass().add("h1");
        Label sub = new Label("Race all six sorting algorithms on identical input");
        sub.getStyleClass().add("dim");
        VBox header = new VBox(2, title, sub);

        // ---------------- Controls ----------------
        Region controls = buildControls();

        // ---------------- Summary card ----------------
        VBox summary = new VBox(4);
        summary.getStyleClass().add("panel");
        Label sh = new Label("SUMMARY");
        sh.getStyleClass().add("section-label");
        sh.setPadding(new Insets(0));
        summaryLabel.getStyleClass().add("mono");
        summaryLabel.setWrapText(true);
        summary.getChildren().addAll(sh, summaryLabel);

        // ---------------- Table ----------------
        ScrollPane tableScroll = new ScrollPane(tableContainer);
        tableScroll.setFitToWidth(true);
        tableScroll.getStyleClass().add("scroll-pane");
        tableScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        tableScroll.setPadding(new Insets(0));
        VBox.setVgrow(tableScroll, Priority.ALWAYS);

        // ---------------- Bottom status bar ----------------
        HBox statusBar = new HBox(10);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(8, 14, 8, 14));
        statusBar.setStyle(panelStyle());
        statusLabel.getStyleClass().add("dim");
        statusLabel.setStyle("-fx-font-size: 11px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statusBar.getChildren().addAll(statusLabel, spacer, copyBtn);

        copyBtn.getStyleClass().add("btn");
        copyBtn.setGraphicTextGap(6);
        copyBtn.setDisable(true);
        copyBtn.setOnAction(e -> copyToClipboard());

        VBox root = new VBox(12, header, controls, summary, tableScroll, statusBar);
        VBox.setVgrow(tableScroll, Priority.ALWAYS);
        setCenter(root);

        // ---------------- Wiring ----------------
        runBtn.setOnAction(e -> runComparison());
        sizeSpinner.valueProperty().addListener((o, ov, nv) -> runComparison());
        metricBox.valueProperty().addListener((o, ov, nv) -> rebuildTable(lastRows));

        // Auto-run on first open
        Platform.runLater(this::runComparison);
    }

    // ============================================================
    //  Controls
    // ============================================================
    private Region buildControls() {
        VBox wrap = new VBox(8);
        wrap.setPadding(new Insets(10, 12, 10, 12));
        wrap.setStyle(panelStyle());

        sizeSpinner.setPrefWidth(84);
        sizeSpinner.setMinWidth(84);

        seedSpinner.setPrefWidth(110);
        seedSpinner.setMinWidth(110);
        seedSpinner.disableProperty().bind(randomizeSeed.selectedProperty());

        randomizeSeed.setSelected(false);
        randomizeSeed.getStyleClass().add("check-box");

        metricBox.getItems().addAll("Comparisons", "Swaps", "Writes", "Time");
        metricBox.getSelectionModel().selectFirst();
        metricBox.getStyleClass().add("field");
        metricBox.setPrefWidth(150);
        metricBox.setMinWidth(150);

        runBtn.getStyleClass().addAll("btn", "btn-primary");
        runBtn.setGraphicTextGap(6);

        progress.setPrefSize(18, 18);
        progress.setVisible(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(10,
                new Label("Array Size"), sizeSpinner,
                sep(),
                new Label("Seed"), seedSpinner, randomizeSeed,
                sep(),
                new Label("Sort Table By"), metricBox,
                spacer,
                progress, runBtn);
        row.setAlignment(Pos.CENTER_LEFT);

        Label hint = new Label(
                "Each algorithm runs " + WARMUP_RUNS + " warm-up rounds + " + TIMED_RUNS
                        + " timed rounds. The fastest time is reported (JIT-friendly).");
        hint.getStyleClass().add("dim");
        hint.setStyle("-fx-font-size: 11px;");

        wrap.getChildren().addAll(row, hint);
        return wrap;
    }

    private Region sep() {
        Region r = new Region();
        r.setPrefWidth(1); r.setMinWidth(1); r.setPrefHeight(22);
        r.setBackground(new Background(new BackgroundFill(
                Theme.border(), CornerRadii.EMPTY, Insets.EMPTY)));
        return r;
    }

    private String panelStyle() {
        return "-fx-background-color: " + web(Theme.panel()) + "; -fx-background-radius: 12; " +
                "-fx-border-color: " + web(Theme.border()) + "; -fx-border-radius: 12; -fx-border-width: 1;";
    }

    // ============================================================
    //  Run
    // ============================================================
    private void runComparison() {
        runBtn.setDisable(true);
        progress.setVisible(true);
        statusLabel.setText("Running…");

        int n = sizeSpinner.getValue();
        int seed = randomizeSeed.isSelected()
                ? (int) (Math.random() * 999_999) + 1
                : seedSpinner.getValue();
        if (randomizeSeed.isSelected()) seedSpinner.getValueFactory().setValue(seed);

        int[] base = new int[n];
        Random r = new Random(seed);
        for (int i = 0; i < n; i++) base[i] = 5 + r.nextInt(95);
        lastInput = base.clone();

        // Run on a background thread to keep the UI responsive
        Thread worker = new Thread(() -> {
            List<Row> rows = new ArrayList<>();

            String[][] algos = {
                    {"Bubble Sort",    "bubble"},
                    {"Selection Sort", "selection"},
                    {"Insertion Sort", "insertion"},
                    {"Merge Sort",     "merge"},
                    {"Quick Sort",     "quick"},
                    {"Heap Sort",      "heap"},
            };

            for (String[] a : algos) {
                // Warm-up runs
                for (int i = 0; i < WARMUP_RUNS; i++) {
                    execute(a[1], base.clone());
                }
                // Timed runs
                long bestTime = Long.MAX_VALUE;
                Result best = null;
                for (int i = 0; i < TIMED_RUNS; i++) {
                    Result res = execute(a[1], base.clone());
                    if (res.timeNanos() < bestTime) {
                        bestTime = res.timeNanos();
                        best = res;
                    }
                }
                if (best == null) continue;

                long totalOps = best.comparisons() + best.swaps() + best.writes();
                rows.add(new Row(a[0], a[1],
                        best.comparisons(), best.swaps(), best.writes(),
                        bestTime / 1e6, totalOps));
            }

            final List<Row> finalRows = rows;
            final int finalSeed = seed;
            Platform.runLater(() -> {
                lastRows = finalRows;
                sortRowsByMetric();
                rebuildTable(lastRows);
                updateSummary(finalRows, n, finalSeed);
                statusLabel.setText("Compared 6 algorithms on " + n
                        + " elements (seed " + finalSeed + ")");
                runBtn.setDisable(false);
                progress.setVisible(false);
                copyBtn.setDisable(false);
            });
        }, "comparison-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private Result execute(String key, int[] arr) {
        return switch (key) {
            case "bubble"    -> SortingAlgorithms.bubble(arr);
            case "selection" -> SortingAlgorithms.selection(arr);
            case "insertion" -> SortingAlgorithms.insertion(arr);
            case "merge"     -> SortingAlgorithms.merge(arr);
            case "quick"     -> SortingAlgorithms.quick(arr);
            case "heap"      -> SortingAlgorithms.heap(arr);
            default          -> SortingAlgorithms.bubble(arr);
        };
    }

    // ============================================================
    //  Summary
    // ============================================================
    private void updateSummary(List<Row> rows, int n, int seed) {
        if (rows.isEmpty()) { summaryLabel.setText("No results"); return; }

        Row byCompare = rows.stream().min((a, b) ->
                Long.compare(a.comparisons(), b.comparisons())).orElse(rows.get(0));
        Row bySwap = rows.stream().min((a, b) ->
                Long.compare(a.swaps(), b.swaps())).orElse(rows.get(0));
        Row byTime = rows.stream().min((a, b) ->
                Double.compare(a.timeMs(), b.timeMs())).orElse(rows.get(0));
        Row byOps = rows.stream().min((a, b) ->
                Long.compare(a.totalOps(), b.totalOps())).orElse(rows.get(0));

        summaryLabel.setText(String.format(
                "n = %d  ·  seed = %d  ·  %d algorithms\n" +
                "Fewest comparisons  →  %s  (%d)\n" +
                "Fewest swaps        →  %s  (%d)\n" +
                "Fewest total ops    →  %s  (%d)\n" +
                "Fastest wall time   →  %s  (%.3f ms)",
                n, seed, rows.size(),
                byCompare.name(), byCompare.comparisons(),
                bySwap.name(), bySwap.swaps(),
                byOps.name(), byOps.totalOps(),
                byTime.name(), byTime.timeMs()));
    }

    // ============================================================
    //  Table
    // ============================================================
    private void sortRowsByMetric() {
        String metric = metricBox.getValue();
        lastRows.sort((a, b) -> switch (metric) {
            case "Comparisons" -> Long.compare(a.comparisons(), b.comparisons());
            case "Swaps"       -> Long.compare(a.swaps(), b.swaps());
            case "Writes"      -> Long.compare(a.writes(), b.writes());
            case "Time"        -> Double.compare(a.timeMs(), b.timeMs());
            default            -> Long.compare(a.totalOps(), b.totalOps());
        });
    }

    private void rebuildTable(List<Row> rows) {
        tableContainer.getChildren().clear();
        if (rows.isEmpty()) {
            Label empty = new Label("No results yet — press Run Comparison");
            empty.getStyleClass().add("dim");
            empty.setPadding(new Insets(24));
            tableContainer.getChildren().add(empty);
            return;
        }

        // Determine maxima for bar scaling
        long maxComp = rows.stream().mapToLong(Row::comparisons).max().orElse(1);
        long maxSwap = rows.stream().mapToLong(Row::swaps).max().orElse(1);
        long maxWrite = rows.stream().mapToLong(Row::writes).max().orElse(1);
        double maxTime = rows.stream().mapToDouble(Row::timeMs).max().orElse(1);

        long minComp = rows.stream().mapToLong(Row::comparisons).min().orElse(0);
        long minSwap = rows.stream().mapToLong(Row::swaps).min().orElse(0);
        long minWrite = rows.stream().mapToLong(Row::writes).min().orElse(0);
        double minTime = rows.stream().mapToDouble(Row::timeMs).min().orElse(0);

        tableContainer.getChildren().add(headerRow());

        int rank = 1;
        for (Row r : rows) {
            tableContainer.getChildren().add(dataRow(
                    rank++, r,
                    maxComp, minComp,
                    maxSwap, minSwap,
                    maxWrite, minWrite,
                    maxTime, minTime));
        }
    }

    private HBox headerRow() {
        HBox h = new HBox(0);
        h.setPadding(new Insets(8, 14, 8, 14));
        h.setAlignment(Pos.CENTER_LEFT);
        h.setStyle("-fx-background-color: " + web(Theme.panel()) + "; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: " + web(Theme.border()) + "; " +
                "-fx-border-radius: 10; -fx-border-width: 1;");

        addCell(h, "#",                50,  Pos.CENTER_LEFT, true);
        addCell(h, "Algorithm",       170,  Pos.CENTER_LEFT, true);
        addCell(h, "Comparisons",     170,  Pos.CENTER_LEFT, true);
        addCell(h, "Swaps",           150,  Pos.CENTER_LEFT, true);
        addCell(h, "Writes",          150,  Pos.CENTER_LEFT, true);
        addCell(h, "Total Ops",       130,  Pos.CENTER_LEFT, true);
        addCell(h, "Time (ms)",       130,  Pos.CENTER_LEFT, true);
        return h;
    }

    private HBox dataRow(int rank, Row r,
                         long maxC, long minC,
                         long maxS, long minS,
                         long maxW, long minW,
                         double maxT, double minT) {

        HBox h = new HBox(0);
        h.setPadding(new Insets(6, 14, 6, 14));
        h.setAlignment(Pos.CENTER_LEFT);
        h.setStyle("-fx-background-color: " + web(Theme.panel()) + "; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: " + web(Theme.border()) + "; " +
                "-fx-border-radius: 10; -fx-border-width: 1;");

        // Rank cell
        Label rankL = new Label(String.valueOf(rank));
        rankL.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 12));
        rankL.setTextFill(rank == 1 ? Theme.accent() : Theme.textDim());
        rankL.setPrefWidth(50);
        rankL.setMinWidth(50);
        h.getChildren().add(rankL);

        // Algorithm name
        Label nameL = new Label(r.name());
        nameL.getStyleClass().add("h3");
        nameL.setPrefWidth(170);
        nameL.setMinWidth(170);
        h.getChildren().add(nameL);

        // Numeric cells with proportional bars
        h.getChildren().add(barCell(r.comparisons(), minC, maxC, 170,
                "comparisons",  Theme.danger(),  r.comparisons() == minC && minC < maxC));
        h.getChildren().add(barCell(r.swaps(),       minS, maxS, 150,
                "swaps",        Theme.warn(),    r.swaps() == minS && minS < maxS));
        h.getChildren().add(barCell(r.writes(),      minW, maxW, 150,
                "writes",       Theme.accent2(), r.writes() == minW && minW < maxW));
        h.getChildren().add(plainCell(String.valueOf(r.totalOps()), 130));

        // Time
        boolean bestTime = Math.abs(r.timeMs() - minT) < 0.0005 && minT < maxT;
        h.getChildren().add(timeCell(r.timeMs(), minT, maxT, bestTime));

        return h;
    }

    /**
     * Cell with a proportional background bar. Lower value = shorter bar = "better".
     * The value label sits on top of the bar.
     */
    private StackPane barCell(long value, long min, long max, double width,
                              String unit, Color barColor, boolean isBest) {
        double ratio = (max <= min) ? 1.0 : 1.0 - ((value - min) / (double) (max - min));
        // Invert: smaller value → bigger bar? No — bigger value → bigger bar so we
        // can see the difference visually. But the WINNER is highlighted.

        ratio = (max <= min) ? 1.0 : (value - min) / (double) (max - min);

        Region bar = new Region();
        bar.setPrefHeight(26);
        bar.setMinHeight(26);
        bar.setMaxHeight(26);
        bar.setPrefWidth(Math.max(6, width * ratio));
        bar.setMinWidth(Math.max(6, width * ratio));
        bar.setMaxWidth(Math.max(6, width * ratio));
        Color fill = barColor.deriveColor(0, 1, 1, 0.22);
        if (isBest) fill = barColor.deriveColor(0, 1, 1, 0.55);
        bar.setBackground(new Background(new BackgroundFill(
                fill, new CornerRadii(6), Insets.EMPTY)));

        Label valueL = new Label(String.valueOf(value));
        valueL.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        valueL.setTextFill(isBest ? Theme.text() : Theme.textDim());
        valueL.setPadding(new Insets(0, 0, 0, 10));

        StackPane stack = new StackPane(bar, valueL);
        StackPane.setAlignment(bar, Pos.CENTER_LEFT);
        StackPane.setAlignment(valueL, Pos.CENTER_LEFT);
        stack.setPrefWidth(width);
        stack.setMinWidth(width);
        stack.setMaxWidth(width);
        stack.setPrefHeight(32);
        stack.setAlignment(Pos.CENTER_LEFT);
        if (isBest) {
            // Subtle accent border for the winner
            stack.setStyle("-fx-border-color: " + web(barColor) +
                    "; -fx-border-radius: 6; -fx-border-width: 1;");
        }
        return stack;
    }

    private StackPane timeCell(double ms, double min, double max, boolean isBest) {
        double ratio = (max <= min) ? 1.0 : (ms - min) / (max - min);
        double width = 130;

        Region bar = new Region();
        bar.setPrefHeight(26);
        bar.setMinHeight(26);
        bar.setMaxHeight(26);
        bar.setPrefWidth(Math.max(6, width * ratio));
        bar.setMinWidth(Math.max(6, width * ratio));
        bar.setMaxWidth(Math.max(6, width * ratio));
        Color fill = Theme.success().deriveColor(0, 1, 1, isBest ? 0.55 : 0.22);
        bar.setBackground(new Background(new BackgroundFill(
                fill, new CornerRadii(6), Insets.EMPTY)));

        Label valueL = new Label(String.format("%.3f", ms));
        valueL.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        valueL.setTextFill(isBest ? Theme.text() : Theme.textDim());
        valueL.setPadding(new Insets(0, 0, 0, 10));

        StackPane stack = new StackPane(bar, valueL);
        StackPane.setAlignment(bar, Pos.CENTER_LEFT);
        StackPane.setAlignment(valueL, Pos.CENTER_LEFT);
        stack.setPrefWidth(width);
        stack.setMinWidth(width);
        stack.setMaxWidth(width);
        stack.setPrefHeight(32);
        stack.setAlignment(Pos.CENTER_LEFT);
        if (isBest) {
            stack.setStyle("-fx-border-color: " + web(Theme.success()) +
                    "; -fx-border-radius: 6; -fx-border-width: 1;");
        }
        return stack;
    }

    private Label plainCell(String s, double width) {
        Label l = new Label(s);
        l.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        l.setTextFill(Theme.textDim());
        l.setPrefWidth(width);
        l.setMinWidth(width);
        return l;
    }

    private void addCell(HBox row, String text, double width, Pos align, boolean bold) {
        Label l = new Label(text);
        l.getStyleClass().add("section-label");
        l.setPadding(new Insets(0));
        l.setPrefWidth(width);
        l.setMinWidth(width);
        l.setAlignment(align);
        row.getChildren().add(l);
    }

    // ============================================================
    //  Copy to clipboard
    // ============================================================
    private void copyToClipboard() {
        if (lastRows.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        sb.append("Algorithm\tComparisons\tSwaps\tWrites\tTotal Ops\tTime (ms)\n");
        for (Row r : lastRows) {
            sb.append(r.name()).append('\t')
              .append(r.comparisons()).append('\t')
              .append(r.swaps()).append('\t')
              .append(r.writes()).append('\t')
              .append(r.totalOps()).append('\t')
              .append(String.format("%.3f", r.timeMs())).append('\n');
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("Results copied to clipboard");
    }

    // ============================================================
    //  Helpers
    // ============================================================
    private String web(Color c) {
        return String.format("#%02x%02x%02x",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }
}