package app.ui.datastructures;

import app.icons.Icons;
import app.theme.Theme;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.*;

/**
 * DATA STRUCTURES LAB
 *
 * Interactive visualization for:
 *   - Stack
 *   - Queue
 *   - Linked List
 *   - Binary Search Tree
 *   - Max Heap
 *
 * Every visualization is generated from the real internal structure.
 *
 * Layout:
 *   ┌───────────────────────────────────────────────┐
 *   │  HEADER (title + badge + subtitle)            │
 *   │  CONTROLS (structure · operation · value · …) │
 *   ├────────────────────────────┬──────────────────┤
 *   │  LIVE VISUALIZATION        │  ANALYTICS       │
 *   │  (scrolls independently)   │  STATUS          │
 *   │                            │  HISTORY (scroll)│
 *   ├────────────────────────────┴──────────────────┤
 *   │  STATUS BAR                                   │
 *   └───────────────────────────────────────────────┘
 */
public class DSView extends BorderPane {

    // =====================================================================
    // REAL DATA STRUCTURES
    // =====================================================================

    private final Deque<Integer> stack = new ArrayDeque<>();
    private final Deque<Integer> queue = new ArrayDeque<>();
    private final LinkedList<Integer> list = new LinkedList<>();
    private final List<Integer> heap = new ArrayList<>();
    private BstNode bstRoot;
    private final Set<Integer> bstValues = new HashSet<>();

    private static final class BstNode {
        int value;
        BstNode left, right;
        BstNode(int value) { this.value = value; }
    }

    // =====================================================================
    // UI FIELDS
    // =====================================================================

    private final ComboBox<String> structureBox = new ComboBox<>();
    private final ComboBox<String> operationBox = new ComboBox<>();
    private final TextField valueField = new TextField();

    /** Content host — its natural size drives the scroll. */
    private final StackPane visualizationHost = new StackPane();

    private final VBox logBox = new VBox(3);
    private ScrollPane logScroll;

    private final Label typeLabel            = new Label("—");
    private final Label sizeLabel            = new Label("0");
    private final Label heightLabel          = new Label("0");
    private final Label rootLabel            = new Label("—");
    private final Label minLabel             = new Label("—");
    private final Label maxLabel             = new Label("—");
    private final Label complexityLabel      = new Label("—");
    private final Label statusLabel          = new Label("Ready");
    private final Label operationCountLabel  = new Label("0 operations");
    private final Label operationHintLabel   = new Label("Select an operation");

    private final Random random = new Random();
    private Integer highlightValue;
    private long operationCount;
    private double slotCursor;

    // =====================================================================
    // CONSTRUCTOR
    // =====================================================================

    public DSView() {
        setPadding(new Insets(16));
        setStyle("-fx-background-color: " + web(Theme.bg()) + ";");

        setTop(buildTopSection());
        setCenter(buildWorkspace());
        setBottom(buildStatusBar());

        updateOperations();
        render();
    }

    // =====================================================================
    // TOP SECTION — header + controls
    // =====================================================================

    private Region buildTopSection() {
        VBox top = new VBox(12, buildHeader(), buildControls());
        top.setPadding(new Insets(0, 0, 12, 0));
        return top;
    }

    private Region buildHeader() {
        VBox header = new VBox(4);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("DATA STRUCTURES LAB");
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
                "Build, inspect and manipulate real data structures in real time.");
        subtitle.getStyleClass().add("dim");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 12px;");

        Label flow = new Label("STACK  ·  QUEUE  ·  LINKED LIST  ·  BST  ·  MAX HEAP");
        flow.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        flow.setTextFill(Theme.accent());

        header.getChildren().addAll(titleRow, subtitle, flow);
        return header;
    }

    private Region buildControls() {
        VBox wrap = new VBox(8);
        wrap.setPadding(new Insets(12));
        wrap.setStyle(panelStyle());

        structureBox.getItems().setAll(
                "Stack", "Queue", "Linked List", "Binary Search Tree", "Max Heap");
        structureBox.getSelectionModel().selectFirst();
        structureBox.setPrefWidth(180);
        structureBox.setMinWidth(150);
        structureBox.getStyleClass().add("field");
        structureBox.valueProperty().addListener((o, ov, nv) -> {
            highlightValue = null;
            updateOperations();
            updateStatus("Switched to " + nv);
            render();
        });

        operationBox.setPrefWidth(190);
        operationBox.setMinWidth(160);
        operationBox.getStyleClass().add("field");
        operationBox.valueProperty().addListener((o, ov, nv) -> {
            if (nv != null) operationHintLabel.setText(operationDescription(nv));
        });

        valueField.setPromptText("integer value");
        valueField.setPrefWidth(130);
        valueField.setMinWidth(100);
        valueField.getStyleClass().add("field");
        valueField.setOnAction(e -> executeOperation());

        Button execute  = primaryButton("Execute", "play", this::executeOperation);
        Button randomBtn = button("Random", this::insertRandom);
        Button generate = button("Generate", this::generateSample);
        Button clear    = button("Clear", this::clearAll);

        VBox structureGroup = labeledControl("STRUCTURE", structureBox);
        VBox operationGroup = labeledControl("OPERATION", operationBox);
        VBox inputGroup     = labeledControl("VALUE", valueField);

        HBox row = new HBox(10,
                structureGroup, operationGroup, inputGroup,
                execute, randomBtn, generate, clear);
        row.setAlignment(Pos.BOTTOM_LEFT);

        ScrollPane horizontal = new ScrollPane(row);
        horizontal.setFitToHeight(true);
        horizontal.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        horizontal.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        horizontal.setPrefViewportHeight(72);
        horizontal.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;");

        operationHintLabel.getStyleClass().add("dim");
        operationHintLabel.setStyle("-fx-font-size: 11px;");
        operationHintLabel.setPadding(new Insets(4, 2, 0, 2));

        wrap.getChildren().addAll(horizontal, operationHintLabel);
        return wrap;
    }

    private VBox labeledControl(String title, Control control) {
        Label label = new Label(title);
        label.setTextFill(Theme.textDim());
        label.setFont(Font.font("System", FontWeight.BOLD, 9));
        return new VBox(5, label, control);
    }

    // =====================================================================
    // WORKSPACE — visualization (left) + sidebar (right)
    // =====================================================================

    private Region buildWorkspace() {
        VBox visualization = buildVisualizationPanel();
        VBox sidebar = buildSidebar();

        sidebar.setPrefWidth(320);
        sidebar.setMinWidth(280);
        sidebar.setMaxWidth(380);

        HBox.setHgrow(visualization, Priority.ALWAYS);

        HBox body = new HBox(14, visualization, sidebar);
        VBox.setVgrow(body, Priority.ALWAYS);
        return body;
    }

    // =====================================================================
    // VISUALIZATION PANEL — clean scroll behaviour
    // =====================================================================

    private VBox buildVisualizationPanel() {
        VBox panel = new VBox(0);
        panel.setStyle(panelStyle());

        // Fixed header
        VBox headerBox = new VBox(3);
        headerBox.setPadding(new Insets(12, 12, 8, 12));

        Label heading = new Label("LIVE VISUALIZATION");
        heading.getStyleClass().add("section-label");

        Label subtitle = new Label(
                "The canvas always represents the actual internal structure.");
        subtitle.getStyleClass().add("dim");
        subtitle.setStyle("-fx-font-size: 11px;");
        subtitle.setWrapText(true);

        headerBox.getChildren().addAll(heading, subtitle);

        // Scrollable canvas
        visualizationHost.setAlignment(Pos.CENTER);
        visualizationHost.setPadding(new Insets(28));
        visualizationHost.setStyle(
                "-fx-background-color: " + web(Theme.panel()) + ";" +
                "-fx-background-radius: 10;");

        ScrollPane scroll = new ScrollPane(visualizationHost);
        scroll.setFitToWidth(true);
        // >>> fitToHeight MUST be false so the content can grow and scroll <<<
        scroll.setFitToHeight(false);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;");

        StackPane scrollWrap = new StackPane(scroll);
        scrollWrap.setPadding(new Insets(0, 12, 12, 12));
        VBox.setVgrow(scrollWrap, Priority.ALWAYS);

        panel.setMinHeight(440);

        panel.getChildren().addAll(headerBox, scrollWrap);
        VBox.setVgrow(scrollWrap, Priority.ALWAYS);
        return panel;
    }

    // =====================================================================
    // SIDEBAR — scrollable column
    // =====================================================================

    private VBox buildSidebar() {
        VBox card = new VBox(0);
        card.setStyle(panelStyle());

        // Fixed title
        Label title = new Label("STRUCTURE INSIGHTS");
        title.getStyleClass().add("section-label");
        title.setPadding(new Insets(12, 12, 8, 12));

        // Scrollable body
        VBox content = new VBox(10);
        content.setPadding(new Insets(0, 12, 12, 12));

        // ---- Analytics ----
        VBox analytics = subCard();
        analytics.getChildren().add(subTitle("ANALYTICS"));
        analytics.getChildren().addAll(
                infoRow("Type",       typeLabel),
                infoRow("Size",       sizeLabel),
                infoRow("Height",     heightLabel),
                infoRow("Root",       rootLabel),
                infoRow("Minimum",    minLabel),
                infoRow("Maximum",    maxLabel),
                infoRow("Complexity", complexityLabel));

        // ---- Status ----
        VBox statusCard = subCard();
        statusCard.getChildren().add(subTitle("CURRENT STATUS"));
        statusLabel.setWrapText(true);
        statusLabel.setTextFill(Theme.accent());
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        statusCard.getChildren().add(statusLabel);

        // ---- History ----
        VBox history = subCard();
        history.getChildren().add(subTitle("OPERATION HISTORY"));
        history.getChildren().add(operationCountLabel);
        logScroll = new ScrollPane(logBox);
        logScroll.setFitToWidth(true);
        logScroll.setPrefHeight(260);
        logScroll.setMinHeight(160);
        logScroll.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;");
        history.getChildren().add(logScroll);

        content.getChildren().addAll(analytics, statusCard, history);

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

    private HBox infoRow(String name, Label value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label key = new Label(name);
        key.setTextFill(Theme.textDim());
        key.setMinWidth(80);
        key.setPrefWidth(80);
        value.getStyleClass().add("mono");
        value.setWrapText(true);
        HBox.setHgrow(value, Priority.ALWAYS);
        row.getChildren().addAll(key, value);
        return row;
    }

    // =====================================================================
    // STATUS BAR
    // =====================================================================

    private Region buildStatusBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 14, 8, 14));
        bar.setStyle(panelStyle());

        Circle dot = new Circle(4, Theme.accent());
        Label status = new Label("Ready · internal state always reflects the visualization");
        status.getStyleClass().add("dim");
        status.setStyle("-fx-font-size: 11px;");

        HBox left = new HBox(8, dot, status);
        left.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("PUSH · POP · ENQUEUE · DEQUEUE · INSERT · DELETE · SEARCH");
        hint.getStyleClass().add("dim");
        hint.setStyle("-fx-font-size: 10px;");

        bar.getChildren().addAll(left, spacer, hint);
        BorderPane.setMargin(bar, new Insets(12, 0, 0, 0));
        return bar;
    }

    // =====================================================================
    // OPERATION LIST
    // =====================================================================

    private void updateOperations() {
        operationBox.getItems().clear();
        String type = structureBox.getValue();
        switch (type) {
            case "Stack" -> operationBox.getItems().setAll(
                    "Push", "Pop", "Peek", "Search");
            case "Queue" -> operationBox.getItems().setAll(
                    "Enqueue", "Dequeue", "Peek", "Search");
            case "Linked List" -> operationBox.getItems().setAll(
                    "Append", "Prepend", "Remove First", "Remove Last", "Search");
            case "Binary Search Tree" -> operationBox.getItems().setAll(
                    "Insert", "Delete", "Search", "Find Minimum", "Find Maximum",
                    "Inorder Traversal", "Preorder Traversal", "Postorder Traversal");
            case "Max Heap" -> operationBox.getItems().setAll(
                    "Insert", "Extract Max", "Peek", "Search");
        }
        if (!operationBox.getItems().isEmpty()) {
            operationBox.getSelectionModel().selectFirst();
        }
    }

    private String operationDescription(String operation) {
        return switch (operation) {
            case "Push"              -> "Add to stack top";
            case "Pop"               -> "Remove stack top";
            case "Peek"              -> "Inspect next element";
            case "Enqueue"           -> "Add to queue back";
            case "Dequeue"           -> "Remove queue front";
            case "Append"            -> "Add to list tail";
            case "Prepend"           -> "Add to list head";
            case "Remove First"      -> "Remove list head";
            case "Remove Last"       -> "Remove list tail";
            case "Insert"            -> "Insert a new value";
            case "Delete"            -> "Delete a value";
            case "Extract Max"       -> "Remove maximum";
            case "Find Minimum"      -> "Find smallest value";
            case "Find Maximum"      -> "Find largest value";
            case "Inorder Traversal" -> "Left → Root → Right";
            case "Preorder Traversal"-> "Root → Left → Right";
            case "Postorder Traversal"->"Left → Right → Root";
            case "Search"            -> "Find a value";
            default -> "";
        };
    }

    // =====================================================================
    // EXECUTION
    // =====================================================================

    private void executeOperation() {
        String type = structureBox.getValue();
        String operation = operationBox.getValue();
        if (type == null || operation == null) return;
        switch (type) {
            case "Stack"             -> executeStack(operation);
            case "Queue"             -> executeQueue(operation);
            case "Linked List"       -> executeList(operation);
            case "Binary Search Tree"-> executeBST(operation);
            case "Max Heap"          -> executeHeap(operation);
        }
    }

    // -------------------- STACK --------------------
    private void executeStack(String operation) {
        switch (operation) {
            case "Push" -> {
                Integer value = readInteger();
                if (value == null) return;
                stack.push(value);
                highlightValue = value;
                log("PUSH", value);
                updateStatus("Pushed " + value + " onto the top.");
                completeMutation();
            }
            case "Pop" -> {
                if (stack.isEmpty()) { emptyOperation("POP", "Stack is empty."); return; }
                int value = stack.pop();
                highlightValue = value;
                log("POP", value);
                updateStatus("Popped " + value + " from the top.");
                completeMutation();
            }
            case "Peek" -> {
                if (stack.isEmpty()) { emptyOperation("PEEK", "Stack is empty."); return; }
                int value = stack.peek();
                highlightValue = value;
                log("PEEK", value);
                updateStatus("Top = " + value);
                render(); pulse();
            }
            case "Search" -> search(stack);
        }
    }

    // -------------------- QUEUE --------------------
    private void executeQueue(String operation) {
        switch (operation) {
            case "Enqueue" -> {
                Integer value = readInteger();
                if (value == null) return;
                queue.addLast(value);
                highlightValue = value;
                log("ENQUEUE", value);
                updateStatus("Added " + value + " to the back.");
                completeMutation();
            }
            case "Dequeue" -> {
                if (queue.isEmpty()) { emptyOperation("DEQUEUE", "Queue is empty."); return; }
                int value = queue.removeFirst();
                highlightValue = value;
                log("DEQUEUE", value);
                updateStatus("Removed " + value + " from the front.");
                completeMutation();
            }
            case "Peek" -> {
                if (queue.isEmpty()) { emptyOperation("PEEK", "Queue is empty."); return; }
                int value = queue.peekFirst();
                highlightValue = value;
                log("PEEK", value);
                updateStatus("Front = " + value);
                render(); pulse();
            }
            case "Search" -> search(queue);
        }
    }

    // -------------------- LINKED LIST --------------------
    private void executeList(String operation) {
        switch (operation) {
            case "Append" -> {
                Integer value = readInteger();
                if (value == null) return;
                list.addLast(value);
                highlightValue = value;
                log("APPEND", value);
                updateStatus("Added " + value + " to the tail.");
                completeMutation();
            }
            case "Prepend" -> {
                Integer value = readInteger();
                if (value == null) return;
                list.addFirst(value);
                highlightValue = value;
                log("PREPEND", value);
                updateStatus("Added " + value + " to the head.");
                completeMutation();
            }
            case "Remove First" -> {
                if (list.isEmpty()) { emptyOperation("REMOVE_FIRST", "Linked list is empty."); return; }
                int value = list.removeFirst();
                highlightValue = value;
                log("REMOVE_FIRST", value);
                updateStatus("Removed head node " + value);
                completeMutation();
            }
            case "Remove Last" -> {
                if (list.isEmpty()) { emptyOperation("REMOVE_LAST", "Linked list is empty."); return; }
                int value = list.removeLast();
                highlightValue = value;
                log("REMOVE_LAST", value);
                updateStatus("Removed tail node " + value);
                completeMutation();
            }
            case "Search" -> search(list);
        }
    }

    // -------------------- BST --------------------
    private void executeBST(String operation) {
        switch (operation) {
            case "Insert" -> {
                Integer value = readInteger();
                if (value == null) return;
                if (bstValues.contains(value)) {
                    highlightValue = value;
                    log("INSERT", value + " → duplicate");
                    updateStatus("BST already contains " + value);
                    render();
                    return;
                }
                bstRoot = bstInsert(bstRoot, value);
                bstValues.add(value);
                highlightValue = value;
                log("INSERT", value);
                updateStatus("Inserted " + value + " into the BST.");
                completeMutation();
            }
            case "Delete" -> {
                Integer value = readInteger();
                if (value == null) return;
                if (!bstValues.contains(value)) {
                    log("DELETE", value + " → not found");
                    updateStatus(value + " is not in the BST.");
                    render();
                    return;
                }
                bstRoot = bstDelete(bstRoot, value);
                bstValues.remove(value);
                highlightValue = value;
                log("DELETE", value);
                updateStatus("Deleted " + value + " from the BST.");
                completeMutation();
            }
            case "Search" -> {
                Integer value = readInteger();
                if (value == null) return;
                boolean found = bstSearch(bstRoot, value);
                highlightValue = found ? value : null;
                log("SEARCH", value + " → " + (found ? "FOUND" : "NOT FOUND"));
                updateStatus(found
                        ? "Found " + value + " in the BST."
                        : value + " was not found.");
                render();
                if (found) pulse();
            }
            case "Find Minimum" -> {
                if (bstRoot == null) { emptyOperation("MINIMUM", "BST is empty."); return; }
                int value = bstMinimum(bstRoot);
                highlightValue = value;
                log("MINIMUM", value);
                updateStatus("Minimum = " + value);
                render(); pulse();
            }
            case "Find Maximum" -> {
                if (bstRoot == null) { emptyOperation("MAXIMUM", "BST is empty."); return; }
                int value = bstMaximum(bstRoot);
                highlightValue = value;
                log("MAXIMUM", value);
                updateStatus("Maximum = " + value);
                render(); pulse();
            }
            case "Inorder Traversal" -> {
                List<Integer> result = new ArrayList<>();
                inorder(bstRoot, result);
                highlightValue = null;
                log("INORDER", result);
                updateStatus("Inorder: " + result);
                render();
            }
            case "Preorder Traversal" -> {
                List<Integer> result = new ArrayList<>();
                preorder(bstRoot, result);
                highlightValue = null;
                log("PREORDER", result);
                updateStatus("Preorder: " + result);
                render();
            }
            case "Postorder Traversal" -> {
                List<Integer> result = new ArrayList<>();
                postorder(bstRoot, result);
                highlightValue = null;
                log("POSTORDER", result);
                updateStatus("Postorder: " + result);
                render();
            }
        }
    }

    // -------------------- HEAP --------------------
    private void executeHeap(String operation) {
        switch (operation) {
            case "Insert" -> {
                Integer value = readInteger();
                if (value == null) return;
                heapInsert(value);
                highlightValue = value;
                log("INSERT", value);
                updateStatus("Inserted " + value + " and restored Max-Heap.");
                completeMutation();
            }
            case "Extract Max" -> {
                if (heap.isEmpty()) { emptyOperation("EXTRACT_MAX", "Heap is empty."); return; }
                int value = heapExtractMax();
                highlightValue = value;
                log("EXTRACT_MAX", value);
                updateStatus("Extracted maximum " + value);
                completeMutation();
            }
            case "Peek" -> {
                if (heap.isEmpty()) { emptyOperation("PEEK", "Heap is empty."); return; }
                int value = heap.get(0);
                highlightValue = value;
                log("PEEK", value);
                updateStatus("Maximum = " + value);
                render(); pulse();
            }
            case "Search" -> search(heap);
        }
    }

    // -------------------- SEARCH --------------------
    private void search(Collection<Integer> collection) {
        Integer value = readInteger();
        if (value == null) return;
        boolean found = collection.contains(value);
        highlightValue = found ? value : null;
        log("SEARCH", value + " → " + (found ? "FOUND" : "NOT FOUND"));
        updateStatus(found ? "Found " + value : value + " was not found.");
        render();
        if (found) pulse();
    }

    // =====================================================================
    // BST ALGORITHMS
    // =====================================================================

    private BstNode bstInsert(BstNode node, int value) {
        if (node == null) return new BstNode(value);
        if (value < node.value) node.left = bstInsert(node.left, value);
        else if (value > node.value) node.right = bstInsert(node.right, value);
        return node;
    }

    private BstNode bstDelete(BstNode node, int value) {
        if (node == null) return null;
        if (value < node.value) node.left = bstDelete(node.left, value);
        else if (value > node.value) node.right = bstDelete(node.right, value);
        else {
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;
            BstNode successor = node.right;
            while (successor.left != null) successor = successor.left;
            node.value = successor.value;
            node.right = bstDelete(node.right, successor.value);
        }
        return node;
    }

    private boolean bstSearch(BstNode node, int value) {
        if (node == null) return false;
        if (value == node.value) return true;
        return value < node.value
                ? bstSearch(node.left, value)
                : bstSearch(node.right, value);
    }

    private int bstMinimum(BstNode node) {
        while (node.left != null) node = node.left;
        return node.value;
    }

    private int bstMaximum(BstNode node) {
        while (node.right != null) node = node.right;
        return node.value;
    }

    private int bstHeight(BstNode node) {
        if (node == null) return 0;
        return 1 + Math.max(bstHeight(node.left), bstHeight(node.right));
    }

    private void inorder(BstNode node, List<Integer> result) {
        if (node == null) return;
        inorder(node.left, result);
        result.add(node.value);
        inorder(node.right, result);
    }

    private void preorder(BstNode node, List<Integer> result) {
        if (node == null) return;
        result.add(node.value);
        preorder(node.left, result);
        preorder(node.right, result);
    }

    private void postorder(BstNode node, List<Integer> result) {
        if (node == null) return;
        postorder(node.left, result);
        postorder(node.right, result);
        result.add(node.value);
    }

    // =====================================================================
    // HEAP ALGORITHMS
    // =====================================================================

    private void heapInsert(int value) {
        heap.add(value);
        int index = heap.size() - 1;
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (heap.get(parent) >= heap.get(index)) break;
            Collections.swap(heap, parent, index);
            index = parent;
        }
    }

    private int heapExtractMax() {
        int maximum = heap.get(0);
        int last = heap.remove(heap.size() - 1);
        if (!heap.isEmpty()) {
            heap.set(0, last);
            heapifyDown(0);
        }
        return maximum;
    }

    private void heapifyDown(int index) {
        while (true) {
            int left = index * 2 + 1;
            int right = index * 2 + 2;
            int largest = index;
            if (left < heap.size() && heap.get(left) > heap.get(largest)) largest = left;
            if (right < heap.size() && heap.get(right) > heap.get(largest)) largest = right;
            if (largest == index) break;
            Collections.swap(heap, index, largest);
            index = largest;
        }
    }

    // =====================================================================
    // RANDOM / GENERATE
    // =====================================================================

    private void insertRandom() {
        int value = random.nextInt(199) - 99;
        valueField.setText(String.valueOf(value));
        executeOperation();
    }

    private void generateSample() {
        clearStructuresOnly();
        String type = structureBox.getValue();
        int amount = 9;
        for (int i = 0; i < amount; i++) {
            int value = random.nextInt(90) + 10;
            switch (type) {
                case "Stack" -> stack.push(value);
                case "Queue" -> queue.addLast(value);
                case "Linked List" -> list.addLast(value);
                case "Binary Search Tree" -> {
                    if (bstValues.add(value)) bstRoot = bstInsert(bstRoot, value);
                }
                case "Max Heap" -> heapInsert(value);
            }
        }
        highlightValue = null;
        log("GENERATE", amount + " random elements");
        updateStatus("Generated " + amount + " values for " + type);
        render();
    }

    // =====================================================================
    // CLEAR
    // =====================================================================

    private void clearAll() {
        clearStructuresOnly();
        highlightValue = null;
        log("CLEAR", "current structure");
        updateStatus("Current structure cleared.");
        render();
    }

    private void clearStructuresOnly() {
        stack.clear();
        queue.clear();
        list.clear();
        heap.clear();
        bstRoot = null;
        bstValues.clear();
    }

    // =====================================================================
    // RENDER
    // =====================================================================

    private void render() {
        visualizationHost.getChildren().clear();
        String type = structureBox.getValue();
        updateAnalytics(type);
        switch (type) {
            case "Stack"             -> renderStack();
            case "Queue"             -> renderQueue();
            case "Linked List"       -> renderLinkedList();
            case "Binary Search Tree"-> renderBST();
            case "Max Heap"          -> renderHeap();
        }
    }

    // =====================================================================
    // ANALYTICS
    // =====================================================================

    private void updateAnalytics(String type) {
        typeLabel.setText(type);
        sizeLabel.setText(String.valueOf(currentSize(type)));
        heightLabel.setText(String.valueOf(currentHeight(type)));
        rootLabel.setText(currentRoot(type));
        minLabel.setText(currentMinimum(type));
        maxLabel.setText(currentMaximum(type));
        complexityLabel.setText(complexity(type));
    }

    private int currentSize(String type) {
        return switch (type) {
            case "Stack"             -> stack.size();
            case "Queue"             -> queue.size();
            case "Linked List"       -> list.size();
            case "Binary Search Tree"-> bstValues.size();
            case "Max Heap"          -> heap.size();
            default -> 0;
        };
    }

    private int currentHeight(String type) {
        return switch (type) {
            case "Stack", "Linked List" -> currentSize(type);
            case "Queue" -> queue.isEmpty() ? 0 : 1;
            case "Binary Search Tree" -> bstHeight(bstRoot);
            case "Max Heap" -> heapHeight();
            default -> 0;
        };
    }

    private String currentRoot(String type) {
        return switch (type) {
            case "Stack" -> stack.isEmpty() ? "—" : String.valueOf(stack.peek());
            case "Queue" -> queue.isEmpty() ? "—" : String.valueOf(queue.peekFirst());
            case "Linked List" -> list.isEmpty() ? "—" : String.valueOf(list.getFirst());
            case "Binary Search Tree" -> bstRoot == null ? "—" : String.valueOf(bstRoot.value);
            case "Max Heap" -> heap.isEmpty() ? "—" : String.valueOf(heap.get(0));
            default -> "—";
        };
    }

    private String currentMinimum(String type) {
        return switch (type) {
            case "Stack" -> minimum(stack);
            case "Queue" -> minimum(queue);
            case "Linked List" -> minimum(list);
            case "Binary Search Tree" -> bstRoot == null ? "—" : String.valueOf(bstMinimum(bstRoot));
            case "Max Heap" -> minimum(heap);
            default -> "—";
        };
    }

    private String currentMaximum(String type) {
        return switch (type) {
            case "Stack" -> maximum(stack);
            case "Queue" -> maximum(queue);
            case "Linked List" -> maximum(list);
            case "Binary Search Tree" -> bstRoot == null ? "—" : String.valueOf(bstMaximum(bstRoot));
            case "Max Heap" -> maximum(heap);
            default -> "—";
        };
    }

    private String minimum(Collection<Integer> values) {
        return values.isEmpty() ? "—" : String.valueOf(Collections.min(values));
    }

    private String maximum(Collection<Integer> values) {
        return values.isEmpty() ? "—" : String.valueOf(Collections.max(values));
    }

    private int heapHeight() {
        if (heap.isEmpty()) return 0;
        return (int) Math.floor(Math.log(heap.size()) / Math.log(2)) + 1;
    }

    private String complexity(String type) {
        return switch (type) {
            case "Stack"             -> "Push/Pop O(1)";
            case "Queue"             -> "Enqueue/Dequeue O(1)";
            case "Linked List"       -> "Head/Tail O(1), Search O(n)";
            case "Binary Search Tree"-> "Average O(log n), Worst O(n)";
            case "Max Heap"          -> "Insert/Extract O(log n)";
            default -> "—";
        };
    }

    // =====================================================================
    // STACK VISUALIZATION
    // =====================================================================

    private void renderStack() {
        if (stack.isEmpty()) {
            emptyCanvas("STACK EMPTY", "Use Push to add an element.");
            return;
        }

        VBox column = new VBox(7);
        column.setAlignment(Pos.BOTTOM_CENTER);

        int index = 0;
        int size = stack.size();
        for (Integer value : stack) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label indexLabel = new Label(String.format("[%02d]", size - index - 1));
            indexLabel.setTextFill(Theme.textDim());
            indexLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
            indexLabel.setMinWidth(40);
            indexLabel.setPrefWidth(40);

            Label box = valueBox(value, isHighlighted(value));
            row.getChildren().addAll(indexLabel, box);

            if (index == 0) row.getChildren().add(chip("TOP", true));
            else if (index == size - 1) row.getChildren().add(chip("BOTTOM", false));

            column.getChildren().add(row);
            index++;
        }

        Label direction = new Label("PUSH ↓     POP ↑");
        direction.setTextFill(Theme.textDim());
        direction.setFont(Font.font("Consolas", FontWeight.BOLD, 11));

        VBox shell = new VBox(13, column, direction);
        shell.setAlignment(Pos.CENTER);
        shell.setPadding(new Insets(22));
        shell.setStyle(shellStyle());

        visualizationHost.getChildren().add(shell);
    }

    // =====================================================================
    // QUEUE VISUALIZATION
    // =====================================================================

    private void renderQueue() {
        if (queue.isEmpty()) {
            emptyCanvas("QUEUE EMPTY", "Use Enqueue to add an element.");
            return;
        }

        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER);

        int index = 0;
        int size = queue.size();
        for (Integer value : queue) {
            VBox cell = new VBox(5);
            cell.setAlignment(Pos.CENTER);
            cell.getChildren().add(valueBox(value, isHighlighted(value)));

            if (index == 0) cell.getChildren().add(chip("FRONT", true));
            else if (index == size - 1) cell.getChildren().add(chip("BACK", false));
            else {
                Region spacer = new Region();
                spacer.setPrefHeight(22);
                cell.getChildren().add(spacer);
            }

            row.getChildren().add(cell);
            if (index < size - 1) {
                Label arrow = new Label("→");
                arrow.setTextFill(Theme.accent());
                arrow.setFont(Font.font("System", FontWeight.BOLD, 21));
                row.getChildren().add(arrow);
            }
            index++;
        }

        Label fifo = new Label("FIFO  ·  FIRST IN → FIRST OUT");
        fifo.setTextFill(Theme.textDim());
        fifo.setFont(Font.font("Consolas", FontWeight.BOLD, 11));

        ScrollPane hscroll = new ScrollPane(row);
        hscroll.setFitToHeight(true);
        hscroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        hscroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        hscroll.setPannable(true);
        hscroll.setMaxWidth(Double.MAX_VALUE);
        hscroll.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;");

        VBox shell = new VBox(14, hscroll, fifo);
        shell.setAlignment(Pos.CENTER);
        shell.setPadding(new Insets(24));
        shell.setStyle(shellStyle());

        visualizationHost.getChildren().add(shell);
    }

    // =====================================================================
    // LINKED LIST VISUALIZATION
    // =====================================================================

    private void renderLinkedList() {
        if (list.isEmpty()) {
            emptyCanvas("LINKED LIST EMPTY", "Append or Prepend a node.");
            return;
        }

        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER);
        row.getChildren().add(chip("HEAD", true));

        for (int i = 0; i < list.size(); i++) {
            int value = list.get(i);
            row.getChildren().add(arrow());
            row.getChildren().add(linkedNode(value, isHighlighted(value)));
        }

        Label nullLabel = new Label(" → NULL");
        nullLabel.setTextFill(Theme.textDim());
        nullLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        row.getChildren().add(nullLabel);

        Label description = new Label("HEAD → NODE → NODE → … → NULL");
        description.setTextFill(Theme.textDim());
        description.setFont(Font.font("Consolas", FontWeight.BOLD, 10));

        ScrollPane hscroll = new ScrollPane(row);
        hscroll.setFitToHeight(true);
        hscroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        hscroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        hscroll.setPannable(true);
        hscroll.setMaxWidth(Double.MAX_VALUE);
        hscroll.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;");
        hscroll.setPadding(new Insets(6, 0, 6, 0));

        VBox shell = new VBox(16, hscroll, description);
        shell.setAlignment(Pos.CENTER);
        shell.setPadding(new Insets(28));
        shell.setStyle(shellStyle());

        visualizationHost.getChildren().add(shell);
    }

    private HBox linkedNode(int value, boolean highlighted) {
        HBox node = new HBox();
        node.setAlignment(Pos.CENTER);

        Label valueLabel = new Label(String.valueOf(value));
        valueLabel.setAlignment(Pos.CENTER);
        valueLabel.setPrefSize(67, 48);
        valueLabel.setMinSize(67, 48);
        valueLabel.setMaxSize(67, 48);
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        valueLabel.setTextFill(highlighted ? Color.WHITE : Theme.text());
        valueLabel.setBackground(new Background(new BackgroundFill(
                highlighted ? Theme.accent() : Theme.panel3(),
                new CornerRadii(9, 0, 0, 9, false), Insets.EMPTY)));

        Label pointer = new Label("next");
        pointer.setAlignment(Pos.CENTER);
        pointer.setPrefSize(48, 48);
        pointer.setMinSize(48, 48);
        pointer.setMaxSize(48, 48);
        pointer.setTextFill(Theme.textDim());
        pointer.setBackground(new Background(new BackgroundFill(
                Theme.panel2(),
                new CornerRadii(0, 9, 9, 0, false), Insets.EMPTY)));

        node.getChildren().addAll(valueLabel, pointer);
        node.setEffect(new DropShadow(7, Color.rgb(0, 0, 0, 0.22)));
        return node;
    }

    private Label arrow() {
        Label label = new Label("→");
        label.setTextFill(Theme.accent());
        label.setFont(Font.font("System", FontWeight.BOLD, 21));
        label.setPadding(new Insets(0, 7, 0, 7));
        return label;
    }

    // =====================================================================
    // BST VISUALIZATION — in-order slot layout
    // =====================================================================

    private void renderBST() {
        if (bstRoot == null) {
            emptyCanvas("BINARY SEARCH TREE EMPTY", "Insert values to build the tree.");
            return;
        }

        int h = bstHeight(bstRoot);
        slotCursor = 0;
        Map<BstNode, double[]> slots = new IdentityHashMap<>();
        assignBstSlots(bstRoot, 0, slots);

        double slot   = 62;
        double levelH = 90;
        double padX   = 50;
        double padY   = 45;

        double contentW = Math.max(1, slotCursor) * slot;
        double width    = Math.max(640, contentW + padX * 2);
        double height   = Math.max(400, h * levelH + padY);

        Pane pane = new Pane();
        pane.setPrefSize(width, height);
        pane.setMinSize(width, height);
        pane.setMaxSize(width, height);

        double x0 = padX + slot / 2;
        double y0 = padY;

        drawBstEdges(pane, bstRoot, slots, x0, y0, slot, levelH);
        drawBstNodes(pane, bstRoot, slots, x0, y0, slot, levelH);

        ScrollPane hscroll = new ScrollPane(pane);
        hscroll.setFitToHeight(true);
        hscroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        hscroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        hscroll.setPannable(true);
        hscroll.setMaxWidth(Double.MAX_VALUE);
        hscroll.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;");

        VBox shell = new VBox(12, hscroll, traversalSummary());
        shell.setAlignment(Pos.CENTER);
        shell.setPadding(new Insets(15));

        visualizationHost.getChildren().add(shell);
    }

    private void assignBstSlots(BstNode node, int depth, Map<BstNode, double[]> out) {
        if (node == null) return;
        assignBstSlots(node.left, depth + 1, out);
        out.put(node, new double[]{ slotCursor++, depth });
        assignBstSlots(node.right, depth + 1, out);
    }

    private void drawBstEdges(Pane pane, BstNode n, Map<BstNode, double[]> slots,
                              double x0, double y0, double slot, double levelH) {
        if (n == null) return;
        double[] p = slots.get(n);
        double x = x0 + p[0] * slot;
        double y = y0 + p[1] * levelH;
        if (n.left != null) {
            double[] cp = slots.get(n.left);
            pane.getChildren().add(edgeLine(x, y + 23,
                    x0 + cp[0] * slot, y0 + cp[1] * levelH - 23));
        }
        if (n.right != null) {
            double[] cp = slots.get(n.right);
            pane.getChildren().add(edgeLine(x, y + 23,
                    x0 + cp[0] * slot, y0 + cp[1] * levelH - 23));
        }
        drawBstEdges(pane, n.left,  slots, x0, y0, slot, levelH);
        drawBstEdges(pane, n.right, slots, x0, y0, slot, levelH);
    }

    private void drawBstNodes(Pane pane, BstNode n, Map<BstNode, double[]> slots,
                              double x0, double y0, double slot, double levelH) {
        if (n == null) return;
        double[] p = slots.get(n);
        addTreeNode(pane, n.value,
                x0 + p[0] * slot,
                y0 + p[1] * levelH,
                isHighlighted(n.value));
        drawBstNodes(pane, n.left,  slots, x0, y0, slot, levelH);
        drawBstNodes(pane, n.right, slots, x0, y0, slot, levelH);
    }

    private VBox traversalSummary() {
        List<Integer> in = new ArrayList<>();
        List<Integer> pre = new ArrayList<>();
        List<Integer> post = new ArrayList<>();
        inorder(bstRoot, in);
        preorder(bstRoot, pre);
        postorder(bstRoot, post);

        VBox box = new VBox(4,
                traversalLine("INORDER", in),
                traversalLine("PREORDER", pre),
                traversalLine("POSTORDER", post));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Label traversalLine(String title, List<Integer> values) {
        Label label = new Label(title + "   " + values);
        label.setTextFill(Theme.textDim());
        label.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        return label;
    }

    // =====================================================================
    // HEAP VISUALIZATION
    // =====================================================================

    private void renderHeap() {
        if (heap.isEmpty()) {
            emptyCanvas("MAX HEAP EMPTY", "Insert values to build the heap.");
            return;
        }

        int h = heapHeight();
        slotCursor = 0;
        Map<Integer, double[]> slots = new HashMap<>();
        assignHeapSlots(0, 0, slots);

        double slot   = 62;
        double levelH = 90;
        double padX   = 50;
        double padY   = 45;

        double contentW = Math.max(1, slotCursor) * slot;
        double width    = Math.max(640, contentW + padX * 2);
        double height   = Math.max(400, h * levelH + padY);

        Pane pane = new Pane();
        pane.setPrefSize(width, height);
        pane.setMinSize(width, height);
        pane.setMaxSize(width, height);

        double x0 = padX + slot / 2;
        double y0 = padY;

        drawHeapEdges(pane, 0, slots, x0, y0, slot, levelH);
        drawHeapNodes(pane, 0, slots, x0, y0, slot, levelH);

        ScrollPane treeScroll = new ScrollPane(pane);
        treeScroll.setFitToHeight(true);
        treeScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        treeScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        treeScroll.setPannable(true);
        treeScroll.setMaxWidth(Double.MAX_VALUE);
        treeScroll.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;");

        VBox shell = new VBox(15, treeScroll, heapArrayView());
        shell.setAlignment(Pos.CENTER);
        shell.setPadding(new Insets(15));

        visualizationHost.getChildren().add(shell);
    }

    private void assignHeapSlots(int index, int depth, Map<Integer, double[]> out) {
        if (index >= heap.size()) return;
        assignHeapSlots(2 * index + 1, depth + 1, out);
        out.put(index, new double[]{ slotCursor++, depth });
        assignHeapSlots(2 * index + 2, depth + 1, out);
    }

    private void drawHeapEdges(Pane pane, int index, Map<Integer, double[]> slots,
                               double x0, double y0, double slot, double levelH) {
        if (index >= heap.size()) return;
        double[] p = slots.get(index);
        double x = x0 + p[0] * slot;
        double y = y0 + p[1] * levelH;
        int left  = 2 * index + 1;
        int right = 2 * index + 2;
        if (left < heap.size()) {
            double[] cp = slots.get(left);
            pane.getChildren().add(edgeLine(x, y + 23,
                    x0 + cp[0] * slot, y0 + cp[1] * levelH - 23));
        }
        if (right < heap.size()) {
            double[] cp = slots.get(right);
            pane.getChildren().add(edgeLine(x, y + 23,
                    x0 + cp[0] * slot, y0 + cp[1] * levelH - 23));
        }
        drawHeapEdges(pane, left,  slots, x0, y0, slot, levelH);
        drawHeapEdges(pane, right, slots, x0, y0, slot, levelH);
    }

    private void drawHeapNodes(Pane pane, int index, Map<Integer, double[]> slots,
                               double x0, double y0, double slot, double levelH) {
        if (index >= heap.size()) return;
        double[] p = slots.get(index);
        addTreeNode(pane, heap.get(index),
                x0 + p[0] * slot,
                y0 + p[1] * levelH,
                isHighlighted(heap.get(index)));
        drawHeapNodes(pane, 2 * index + 1, slots, x0, y0, slot, levelH);
        drawHeapNodes(pane, 2 * index + 2, slots, x0, y0, slot, levelH);
    }

    private Node heapArrayView() {
        FlowPane row = new FlowPane(5, 5);
        row.setAlignment(Pos.CENTER);
        row.setPrefWrapLength(820);
        row.setMaxWidth(820);

        for (int i = 0; i < heap.size(); i++) {
            Label index = new Label("[" + i + "]");
            index.setTextFill(Theme.textDim());
            index.setFont(Font.font("Consolas", 9));
            index.setAlignment(Pos.CENTER);

            Label value = valueBox(heap.get(i), isHighlighted(heap.get(i)), 54, 38, 13);

            VBox cell = new VBox(3, index, value);
            cell.setAlignment(Pos.CENTER);
            row.getChildren().add(cell);
        }

        Label caption = new Label("Array representation  (parent i → children 2i+1, 2i+2)");
        caption.setTextFill(Theme.textDim());
        caption.setFont(Font.font("Consolas", FontWeight.BOLD, 10));

        VBox wrap = new VBox(6, caption, row);
        wrap.setAlignment(Pos.CENTER);
        return wrap;
    }

    // =====================================================================
    // TREE NODES
    // =====================================================================

    private void addTreeNode(Pane pane, int value, double x, double y, boolean highlighted) {
        StackPane node = new StackPane();

        Circle circle = new Circle(25);
        circle.setFill(highlighted ? Theme.accent() : Theme.panel3());
        circle.setStroke(highlighted ? Theme.accent2() : Theme.border());
        circle.setStrokeWidth(2);
        circle.setEffect(new DropShadow(9, Color.rgb(0, 0, 0, 0.30)));

        Label label = new Label(String.valueOf(value));
        label.setFont(Font.font("System", FontWeight.BOLD, 13));
        label.setTextFill(highlighted ? Color.WHITE : Theme.text());
        label.setMouseTransparent(true);

        node.getChildren().addAll(circle, label);
        node.setLayoutX(x - 25);
        node.setLayoutY(y - 25);

        FadeTransition fade = new FadeTransition(Duration.millis(180), node);
        fade.setFromValue(0.15);
        fade.setToValue(1);
        fade.play();

        pane.getChildren().add(node);
    }

    private Line edgeLine(double x1, double y1, double x2, double y2) {
        Line line = new Line(x1, y1, x2, y2);
        line.setStroke(Theme.border());
        line.setStrokeWidth(2);
        return line;
    }

    // =====================================================================
    // EMPTY STATE
    // =====================================================================

    private void emptyCanvas(String title, String description) {
        VBox box = new VBox(9);
        box.setAlignment(Pos.CENTER);

        Polygon diamond = new Polygon(0, -22, 22, 0, 0, 22, -22, 0);
        diamond.setFill(Theme.accent().deriveColor(0, 1, 1, 0.20));
        diamond.setStroke(Theme.accent());
        diamond.setStrokeWidth(2);

        Label heading = new Label(title);
        heading.setTextFill(Theme.text());
        heading.setFont(Font.font("System", FontWeight.BOLD, 14));

        Label text = new Label(description);
        text.setTextFill(Theme.textDim());
        text.setWrapText(true);

        box.getChildren().addAll(diamond, heading, text);
        visualizationHost.getChildren().add(box);
    }

    // =====================================================================
    // VALUE BOX
    // =====================================================================

    private Label valueBox(int value, boolean highlighted) {
        return valueBox(value, highlighted, 72, 52, 15);
    }

    private Label valueBox(int value, boolean highlighted,
                           double w, double h, double fontSize) {
        Label label = new Label(String.valueOf(value));
        label.setAlignment(Pos.CENTER);
        label.setPrefSize(w, h);
        label.setMinSize(w, h);
        label.setMaxSize(w, h);
        label.setFont(Font.font("System", FontWeight.BOLD, fontSize));

        Color top    = highlighted ? Theme.accent() : Theme.panel3();
        Color bottom = highlighted ? Theme.accent2() : Theme.panel2();
        label.setTextFill(highlighted ? Color.WHITE : Theme.text());

        double radius = Math.min(10, h * 0.25);

        label.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, top), new Stop(1, bottom)),
                new CornerRadii(radius), Insets.EMPTY)));

        label.setBorder(new Border(new BorderStroke(
                highlighted ? Theme.accent2() : Theme.border(),
                BorderStrokeStyle.SOLID, new CornerRadii(radius), new BorderWidths(1))));

        label.setEffect(new DropShadow(7, Color.rgb(0, 0, 0, 0.23)));

        FadeTransition fade = new FadeTransition(Duration.millis(160), label);
        fade.setFromValue(0.65);
        fade.setToValue(1);
        fade.play();

        return label;
    }

    // =====================================================================
    // CHIPS
    // =====================================================================

    private Label chip(String text, boolean active) {
        Label label = new Label(text);
        label.setPadding(new Insets(5, 9, 5, 9));
        label.setFont(Font.font("System", FontWeight.BOLD, 9));
        label.setTextFill(active ? Color.WHITE : Theme.textDim());
        label.setBackground(new Background(new BackgroundFill(
                active ? Theme.accent() : Theme.panel3(),
                new CornerRadii(20), Insets.EMPTY)));
        return label;
    }

    // =====================================================================
    // BUTTONS
    // =====================================================================

    private Button primaryButton(String text, String icon, Runnable action) {
        Button button = new Button(text, Icons.get(icon, 14));
        button.getStyleClass().addAll("btn", "btn-primary");
        button.setOnAction(e -> action.run());
        return button;
    }

    private Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("btn");
        button.setOnAction(e -> action.run());
        return button;
    }

    // =====================================================================
    // INPUT
    // =====================================================================

    private Integer readInteger() {
        String input = valueField.getText().trim();
        if (input.isEmpty()) {
            updateStatus("Enter an integer first.");
            return null;
        }
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            updateStatus("Invalid integer: " + input);
            return null;
        }
    }

    // =====================================================================
    // OPERATION HELPERS
    // =====================================================================

    private void completeMutation() {
        valueField.clear();
        render();
        pulse();
    }

    private void emptyOperation(String operation, String message) {
        log(operation, "EMPTY");
        updateStatus(message);
        render();
    }

    private void log(String operation, Object result) {
        operationCount++;
        Label entry = new Label(String.format("#%03d  %-14s %s",
                operationCount, operation, String.valueOf(result)));
        entry.setTextFill(Theme.textDim());
        entry.setFont(Font.font("Consolas", 10));
        entry.setPadding(new Insets(4, 5, 4, 5));
        entry.setWrapText(true);
        logBox.getChildren().add(0, entry);
        while (logBox.getChildren().size() > 80) {
            logBox.getChildren().remove(logBox.getChildren().size() - 1);
        }
        operationCountLabel.setText(operationCount + " operations");
        // Scroll the log to the top so newest entry is visible.
        if (logScroll != null) {
            javafx.application.Platform.runLater(() -> logScroll.setVvalue(0));
        }
    }

    // =====================================================================
    // STATUS
    // =====================================================================

    private void updateStatus(String message) {
        statusLabel.setText(message);
        String lower = message.toLowerCase(Locale.ROOT);
        boolean error = lower.contains("empty")
                || lower.contains("invalid")
                || lower.contains("not found");
        statusLabel.setTextFill(error ? Color.web("#ff7272") : Theme.accent());
    }

    // =====================================================================
    // ANIMATION
    // =====================================================================

    private void pulse() {
        if (visualizationHost.getChildren().isEmpty()) return;
        Node root = visualizationHost.getChildren().get(0);
        ScaleTransition scale = new ScaleTransition(Duration.millis(170), root);
        scale.setFromX(1); scale.setFromY(1);
        scale.setToX(1.025); scale.setToY(1.025);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }

    // =====================================================================
    // HELPERS
    // =====================================================================

    private boolean isHighlighted(int value) {
        return highlightValue != null && highlightValue == value;
    }

    private String panelStyle() {
        return "-fx-background-color: " + web(Theme.panel()) + ";" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: " + web(Theme.border()) + ";" +
                "-fx-border-radius: 14;" +
                "-fx-border-width: 1;";
    }

    private String shellStyle() {
        return "-fx-background-color: " + web(Theme.panel2()) + ";" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: " + web(Theme.border()) + ";" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1.5;";
    }

    private String web(Color color) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }
}