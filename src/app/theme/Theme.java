package app.theme;

import javafx.scene.Scene;
import javafx.scene.paint.Color;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class Theme {
    public enum Mode { DARK, LIGHT }
    private static Mode mode = Mode.DARK;
    private static Scene scene;
    private static final List<Runnable> listeners = new ArrayList<>();
    private Theme() {}

    public static void init() {}

    public static boolean isDark() { return mode == Mode.DARK; }
    public static Mode getMode() { return mode; }

    public static void register(Scene s) { scene = s; apply(); }
    public static void addListener(Runnable r) { listeners.add(r); }
    public static void removeListener(Runnable r) { listeners.remove(r); }

    public static void toggle() {
        mode = mode == Mode.DARK ? Mode.LIGHT : Mode.DARK;
        apply();
        for (Runnable r : new ArrayList<>(listeners)) r.run();
    }

    public static void apply() {
        if (scene == null) return;
        String css = buildCss();
        String uri = "data:text/css;base64," +
                Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
        scene.getStylesheets().setAll(uri);
    }

    // -------- Java colors (for canvas-drawn shapes) --------
    public static Color bg()       { return c("#0b0e14", "#f4f6fb"); }
    public static Color panel()    { return c("#131824", "#ffffff"); }
    public static Color panel2()   { return c("#1a2133", "#eef2f8"); }
    public static Color panel3()   { return c("#232c42", "#e2e8f2"); }
    public static Color border()   { return c("#232b3d", "#d8dee9"); }
    public static Color grid()     { return c("#1d2436", "#e6ebf3"); }
    public static Color text()     { return c("#e6ecf5", "#1a2033"); }
    public static Color textDim()  { return c("#8b96ad", "#5f6a80"); }
    public static Color accent()   { return c("#4d9eff", "#2c7fff"); }
    public static Color accent2()  { return c("#7c5cff", "#6a3cff"); }
    public static Color success()  { return Color.web("#35d07f"); }
    public static Color warn()     { return Color.web("#ffb454"); }
    public static Color danger()   { return Color.web("#ff5c72"); }

    private static Color c(String dark, String light) { return Color.web(isDark() ? dark : light); }

    private static String buildCss() {
        String bg      = isDark() ? "#0b0e14" : "#f4f6fb";
        String panel   = isDark() ? "#131824" : "#ffffff";
        String panel2  = isDark() ? "#1a2133" : "#eef2f8";
        String panel3  = isDark() ? "#232c42" : "#e2e8f2";
        String border  = isDark() ? "#232b3d" : "#d8dee9";
        String text    = isDark() ? "#e6ecf5" : "#1a2033";
        String textDim = isDark() ? "#8b96ad" : "#5f6a80";
        String accent  = isDark() ? "#4d9eff" : "#2c7fff";
        String accent2 = isDark() ? "#7c5cff" : "#6a3cff";
        String danger  = "#ff5c72";
        String success = "#35d07f";

        return """
        .root {
            -fx-background-color: %BG%;
            -fx-font-family: "Inter", "SF Pro Display", "Segoe UI", "System";
            -fx-font-size: 13px;
            -fx-text-fill: %TEXT%;
        }
        .label { -fx-text-fill: %TEXT%; }
        .label-dim { -fx-text-fill: %DIM%; }

        .app-bg { -fx-background-color: %BG%; }

        .topbar {
            -fx-background-color: %PANEL%;
            -fx-border-color: transparent transparent %BORDER% transparent;
            -fx-border-width: 0 0 1 0;
            -fx-padding: 10 16 10 16;
        }
        .statusbar {
            -fx-background-color: %PANEL%;
            -fx-border-color: %BORDER% transparent transparent transparent;
            -fx-border-width: 1 0 0 0;
            -fx-padding: 6 16 6 16;
        }
        .sidebar {
            -fx-background-color: %PANEL%;
            -fx-border-color: transparent %BORDER% transparent transparent;
            -fx-border-width: 0 1 0 0;
            -fx-padding: 12 10 12 10;
        }

        .brand-title { -fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: %TEXT%; }
        .brand-sub   { -fx-font-size: 11px; -fx-text-fill: %DIM%; }

        .section-label {
            -fx-font-size: 10px; -fx-font-weight: 800;
            -fx-text-fill: %DIM%;
            -fx-padding: 14 8 6 8;
        }

        .nav-item {
            -fx-background-color: transparent;
            -fx-background-radius: 10;
            -fx-padding: 8 12 8 12;
            -fx-cursor: hand;
        }
        .nav-item:hover { -fx-background-color: %PANEL2%; }
        .nav-item.active {
            -fx-background-color: %PANEL3%;
        }
        .nav-item.active .nav-text { -fx-text-fill: %ACCENT%; -fx-font-weight: 700; }
        .nav-text { -fx-text-fill: %TEXT%; -fx-font-size: 12.5px; }

        .icon-path { -fx-fill: %TEXT%; }
        .nav-item.active .icon-path { -fx-fill: %ACCENT%; }
        .icon-dim .icon-path { -fx-fill: %DIM%; }

        .card {
            -fx-background-color: %PANEL%;
            -fx-background-radius: 14;
            -fx-border-color: %BORDER%;
            -fx-border-radius: 14;
            -fx-border-width: 1;
            -fx-padding: 18;
            -fx-cursor: hand;
        }
        .card:hover {
            -fx-border-color: %ACCENT%;
            -fx-effect: dropshadow(gaussian, rgba(77,158,255,0.22), 20, 0.1, 0, 4);
        }

        .panel {
            -fx-background-color: %PANEL%;
            -fx-background-radius: 12;
            -fx-border-color: %BORDER%;
            -fx-border-radius: 12;
            -fx-border-width: 1;
            -fx-padding: 14;
        }
        .panel-tight { -fx-padding: 10; }

        .h1 { -fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: %TEXT%; }
        .h2 { -fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: %TEXT%; }
        .h3 { -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: %TEXT%; }
        .dim { -fx-text-fill: %DIM%; }
        .mono { -fx-font-family: "JetBrains Mono", "Consolas", monospace; -fx-font-size: 12px; -fx-text-fill: %TEXT%; }
        .mono-dim { -fx-font-family: "JetBrains Mono", "Consolas", monospace; -fx-font-size: 12px; -fx-text-fill: %DIM%; }

        .btn {
            -fx-background-color: %PANEL2%;
            -fx-text-fill: %TEXT%;
            -fx-background-radius: 10;
            -fx-padding: 8 14 8 14;
            -fx-cursor: hand;
            -fx-border-color: %BORDER%;
            -fx-border-radius: 10;
            -fx-border-width: 1;
        }
        .btn:hover { -fx-background-color: %PANEL3%; -fx-border-color: %ACCENT%; }
        .btn:pressed { -fx-translate-y: 1; }

        .btn-primary {
            -fx-background-color: %ACCENT%;
            -fx-text-fill: white;
            -fx-border-color: transparent;
            -fx-font-weight: 700;
        }
        .btn-primary:hover { -fx-background-color: derive(%ACCENT%, 10%); }

        .btn-accent2 {
            -fx-background-color: %ACCENT2%;
            -fx-text-fill: white;
            -fx-border-color: transparent;
            -fx-font-weight: 700;
        }
        .btn-accent2:hover { -fx-background-color: derive(%ACCENT2%, 10%); }

        .btn-ghost {
            -fx-background-color: transparent;
            -fx-border-color: transparent;
            -fx-text-fill: %TEXT%;
        }
        .btn-ghost:hover { -fx-background-color: %PANEL2%; -fx-border-color: transparent; }

        .icon-btn {
            -fx-background-color: transparent;
            -fx-padding: 8;
            -fx-background-radius: 10;
            -fx-cursor: hand;
        }
        .icon-btn:hover { -fx-background-color: %PANEL2%; }
        .icon-btn.active { -fx-background-color: %PANEL3%; }

        .field {
            -fx-background-color: %PANEL2%;
            -fx-background-radius: 10;
            -fx-border-color: %BORDER%;
            -fx-border-radius: 10;
            -fx-padding: 8 12 8 12;
            -fx-text-fill: %TEXT%;
            -fx-prompt-text-fill: %DIM%;
        }
        .field:focused { -fx-border-color: %ACCENT%; }

        .chip {
            -fx-background-color: %PANEL2%;
            -fx-background-radius: 8;
            -fx-padding: 4 10 4 10;
            -fx-text-fill: %DIM%;
            -fx-font-size: 11px;
            -fx-font-weight: 700;
        }
        .chip-accent { -fx-background-color: rgba(77,158,255,0.15); -fx-text-fill: %ACCENT%; }

        .scroll-pane, .scroll-pane > .viewport { -fx-background-color: transparent; }
        .scroll-bar:vertical, .scroll-bar:horizontal { -fx-background-color: transparent; }
        .scroll-bar .thumb { -fx-background-color: %BORDER%; -fx-background-radius: 6; }
        .scroll-bar .thumb:hover { -fx-background-color: %DIM%; }
        .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-background-color: transparent; -fx-padding: 0; }
        .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-shape: " "; -fx-padding: 0; }

        .separator .line { -fx-border-color: %BORDER% transparent transparent transparent; }

        .slider .track { -fx-background-color: %PANEL2%; -fx-pref-height: 4; }
        .slider .thumb { -fx-background-color: %ACCENT%; -fx-pref-width: 14; -fx-pref-height: 14; }

        .tab-pane .tab-header-background { -fx-background-color: transparent; }
        .tab-pane .tab { -fx-background-color: transparent; -fx-padding: 6 12 6 12; }
        .tab-pane .tab:selected { -fx-background-color: %PANEL2%; -fx-background-radius: 8; }
        .tab-pane .tab-label { -fx-text-fill: %DIM%; -fx-font-weight: 700; -fx-font-size: 11.5px; }
        .tab-pane .tab:selected .tab-label { -fx-text-fill: %ACCENT%; }
        .tab-pane .tab-header-area .tab-header-background { -fx-background-color: transparent; }

        .list-view { -fx-background-color: transparent; -fx-border-color: transparent; }
        .list-view .list-cell {
            -fx-background-color: transparent;
            -fx-text-fill: %TEXT%;
            -fx-padding: 6 10 6 10;
        }
        .list-view .list-cell:filled:hover { -fx-background-color: %PANEL2%; -fx-background-radius: 6; }
        .list-view .list-cell:filled:selected { -fx-background-color: %PANEL3%; -fx-background-radius: 6; }

        .dialog-pane { -fx-background-color: %PANEL%; }
        .dialog-pane .label { -fx-text-fill: %TEXT%; }

        .check-box .box { -fx-background-color: %PANEL2%; -fx-border-color: %BORDER%; -fx-border-radius: 4; -fx-background-radius: 4; }
        .check-box:selected .box { -fx-background-color: %ACCENT%; -fx-border-color: %ACCENT%; }
        .check-box:selected .mark { -fx-background-color: white; }
        .check-box .text { -fx-fill: %TEXT%; }

        .toggle-btn {
            -fx-background-color: %PANEL2%;
            -fx-background-radius: 10;
            -fx-border-color: %BORDER%;
            -fx-border-radius: 10;
            -fx-padding: 6 12;
            -fx-text-fill: %TEXT%;
            -fx-cursor: hand;
        }
        .toggle-btn:selected { -fx-background-color: rgba(77,158,255,0.15); -fx-text-fill: %ACCENT%; -fx-border-color: %ACCENT%; }
        """
        .replace("%BG%", bg).replace("%PANEL%", panel).replace("%PANEL2%", panel2)
        .replace("%PANEL3%", panel3).replace("%BORDER%", border).replace("%TEXT%", text)
        .replace("%DIM%", textDim).replace("%ACCENT%", accent).replace("%ACCENT2%", accent2)
        .replace("%DANGER%", danger).replace("%SUCCESS%", success);
    }
}