package app.ui.hanoi;

import app.algorithms.hanoi.HanoiSolver;
import app.algorithms.hanoi.HanoiSolver.Step;
import app.icons.Icons;
import app.theme.Theme;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.*;

public class HanoiView extends BorderPane {

    // =================== Model ===================
    private int diskCount = 5;
    private List<Step> steps = List.of();
    private int stepIndex = -1;
    private boolean animating = false;
    private boolean pendingStop = false;

    @SuppressWarnings("unchecked")
    private final Deque<Integer>[] towers = new ArrayDeque[3];

    // =================== Visual layers ===================
    private final Pane canvasPane = new Pane();
    private final Pane pegsLayer = new Pane();
    private final Pane disksLayer = new Pane();
    private final Pane effectsLayer = new Pane();
    private final List<DiskNode> disks = new ArrayList<>();

    // =================== Status ===================
    private final Label moveCounter = new Label("0 / 0");
    private final Label currentState = new Label("—");
    private final Label currentDisk = new Label("—");
    private final Label remaining = new Label("—");
    private final Label minimum = new Label("—");
    private final Label complexityLabel = new Label("Time  O(2ⁿ)\nSpace  O(n)");
    private final Label speedValue = new Label("15");

    // =================== Panels ===================
    private final VBox historyBox = new VBox(2);
    private final VBox callStackBox = new VBox(4);
    private final VBox recursionTreeBox = new VBox(2);
    private final CodePanel codePanel = new CodePanel();

    // =================== Controls ===================
    private final Slider speedSlider = new Slider(1, 50, 15);
    private final Button playBtn = new Button("Play", Icons.get("play", 16));
    private final Button pauseBtn = new Button("Pause", Icons.get("pause", 16));
    private final Button stepBtn = new Button("Step", Icons.get("step", 16));
    private final Button prevBtn = new Button("Prev", Icons.get("prev", 16));
    private final Button resetBtn = new Button("Reset", Icons.get("reset", 16));
    private final Button autoSolveBtn = new Button("Auto Solve");
    private final Spinner<Integer> diskSpinner = new Spinner<>(1, 15, diskCount);

    // =================== Timeline ===================
    private final Canvas timelineCanvas = new Canvas(600, 30);
    private final Label timelineHint = new Label("—");

    // =================== Math ===================
    private final Canvas mathCanvas = new Canvas(340, 180);

    private Timeline autoTimeline;

    // =============================================================
    //  DiskNode — single StackPane so rect + label never desync
    // =============================================================
    private static final class DiskNode {
        final int id;
        final Rectangle rect;
        final Label label;
        final StackPane view;
        final double width;

        DiskNode(int id, double width, double height) {
            this.id = id;
            this.width = width;
            this.rect = new Rectangle(width, height);
            double arc = Math.max(10, height * 0.55);
            rect.setArcWidth(arc);
            rect.setArcHeight(arc);
            rect.setStrokeWidth(1.0);

            label = new Label(String.valueOf(id));
            label.setFont(Font.font("System", FontWeight.EXTRA_BOLD,
                    Math.max(10, height * 0.55)));
            label.setTextFill(Color.WHITE);
            label.setMouseTransparent(true);
            label.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 2, 0, 0, 1);");

            view = new StackPane(rect, label);
            view.setPrefSize(width, height);
            view.setMinSize(width, height);
            view.setMaxSize(width, height);
            view.setPickOnBounds(true);
        }
    }

    // =============================================================
    //  Constructor
    // =============================================================
    @SuppressWarnings("unchecked")
    public HanoiView() {
        towers[0] = new ArrayDeque<>();
        towers[1] = new ArrayDeque<>();
        towers[2] = new ArrayDeque<>();

        setPadding(new Insets(18));
        setStyle("-fx-background-color: " + web(Theme.bg()) + ";");

        // ---- Header ----
        Label title = new Label("TOWER OF HANOI");
        title.getStyleClass().add("h1");
        Label sub = new Label("Recursive puzzle · step-by-step visualization");
        sub.getStyleClass().add("dim");
        VBox header = new VBox(2, title, sub);
        header.setPadding(new Insets(0, 0, 10, 4));

        // ---- Controls ----
        Region controls = buildControls();

        // ---- Canvas ----
        canvasPane.setStyle("-fx-background-color: " + web(Theme.panel()) + "; " +
                "-fx-background-radius: 14; -fx-border-radius: 14; " +
                "-fx-border-color: " + web(Theme.border()) + "; -fx-border-width: 1;");
        canvasPane.setMinHeight(380);
        canvasPane.setPrefHeight(460);

        pegsLayer.setMouseTransparent(true);
        effectsLayer.setMouseTransparent(true);
        canvasPane.getChildren().addAll(pegsLayer, disksLayer, effectsLayer);

        canvasPane.widthProperty().addListener((o, ov, nv) -> relayout());
        canvasPane.heightProperty().addListener((o, ov, nv) -> relayout());

        // ---- Timeline ----
        Region timeline = buildTimeline();

        VBox canvasBox = new VBox(10, canvasPane, timeline);
        VBox.setVgrow(canvasPane, Priority.ALWAYS);

        // ---- Inspector ----
        Region inspector = buildInspector();
        inspector.setPrefWidth(430);
        inspector.setMinWidth(360);
        inspector.setMaxWidth(560);

        VBox center = new VBox(14, header, controls, canvasBox);
        HBox.setHgrow(center, Priority.ALWAYS);

        HBox body = new HBox(16, center, inspector);
        HBox.setHgrow(center, Priority.ALWAYS);
        setCenter(body);

        // ---- Keyboard ----
        setFocusTraversable(true);
        setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case SPACE -> { if (autoTimeline != null) stopAuto(); else autoPlay(); }
                case RIGHT -> stepForward();
                case LEFT  -> stepBackward();
                case R     -> resetAlgorithm();
                case PLUS, EQUALS, ADD -> speedSlider.setValue(Math.min(50, speedSlider.getValue() + 3));
                case MINUS, SUBTRACT  -> speedSlider.setValue(Math.max(1, speedSlider.getValue() - 3));
                default -> { }
            }
        });

        Platform.runLater(() -> {
            resetAlgorithm();
            requestFocus();
        });
    }

    // =============================================================
    //  Controls
    // =============================================================
    private Region buildControls() {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12, 14, 12, 14));
        box.setStyle(panelStyle());

        Label diskLabel = new Label("DISKS");
        diskLabel.getStyleClass().add("section-label");
        diskLabel.setPadding(new Insets(0));

        diskSpinner.setPrefWidth(80);
        diskSpinner.setMinWidth(80);
        diskSpinner.valueProperty().addListener((o, ov, nv) -> {
            diskCount = nv;
            resetAlgorithm();
        });

        Button minus = iconBtn("minus", () -> { if (diskCount > 1) diskSpinner.decrement(); });
        Button plus  = iconBtn("plus",  () -> { if (diskCount < 15) diskSpinner.increment(); });

        HBox spinnerBox = new HBox(2, minus, diskSpinner, plus);
        spinnerBox.setAlignment(Pos.CENTER_LEFT);

        playBtn.getStyleClass().addAll("btn", "btn-primary");
        pauseBtn.getStyleClass().add("btn");
        stepBtn.getStyleClass().add("btn");
        prevBtn.getStyleClass().add("btn");
        resetBtn.getStyleClass().add("btn");
        for (Button b : List.of(playBtn, pauseBtn, stepBtn, prevBtn, resetBtn))
            b.setGraphicTextGap(8);

        playBtn.setOnAction(e -> autoPlay());
        pauseBtn.setOnAction(e -> stopAuto());
        stepBtn.setOnAction(e -> stepForward());
        prevBtn.setOnAction(e -> stepBackward());
        resetBtn.setOnAction(e -> resetAlgorithm());

        speedSlider.setPrefWidth(150);
        speedSlider.setMinWidth(120);
        speedSlider.setShowTickMarks(false);
        speedValue.getStyleClass().add("chip");
        speedValue.setMinWidth(34);
        speedValue.setAlignment(Pos.CENTER);
        speedSlider.valueProperty().addListener((o, ov, nv) ->
                speedValue.setText(String.format("%.0f", nv.doubleValue())));
        speedValue.setText("15");

        autoSolveBtn.getStyleClass().addAll("btn", "btn-accent2");
        autoSolveBtn.setOnAction(e -> {
            resetAlgorithm();
            Platform.runLater(this::autoPlay);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox speedBox = new HBox(6, speedSlider, speedValue);
        speedBox.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(
                diskLabel, spinnerBox, sep(),
                playBtn, pauseBtn, stepBtn, prevBtn, resetBtn,
                sep(),
                new Label("Speed") {{ getStyleClass().add("dim"); }},
                speedBox,
                spacer,
                autoSolveBtn);
        return box;
    }

    private Button iconBtn(String icon, Runnable action) {
        Button b = new Button();
        b.setGraphic(Icons.get(icon, 14));
        b.getStyleClass().add("icon-btn");
        b.setOnAction(e -> action.run());
        return b;
    }

    private Region sep() {
        Region r = new Region();
        r.setPrefWidth(1); r.setMinWidth(1); r.setPrefHeight(22);
        r.setBackground(new Background(new BackgroundFill(
                Theme.border(), CornerRadii.EMPTY, Insets.EMPTY)));
        return r;
    }

    // =============================================================
    //  Inspector (right side)
    // =============================================================
    private Region buildInspector() {
        VBox box = new VBox(12);

        VBox info = new VBox(10);
        info.getStyleClass().add("panel");
        Label h = new Label("STATUS");
        h.getStyleClass().add("section-label");
        h.setPadding(new Insets(0));
        info.getChildren().addAll(h,
                row("Current Move",    moveCounter),
                row("Current State",   currentState),
                row("Disk",            currentDisk),
                row("Remaining Moves", remaining),
                row("Minimum Moves",   minimum),
                row("Complexity",      complexityLabel));

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("tab-pane");
        tabs.getTabs().addAll(
                new Tab("RECURSION", wrap(recursionTreeBox)),
                new Tab("STACK",     wrap(callStackBox)),
                new Tab("CODE",      codePanel),
                new Tab("HISTORY",   wrap(historyBox)),
                new Tab("MATH",      buildMathPanel()));
        VBox.setVgrow(tabs, Priority.ALWAYS);

        box.getChildren().addAll(info, tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        return box;
    }

    private Node wrap(Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("scroll-pane");
        sp.setPadding(new Insets(10));
        return sp;
    }

    private HBox row(String k, Label v) {
        HBox h = new HBox(10);
        h.setAlignment(Pos.CENTER_LEFT);
        Label kl = new Label(k);
        kl.getStyleClass().add("dim");
        kl.setMinWidth(120);
        kl.setPrefWidth(120);
        v.getStyleClass().add("mono");
        v.setWrapText(true);
        HBox.setHgrow(v, Priority.ALWAYS);
        h.getChildren().addAll(kl, v);
        return h;
    }

    // =============================================================
    //  Timeline
    // =============================================================
    private Region buildTimeline() {
        VBox box = new VBox(4);
        box.setPadding(new Insets(10, 14, 12, 14));
        box.setStyle(panelStyle());

        HBox head = new HBox(8);
        Label t = new Label("STATE TIMELINE");
        t.getStyleClass().add("section-label");
        t.setPadding(new Insets(0));
        timelineHint.getStyleClass().add("dim");
        timelineHint.setStyle("-fx-font-size: 11px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        head.getChildren().addAll(t, spacer, timelineHint);

        timelineCanvas.setHeight(28);
        timelineCanvas.widthProperty().addListener((o, ov, nv) -> drawTimeline());
        timelineCanvas.setOnMouseClicked(e -> onTimelineClick(e.getX()));
        timelineCanvas.setOnMouseMoved(e -> onTimelineHover(e.getX()));

        box.getChildren().addAll(head, timelineCanvas);
        return box;
    }

    private void drawTimeline() {
        GraphicsContext g = timelineCanvas.getGraphicsContext2D();
        double W = timelineCanvas.getWidth(), H = timelineCanvas.getHeight();
        g.clearRect(0, 0, W, H);
        if (steps.isEmpty()) return;

        double padX = 8;
        double usable = W - padX * 2;
        int n = steps.size();
        double step = usable / n;

        // base line
        g.setStroke(Theme.border());
        g.setLineWidth(2);
        g.strokeLine(padX, H / 2, W - padX, H / 2);

        // dots
        double dotR = Math.max(1.6, Math.min(4, step * 0.30));
        for (int i = 0; i < n; i++) {
            double x = padX + (i + 0.5) * step;
            boolean done = i <= stepIndex;
            g.setFill(done ? Theme.accent() : Theme.panel3());
            g.fillOval(x - dotR, H / 2 - dotR, dotR * 2, dotR * 2);
        }
        // current highlight
        if (stepIndex >= 0 && stepIndex < n) {
            double x = padX + (stepIndex + 0.5) * step;
            double ringR = Math.max(4, dotR * 2.2);
            g.setStroke(Theme.accent2());
            g.setLineWidth(2);
            g.strokeOval(x - ringR, H / 2 - ringR, ringR * 2, ringR * 2);
        }
    }

    private void onTimelineClick(double x) {
        if (steps.isEmpty() || animating) return;
        double padX = 8;
        double usable = timelineCanvas.getWidth() - padX * 2;
        int n = steps.size();
        double step = usable / n;
        int idx = (int) Math.floor((x - padX) / step);
        if (idx < 0) idx = -1;   // → start
        if (idx >= n) idx = n - 1;
        rebuildTo(idx);
    }

    private void onTimelineHover(double x) {
        if (steps.isEmpty()) { timelineHint.setText("—"); return; }
        double padX = 8;
        double usable = timelineCanvas.getWidth() - padX * 2;
        int n = steps.size();
        double step = usable / n;
        int idx = (int) Math.floor((x - padX) / step);
        if (idx < 0) idx = 0;
        if (idx >= n) idx = n - 1;
        Step s = steps.get(idx);
        timelineHint.setText(String.format("Move %d / %d   ·   disk %d   %c → %c",
                s.moveNumber(), n, s.disk(), s.from(), s.to()));
    }

    // =============================================================
    //  Reset / rebuild
    // =============================================================
    private void resetAlgorithm() {
        stopAuto();
        animating = false;

        pegsLayer.getChildren().clear();
        disksLayer.getChildren().clear();
        effectsLayer.getChildren().clear();
        disks.clear();

        for (Deque<Integer> t : towers) t.clear();
        for (int i = diskCount; i >= 1; i--) towers[0].push(i);

        steps = HanoiSolver.solve(diskCount);
        stepIndex = -1;

        historyBox.getChildren().clear();
        callStackBox.getChildren().clear();
        recursionTreeBox.getChildren().clear();
        codePanel.reset();

        buildDiskNodes();
        Platform.runLater(() -> {
            layoutStatic();
            relayout();
            drawTimeline();
            drawMathGraph();
            updateStatus();
            updateRecursionPanel();
            updateCallStack();
        });
    }

    private void buildDiskNodes() {
        double W = Math.max(canvasPane.getWidth(), 600);
        double H = Math.max(canvasPane.getHeight(), 400);
        double towerGap = W / 3.0;
        double maxDiskW = towerGap * 0.82;
        double diskH = Math.min(26, (H - 120) / Math.max(10, diskCount + 4));

        for (int i = 1; i <= diskCount; i++) {
            double w = (maxDiskW * 0.30) + (maxDiskW * 0.70) * (i / (double) diskCount);
            DiskNode dn = new DiskNode(i, w, Math.max(16, diskH));

            Color c1 = diskColor(i);
            dn.rect.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, c1.brighter().brighter()),
                    new Stop(0.45, c1),
                    new Stop(1, c1.darker().darker())));
            dn.rect.setStroke(c1.brighter());
            dn.rect.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.40)));

            int diskId = i;
            dn.view.setOnMouseEntered(e -> {
                if (!animating) {
                    ScaleTransition st = new ScaleTransition(Duration.millis(140), dn.view);
                    st.setToX(1.045); st.setToY(1.045);
                    st.play();
                }
            });
            dn.view.setOnMouseExited(e -> {
                if (!animating) {
                    ScaleTransition st = new ScaleTransition(Duration.millis(140), dn.view);
                    st.setToX(1.0); st.setToY(1.0);
                    st.play();
                }
            });
            dn.view.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && !animating) {
                    // Clicking a disk jumps to the last state where it stays put? Simpler: pulse it.
                    pulse(dn);
                }
            });

            disks.add(dn);
            disksLayer.getChildren().add(dn.view);
        }
    }

    private Color diskColor(int i) {
        double hue = (i * 37.0 + 190) % 360;
        return Color.hsb(hue, 0.60, Theme.isDark() ? 0.88 : 0.80);
    }

    /** Draw pegs, base, and labels once per reset/resize. */
    private void layoutStatic() {
        pegsLayer.getChildren().clear();
        double W = Math.max(canvasPane.getWidth(), 600);
        double H = Math.max(canvasPane.getHeight(), 400);
        double towerGap = W / 3.0;
        double baseY = H - 46;
        double pegH = H * 0.66;

        // Ground bar
        Rectangle ground = new Rectangle(W * 0.06, baseY + 10, W * 0.88, 4);
        ground.setArcWidth(4); ground.setArcHeight(4);
        ground.setFill(Theme.panel3());
        pegsLayer.getChildren().add(ground);

        String[] tags = {"SOURCE", "AUXILIARY", "DESTINATION"};
        for (int t = 0; t < 3; t++) {
            double cx = towerGap * (t + 0.5);

            Rectangle peg = new Rectangle(cx - 4, baseY - pegH, 8, pegH);
            peg.setArcWidth(8); peg.setArcHeight(8);
            peg.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Theme.panel2()),
                    new Stop(0.5, Theme.panel3()),
                    new Stop(1, Theme.panel2())));
            peg.setStroke(Theme.border());
            pegsLayer.getChildren().add(peg);

            Rectangle base = new Rectangle(cx - towerGap * 0.44, baseY, towerGap * 0.88, 10);
            base.setArcWidth(10); base.setArcHeight(10);
            base.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Theme.panel3()),
                    new Stop(1, Theme.panel2())));
            base.setStroke(Theme.border());
            pegsLayer.getChildren().add(base);

            VBox lbl = new VBox(-2);
            Label t1 = new Label(tags[t]);
            t1.setFont(Font.font("System", FontWeight.BOLD, 10));
            t1.setTextFill(Theme.textDim());
            Label t2 = new Label(String.valueOf((char) ('A' + t)));
            t2.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 18));
            t2.setTextFill(Theme.text());
            lbl.getChildren().addAll(t1, t2);
            lbl.setAlignment(Pos.CENTER);
            lbl.setLayoutX(cx - 50);
            lbl.setLayoutY(baseY + 20);
            lbl.setPrefWidth(100);
            lbl.setMinWidth(100);
            lbl.setMaxWidth(100);
            pegsLayer.getChildren().add(lbl);
        }
    }

    /** Reposition all disks instantly (used after resize/reset/rebuild). */
    private void relayout() {
        if (animating) return;
        if (canvasPane.getWidth() <= 0 || canvasPane.getHeight() <= 0) return;

        layoutStatic();

        double W = canvasPane.getWidth();
        double H = canvasPane.getHeight();
        double towerGap = W / 3.0;
        double baseY = H - 46;
        double diskH = Math.min(26, (H - 120) / Math.max(10, diskCount + 4));

        for (int t = 0; t < 3; t++) {
            double cx = towerGap * (t + 0.5);
            Deque<Integer> tower = towers[t];
            int idx = 0;
            for (Integer d : tower) {
                DiskNode dn = disks.get(d - 1);
                double x = cx - dn.width / 2;
                double y = baseY - (idx + 1) * (diskH + 2) - 2;
                dn.view.setTranslateX(0);
                dn.view.setTranslateY(0);
                dn.view.setLayoutX(x);
                dn.view.setLayoutY(y);
                idx++;
            }
        }
    }

    // =============================================================
    //  Status
    // =============================================================
    private void updateStatus() {
        int total = steps.size();
        int cur = stepIndex + 1;
        moveCounter.setText(cur + " / " + total);
        minimum.setText(String.valueOf(HanoiSolver.minMoves(diskCount)));
        remaining.setText(String.valueOf(Math.max(0, total - cur)));
        if (stepIndex >= 0 && stepIndex < total) {
            Step s = steps.get(stepIndex);
            currentState.setText(s.from() + " → " + s.to());
            currentDisk.setText(String.valueOf(s.disk()));
        } else {
            currentState.setText("—");
            currentDisk.setText("—");
        }
        drawTimeline();
    }

    // =============================================================
    //  Step / Play
    // =============================================================
    private void stepForward() {
        if (animating || steps.isEmpty()) return;
        if (stepIndex + 1 >= steps.size()) { stopAuto(); return; }
        applyStep(stepIndex + 1, null);
    }

    private void stepBackward() {
        if (animating || steps.isEmpty()) return;
        int target = Math.max(-1, stepIndex - 1);
        rebuildTo(target);
    }

    private void rebuildTo(int target) {
        stopAuto();
        for (Deque<Integer> t : towers) t.clear();
        for (int i = diskCount; i >= 1; i--) towers[0].push(i);
        for (int i = 0; i <= target; i++) {
            Step s = steps.get(i);
            int from = s.from() - 'A';
            int to   = s.to() - 'A';
            Integer d = towers[from].poll();
            if (d != null) towers[to].push(d);
        }
        stepIndex = target;
        relayout();
        updateStatus();
        updateHistorySelection();
        updateRecursionPanel();
        updateCallStack();
        if (target >= 0) codePanel.highlight(steps.get(target).codeLine());
        else codePanel.highlight(-1);
    }

    /**
     * The heart of the animation. The model is updated only AFTER the animation
     * finishes, which prevents the destination slot being reserved prematurely.
     */
    private void applyStep(int idx, Runnable onComplete) {
        Step s = steps.get(idx);
        int from = s.from() - 'A';
        int to   = s.to() - 'A';

        Integer top = towers[from].peek();
        if (top == null || !top.equals(s.disk())) {
            if (onComplete != null) onComplete.run();
            return;
        }

        DiskNode dn = disks.get(s.disk() - 1);

        double W = canvasPane.getWidth();
        double H = canvasPane.getHeight();
        double towerGap = W / 3.0;
        double baseY = H - 46;
        double diskH = Math.min(26, (H - 120) / Math.max(10, diskCount + 4));

        double cxTo = towerGap * (to + 0.5);
        double targetX = cxTo - dn.width / 2;
        double targetY = baseY - (towers[to].size() + 1) * (diskH + 2) - 2;

        double startX = dn.view.getLayoutX();
        double startY = dn.view.getLayoutY();
        double peakY = baseY - H * 0.66 - 10;

        // Speed profile: 1 = slow (1.5 s total), 50 = fast (~60 ms total)
        double v = speedSlider.getValue();
        double upMs   = Math.max(50, 350 - v * 6);
        double moveMs = Math.max(80, 700 - v * 12);
        double downMs = upMs;

        animating = true;
        playBtn.setDisable(true);
        stepBtn.setDisable(true);
        prevBtn.setDisable(true);
        resetBtn.setDisable(true);

        // Glow the moving disk
        Glow glow = new Glow(0.55);
        dn.view.setEffect(glow);

        TranslateTransition up = new TranslateTransition(Duration.millis(upMs), dn.view);
        up.setToY(peakY - startY);
        up.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition across = new TranslateTransition(Duration.millis(moveMs), dn.view);
        across.setToX(targetX - startX);
        across.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition down = new TranslateTransition(Duration.millis(downMs), dn.view);
        down.setToY(targetY - peakY);
        down.setInterpolator(Interpolator.EASE_IN);

        SequentialTransition seq = new SequentialTransition(up, across, down);
        seq.setOnFinished(e -> {
            // Snap to exact final position
            dn.view.setTranslateX(0);
            dn.view.setTranslateY(0);
            dn.view.setLayoutX(targetX);
            dn.view.setLayoutY(targetY);
            dn.view.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.40)));

            // Update model AFTER animation
            towers[from].poll();
            towers[to].push(s.disk());

            stepIndex = idx;
            updateStatus();
            addHistoryEntry(s);
            updateHistorySelection();
            updateCallStack();
            updateRecursionPanel();
            codePanel.highlight(s.codeLine());
            highlightActiveStack();

            app.ApplicationController.setOperations(idx + 1);
            app.ApplicationController.setComplexity("O(2ⁿ)");

            animating = false;
            playBtn.setDisable(false);
            stepBtn.setDisable(false);
            prevBtn.setDisable(false);
            resetBtn.setDisable(false);

            if (onComplete != null) onComplete.run();
        });
        seq.play();
    }

    private void pulse(DiskNode dn) {
        ScaleTransition st = new ScaleTransition(Duration.millis(180), dn.view);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.15); st.setToY(1.15);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    private void autoPlay() {
        if (steps.isEmpty()) return;
        if (stepIndex + 1 >= steps.size()) resetAlgorithm();
        if (autoTimeline != null) return;
        pendingStop = false;
        playBtn.setText("Playing");
        playBtn.setDisable(true);
        stepInternal();
    }

    private void stepInternal() {
        if (pendingStop) { stopAuto(); return; }
        if (stepIndex + 1 >= steps.size()) { stopAuto(); return; }
        applyStep(stepIndex + 1, () -> {
            if (pendingStop) { stopAuto(); return; }
            PauseTransition pt = new PauseTransition(Duration.millis(stepDelay()));
            pt.setOnFinished(e -> stepInternal());
            pt.play();
        });
    }

    private double stepDelay() {
        double v = speedSlider.getValue();
        return Math.max(20, 320 - v * 6);
    }

    private void stopAuto() {
        pendingStop = true;
        if (autoTimeline != null) { autoTimeline.stop(); autoTimeline = null; }
        playBtn.setText("Play");
        playBtn.setDisable(false);
        // don't re-enable step/prev/reset here; applyStep's onFinished will
    }

    // =============================================================
    //  History
    // =============================================================
    private void addHistoryEntry(Step s) {
        Label l = new Label(String.format("#%02d   Disk %d   %c → %c",
                s.moveNumber(), s.disk(), s.from(), s.to()));
        l.getStyleClass().add("mono-dim");
        l.setPadding(new Insets(4, 8, 4, 8));
        l.setStyle("-fx-background-radius: 6;");
        int idx = s.moveNumber() - 1;
        l.setOnMouseClicked(e -> { if (!animating) rebuildTo(idx); });
        l.setOnMouseEntered(e -> l.setStyle(
                "-fx-background-color: " + web(Theme.panel2()) + "; -fx-background-radius: 6;"));
        l.setOnMouseExited(e -> {
            if (idx != stepIndex) l.setStyle("-fx-background-radius: 6;");
        });
        historyBox.getChildren().add(l);
        if (historyBox.getParent() != null &&
                historyBox.getParent().getParent() instanceof ScrollPane sp) {
            Platform.runLater(() -> sp.setVvalue(1.0));
        }
    }

    private void updateHistorySelection() {
        int n = historyBox.getChildren().size();
        for (int i = 0; i < n; i++) {
            Node child = historyBox.getChildren().get(i);
            if (i == stepIndex)
                child.setStyle("-fx-background-color: " + web(Theme.panel3()) +
                        "; -fx-background-radius: 6;");
            else child.setStyle("-fx-background-radius: 6;");
        }
    }

    // =============================================================
    //  Recursion panels
    // =============================================================
    private void updateCallStack() {
        callStackBox.getChildren().clear();
        if (stepIndex < 0) {
            Label empty = new Label("Not started");
            empty.getStyleClass().add("dim");
            callStackBox.getChildren().add(empty);
            return;
        }
        List<HanoiSolver.Frame> stack = steps.get(stepIndex).stack();
        for (int i = stack.size() - 1; i >= 0; i--) {
            HanoiSolver.Frame f = stack.get(i);
            Label l = new Label(String.format("solve(%d, %c, %c, %c)",
                    f.n(), f.src(), f.dst(), f.aux()));
            l.getStyleClass().add("mono");
            l.setPadding(new Insets(8, 12, 8, 12));
            l.setMaxWidth(Double.MAX_VALUE);
            l.setStyle("-fx-background-color: " + web(Theme.panel2()) +
                    "; -fx-background-radius: 8;");
            callStackBox.getChildren().add(l);
        }
    }

    private void highlightActiveStack() {
        for (Node n : callStackBox.getChildren()) n.setStyle(n.getStyle() + "");
        if (callStackBox.getChildren().isEmpty()) return;
        Node last = callStackBox.getChildren().get(callStackBox.getChildren().size() - 1);
        last.setStyle(last.getStyle() +
                "-fx-border-color: " + web(Theme.accent()) +
                "; -fx-border-radius: 8; -fx-border-width: 1.5;");
    }

    private void updateRecursionPanel() {
        recursionTreeBox.getChildren().clear();
        List<HanoiSolver.Frame> stack = stepIndex >= 0
                ? steps.get(stepIndex).stack()
                : List.of(new HanoiSolver.Frame(diskCount, 'A', 'B', 'C'));
        for (int i = 0; i < stack.size(); i++) {
            HanoiSolver.Frame f = stack.get(i);
            HBox row = new HBox(4);
            StringBuilder indent = new StringBuilder();
            for (int k = 0; k < i; k++) indent.append("   ");
            Label l = new Label(indent + (i == stack.size() - 1 ? "▶ " : "│ ") +
                    String.format("solve(%d, %c, %c, %c)",
                            f.n(), f.src(), f.dst(), f.aux()));
            l.getStyleClass().add("mono");
            if (i == stack.size() - 1) {
                l.setTextFill(Theme.accent());
                l.setStyle("-fx-font-weight: 700;");
            }
            row.getChildren().add(l);
            recursionTreeBox.getChildren().add(row);
        }
    }

    // =============================================================
    //  Math panel
    // =============================================================
    private Node buildMathPanel() {
        VBox v = new VBox(10);
        v.setPadding(new Insets(12));
        Label formula = new Label("T(n) = 2·T(n−1) + 1");
        formula.getStyleClass().add("mono");
        Label formula2 = new Label("T(n) = 2ⁿ − 1");
        formula2.getStyleClass().add("mono");
        formula2.setTextFill(Theme.accent());
        Label hdr = new Label("MINIMUM MOVES");
        hdr.getStyleClass().add("section-label");
        hdr.setPadding(new Insets(0));
        v.getChildren().addAll(hdr, formula, formula2, mathCanvas);
        drawMathGraph();
        return v;
    }

    private void drawMathGraph() {
        GraphicsContext g = mathCanvas.getGraphicsContext2D();
        double W = mathCanvas.getWidth(), H = mathCanvas.getHeight();
        g.clearRect(0, 0, W, H);

        double padL = 46, padR = 16, padT = 14, padB = 30;
        double x0 = padL, x1 = W - padR;
        double y0 = H - padB, y1 = padT;

        // grid
        g.setStroke(Theme.grid());
        g.setLineWidth(1);
        for (int i = 0; i <= 4; i++) {
            double y = y0 - (y0 - y1) * i / 4.0;
            g.strokeLine(x0, y, x1, y);
        }

        // axes
        g.setStroke(Theme.border());
        g.setLineWidth(1.5);
        g.strokeLine(x0, y0, x1, y0);
        g.strokeLine(x0, y0, x0, y1);

        g.setFill(Theme.textDim());
        g.setFont(Font.font("System", 10));
        g.fillText("n (disks)", (x0 + x1) / 2 - 20, H - 8);
        g.save();
        g.translate(14, (y0 + y1) / 2 + 8);
        g.rotate(-90);
        g.fillText("moves", -18, 0);
        g.restore();

        long maxMoves = Math.max(1, HanoiSolver.minMoves(diskCount));

        // area fill under curve
        g.beginPath();
        g.moveTo(x0, y0);
        for (int n = 1; n <= diskCount; n++) {
            double x = x0 + (x1 - x0) * (n - 1) / (double) Math.max(1, diskCount - 1);
            double y = y0 - (y0 - y1) * (HanoiSolver.minMoves(n) / (double) maxMoves);
            g.lineTo(x, y);
        }
        g.lineTo(x1, y0);
        g.closePath();
        g.setFill(Theme.accent().deriveColor(0, 1, 1, 0.14));
        g.fill();

        // curve
        g.setStroke(Theme.accent());
        g.setLineWidth(2.4);
        g.beginPath();
        for (int n = 1; n <= diskCount; n++) {
            double x = x0 + (x1 - x0) * (n - 1) / (double) Math.max(1, diskCount - 1);
            double y = y0 - (y0 - y1) * (HanoiSolver.minMoves(n) / (double) maxMoves);
            if (n == 1) g.moveTo(x, y); else g.lineTo(x, y);
        }
        g.stroke();

        // points
        g.setFill(Theme.accent());
        for (int n = 1; n <= diskCount; n++) {
            double x = x0 + (x1 - x0) * (n - 1) / (double) Math.max(1, diskCount - 1);
            double y = y0 - (y0 - y1) * (HanoiSolver.minMoves(n) / (double) maxMoves);
            g.fillOval(x - 3, y - 3, 6, 6);
        }

        // axis labels
        g.setFill(Theme.textDim());
        g.setFont(Font.font("System", 10));
        for (int n = 1; n <= diskCount; n++) {
            if (diskCount > 8 && n % 2 != 0 && n != 1 && n != diskCount) continue;
            double x = x0 + (x1 - x0) * (n - 1) / (double) Math.max(1, diskCount - 1);
            g.fillText(String.valueOf(n), x - 3, y0 + 14);
        }
    }

    // =============================================================
    //  Helpers
    // =============================================================
    private String panelStyle() {
        return "-fx-background-color: " + web(Theme.panel()) + "; " +
                "-fx-background-radius: 12; " +
                "-fx-border-color: " + web(Theme.border()) + "; " +
                "-fx-border-radius: 12; -fx-border-width: 1;";
    }

    private String web(Color c) {
        return String.format("#%02x%02x%02x",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    // =============================================================
    //  Code panel
    // =============================================================
    private class CodePanel extends ScrollPane {
        private final String[] lines = {
                "void solve(int n, char source,",
                "           char target, char auxiliary) {",
                "    if (n == 1) { move(source, target); return; }",
                "    solve(n - 1, source, auxiliary, target);",
                "    move(source, target);",
                "    solve(n - 1, auxiliary, target, source);",
                "}"
        };
        private final List<Label> labels = new ArrayList<>();

        CodePanel() {
            setFitToWidth(true);
            getStyleClass().add("scroll-pane");
            VBox box = new VBox(2);
            box.setPadding(new Insets(10));
            for (int i = 0; i < lines.length; i++) {
                Label l = new Label(String.format("%2d  %s", i + 1, lines[i]));
                l.getStyleClass().add("mono");
                l.setPadding(new Insets(3, 8, 3, 8));
                l.setStyle("-fx-background-radius: 6;");
                labels.add(l);
                box.getChildren().add(l);
            }
            setContent(box);
        }

        void highlight(int idx) {
            for (int i = 0; i < labels.size(); i++) {
                if (i == idx) {
                    labels.get(i).setStyle(
                            "-fx-background-color: rgba(77,158,255,0.18); " +
                            "-fx-background-radius: 6; " +
                            "-fx-text-fill: " + web(Theme.accent()) + "; " +
                            "-fx-font-weight: 700;");
                } else {
                    labels.get(i).setStyle("-fx-background-radius: 6;");
                }
            }
        }

        void reset() { highlight(-1); }
    }
}