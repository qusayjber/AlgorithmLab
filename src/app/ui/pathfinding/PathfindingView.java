package app.ui.pathfinding;

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
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.*;

public class PathfindingView extends BorderPane {

    // ================= Grid & terrain =================
    private static final int COLS = 40;
    private static final int ROWS = 25;

    // Terrain types
    private static final int T_EMPTY    = 0;   // cost 1
    private static final int T_FOREST   = 1;   // cost 3
    private static final int T_WATER    = 2;   // cost 5
    private static final int T_MOUNTAIN = 3;   // cost 10
    private static final int T_WALL     = 4;   // impassable

    private static int costOf(int t) {
        return switch (t) {
            case T_FOREST   -> 3;
            case T_WATER    -> 5;
            case T_MOUNTAIN -> 10;
            default          -> 1;
        };
    }

    // ================= State =================
    private final int[][] terrain = new int[ROWS][COLS];
    private int startR = 12, startC = 3;
    private int endR   = 12, endC   = 36;

    private boolean[][] revealedVisited = new boolean[ROWS][COLS];
    private boolean[][] revealedPath    = new boolean[ROWS][COLS];

    // A* scores (populated after a run, for the inspector)
    private int[][] gScore = new int[ROWS][COLS];
    private int[][] hScore = new int[ROWS][COLS];
    private int[][] fScore = new int[ROWS][COLS];
    private boolean hasScores = false;

    // Animation
    private record Ev(int r, int c, boolean isPath) {}
    private List<Ev> events = new ArrayList<>();
    private int stepIdx = -1;
    private Timeline animation;
    private boolean playing = false;

    // Live counters during animation
    private int liveVisited = 0;
    private int livePath    = 0;

    // ================= UI =================
    private final Canvas canvas = new Canvas();
    private final StackPane canvasHolder = new StackPane();
    private double cellSize = 16;
    private double gridX0 = 0, gridY0 = 0;

    private final ComboBox<String> algoBox = new ComboBox<>();
    private final Slider speedSlider = new Slider(1, 50, 15);
    private final Label speedValue = new Label("15");
    private final ToggleGroup toolGroup = new ToggleGroup();

    private final Label visitedLabel = new Label("0");
    private final Label pathLabel    = new Label("0");
    private final Label costLabel    = new Label("0");
    private final Label statusLabel  = new Label("Ready");

    private final Label cellInfo = new Label("Hover a cell to inspect");

    private final Button playBtn  = new Button("Play",  Icons.get("play", 14));
    private final Button pauseBtn = new Button("Pause", Icons.get("pause", 14));
    private final Button stepBtn  = new Button("Step",  Icons.get("step", 14));

    private int hoverR = -1, hoverC = -1;

    public PathfindingView() {
        setPadding(new Insets(18));
        setStyle("-fx-background-color: " + web(Theme.bg()) + ";");

        // ---------- Header ----------
        Label title = new Label("PATHFINDING LABORATORY");
        title.getStyleClass().add("h1");
        Label sub = new Label("BFS · Dijkstra · A* — weighted grid, real maze generation, live metrics");
        sub.getStyleClass().add("dim");
        VBox header = new VBox(2, title, sub);

        // ---------- Controls ----------
        Region controls = buildControls();

        // ---------- Canvas ----------
        canvasHolder.setMinSize(300, 240);
        canvasHolder.setStyle("-fx-background-color: " + web(Theme.panel()) + "; " +
                "-fx-background-radius: 14; " +
                "-fx-border-color: " + web(Theme.border()) + "; " +
                "-fx-border-radius: 14; -fx-border-width: 1;");
        canvas.widthProperty().bind(canvasHolder.widthProperty().subtract(8));
        canvas.heightProperty().bind(canvasHolder.heightProperty().subtract(8));
        canvasHolder.getChildren().add(canvas);
        canvasHolder.setAlignment(Pos.CENTER);
        VBox.setVgrow(canvasHolder, Priority.ALWAYS);

        canvas.widthProperty().addListener((o, ov, nv) -> draw());
        canvas.heightProperty().addListener((o, ov, nv) -> draw());
        canvas.setOnMousePressed(this::onMouseDown);
        canvas.setOnMouseDragged(this::onMouseDrag);
        canvas.setOnMouseMoved(this::onMouseMove);
        canvas.setOnMouseExited(e -> { hoverR = hoverC = -1; updateCellInfo(); draw(); });

        // ---------- Legend ----------
        Region legend = buildLegend();

        // ---------- Side panel ----------
        VBox stats = (VBox) buildSidePanel();
        stats.setPrefWidth(280);
        stats.setMinWidth(240);

        // ---------- Layout ----------
        VBox center = new VBox(12, header, controls, canvasHolder, legend);
        HBox.setHgrow(center, Priority.ALWAYS);

        HBox body = new HBox(16, center, stats);
        setCenter(body);

        // ---------- Initial ----------
        loadPreset("Random weights");
    }

    // ============================================================
    //  Controls
    // ============================================================
    private Region buildControls() {
        VBox wrap = new VBox(8);
        wrap.setPadding(new Insets(10, 12, 10, 12));
        wrap.setStyle(panelStyle());

        // Row 1: algorithm + tools
        algoBox.getItems().addAll("BFS", "Dijkstra", "A*");
        algoBox.getSelectionModel().selectFirst();
        algoBox.getStyleClass().add("field");
        algoBox.setPrefWidth(110);
        algoBox.setMinWidth(110);

        HBox row1 = new HBox(8,
                new Label("Algorithm"), algoBox,
                sep(),
                new Label("Tool"),
                toolBtn("Wall",     "wall",     true),
                toolBtn("Erase",    "erase",    false),
                toolBtn("Forest",   "forest",   false),
                toolBtn("Water",    "water",    false),
                toolBtn("Mountain", "mountain", false),
                toolBtn("Start",    "start",    false),
                toolBtn("End",      "end",      false));
        row1.setAlignment(Pos.CENTER_LEFT);

        // Row 2: run + animation + speed
        playBtn.getStyleClass().addAll("btn", "btn-primary");
        pauseBtn.getStyleClass().add("btn");
        stepBtn.getStyleClass().add("btn");
        playBtn.setGraphicTextGap(6);
        pauseBtn.setGraphicTextGap(6);
        stepBtn.setGraphicTextGap(6);
        playBtn.setOnAction(e -> togglePlay());
        pauseBtn.setOnAction(e -> stopAnimation());
        stepBtn.setOnAction(e -> stepOnce());

        Button runBtn = new Button("Run", Icons.get("play", 14));
        runBtn.getStyleClass().addAll("btn", "btn-accent2");
        runBtn.setGraphicTextGap(6);
        runBtn.setOnAction(e -> runAlgorithm());

        Button resetBtn = new Button("Clear Path", Icons.get("reset", 14));
        resetBtn.getStyleClass().add("btn");
        resetBtn.setGraphicTextGap(6);
        resetBtn.setOnAction(e -> clearPath());

        Button mazeBtn = new Button("Maze", Icons.get("maze", 14));
        mazeBtn.getStyleClass().add("btn");
        mazeBtn.setGraphicTextGap(6);
        mazeBtn.setOnAction(e -> generateMaze());

        Button clearBtn = new Button("Clear All");
        clearBtn.getStyleClass().add("btn");
        clearBtn.setOnAction(e -> clearAll());

        Button presetBtn = new Button("Random Weights");
        presetBtn.getStyleClass().add("btn");
        presetBtn.setOnAction(e -> loadPreset("Random weights"));

        speedSlider.setPrefWidth(140);
        speedSlider.setMinWidth(110);
        speedValue.getStyleClass().add("chip");
        speedValue.setMinWidth(34);
        speedValue.setAlignment(Pos.CENTER);
        speedSlider.valueProperty().addListener((o, ov, nv) ->
                speedValue.setText(String.format("%.0f", nv.doubleValue())));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row2 = new HBox(8,
                runBtn, sep(),
                playBtn, pauseBtn, stepBtn, sep(),
                resetBtn, mazeBtn, clearBtn, presetBtn,
                spacer,
                new Label("Speed") {{ getStyleClass().add("dim"); }},
                speedSlider, speedValue);
        row2.setAlignment(Pos.CENTER_LEFT);

        wrap.getChildren().addAll(row1, row2);
        return wrap;
    }

    private ToggleButton toolBtn(String label, String id, boolean selected) {
        ToggleButton b = new ToggleButton(label);
        b.setToggleGroup(toolGroup);
        b.getStyleClass().add("toggle-btn");
        b.setUserData(id);
        b.setSelected(selected);
        return b;
    }

    private Region buildLegend() {
        HBox box = new HBox(14);
        box.setPadding(new Insets(8, 14, 8, 14));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: " + web(Theme.panel()) + "; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: " + web(Theme.border()) + "; " +
                "-fx-border-radius: 10; -fx-border-width: 1;");

        box.getChildren().addAll(
                swatch(Theme.success(), "Start"),
                swatch(Theme.danger(),  "End"),
                swatch(Theme.isDark() ? Color.web("#2a2a3a") : Color.web("#4a4a5a"), "Wall"),
                swatch(Theme.isDark() ? Color.web("#1e3a2a") : Color.web("#c8e6c9"), "Forest (3)"),
                swatch(Theme.isDark() ? Color.web("#1a2a44") : Color.web("#bbdefb"), "Water (5)"),
                swatch(Theme.isDark() ? Color.web("#3a2a1a") : Color.web("#d7ccc8"), "Mountain (10)"),
                swatch(Theme.accent().deriveColor(0, 1, 1, 0.45), "Visited"),
                swatch(Theme.accent2(), "Path"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statusLabel.getStyleClass().add("dim");
        statusLabel.setStyle("-fx-font-size: 11px;");
        box.getChildren().addAll(spacer, statusLabel);
        return box;
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

    private Region buildSidePanel() {
        VBox box = new VBox(12);

        // ---------- Result card ----------
        VBox result = new VBox(10);
        result.getStyleClass().add("panel");
        Label rh = new Label("RESULT");
        rh.getStyleClass().add("section-label");
        rh.setPadding(new Insets(0));
        result.getChildren().addAll(rh,
                row("Visited",     visitedLabel),
                row("Path Length", pathLabel),
                row("Total Cost",  costLabel));

        // ---------- A* inspector ----------
        VBox inspect = new VBox(10);
        inspect.getStyleClass().add("panel");
        Label ih = new Label("CELL INSPECTOR");
        ih.getStyleClass().add("section-label");
        ih.setPadding(new Insets(0));
        cellInfo.getStyleClass().add("mono");
        cellInfo.setWrapText(true);
        cellInfo.setMinHeight(90);
        inspect.getChildren().addAll(ih, cellInfo);

        // ---------- Explanatory card ----------
        VBox about = new VBox(8);
        about.getStyleClass().add("panel");
        Label ah = new Label("HOW IT WORKS");
        ah.getStyleClass().add("section-label");
        ah.setPadding(new Insets(0));
        Label text = new Label(
                "• BFS explores uniformly — optimal only for unweighted grids.\n" +
                "• Dijkstra expands by lowest accumulated cost g(n).\n" +
                "• A* uses f(n) = g(n) + h(n), h = Manhattan distance.");
        text.getStyleClass().add("dim");
        text.setWrapText(true);
        text.setStyle("-fx-font-size: 11.5px;");
        about.getChildren().addAll(ah, text);

        box.getChildren().addAll(result, inspect, about);
        return box;
    }

    private HBox row(String k, Label v) {
        HBox h = new HBox(8);
        h.setAlignment(Pos.CENTER_LEFT);
        Label kl = new Label(k);
        kl.getStyleClass().add("dim");
        kl.setMinWidth(100); kl.setPrefWidth(100);
        v.getStyleClass().add("mono");
        h.getChildren().addAll(kl, v);
        return h;
    }

    private Region sep() {
        Region r = new Region();
        r.setPrefWidth(1); r.setMinWidth(1); r.setPrefHeight(22);
        r.setBackground(new Background(new BackgroundFill(Theme.border(), CornerRadii.EMPTY, Insets.EMPTY)));
        return r;
    }

    private String panelStyle() {
        return "-fx-background-color: " + web(Theme.panel()) + "; -fx-background-radius: 12; " +
                "-fx-border-color: " + web(Theme.border()) + "; -fx-border-radius: 12; -fx-border-width: 1;";
    }

    // ============================================================
    //  Mouse
    // ============================================================
    private void onMouseDown(javafx.scene.input.MouseEvent e) {
        if (cellSize <= 0) return;
        if (e.getButton() == MouseButton.SECONDARY) { eraseCell(e.getX(), e.getY()); return; }
        applyTool(e.getX(), e.getY(), e.isShiftDown());
    }

    private void onMouseDrag(javafx.scene.input.MouseEvent e) {
        if (e.getButton() == MouseButton.SECONDARY) { eraseCell(e.getX(), e.getY()); return; }
        applyTool(e.getX(), e.getY(), e.isShiftDown());
    }

    private void onMouseMove(javafx.scene.input.MouseEvent e) {
        int c = cellCol(e.getX()), r = cellRow(e.getY());
        if (r != hoverR || c != hoverC) {
            hoverR = r; hoverC = c;
            updateCellInfo();
            draw();
        }
    }

    private int cellCol(double x) { return (int) Math.floor((x - gridX0) / cellSize); }
    private int cellRow(double y) { return (int) Math.floor((y - gridY0) / cellSize); }

    private void applyTool(double x, double y, boolean shift) {
        int c = cellCol(x), r = cellRow(y);
        if (!inBounds(r, c)) return;

        String tool = currentTool();
        // Shift overrides to erase
        if (shift) tool = "erase";

        switch (tool) {
            case "wall" -> {
                if ((r == startR && c == startC) || (r == endR && c == endC)) return;
                terrain[r][c] = T_WALL;
            }
            case "erase" -> {
                if ((r == startR && c == startC) || (r == endR && c == endC)) return;
                terrain[r][c] = T_EMPTY;
            }
            case "forest" -> {
                if ((r == startR && c == startC) || (r == endR && c == endC)) return;
                terrain[r][c] = T_FOREST;
            }
            case "water" -> {
                if ((r == startR && c == startC) || (r == endR && c == endC)) return;
                terrain[r][c] = T_WATER;
            }
            case "mountain" -> {
                if ((r == startR && c == startC) || (r == endR && c == endC)) return;
                terrain[r][c] = T_MOUNTAIN;
            }
            case "start" -> {
                if (r == endR && c == endC) return;
                if (terrain[r][c] == T_WALL) terrain[r][c] = T_EMPTY;
                startR = r; startC = c;
                clearPath();
            }
            case "end" -> {
                if (r == startR && c == startC) return;
                if (terrain[r][c] == T_WALL) terrain[r][c] = T_EMPTY;
                endR = r; endC = c;
                clearPath();
            }
        }
        draw();
    }

    private void eraseCell(double x, double y) {
        int c = cellCol(x), r = cellRow(y);
        if (!inBounds(r, c)) return;
        if ((r == startR && c == startC) || (r == endR && c == endC)) return;
        terrain[r][c] = T_EMPTY;
        draw();
    }

    private String currentTool() {
        Toggle t = toolGroup.getSelectedToggle();
        return t == null ? "wall" : (String) t.getUserData();
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < ROWS && c >= 0 && c < COLS;
    }

    // ============================================================
    //  Presets & maze
    // ============================================================
    private void clearAll() {
        stopAnimation();
        for (int r = 0; r < ROWS; r++) Arrays.fill(terrain[r], T_EMPTY);
        startR = 12; startC = 3;
        endR   = 12; endC   = 36;
        clearPath();
        statusLabel.setText("Grid cleared");
    }

    private void clearPath() {
        stopAnimation();
        events.clear();
        stepIdx = -1;
        liveVisited = livePath = 0;
        for (int r = 0; r < ROWS; r++) {
            Arrays.fill(revealedVisited[r], false);
            Arrays.fill(revealedPath[r], false);
        }
        hasScores = false;
        visitedLabel.setText("0");
        pathLabel.setText("0");
        costLabel.setText("0");
        updateCellInfo();
        draw();
    }

    private void loadPreset(String kind) {
        clearAll();
        Random rnd = new Random(42);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if ((r == startR && c == startC) || (r == endR && c == endC)) continue;
                double roll = rnd.nextDouble();
                if (roll < 0.06) terrain[r][c] = T_FOREST;
                else if (roll < 0.10) terrain[r][c] = T_WATER;
                else if (roll < 0.12) terrain[r][c] = T_MOUNTAIN;
            }
        }
        // A few walls to make things interesting
        for (int i = 0; i < 4; i++) {
            int rr = 3 + rnd.nextInt(ROWS - 6);
            int rc = 5 + rnd.nextInt(COLS - 10);
            int len = 3 + rnd.nextInt(6);
            for (int k = 0; k < len && rc + k < COLS - 1; k++)
                if (!(rr == startR && rc + k == startC) && !(rr == endR && rc + k == endC))
                    terrain[rr][rc + k] = T_WALL;
        }
        statusLabel.setText("Random weights loaded");
        draw();
    }

    private void generateMaze() {
        clearAll();
        Random rnd = new Random();

        // Fill with walls
        for (int r = 0; r < ROWS; r++) Arrays.fill(terrain[r], T_WALL);

        // Recursive backtracker on odd cells
        boolean[][] carved = new boolean[ROWS][COLS];
        int sR = 1 + 2 * rnd.nextInt((ROWS - 1) / 2);
        int sC = 1 + 2 * rnd.nextInt((COLS - 1) / 2);
        terrain[sR][sC] = T_EMPTY;
        carved[sR][sC] = true;

        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{sR, sC});
        int[][] dirs = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};

        while (!stack.isEmpty()) {
            int[] cur = stack.peek();
            List<int[]> nbrs = new ArrayList<>();
            for (int[] d : dirs) {
                int nr = cur[0] + d[0], nc = cur[1] + d[1];
                if (nr > 0 && nr < ROWS - 1 && nc > 0 && nc < COLS - 1 && !carved[nr][nc])
                    nbrs.add(new int[]{nr, nc, d[0], d[1]});
            }
            if (nbrs.isEmpty()) { stack.pop(); continue; }
            int[] pick = nbrs.get(rnd.nextInt(nbrs.size()));
            int nr = pick[0], nc = pick[1];
            int wr = cur[0] + pick[2] / 2, wc = cur[1] + pick[3] / 2;
            terrain[wr][wc] = T_EMPTY;
            terrain[nr][nc] = T_EMPTY;
            carved[nr][nc] = true;
            stack.push(new int[]{nr, nc});
        }

        // Sprinkle a few weighted patches on open cells
        for (int r = 1; r < ROWS - 1; r++) {
            for (int c = 1; c < COLS - 1; c++) {
                if (terrain[r][c] != T_EMPTY) continue;
                double roll = rnd.nextDouble();
                if (roll < 0.04) terrain[r][c] = T_FOREST;
                else if (roll < 0.06) terrain[r][c] = T_WATER;
            }
        }

        // Force endpoints open
        terrain[startR][startC] = T_EMPTY;
        terrain[endR][endC] = T_EMPTY;

        statusLabel.setText("Maze generated");
        draw();
    }

    // ============================================================
    //  Drawing
    // ============================================================
    private void draw() {
        double W = canvas.getWidth(), H = canvas.getHeight();
        if (W <= 4 || H <= 4) return;

        cellSize = Math.floor(Math.min(W / COLS, H / ROWS));
        if (cellSize < 4) cellSize = 4;
        double gw = cellSize * COLS;
        double gh = cellSize * ROWS;
        gridX0 = (W - gw) / 2;
        gridY0 = (H - gh) / 2;

        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, W, H);

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                double x = gridX0 + c * cellSize;
                double y = gridY0 + r * cellSize;

                Color fill;
                if (r == startR && c == startC)         fill = Theme.success();
                else if (r == endR && c == endC)         fill = Theme.danger();
                else if (revealedPath[r][c])             fill = Theme.accent2();
                else if (revealedVisited[r][c])          fill = visitedColor();
                else                                     fill = terrainColor(terrain[r][c]);

                g.setFill(fill);
                double inset = cellSize > 10 ? 0.5 : 0;
                g.fillRect(x + inset, y + inset, cellSize - inset * 2, cellSize - inset * 2);
            }
        }

        // Hover cell outline
        if (inBounds(hoverR, hoverC)) {
            g.setStroke(Theme.accent());
            g.setLineWidth(2);
            double x = gridX0 + hoverC * cellSize;
            double y = gridY0 + hoverR * cellSize;
            g.strokeRect(x + 1, y + 1, cellSize - 2, cellSize - 2);
        }

        // Start/End markers (drawn on top so they show clearly)
        drawEndpoint(g, startR, startC, "S");
        drawEndpoint(g, endR, endC, "E");
    }

    private void drawEndpoint(GraphicsContext g, int r, int c, String letter) {
        if (!inBounds(r, c)) return;
        double x = gridX0 + c * cellSize;
        double y = gridY0 + r * cellSize;
        g.setFill(Color.rgb(0, 0, 0, 0.55));
        g.setFont(Font.font("System", FontWeight.EXTRA_BOLD,
                Math.max(9, cellSize * 0.75)));
        double tx = x + cellSize * 0.28;
        double ty = y + cellSize * 0.74;
        g.fillText(letter, tx, ty);
    }

    private Color visitedColor() {
        // More saturated the "later" the visit? For now flat overlay.
        return Theme.accent().deriveColor(0, 1, 1, 0.42);
    }

    private Color terrainColor(int t) {
        boolean dark = Theme.isDark();
        return switch (t) {
            case T_WALL     -> dark ? Color.web("#2a2a3a") : Color.web("#4a4a5a");
            case T_FOREST   -> dark ? Color.web("#1e3a2a") : Color.web("#c8e6c9");
            case T_WATER    -> dark ? Color.web("#1a2a44") : Color.web("#bbdefb");
            case T_MOUNTAIN -> dark ? Color.web("#3a2a1a") : Color.web("#d7ccc8");
            default         -> Theme.panel2();
        };
    }

    // ============================================================
    //  Algorithms — all run synchronously and return event list
    // ============================================================
    private record RunResult(List<Ev> events, int visited, int pathLen, int cost, boolean found) {}

    private void runAlgorithm() {
        clearPath();
        String kind = algoBox.getValue();
        RunResult result = switch (kind) {
            case "BFS"      -> runBfs();
            case "Dijkstra" -> runDijkstra();
            case "A*"       -> runAStar();
            default         -> runBfs();
        };

        events = result.events();
        visitedLabel.setText(String.valueOf(result.visited()));
        pathLabel.setText(String.valueOf(result.pathLen()));
        costLabel.setText(result.found() ? String.valueOf(result.cost()) : "no path");
        statusLabel.setText(kind + " — " + (result.found()
                ? "path found (" + result.visited() + " visited)"
                : "no path reachable"));

        if (!result.found()) return;
        if (events.isEmpty()) return;

        play();
    }

    private RunResult runBfs() {
        List<Ev> ev = new ArrayList<>();
        int[][] parent = new int[ROWS][COLS];
        for (int[] row : parent) Arrays.fill(row, -1);
        boolean[][] seen = new boolean[ROWS][COLS];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{startR, startC});
        seen[startR][startC] = true;
        int visited = 0;
        boolean found = false;

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            ev.add(new Ev(cur[0], cur[1], false));
            visited++;
            if (cur[0] == endR && cur[1] == endC) { found = true; break; }
            for (int[] d : DIRS4) {
                int nr = cur[0] + d[0], nc = cur[1] + d[1];
                if (!inBounds(nr, nc)) continue;
                if (terrain[nr][nc] == T_WALL || seen[nr][nc]) continue;
                seen[nr][nc] = true;
                parent[nr][nc] = cur[0] * COLS + cur[1];
                q.add(new int[]{nr, nc});
            }
        }

        List<int[]> path = reconstruct(parent, found);
        for (int[] p : path) ev.add(new Ev(p[0], p[1], true));
        return new RunResult(ev, visited, path.size(), path.size(), found);
    }

    private RunResult runDijkstra() {
        List<Ev> ev = new ArrayList<>();
        int[][] dist = new int[ROWS][COLS];
        int[][] parent = new int[ROWS][COLS];
        for (int[] row : dist) Arrays.fill(row, Integer.MAX_VALUE);
        for (int[] row : parent) Arrays.fill(row, -1);
        dist[startR][startC] = 0;

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(x -> x[2]));
        pq.add(new int[]{startR, startC, 0});
        boolean[][] done = new boolean[ROWS][COLS];
        int visited = 0;
        boolean found = false;

        // Expose g for inspector
        for (int r = 0; r < ROWS; r++) {
            Arrays.fill(gScore[r], Integer.MAX_VALUE);
            Arrays.fill(hScore[r], Integer.MAX_VALUE);
            Arrays.fill(fScore[r], Integer.MAX_VALUE);
        }

        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            if (done[cur[0]][cur[1]]) continue;
            done[cur[0]][cur[1]] = true;
            ev.add(new Ev(cur[0], cur[1], false));
            visited++;
            gScore[cur[0]][cur[1]] = cur[2];
            if (cur[0] == endR && cur[1] == endC) { found = true; break; }
            for (int[] d : DIRS4) {
                int nr = cur[0] + d[0], nc = cur[1] + d[1];
                if (!inBounds(nr, nc)) continue;
                if (terrain[nr][nc] == T_WALL) continue;
                int w = costOf(terrain[nr][nc]);
                int nd = dist[cur[0]][cur[1]] + w;
                if (nd < dist[nr][nc]) {
                    dist[nr][nc] = nd;
                    parent[nr][nc] = cur[0] * COLS + cur[1];
                    pq.add(new int[]{nr, nc, nd});
                }
            }
        }
        hasScores = true;

        List<int[]> path = reconstruct(parent, found);
        for (int[] p : path) ev.add(new Ev(p[0], p[1], true));
        int cost = found ? dist[endR][endC] : 0;
        return new RunResult(ev, visited, path.size(), cost, found);
    }

    private RunResult runAStar() {
        List<Ev> ev = new ArrayList<>();
        int[][] g = new int[ROWS][COLS];
        int[][] f = new int[ROWS][COLS];
        int[][] parent = new int[ROWS][COLS];
        for (int[] row : g) Arrays.fill(row, Integer.MAX_VALUE);
        for (int[] row : parent) Arrays.fill(row, -1);
        g[startR][startC] = 0;
        f[startR][startC] = h(startR, startC);

        // Reset score arrays
        for (int r = 0; r < ROWS; r++) {
            Arrays.fill(gScore[r], Integer.MAX_VALUE);
            Arrays.fill(hScore[r], Integer.MAX_VALUE);
            Arrays.fill(fScore[r], Integer.MAX_VALUE);
        }
        hScore[startR][startC] = h(startR, startC);
        gScore[startR][startC] = 0;
        fScore[startR][startC] = h(startR, startC);

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(x -> x[2]));
        pq.add(new int[]{startR, startC, f[startR][startC]});
        boolean[][] done = new boolean[ROWS][COLS];
        int visited = 0;
        boolean found = false;

        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            if (done[cur[0]][cur[1]]) continue;
            done[cur[0]][cur[1]] = true;
            ev.add(new Ev(cur[0], cur[1], false));
            visited++;
            if (cur[0] == endR && cur[1] == endC) { found = true; break; }
            for (int[] d : DIRS4) {
                int nr = cur[0] + d[0], nc = cur[1] + d[1];
                if (!inBounds(nr, nc)) continue;
                if (terrain[nr][nc] == T_WALL) continue;
                int w = costOf(terrain[nr][nc]);
                int ng = g[cur[0]][cur[1]] + w;
                if (ng < g[nr][nc]) {
                    g[nr][nc] = ng;
                    hScore[nr][nc] = h(nr, nc);
                    fScore[nr][nc] = ng + hScore[nr][nc];
                    gScore[nr][nc] = ng;
                    parent[nr][nc] = cur[0] * COLS + cur[1];
                    pq.add(new int[]{nr, nc, fScore[nr][nc]});
                }
            }
        }
        hasScores = true;

        List<int[]> path = reconstruct(parent, found);
        for (int[] p : path) ev.add(new Ev(p[0], p[1], true));
        int cost = found ? g[endR][endC] : 0;
        return new RunResult(ev, visited, path.size(), cost, found);
    }

    private int h(int r, int c) {
        return Math.abs(r - endR) + Math.abs(c - endC);
    }

    private List<int[]> reconstruct(int[][] parent, boolean found) {
        List<int[]> path = new ArrayList<>();
        if (!found) return path;
        int r = endR, c = endC;
        while (!(r == startR && c == startC)) {
            path.add(0, new int[]{r, c});
            int p = parent[r][c];
            if (p < 0) break;
            r = p / COLS;
            c = p % COLS;
        }
        path.add(0, new int[]{startR, startC});
        return path;
    }

    private static final int[][] DIRS4 = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    // ============================================================
    //  Animation
    // ============================================================
    private void togglePlay() {
        if (playing) stopAnimation();
        else play();
    }

    private void play() {
        if (events.isEmpty()) return;
        if (stepIdx >= events.size() - 1) {
            // restart reveal
            for (int r = 0; r < ROWS; r++) {
                Arrays.fill(revealedVisited[r], false);
                Arrays.fill(revealedPath[r], false);
            }
            liveVisited = livePath = 0;
            stepIdx = -1;
            draw();
        }
        if (animation != null) return;

        playing = true;
        playBtn.setText("Playing");
        animation = new Timeline(new KeyFrame(Duration.millis(16), e -> tick()));
        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();
    }

    private void stopAnimation() {
        playing = false;
        if (animation != null) { animation.stop(); animation = null; }
        playBtn.setText("Play");
    }

    private void stepOnce() {
        if (events.isEmpty()) return;
        if (stepIdx >= events.size() - 1) return;
        applyNext(1);
        draw();
    }

    private void tick() {
        int batch = (int) Math.max(1, speedSlider.getValue() * 0.5);
        boolean done = false;
        for (int i = 0; i < batch; i++) {
            if (stepIdx >= events.size() - 1) { done = true; break; }
            applyNext(1);
        }
        draw();
        if (done) stopAnimation();
    }

    private void applyNext(int n) {
        for (int i = 0; i < n; i++) {
            if (stepIdx >= events.size() - 1) break;
            stepIdx++;
            Ev e = events.get(stepIdx);
            if (e.isPath()) {
                revealedPath[e.r()][e.c()] = true;
                livePath++;
            } else {
                revealedVisited[e.r()][e.c()] = true;
                liveVisited++;
            }
        }
        visitedLabel.setText(String.valueOf(liveVisited));
        pathLabel.setText(String.valueOf(livePath));
    }

    // ============================================================
    //  Inspector
    // ============================================================
    private void updateCellInfo() {
        if (!inBounds(hoverR, hoverC)) {
            cellInfo.setText("Hover a cell to inspect");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Position  (").append(hoverC).append(", ").append(hoverR).append(")\n");
        sb.append("Terrain   ").append(terrainName(terrain[hoverR][hoverC])).append('\n');
        if (hasScores) {
            String g = gScore[hoverR][hoverC] == Integer.MAX_VALUE
                    ? "∞" : String.valueOf(gScore[hoverR][hoverC]);
            String h = hScore[hoverR][hoverC] == Integer.MAX_VALUE
                    ? "∞" : String.valueOf(hScore[hoverR][hoverC]);
            String f = fScore[hoverR][hoverC] == Integer.MAX_VALUE
                    ? "∞" : String.valueOf(fScore[hoverR][hoverC]);
            sb.append("g(n)      ").append(g).append('\n');
            sb.append("h(n)      ").append(h).append('\n');
            sb.append("f(n)      ").append(f);
        } else {
            sb.append("Run A* or Dijkstra\nfor g / h / f scores");
        }
        cellInfo.setText(sb.toString());
    }

    private String terrainName(int t) {
        return switch (t) {
            case T_WALL     -> "Wall";
            case T_FOREST   -> "Forest (cost 3)";
            case T_WATER    -> "Water (cost 5)";
            case T_MOUNTAIN -> "Mountain (cost 10)";
            default         -> "Open (cost 1)";
        };
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