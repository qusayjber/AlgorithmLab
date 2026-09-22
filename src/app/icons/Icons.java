package app.icons;

import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

import java.util.HashMap;
import java.util.Map;

public final class Icons {
    private static final Map<String, String> P = new HashMap<>();
    static {
        P.put("home",      "M12 3 2 12h3v8h6v-6h2v6h6v-8h3z");
        P.put("algorithms","M4 6h6v6H4zM14 12h6v6h-6zM7 12v3h4");
        P.put("hanoi",     "M4 18h16v2H4zM5 8h2v9H5zM11 6h2v11h-2zM17 4h2v13h-2zM3 15h6v1H3zM9 13h6v1H9zM15 11h6v1h-6z");
        P.put("sorting",   "M4 18V10h2v8zm4 0V6h2v12zm4 0v-8h2v8zm4 0V4h2v14z");
        P.put("search",    "M11 4a7 7 0 1 0 4.9 12l4 4 1.4-1.4-4-4A7 7 0 0 0 11 4zm0 2a5 5 0 1 1 0 10 5 5 0 0 1 0-10z");
        P.put("graph",     "M6 6a2 2 0 1 1 0-.1zM6 4a4 4 0 1 0 0 8 4 4 0 0 0 0-8zm12 8a3 3 0 1 0 0 6 3 3 0 0 0 0-6zM8 8l8 6");
        P.put("path",      "M4 20h4v-4H4zm6 0h4v-4h-4zm6 0h4v-4h-4zM6 14V10h12v4M6 8V4h12v4");
        P.put("recursion", "M12 3a9 9 0 0 0-9 9h3a6 6 0 0 1 12 0h3a9 9 0 0 0-9-9zm0 5a4 4 0 0 0-4 4h3a1 1 0 0 1 2 0h3a4 4 0 0 0-4-4zM11 15v4h2v-4z");
        P.put("ds",        "M4 4h16v3H4zm0 6.5h16v3H4zM4 17h16v3H4z");
        P.put("settings",  "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8zm0 2a2 2 0 1 1 0 4 2 2 0 0 1 0-4zM10.5 2h3l.4 2.3 2.1.9 2-1.2 2.1 2.1-1.2 2 .9 2.1L22 10.5v3l-2.3.4-.9 2.1 1.2 2-2.1 2.1-2-1.2-2.1.9L13.5 22h-3l-.4-2.3-2.1-.9-2 1.2-2.1-2.1 1.2-2-.9-2.1L2 13.5v-3l2.3-.4.9-2.1-1.2-2 2.1-2.1 2 1.2 2.1-.9z");
        P.put("theme",     "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zm0 2v14a7 7 0 0 1 0-14z");
        P.put("play",      "M7 4l14 8-14 8z");
        P.put("pause",     "M6 4h4v16H6zM14 4h4v16h-4z");
        P.put("step",      "M5 4l10 8-10 8zM17 4h2v16h-2z");
        P.put("prev",      "M19 4l-10 8 10 8zM5 4h2v16H5z");
        P.put("reset",     "M12 4V2L8 6l4 4V8a6 6 0 1 1-6 6H4a8 8 0 1 0 8-10z");
        P.put("speed",     "M12 4a10 10 0 0 0-10 10h4a6 6 0 0 1 12 0h4A10 10 0 0 0 12 4zm1 4h-2v7l5 3 1-1.7-4-2.3z");
        P.put("stats",     "M4 20h4V10H4zm6 0h4V4h-4zm6 0h4v-8h-4z");
        P.put("code",      "M8 6 2 12l6 6 1.4-1.4L4.8 12l4.6-4.6zM16 6l-1.4 1.4L19.2 12l-4.6 4.6L16 18l6-6z");
        P.put("history",   "M13 3a9 9 0 0 0-9 9H1l4 4 4-4H6a7 7 0 1 1 7 7 6.9 6.9 0 0 1-4.9-2l-1.4 1.4A9 9 0 1 0 13 3zm-1 5v5l4 2 .8-1.4-3.3-1.8V8z");
        P.put("searchbox", "M10 2a8 8 0 1 0 4.9 14.3l4.9 4.9 1.4-1.4-4.9-4.9A8 8 0 0 0 10 2zm0 2a6 6 0 1 1 0 12 6 6 0 0 1 0-12z");
        P.put("plus",      "M11 5h2v6h6v2h-6v6h-2v-6H5v-2h6z");
        P.put("minus",     "M5 11h14v2H5z");
        P.put("close",     "M18.3 5.7 12 12l6.3 6.3-1.4 1.4L10.6 13.4 4.3 19.7 2.9 18.3 9.2 12 2.9 5.7 4.3 4.3l6.3 6.3 6.3-6.3z");
        P.put("chevron",   "M9 6l6 6-6 6z");
        P.put("compare",   "M4 18V6h6v12zm10 0V6h6v12z");
        P.put("metrics",   "M4 20V10h3v10zm6.5 0V4h3v16zM17 20v-7h3v7z");
        P.put("playground","M12 2 2 7l10 5 10-5zM2 12l10 5 10-5M2 17l10 5 10-5");
        P.put("dashboard", "M3 3h8v8H3zm10 0h8v5h-8zM3 13h8v8H3zm10 3h8v5h-8z");
        P.put("about",     "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm1 15h-2v-6h2zm0-8h-2V7h2z");
        P.put("maze",      "M3 3h18v18H3zm3 3v12h12V6zm3 3h6v6H9z");
        P.put("grid",      "M3 3h8v8H3zm10 0h8v8h-8zM3 13h8v8H3zm10 0h8v8h-8z");
        P.put("node",      "M12 2a4 4 0 1 0 0 8 4 4 0 0 0 0-8zm0 12a4 4 0 1 0 0 8 4 4 0 0 0 0-8zM6 12h12");
    }

    private Icons() {}

    public static Region get(String name, double size) {
        SVGPath p = new SVGPath();
        p.setContent(P.getOrDefault(name, P.get("home")));
        p.getStyleClass().add("icon-path");
        StackPane holder = new StackPane(p);
        holder.setPrefSize(size, size);
        holder.setMinSize(size, size);
        holder.setMaxSize(size, size);
        double s = size / 24.0;
        p.setScaleX(s);
        p.setScaleY(s);
        return holder;
    }

    public static SVGPath raw(String name) {
        SVGPath p = new SVGPath();
        p.setContent(P.getOrDefault(name, P.get("home")));
        return p;
    }
}