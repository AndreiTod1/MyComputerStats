package org.example.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.example.ui.MainLayoutController;
import org.example.core.settings.AppSettings;
import org.example.core.settings.SettingsManager;
import org.example.core.settings.SettingsChangeListener;

public class MainApp extends Application {

    private MainLayoutController mainController;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // kill any leftover bridge processes on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                new ProcessBuilder("taskkill", "/f", "/im", "MonitorBridge.exe")
                        .redirectErrorStream(true)
                        .start()
                        .waitFor();
            } catch (Exception e) {
                // ignore
            }
        }));

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/fxml/main_layout.fxml"));
        Parent root = loader.load();
        mainController = loader.getController();

        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/ui/css/main.css").toExternalForm());

        AppSettings settings = SettingsManager.getInstance().getSettings();
        primaryStage.setAlwaysOnTop(settings.isAlwaysOnTop());

        primaryStage.setTitle("MyComputerStats - System Monitor");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setOnCloseRequest(event -> {
            if (mainController != null) {
                mainController.shutdown();
            }
            Platform.exit();
        });

        SettingsChangeListener.getInstance().addListener(newSettings -> {
            primaryStage.setAlwaysOnTop(newSettings.isAlwaysOnTop());
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}