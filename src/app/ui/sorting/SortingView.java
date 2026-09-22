package app.ui.sorting;

import app.algorithms.sorting.SortingAlgorithms;
import app.algorithms.sorting.SortingAlgorithms.Op;
import app.algorithms.sorting.SortingAlgorithms.Result;
import app.icons.Icons;
import app.theme.Theme;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.Random;

public class SortingView extends BorderPane {

	// ============================================================
	// Model
	// ============================================================

	private int[] master;
	private int[] working;
	private boolean[] sorted;

	private Result trace;
	private int opIndex = -1;

	// Runtime counters
	private long comparisons;
	private long swaps;
	private long writes;

	// Visual state
	private int compareA = -1;
	private int compareB = -1;
	private int pivotIdx = -1;
	private int writeIdx = -1;

	private String stepDescription = "Ready";

	// ============================================================
	// UI
	// ============================================================

	private final Canvas canvas = new Canvas();
	private final StackPane canvasHolder = new StackPane();

	private final ComboBox<String> algoBox = new ComboBox<>();
	private final Spinner<Integer> sizeSpinner = new Spinner<>(5, 200, 30);

	private final Slider speedSlider = new Slider(1, 60, 20);

	private final Label speedValue = new Label("20");

	private final Label comparisonsLabel = new Label("0");
	private final Label swapsLabel = new Label("0");
	private final Label writesLabel = new Label("0");
	private final Label timeLabel = new Label("0.0000 s");

	private final Label arraySizeLabel = new Label("30");
	private final Label complexityLabel = new Label("O(n²)");
	private final Label progressLabel = new Label("0 / 0");

	private final Label stepLabel = new Label("Ready");
	private final Label statusLabel = new Label("Ready");

	private final ProgressBar progress = new ProgressBar(0);

	private final Button playBtn = new Button("Play", Icons.get("play", 14));

	private final Button pauseBtn = new Button("Pause", Icons.get("pause", 14));

	private final Button stepBtn = new Button("Step", Icons.get("step", 14));

	private final Button prevBtn = new Button("Back", Icons.get("prev", 14));

	private final Button skipBtn = new Button("Skip", Icons.get("chevron", 14));

	private final Button resetBtn = new Button("Reset", Icons.get("reset", 14));

	private Timeline timeline;
	private boolean playing;

	private int frameSkipCounter;

	private final Random random = new Random();

	// ============================================================
	// Constructor
	// ============================================================

	public SortingView() {

		setPadding(new Insets(18));

		setStyle("-fx-background-color: " + web(Theme.bg()) + ";");

		buildView();
		installKeyboardShortcuts();
		installListeners();

		Platform.runLater(() -> {
			resetArray(true);
			requestFocus();
		});
	}

	// ============================================================
	// Main UI
	// ============================================================

	private void buildView() {

	    Label title = new Label("SORTING LABORATORY");
	    title.getStyleClass().add("h1");

	    Label subtitle = new Label("Every comparison, swap, write and decision — visualized in real time");
	    subtitle.getStyleClass().add("dim");

	    VBox header = new VBox(2, title, subtitle);

	    Region controls = buildControls();

	    canvasHolder.setMinHeight(380);
	    canvasHolder.setPrefHeight(500);

	    canvasHolder.setStyle("-fx-background-color: " + web(Theme.panel()) + ";" 
	            + "-fx-background-radius: 14;"
	            + "-fx-border-color: " + web(Theme.border()) + ";" 
	            + "-fx-border-radius: 14;" 
	            + "-fx-border-width: 1;");

	    canvasHolder.getChildren().add(canvas);

	    canvas.setManaged(false);

	    canvas.widthProperty().bind(canvasHolder.widthProperty());
	    canvas.heightProperty().bind(canvasHolder.heightProperty());

	    canvas.widthProperty().addListener((obs, oldValue, newValue) -> draw());
	    canvas.heightProperty().addListener((obs, oldValue, newValue) -> draw());

	    VBox.setVgrow(canvasHolder, Priority.ALWAYS);

	    VBox center = new VBox(12, header, controls, canvasHolder);
	    HBox.setHgrow(center, Priority.ALWAYS);

	    VBox side = buildSidePanel();
	    side.setPrefWidth(300);
	    side.setMinWidth(240);
	    side.setMaxWidth(340);

	    HBox body = new HBox(16, center, side);
	    body.setPadding(new Insets(10));

	 
	    ScrollPane scrollPane = new ScrollPane(body);
	    scrollPane.setFitToWidth(true); 
	    scrollPane.setFitToHeight(true); 
	    scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

	    setCenter(scrollPane);
	}
	// ============================================================
	// Controls
	// ============================================================

	private Region buildControls() {

		VBox wrapper = new VBox(8);

		wrapper.setPadding(new Insets(10, 12, 10, 12));

		wrapper.setStyle(panelStyle());

		algoBox.getItems().setAll("Bubble Sort", "Selection Sort", "Insertion Sort", "Merge Sort", "Quick Sort",
				"Heap Sort");

		algoBox.getSelectionModel().selectFirst();

		algoBox.getStyleClass().add("field");

		algoBox.setPrefWidth(165);
		algoBox.setMinWidth(165);

		sizeSpinner.setPrefWidth(84);
		sizeSpinner.setMinWidth(84);

		Button randomize = new Button("Randomize", Icons.get("reset", 14));

		randomize.getStyleClass().add("btn");
		randomize.setGraphicTextGap(6);

		randomize.setOnAction(e -> resetArray(true));

		styleButton(playBtn, true);
		styleButton(pauseBtn, false);
		styleButton(stepBtn, false);
		styleButton(prevBtn, false);
		styleButton(skipBtn, false);
		styleButton(resetBtn, false);

		playBtn.setOnAction(e -> togglePlay());

		pauseBtn.setOnAction(e -> stop());

		stepBtn.setOnAction(e -> stepOnceUI());

		prevBtn.setOnAction(e -> stepBackOnce());

		skipBtn.setOnAction(e -> skipToEnd());

		resetBtn.setOnAction(e -> resetArray(false));

		speedSlider.setPrefWidth(150);
		speedSlider.setMinWidth(110);

		speedSlider.setShowTickMarks(false);
		speedSlider.setShowTickLabels(false);

		speedValue.getStyleClass().add("chip");

		speedValue.setMinWidth(36);
		speedValue.setAlignment(Pos.CENTER);

		speedSlider.valueProperty().addListener(
				(obs, oldValue, newValue) -> speedValue.setText(String.format("%.0f", newValue.doubleValue())));

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		Label speedLabel = new Label("Speed");
		speedLabel.getStyleClass().add("dim");

		HBox row1 = new HBox(10, new Label("Algorithm"), algoBox, separator(), new Label("Size"), sizeSpinner,
				randomize, spacer, speedLabel, speedSlider, speedValue);

		row1.setAlignment(Pos.CENTER_LEFT);

		HBox row2 = new HBox(8, playBtn, pauseBtn, stepBtn, prevBtn, skipBtn, resetBtn);

		row2.setAlignment(Pos.CENTER_LEFT);

		wrapper.getChildren().addAll(row1, row2);

		return wrapper;
	}

	private void styleButton(Button button, boolean primary) {

		if (primary) {
			button.getStyleClass().addAll("btn", "btn-primary");
		} else {
			button.getStyleClass().add("btn");
		}

		button.setGraphicTextGap(6);
	}

	// ============================================================
	// Side Panel
	// ============================================================

	private VBox buildSidePanel() {

		VBox box = new VBox(12);

		VBox execution = new VBox(8);
		execution.getStyleClass().add("panel");

		Label executionHeader = sectionLabel("EXECUTION");

		execution.getChildren().addAll(executionHeader, statRow("Comparisons", comparisonsLabel),
				statRow("Swaps", swapsLabel), statRow("Writes", writesLabel), statRow("Elapsed", timeLabel));

		VBox metrics = new VBox(8);
		metrics.getStyleClass().add("panel");

		Label metricsHeader = sectionLabel("METRICS");

		metrics.getChildren().addAll(metricsHeader, statRow("Array Size", arraySizeLabel),
				statRow("Complexity", complexityLabel), statRow("Progress", progressLabel));

		progress.setPrefWidth(Double.MAX_VALUE);

		progress.setStyle("-fx-accent: " + web(Theme.accent()) + ";");

		metrics.getChildren().add(progress);

		VBox currentStep = new VBox(6);
		currentStep.getStyleClass().add("panel");

		Label stepHeader = sectionLabel("CURRENT OPERATION");

		stepLabel.getStyleClass().add("mono");
		stepLabel.setWrapText(true);
		stepLabel.setMaxWidth(Double.MAX_VALUE);
		stepLabel.setMinHeight(55);

		currentStep.getChildren().addAll(stepHeader, stepLabel);

		VBox status = new VBox(6);
		status.getStyleClass().add("panel");

		Label statusHeader = sectionLabel("STATUS");

		statusLabel.getStyleClass().add("mono");
		statusLabel.setWrapText(true);

		status.getChildren().addAll(statusHeader, statusLabel);

		VBox legend = new VBox(6);
		legend.getStyleClass().add("panel");

		Label legendHeader = sectionLabel("LEGEND");

		legend.getChildren().addAll(legendHeader, legendRow(Theme.accent(), "unsorted"),
				legendRow(Theme.danger(), "compare A"), legendRow(Theme.warn(), "compare B / write"),
				legendRow(Theme.accent2(), "pivot"), legendRow(Theme.success(), "sorted"));

		box.getChildren().addAll(execution, metrics, currentStep, status, legend);

		return box;
	}

	private Label sectionLabel(String text) {

		Label label = new Label(text);

		label.getStyleClass().add("section-label");

		label.setPadding(new Insets(0));

		return label;
	}

	private HBox statRow(String name, Label value) {

		HBox row = new HBox(8);

		row.setAlignment(Pos.CENTER_LEFT);

		Label nameLabel = new Label(name);

		nameLabel.getStyleClass().add("dim");

		nameLabel.setMinWidth(110);
		nameLabel.setPrefWidth(110);

		value.getStyleClass().add("mono");

		row.getChildren().addAll(nameLabel, value);

		return row;
	}

	private HBox legendRow(Color color, String text) {

		Region swatch = new Region();

		swatch.setPrefSize(14, 14);
		swatch.setMinSize(14, 14);
		swatch.setMaxSize(14, 14);

		swatch.setBackground(new Background(new BackgroundFill(color, new CornerRadii(4), Insets.EMPTY)));

		Label label = new Label(text);
		label.getStyleClass().add("dim");

		HBox row = new HBox(8, swatch, label);

		row.setAlignment(Pos.CENTER_LEFT);

		return row;
	}

	private Region separator() {

		Region line = new Region();

		line.setPrefWidth(1);
		line.setMinWidth(1);
		line.setPrefHeight(22);

		line.setBackground(new Background(new BackgroundFill(Theme.border(), CornerRadii.EMPTY, Insets.EMPTY)));

		return line;
	}

	private String panelStyle() {

		return "-fx-background-color: " + web(Theme.panel()) + ";" + "-fx-background-radius: 12;" + "-fx-border-color: "
				+ web(Theme.border()) + ";" + "-fx-border-radius: 12;" + "-fx-border-width: 1;";
	}

	// ============================================================
	// Listeners
	// ============================================================

	private void installListeners() {

		algoBox.valueProperty().addListener((obs, oldValue, newValue) -> onAlgorithmChanged());

		sizeSpinner.valueProperty().addListener((obs, oldValue, newValue) -> resetArray(true));
	}

	// ============================================================
	// Keyboard
	// ============================================================

	private void installKeyboardShortcuts() {

		setFocusTraversable(true);

		addEventFilter(KeyEvent.KEY_PRESSED, event -> {

			if (event.getTarget() instanceof TextInputControl) {
				return;
			}

			switch (event.getCode()) {

			case SPACE -> {

				togglePlay();

				event.consume();
			}

			case RIGHT -> {

				stepOnceUI();

				event.consume();
			}

			case LEFT -> {

				stepBackOnce();

				event.consume();
			}

			case R -> {

				resetArray(false);

				event.consume();
			}

			case PLUS, EQUALS, ADD -> {

				speedSlider.setValue(Math.min(60, speedSlider.getValue() + 3));

				event.consume();
			}

			case MINUS, SUBTRACT -> {

				speedSlider.setValue(Math.max(1, speedSlider.getValue() - 3));

				event.consume();
			}

			default -> {
				// Ignore
			}
			}
		});
	}

	// ============================================================
	// Algorithm
	// ============================================================

	private void onAlgorithmChanged() {

		String algorithm = algoBox.getValue();

		if (algorithm == null) {
			return;
		}

		complexityLabel.setText(complexityFor(algorithm));

		/*
		 * Important:
		 *
		 * Do NOT generate another random array here. This allows meaningful algorithm
		 * comparison.
		 */
		resetArray(false);
	}

	private String complexityFor(String algorithm) {

		return switch (algorithm) {

		case "Bubble Sort", "Selection Sort", "Insertion Sort" -> "O(n²)";

		case "Merge Sort", "Heap Sort" -> "O(n log n)";

		case "Quick Sort" -> "O(n log n) avg";

		default -> "—";
		};
	}

	// ============================================================
	// Reset
	// ============================================================

	private void resetArray(boolean regenerate) {

		stop();

		int n = sizeSpinner.getValue();

		if (regenerate || master == null || master.length != n) {

			master = new int[n];

			for (int i = 0; i < n; i++) {

				/*
				 * Keep values in a visually useful range.
				 */
				master[i] = 5 + random.nextInt(96);
			}
		}

		working = master.clone();

		sorted = new boolean[n];

		String algorithm = algoBox.getValue();

		if (algorithm == null) {
			algorithm = "Bubble Sort";
		}

		int[] traceInput = master.clone();

		trace = switch (algorithm) {

		case "Bubble Sort" -> SortingAlgorithms.bubble(traceInput);

		case "Selection Sort" -> SortingAlgorithms.selection(traceInput);

		case "Insertion Sort" -> SortingAlgorithms.insertion(traceInput);

		case "Merge Sort" -> SortingAlgorithms.merge(traceInput);

		case "Quick Sort" -> SortingAlgorithms.quick(traceInput);

		case "Heap Sort" -> SortingAlgorithms.heap(traceInput);

		default -> SortingAlgorithms.bubble(traceInput);
		};

		opIndex = -1;

		comparisons = 0;
		swaps = 0;
		writes = 0;

		compareA = -1;
		compareB = -1;
		pivotIdx = -1;
		writeIdx = -1;

		stepDescription = "Ready";

		arraySizeLabel.setText(String.valueOf(n));

		complexityLabel.setText(complexityFor(algorithm));

		int total = trace == null ? 0 : trace.ops().size();

		progressLabel.setText("0 / " + total);

		progress.setProgress(0);

		stepLabel.setText(stepDescription);

		statusLabel.setText(algorithm + " ready");

		if (trace != null) {

			timeLabel.setText(String.format("%.4f s", trace.timeNanos() / 1_000_000_000.0));

		} else {

			timeLabel.setText("0.0000 s");
		}

		updateLabels();
		draw();
	}

	// ============================================================
	// Playback
	// ============================================================

	private void togglePlay() {

		if (playing) {
			stop();
		} else {
			play();
		}
	}

	private void play() {

		if (trace == null) {
			return;
		}

		if (trace.ops().isEmpty()) {

			finishRun();
			return;
		}

		/*
		 * If already finished, restart from the beginning instead of doing nothing.
		 */
		if (opIndex >= trace.ops().size() - 1) {

			resetArray(false);
		}

		if (timeline != null) {
			return;
		}

		playing = true;

		playBtn.setText("Playing");
		playBtn.setDisable(true);

		pauseBtn.setDisable(false);

		frameSkipCounter = 0;

		timeline = new Timeline(new KeyFrame(Duration.millis(16), event -> tick()));

		timeline.setCycleCount(Animation.INDEFINITE);

		timeline.play();

		statusLabel.setText("Running " + algoBox.getValue());
	}

	private void stop() {

		playing = false;

		if (timeline != null) {

			timeline.stop();
			timeline = null;
		}

		playBtn.setText("Play");
		playBtn.setDisable(false);
	}

	private void tick() {

		if (trace == null) {

			stop();
			return;
		}

		int total = trace.ops().size();

		int remaining = total - 1 - opIndex;

		if (remaining <= 0) {

			stop();
			finishRun();
			return;
		}

		int speed = (int) speedSlider.getValue();

		if (speed <= 5) {

			frameSkipCounter++;

			int interval = 6 - speed;

			if (frameSkipCounter < interval) {
				return;
			}

			frameSkipCounter = 0;

			applyNext();

		} else if (speed <= 20) {

			applyNext();

		} else {

			int batch = 1 + (speed - 20) / 3;

			for (int i = 0; i < batch && opIndex < total - 1; i++) {

				applyNext();
			}
		}

		updateLabels();
		draw();

		if (opIndex >= total - 1) {

			stop();
			finishRun();
		}
	}

	private void finishRun() {

		if (sorted != null) {

			Arrays.fill(sorted, true);
		}

		compareA = -1;
		compareB = -1;
		pivotIdx = -1;
		writeIdx = -1;

		stepDescription = "Sorting completed";

		stepLabel.setText(stepDescription);

		statusLabel.setText("✓ " + algoBox.getValue() + " completed — " + trace.ops().size() + " operations");

		updateLabels();
		draw();
	}

	// ============================================================
	// Single Step
	// ============================================================

	private void stepOnceUI() {

		if (trace == null) {
			return;
		}

		if (opIndex >= trace.ops().size() - 1) {
			return;
		}

		stop();

		applyNext();

		updateLabels();
		draw();
	}

	private void stepBackOnce() {

		if (trace == null || opIndex < 0) {
			return;
		}

		stop();

		rebuildTo(opIndex - 1);
	}

	private void skipToEnd() {

		if (trace == null) {
			return;
		}

		stop();

		rebuildTo(trace.ops().size() - 1);

		finishRun();
	}

	// ============================================================
	// Replay
	// ============================================================

	private void rebuildTo(int targetIndex) {

		if (trace == null) {
			return;
		}

		working = master.clone();

		sorted = new boolean[master.length];

		comparisons = 0;
		swaps = 0;
		writes = 0;

		compareA = -1;
		compareB = -1;
		pivotIdx = -1;
		writeIdx = -1;

		stepDescription = "Ready";

		if (targetIndex < 0) {

			opIndex = -1;

			updateLabels();
			draw();

			return;
		}

		int safeTarget = Math.min(targetIndex, trace.ops().size() - 1);

		for (int i = 0; i < safeTarget; i++) {

			applyOp(trace.ops().get(i), false);
		}

		applyOp(trace.ops().get(safeTarget), true);

		opIndex = safeTarget;

		updateLabels();
		draw();
	}

	private void applyNext() {

		if (trace == null) {
			return;
		}

		if (opIndex >= trace.ops().size() - 1) {
			return;
		}

		opIndex++;

		applyOp(trace.ops().get(opIndex), true);
	}

	// ============================================================
	// Operation Application
	// ============================================================

	private void applyOp(Op op, boolean describe) {

		compareA = -1;
		compareB = -1;
		writeIdx = -1;

		if (op == null) {
			return;
		}

		switch (op.type()) {

		case SortingAlgorithms.COMPARE -> {

			comparisons++;

			compareA = validIndex(op.i()) ? op.i() : -1;

			compareB = validIndex(op.j()) ? op.j() : -1;

			if (describe) {

				int a = op.i();
				int b = op.j();

				int va = validIndex(a) ? working[a] : 0;

				int vb = validIndex(b) ? working[b] : 0;

				stepDescription = "compare   [" + a + "]=" + va + "  vs  [" + b + "]=" + vb;
			}
		}

		case SortingAlgorithms.SWAP -> {

			int i = op.i();
			int j = op.j();

			if (!validIndex(i) || !validIndex(j)) {
				return;
			}

			int temp = working[i];

			working[i] = working[j];

			working[j] = temp;

			swaps++;

			compareA = i;
			compareB = j;

			if (describe) {

				stepDescription = "swap      [" + i + "] ↔ [" + j + "]";
			}
		}

		case SortingAlgorithms.WRITE -> {

			int index = op.i();

			if (!validIndex(index)) {
				return;
			}

			working[index] = op.value();

			writes++;

			writeIdx = index;

			if (describe) {

				stepDescription = "write     [" + index + "] = " + op.value();
			}
		}

		case SortingAlgorithms.PIVOT -> {

			pivotIdx = validIndex(op.pivot()) ? op.pivot() : -1;

			if (describe) {

				stepDescription = "pivot     at index " + op.pivot();
			}
		}

		case SortingAlgorithms.MARK_SORTED -> {

			int index = op.i();

			if (sorted != null && index >= 0 && index < sorted.length) {

				sorted[index] = true;
			}

			if (describe) {

				stepDescription = "finalize  [" + index + "]";
			}
		}

		default -> {
			// Future operation types
			// can safely be ignored here.
		}
		}
	}

	private boolean validIndex(int index) {

		return working != null && index >= 0 && index < working.length;
	}

	// ============================================================
	// Labels
	// ============================================================

	private void updateLabels() {

		comparisonsLabel.setText(String.valueOf(comparisons));

		swapsLabel.setText(String.valueOf(swaps));

		writesLabel.setText(String.valueOf(writes));

		int total = trace == null ? 0 : trace.ops().size();

		int done = Math.max(0, opIndex + 1);

		progressLabel.setText(done + " / " + total);

		if (total == 0) {

			progress.setProgress(0);

		} else {

			progress.setProgress(Math.min(1.0, done / (double) total));
		}

		stepLabel.setText(stepDescription);
	}

	// ============================================================
	// Rendering
	// ============================================================

	private void draw() {

		GraphicsContext g = canvas.getGraphicsContext2D();

		double width = canvas.getWidth();

		double height = canvas.getHeight();

		if (width < 20 || height < 20) {
			return;
		}

		g.clearRect(0, 0, width, height);

		drawBackground(g, width, height);

		if (working == null || working.length == 0) {

			drawEmptyState(g, width, height);

			return;
		}

		drawArray(g, width, height);

		drawOverlay(g, width);
	}

	private void drawBackground(GraphicsContext g, double width, double height) {

		g.setFill(Theme.panel());

		g.fillRect(0, 0, width, height);

		/*
		 * Very subtle horizontal grid.
		 */
		g.setStroke(Color.rgb(255, 255, 255, 0.035));

		g.setLineWidth(1);

		for (double y = 40; y < height - 20; y += 40) {

			g.strokeLine(0, y, width, y);
		}
	}

	private void drawEmptyState(GraphicsContext g, double width, double height) {

		g.setFill(Theme.textDim());

		g.setFont(Font.font("System", FontWeight.BOLD, 14));

		g.setTextAlign(TextAlignment.CENTER);

		g.fillText("No data", width / 2, height / 2);

		g.setTextAlign(TextAlignment.LEFT);
	}

	private void drawArray(GraphicsContext g, double width, double height) {

		int n = working.length;

		double padLeft = 20;
		double padRight = 20;
		double padTop = 42;
		double padBottom = 36;

		double availableWidth = Math.max(10, width - padLeft - padRight);

		double plotHeight = Math.max(10, height - padTop - padBottom);

		double barWidth = availableWidth / n;

		double maxValue = calculateMaxValue();

		/*
		 * Baseline.
		 */
		double baseline = height - padBottom;

		g.setStroke(Theme.border());

		g.setLineWidth(1);

		g.strokeLine(padLeft, baseline, width - padRight, baseline);

		boolean showValues = n <= 40 && barWidth > 14;

		for (int i = 0; i < n; i++) {

			int value = working[i];

			double normalized = maxValue == 0 ? 0 : value / maxValue;

			double barHeight = plotHeight * normalized;

			/*
			 * Minimum visual height makes very small values still visible.
			 */
			if (value > 0 && barHeight < 3) {

				barHeight = 3;
			}

			double x = padLeft + i * barWidth;

			double y = baseline - barHeight;

			Color color = colorForIndex(i);

			g.setFill(color);

			double gap = barWidth >= 5 ? 1.5 : 0;

			double actualWidth = Math.max(1, barWidth - gap);

			g.fillRoundRect(x + gap / 2, y, actualWidth, barHeight, 4, 4);

			/*
			 * Small highlight at the top of the bar.
			 */
			if (barWidth > 5) {

				g.setFill(Color.rgb(255, 255, 255, 0.13));

				g.fillRoundRect(x + gap / 2, y, actualWidth, Math.min(2, barHeight), 3, 3);
			}

			if (showValues) {

				g.setFill(Theme.textDim());

				g.setFont(Font.font("System", 9));

				g.setTextAlign(TextAlignment.CENTER);

				g.fillText(String.valueOf(value), x + barWidth / 2, baseline + 15);
			}
		}

		/*
		 * Draw index markers for small arrays.
		 */
		if (n <= 30 && barWidth > 18) {

			g.setFill(Theme.textDim());

			g.setFont(Font.font("System", 8));

			g.setTextAlign(TextAlignment.CENTER);

			for (int i = 0; i < n; i++) {

				double x = padLeft + i * barWidth + barWidth / 2;

				g.fillText(String.valueOf(i), x, height - 7);
			}
		}

		g.setTextAlign(TextAlignment.LEFT);
	}

	private double calculateMaxValue() {

		if (working == null || working.length == 0) {
			return 1;
		}

		int max = 1;

		for (int value : working) {

			max = Math.max(max, Math.abs(value));
		}

		return max;
	}

	private Color colorForIndex(int index) {

		if (sorted != null && index < sorted.length && sorted[index]) {

			return Theme.success();
		}

		if (index == pivotIdx) {
			return Theme.accent2();
		}

		if (index == compareA) {
			return Theme.danger();
		}

		if (index == compareB) {
			return Theme.warn();
		}

		if (index == writeIdx) {
			return Theme.warn();
		}

		return Theme.accent();
	}

	private void drawOverlay(GraphicsContext g, double width) {

		if (trace == null) {
			return;
		}

		int total = trace.ops().size();

		int current = Math.max(0, opIndex + 1);

		g.setFill(Theme.textDim());

		g.setFont(Font.font("System", FontWeight.BOLD, 11));

		g.setTextAlign(TextAlignment.LEFT);

		String algorithm = algoBox.getValue();

		String text = algorithm + "   ·   n = " + working.length + "   ·   operations " + current + " / " + total;

		g.fillText(text, 20, 20);

		/*
		 * Playback state indicator.
		 */
		String state;

		if (playing) {

			state = "● RUNNING";

		} else if (opIndex >= total - 1 && total > 0) {

			state = "✓ COMPLETE";

		} else if (opIndex >= 0) {

			state = "Ⅱ PAUSED";

		} else {

			state = "READY";
		}

		g.setTextAlign(TextAlignment.RIGHT);

		g.fillText(state, width - 20, 20);

		g.setTextAlign(TextAlignment.LEFT);
	}

	// ============================================================
	// Utility
	// ============================================================

	private String web(Color color) {

		int red = (int) Math.round(color.getRed() * 255);

		int green = (int) Math.round(color.getGreen() * 255);

		int blue = (int) Math.round(color.getBlue() * 255);

		return String.format("#%02x%02x%02x", red, green, blue);
	}
}