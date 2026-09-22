package app.ui;

import app.theme.Theme;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class MetricsView extends BorderPane {
    private final Label ops = new Label("0");
    private final Label mem = new Label("—");
    private final Label threads = new Label("—");
    private final Label uptime = new Label("—");
    private final long start = System.nanoTime();

    public MetricsView() {
        setPadding(new Insets(24));
        setStyle("-fx-background-color: " + web(Theme.bg()) + ";");

        Label t = new Label("PERFORMANCE MONITOR");
        t.getStyleClass().add("h1");
        Label s = new Label("Live runtime metrics of the Algorithm Lab engine");
        s.getStyleClass().add("dim");
        VBox header = new VBox(2, t, s);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.add(metricCard("OPERATIONS", ops, "Total algorithm operations"), 0, 0);
        grid.add(metricCard("MEMORY", mem, "JVM heap usage"), 1, 0);
        grid.add(metricCard("THREADS", threads, "Active JVM threads"), 0, 1);
        grid.add(metricCard("UPTIME", uptime, "Since application start"), 1, 1);

        VBox root = new VBox(16, header, grid);
        setCenter(root);

        Timeline tl = new Timeline(new KeyFrame(Duration.millis(500), e -> update()));
        tl.setCycleCount(Animation.INDEFINITE);
        tl.play();
        update();
    }

    private VBox metricCard(String title, Label value, String subtitle) {
        VBox v = new VBox(6);
        v.getStyleClass().add("card");
        v.setPrefWidth(280);
        v.setMinHeight(140);
        v.setStyle(v.getStyle() + " -fx-cursor: default;");

        Circle dot = new Circle(4, Theme.accent());
        Label t = new Label(title);
        t.getStyleClass().add("section-label");
        t.setPadding(new Insets(0));
        value.getStyleClass().add("h1");
        Label s = new Label(subtitle);
        s.getStyleClass().add("dim");

        HBox head = new HBox(8, dot, t);
        head.setAlignment(Pos.CENTER_LEFT);

        v.getChildren().addAll(head, value, s);
        return v;
    }

    private void update() {
        Runtime rt = Runtime.getRuntime();

        // Heap usage = (total - free) MB. No java.lang.management needed.
        long totalBytes = rt.totalMemory();
        long freeBytes  = rt.freeMemory();
        long usedBytes  = totalBytes - freeBytes;
        long usedMb     = usedBytes / (1024L * 1024L);
        long maxMb      = rt.maxMemory() / (1024L * 1024L);

        mem.setText(usedMb + " / " + maxMb + " MB");
        threads.setText(String.valueOf(Thread.activeCount()));

        long up = (System.nanoTime() - start) / 1_000_000_000L;
        uptime.setText(up + " s");
    }

    private String web(Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed() * 255), (int)(c.getGreen() * 255), (int)(c.getBlue() * 255));
    }
}