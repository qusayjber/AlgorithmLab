package app.ui.searching;

import app.icons.Icons;
import app.theme.Theme;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.Random;

public class SearchingView extends BorderPane {

    // ==================== Model ====================
    private int[] data = new int[0];
    private int target;
    private int current = -1;
    private int lo = -1, hi = -1;
    private int found = -1;
    private long comparisons = 0;
    private boolean finished = false;
    private String stepDescription = "Ready — press Play or Step";

    // Linear search state
    private int linearIdx = 0;

    // Binary search state
    private int binLo = 0, binHi = 0;
    private boolean binInitialized = false;

    // ==================== UI ====================
    private final HBox cells  = new HBox(6);
    private final HBox arrows = new HBox(6);

    private final ComboBox<String> algoBox = new ComboBox<>();
    private final Spinner<Integer> sizeSpinner = new Spinner<>(6, 40, 15);
    private final TextField targetField = new TextField();
    private final Slider speedSlider = new Slider(1, 50, 15);
    private final Label speedValue = new Label("15");

    private final Label comparisonsLabel = new Label("0");
    private final Label intervalLabel    = new Label("—");
    private final Label targetLabel      = new Label("—");
    private final Label stepLabel        = new Label("—");
    private final Label statusLabel      = new Label("Ready");
    private final Label indicesLabel     = new Label("—");

    private final Button playBtn  = new Button("Play",  Icons.get("play", 14));
    private final Button pauseBtn = new Button("Pause", Icons.get("pause", 14));
    private final Button stepBtn  = new Button("Step",  Icons.get("step", 14));
    private final Button resetBtn = new Button("Reset", Icons.get("reset", 14));

    private Timeline timeline;
    private boolean playing = false;

    private static final int MAX_CELL_W = 46;
    private static final int MIN_CELL_W = 22;

    public SearchingView() {
        setPadding(new Insets(18));
        setStyle("-fx-background-color: " + web(Theme.bg()) + ";");

        // ---------------- Header ----------------
        Label title = new Label("SEARCHING LABORATORY");
        title.getStyleClass().add("h1");
        Label sub = new Label("Linear and binary search visualized step by step");
        sub.getStyleClass().add("dim");
        VBox header = new VBox(2, title, sub);

        // ---------------- Controls ----------------
        Region controls = buildControls();

        // ---------------- Canvas ----------------
        VBox canvasBox = buildCanvasBox();
        VBox.setVgrow(canvasBox, Priority.ALWAYS);

        // ---------------- Side panel ----------------
        VBox side = buildSidePanel();
        side.setPrefWidth(280);
        side.setMinWidth(240);

        VBox center = new VBox(14, header, controls, canvasBox);
        HBox.setHgrow(center, Priority.ALWAYS);

        HBox body = new HBox(16, center, side);
        setCenter(body);

        // ---------------- Init ----------------
        sizeSpinner.valueProperty().addListener((o, ov, nv) -> regenerate());
        algoBox.valueProperty().addListener((o, ov, nv) -> {
            resetPlayback();
            regenerate(false);
        });

        regenerate();
    }

    // ============================================================
    //  Controls
    // ============================================================
    private Region buildControls() {
        VBox wrap = new VBox(8);
        wrap.setPadding(new Insets(10, 12, 10, 12));
        wrap.setStyle(panelStyle());

        algoBox.getItems().addAll("Linear Search", "Binary Search");
        algoBox.getSelectionModel().selectFirst();
        algoBox.getStyleClass().add("field");
        algoBox.setPrefWidth(160);
        algoBox.setMinWidth(160);

        sizeSpinner.setPrefWidth(84);
        sizeSpinner.setMinWidth(84);

        targetField.setPromptText("target");
        targetField.getStyleClass().add("field");
        targetField.setPrefWidth(90);
        targetField.setMinWidth(90);
        targetField.setOnAction(e -> applyCustomTarget());

        Button setTarget = new Button("Set");
        setTarget.getStyleClass().add("btn");
        setTarget.setOnAction(e -> applyCustomTarget());

        Button randomize = new Button("Randomize", Icons.get("reset", 14));
        randomize.getStyleClass().add("btn");
        randomize.setGraphicTextGap(6);
        randomize.setOnAction(e -> regenerate());

        playBtn.getStyleClass().addAll("btn", "btn-primary");
        pauseBtn.getStyleClass().add("btn");
        stepBtn.getStyleClass().add("btn");
        resetBtn.getStyleClass().add("btn");
        for (Button b : new Button[]{playBtn, pauseBtn, stepBtn, resetBtn}) b.setGraphicTextGap(6);

        playBtn.setOnAction(e -> togglePlay());
        pauseBtn.setOnAction(e -> stop());
        stepBtn.setOnAction(e -> stepOnce());
        resetBtn.setOnAction(e -> resetPlayback());

        speedSlider.setPrefWidth(150);
        speedSlider.setMinWidth(110);
        speedSlider.setShowTickMarks(false);
        speedValue.getStyleClass().add("chip");
        speedValue.setMinWidth(34);
        speedValue.setAlignment(Pos.CENTER);
        speedSlider.valueProperty().addListener((o, ov, nv) ->
                speedValue.setText(String.format("%.0f", nv.doubleValue())));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row1 = new HBox(10,
                new Label("Algorithm"), algoBox,
                sep(),
                new Label("Size"), sizeSpinner,
                randomize,
                sep(),
                new Label("Target"), targetField, setTarget,
                spacer,
                new Label("Speed") {{ getStyleClass().add("dim"); }},
                speedSlider, speedValue);
        row1.setAlignment(Pos.CENTER_LEFT);

        HBox row2 = new HBox(10,
                playBtn, pauseBtn, stepBtn, resetBtn);
        row2.setAlignment(Pos.CENTER_LEFT);

        wrap.getChildren().addAll(row1, row2);
        return wrap;
    }

    private VBox buildCanvasBox() {
        cells.setAlignment(Pos.CENTER);
        arrows.setAlignment(Pos.CENTER);

        VBox stack = new VBox(4, cells, arrows);
        stack.setAlignment(Pos.CENTER);
        stack.setPadding(new Insets(30, 20, 30, 20));

        StackPane center = new StackPane(stack);
        center.setPadding(new Insets(0));
        center.setStyle("-fx-background-color: " + web(Theme.panel()) + "; " +
                "-fx-background-radius: 14; " +
                "-fx-border-color: " + web(Theme.border()) + "; " +
                "-fx-border-radius: 14; -fx-border-width: 1;");

        ScrollPane sp = new ScrollPane(center);
        sp.setFitToHeight(true);
        sp.getStyleClass().add("scroll-pane");
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        sp.setMinHeight(240);
        VBox.setVgrow(sp, Priority.ALWAYS);

        // Legend row at bottom of canvas
        HBox legend = new HBox(14);
        legend.setAlignment(Pos.CENTER);
        legend.setPadding(new Insets(6, 0, 4, 0));
        legend.getChildren().addAll(
                swatch(Theme.accent(), "current"),
                swatch(Theme.success(), "found"),
                swatch(Theme.panel2(), "unsearched"),
                swatch(Theme.panel(), "eliminated"),
                arrowLegend());

        VBox box = new VBox(6, sp, legend);
        VBox.setVgrow(sp, Priority.ALWAYS);
        return box;
    }

    private HBox arrowLegend() {
        Label a = new Label("▲");
        a.setTextFill(Theme.accent2());
        a.setFont(Font.font("System", FontWeight.BOLD, 12));
        Label l = new Label("matches target");
        l.getStyleClass().add("dim");
        l.setStyle("-fx-font-size: 11px;");
        HBox h = new HBox(4, a, l);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private HBox swatch(Color c, String label) {
        Region r = new Region();
        r.setPrefSize(12, 12);
        r.setMinSize(12, 12);
        r.setMaxSize(12, 12);
        r.setBackground(new Background(new BackgroundFill(c, new CornerRadii(3), Insets.EMPTY)));
        r.setBorder(new Border(new BorderStroke(Theme.border(),
                BorderStrokeStyle.SOLID, new CornerRadii(3), new BorderWidths(1))));
        Label l = new Label(label);
        l.getStyleClass().add("dim");
        l.setStyle("-fx-font-size: 11px;");
        HBox h = new HBox(5, r, l);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private VBox buildSidePanel() {
        VBox box = new VBox(12);

        // ---- Search state ----
        VBox stats = new VBox(8);
        stats.getStyleClass().add("panel");
        Label h = new Label("SEARCH STATE");
        h.getStyleClass().add("section-label");
        h.setPadding(new Insets(0));
        stats.getChildren().addAll(h,
                statRow("Comparisons", comparisonsLabel),
                statRow("Target",      targetLabel),
                statRow("Interval",    intervalLabel));

        // ---- Current step ----
        VBox stepBox = new VBox(6);
        stepBox.getStyleClass().add("panel");
        Label sh = new Label("CURRENT STEP");
        sh.getStyleClass().add("section-label");
        sh.setPadding(new Insets(0));
        stepLabel.getStyleClass().add("mono");
        stepLabel.setWrapText(true);
        stepLabel.setMaxWidth(Double.MAX_VALUE);
        stepLabel.setMinHeight(60);
        stepBox.getChildren().addAll(sh, stepLabel);

        // ---- Indices ----
        VBox idxBox = new VBox(6);
        idxBox.getStyleClass().add("panel");
        Label ih = new Label("INDICES");
        ih.getStyleClass().add("section-label");
        ih.setPadding(new Insets(0));
        indicesLabel.getStyleClass().add("mono");
        indicesLabel.setWrapText(true);
        idxBox.getChildren().addAll(ih, indicesLabel);

        // ---- Status ----
        VBox stat = new VBox(6);
        stat.getStyleClass().add("panel");
        Label sth = new Label("STATUS");
        sth.getStyleClass().add("section-label");
        sth.setPadding(new Insets(0));
        statusLabel.getStyleClass().add("mono");
        statusLabel.setWrapText(true);
        stat.getChildren().addAll(sth, statusLabel);

        // ---- Explanation ----
        VBox about = new VBox(6);
        about.getStyleClass().add("panel");
        Label ah = new Label("HOW IT WORKS");
        ah.getStyleClass().add("section-label");
        ah.setPadding(new Insets(0));
        Label text = new Label(
                "• Linear Search scans every element left to right — O(n).\n" +
                "• Binary Search halves the search interval each step — O(log n).\n" +
                "• Requires a sorted array.\n" +
                "• The ▲ marker shows every cell equal to the target.");
        text.getStyleClass().add("dim");
        text.setWrapText(true);
        text.setStyle("-fx-font-size: 11.5px;");
        about.getChildren().addAll(ah, text);

        box.getChildren().addAll(stats, stepBox, idxBox, stat, about);
        return box;
    }

    private HBox statRow(String k, Label v) {
        HBox h = new HBox(8);
        h.setAlignment(Pos.CENTER_LEFT);
        Label kl = new Label(k);
        kl.getStyleClass().add("dim");
        kl.setMinWidth(100); kl.setPrefWidth(100);
        v.getStyleClass().add("mono");
        v.setWrapText(true);
        HBox.setHgrow(v, Priority.ALWAYS);
        h.getChildren().addAll(kl, v);
        return h;
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
    //  Regeneration
    // ============================================================
    private void regenerate() { regenerate(true); }

    private void regenerate(boolean newArray) {
        stop();
        int n = sizeSpinner.getValue();

        if (newArray || data.length != n) {
            data = new int[n];
            Random r = new Random();
            for (int i = 0; i < n; i++) data[i] = 3 + r.nextInt(30);
            Arrays.sort(data);

            // 70% target is in the array, 30% it is not
            if (r.nextDouble() < 0.7) {
                target = data[r.nextInt(n)];
            } else {
                // pick a value guaranteed not to be in the array
                outer:
                for (int attempt = 0; attempt < 100; attempt++) {
                    int candidate = 1 + r.nextInt(40);
                    for (int v : data) if (v == candidate) continue outer;
                    target = candidate;
                    break;
                }
            }
        }
        resetPlayback();
        targetField.clear();
    }

    private void applyCustomTarget() {
        String s = targetField.getText().trim();
        if (s.isEmpty()) return;
        try {
            int v = Integer.parseInt(s);
            target = v;
            resetPlayback();
            targetField.clear();
        } catch (NumberFormatException ex) {
            statusLabel.setText("Invalid target: " + s);
        }
    }

    // ============================================================
    //  Playback
    // ============================================================
    private void resetPlayback() {
        stop();
        current = -1;
        found = -1;
        comparisons = 0;
        finished = false;
        stepDescription = "Ready — press Play or Step";
        linearIdx = 0;
        binLo = 0;
        binHi = data.length - 1;
        binInitialized = false;
        lo = -1;
        hi = -1;

        comparisonsLabel.setText("0");
        intervalLabel.setText("—");
        targetLabel.setText(String.valueOf(target));
        stepLabel.setText(stepDescription);
        indicesLabel.setText("—");
        statusLabel.setText(algoBox.getValue());

        render();
    }

    private void togglePlay() {
        if (playing) stop();
        else play();
    }

    private void play() {
        if (data.length == 0) return;
        if (finished) resetPlayback();
        if (timeline != null) return;

        playing = true;
        playBtn.setText("Playing");
        playBtn.setDisable(true);

        // Interval between steps: 1 → 900ms, 50 → 20ms
        double ms = Math.max(20, 900 - speedSlider.getValue() * 18);
        timeline = new Timeline(new KeyFrame(Duration.millis(ms), e -> stepOnce()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void stop() {
        playing = false;
        if (timeline != null) { timeline.stop(); timeline = null; }
        playBtn.setText("Play");
        playBtn.setDisable(false);
    }

    private void stepOnce() {
        if (data.length == 0 || finished) {
            stop();
            return;
        }
        String a = algoBox.getValue();
        if (a.equals("Linear Search")) stepLinear();
        else stepBinary();

        comparisonsLabel.setText(String.valueOf(comparisons));
        stepLabel.setText(stepDescription);
        updateIndices();
        render();

        if (finished) {
            stop();
            statusLabel.setText(found >= 0
                    ? "✓ Target found in " + comparisons + " comparisons"
                    : "✗ Target not present after " + comparisons + " comparisons");
            if (found >= 0) pulseCell(found);
        } else {
            statusLabel.setText(algoBox.getValue() + " — searching…");
        }
        app.ApplicationController.setOperations(comparisons);
    }

    private void stepLinear() {
        if (linearIdx >= data.length) {
            finished = true;
            found = -1;
            stepDescription = "Reached end of array — target " + target + " not found";
            return;
        }
        current = linearIdx;
        comparisons++;
        if (data[linearIdx] == target) {
            found = linearIdx;
            finished = true;
            stepDescription = "data[" + linearIdx + "] = " + data[linearIdx]
                    + "  ✓ equals target";
        } else {
            stepDescription = "data[" + linearIdx + "] = " + data[linearIdx]
                    + "  ≠ " + target + " — advance";
        }
        linearIdx++;
    }

    private void stepBinary() {
        if (!binInitialized) {
            binLo = 0;
            binHi = data.length - 1;
            binInitialized = true;
        }
        if (binLo > binHi) {
            finished = true;
            found = -1;
            stepDescription = "Search interval is empty — target " + target + " not in array";
            lo = binLo; hi = binHi;
            return;
        }
        int mid = (binLo + binHi) / 2;
        current = mid;
        comparisons++;
        if (data[mid] == target) {
            found = mid;
            finished = true;
            stepDescription = "data[" + mid + "] = " + data[mid] + "  ✓ equals target";
        } else if (data[mid] < target) {
            stepDescription = "data[" + mid + "] = " + data[mid] + "  < " + target
                    + " — discard left half, search right";
            binLo = mid + 1;
        } else {
            stepDescription = "data[" + mid + "] = " + data[mid] + "  > " + target
                    + " — discard right half, search left";
            binHi = mid - 1;
        }
        lo = binLo;
        hi = binHi;
    }

    private void updateIndices() {
        String a = algoBox.getValue();
        if (a.equals("Linear Search")) {
            indicesLabel.setText("i = " + Math.min(linearIdx - 1, data.length - 1)
                    + (found >= 0 ? "   found = " + found : ""));
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("lo = ").append(Math.max(0, lo)).append("   ");
            sb.append("hi = ").append(Math.max(0, hi)).append("   ");
            if (current >= 0) sb.append("mid = ").append(current);
            indicesLabel.setText(sb.toString());
        }
        // Update interval display
        if (a.equals("Binary Search")) {
            if (lo < 0 || hi < 0 || lo > hi) intervalLabel.setText("—");
            else intervalLabel.setText("[" + lo + " … " + hi + "]  ("
                    + (hi - lo + 1) + " cells)");
        } else {
            intervalLabel.setText("[0 … " + (data.length - 1) + "]");
        }
    }

    private void pulseCell(int index) {
        if (index < 0 || index >= cells.getChildren().size()) return;
        Node cell = cells.getChildren().get(index);
        ScaleTransition st = new ScaleTransition(Duration.millis(180), cell);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.18); st.setToY(1.18);
        st.setAutoReverse(true);
        st.setCycleCount(3);
        st.play();
    }

    // ============================================================
    //  Rendering
    // ============================================================
    private int cellSize() {
        int n = data.length;
        if (n == 0) return MAX_CELL_W;
        int s = (int) Math.floor(720.0 / n);
        if (s < MIN_CELL_W) s = MIN_CELL_W;
        if (s > MAX_CELL_W) s = MAX_CELL_W;
        return s;
    }

    private void render() {
        cells.getChildren().clear();
        arrows.getChildren().clear();

        int cs = cellSize();

        for (int i = 0; i < data.length; i++) {
            // ----- value cell -----
            Label l = new Label(String.valueOf(data[i]));
            l.setFont(Font.font("System", FontWeight.BOLD, Math.max(10, cs * 0.34)));
            l.setPrefSize(cs, cs);
            l.setMinSize(cs, cs);
            l.setMaxSize(cs, cs);
            l.setAlignment(Pos.CENTER);

            Color bg = Theme.panel2();
            Color fg = Theme.text();
            Color border = Theme.border();

            if (i == found) {
                bg = Theme.success();
                fg = Color.WHITE;
                border = Theme.success();
            } else if (i == current && !finished) {
                bg = Theme.accent();
                fg = Color.WHITE;
                border = Theme.accent2();
            } else if (isEliminated(i)) {
                bg = Theme.panel();
                fg = Theme.textDim();
                border = Theme.border();
            } else if (i == current && finished && found < 0) {
                // last checked cell in a not-found result
                bg = Theme.danger();
                fg = Color.WHITE;
                border = Theme.danger();
            }

            l.setStyle(
                    "-fx-background-color: " + web(bg) + ";" +
                    "-fx-text-fill: " + web(fg) + ";" +
                    "-fx-background-radius: 10;" +
                    "-fx-border-color: " + web(border) + ";" +
                    "-fx-border-radius: 10;" +
                    "-fx-border-width: 1.2;");
            if (i == found || i == current) {
                l.setEffect(new DropShadow(8, bg.deriveColor(0, 1, 1, 0.6)));
            }
            cells.getChildren().add(l);

            // ----- arrow below -----
            Label arrow = new Label(data[i] == target ? "▲" : " ");
            arrow.setFont(Font.font("System", FontWeight.BOLD, 12));
            arrow.setTextFill(data[i] == target ? Theme.accent2() : Color.TRANSPARENT);
            arrow.setPrefSize(cs, 18);
            arrow.setMinSize(cs, 18);
            arrow.setMaxSize(cs, 18);
            arrow.setAlignment(Pos.CENTER);
            arrows.getChildren().add(arrow);
        }
    }

    private boolean isEliminated(int i) {
        String a = algoBox.getValue();
        if (a.equals("Binary Search")) {
            return lo >= 0 && hi >= 0 && (i < lo || i > hi) && !finished;
        }
        // Linear: cells strictly before the current scan position
        if (found >= 0) return i < found;
        return i < linearIdx - 1;
    }

    // ============================================================
    //  Utils
    // ============================================================
    private String web(Color c) {
        return String.format("#%02x%02x%02x",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }
}