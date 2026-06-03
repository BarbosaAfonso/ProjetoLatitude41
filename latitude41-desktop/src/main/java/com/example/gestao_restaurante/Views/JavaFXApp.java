package com.example.gestao_restaurante.Views;

import javafx.application.Application;
import javafx.stage.Stage;

public class JavaFXApp extends Application {

    private static Runnable onStopCallback;

    public static void setOnStopCallback(Runnable callback) {
        onStopCallback = callback;
    }

    @Override
    public void start(Stage stage) {
        DesktopAppContext.init(stage);
        DesktopAppContext.showLogin();
    }

    @Override
    public void stop() {
        if (onStopCallback != null) {
            onStopCallback.run();
        }
    }

    public static void main(String[] args) {
        JavaFXLauncher.main(args);
    }
}
