package app;

import app.ui.MainWindow;

public final class ApplicationController {
    private static MainWindow window;
    private ApplicationController() {}
    public static void setWindow(MainWindow w) { window = w; }
    public static MainWindow window() { return window; }
    public static void setStatus(String s) { if (window != null) window.setStatus(s); }
    public static void setOperations(long n) { if (window != null) window.setOperations(n); }
    public static void setComplexity(String s) { if (window != null) window.setComplexity(s); }
}