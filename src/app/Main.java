package app;

import app.theme.Theme;
import app.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override public void start(Stage stage) {
        Theme.init();
        new MainWindow(stage).show();
    }
    public static void main(String[] args) { launch(args); }
}
