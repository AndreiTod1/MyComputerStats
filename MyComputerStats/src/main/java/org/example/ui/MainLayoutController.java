package org.example.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.ui.cpu.CpuPageController;
import org.example.ui.ram.RamPageController;
import oshi.SystemInfo;
import oshi.software.os.OperatingSystem;

import java.io.IOException;

public class MainLayoutController {

    @FXML
    private StackPane contentArea;
    @FXML
    private Button cpuButton;
    @FXML
    private Button gpuButton;
    @FXML
    private Button ramButton;
    @FXML
    private Button networkButton;
    @FXML
    private Button diskButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Label systemInfoLabel;
    @FXML
    private Label uptimeLabel;

    private Button currentActiveButton;
    private SystemInfo systemInfo;
    private Timeline uptimeTimeline;
    private Object currentController;

    @FXML
    public void initialize() {
        systemInfo = new SystemInfo();
        currentActiveButton = cpuButton;
        updateSystemInfo();
        startUptimeUpdater();
        showCpuPage();
    }

    private void updateSystemInfo() {
        OperatingSystem os = systemInfo.getOperatingSystem();
        String osInfo = os.getFamily() + " " + os.getVersionInfo().getVersion();
        systemInfoLabel.setText(osInfo);
    }

    private void startUptimeUpdater() {
        uptimeTimeline = new Timeline(
                new KeyFrame(Duration.seconds(60), event -> updateUptime()));
        uptimeTimeline.setCycleCount(Timeline.INDEFINITE);
        uptimeTimeline.play();
        updateUptime();
    }

    private void updateUptime() {
        long uptimeSeconds = systemInfo.getOperatingSystem().getSystemUptime();
        long hours = uptimeSeconds / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        uptimeLabel.setText(String.format("Uptime: %dh %dm", hours, minutes));
    }

    @FXML
    private void showCpuPage() {
        loadPage("/ui/fxml/cpu_page.fxml", cpuButton);
    }

    @FXML
    private void showGpuPage() {
        showPlaceholder("GPU Monitor - Coming Soon", "🎮", gpuButton);
    }

    @FXML
    private void showRamPage() {
        loadPage("/ui/fxml/ram_page.fxml", ramButton);
    }

    @FXML
    private void showNetworkPage() {
        showPlaceholder("Network Monitor - Coming Soon", "🌐", networkButton);
    }

    @FXML
    private void showDiskPage() {
        loadPage("/ui/fxml/disk_page.fxml", diskButton);
    }

    @FXML
    private void showSettingsPage() {
        loadPage("/ui/fxml/settings_page.fxml", settingsButton);
    }

    private void showPlaceholder(String title, String icon, Button clickedButton) {
        stopCurrentPageMonitoring();

        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("menu-button-active");
        }
        clickedButton.getStyleClass().add("menu-button-active");
        currentActiveButton = clickedButton;

        VBox placeholder = new VBox(20);
        placeholder.setAlignment(javafx.geometry.Pos.CENTER);
        placeholder.setStyle("-fx-background-color: transparent;");

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 80px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-font-size: 28px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #00d4ff;");

        Label messageLabel = new Label("This feature will be implemented soon..");
        messageLabel.setStyle(
                "-fx-font-size: 16px; " +
                        "-fx-text-fill: #888;");

        placeholder.getChildren().addAll(iconLabel, titleLabel, messageLabel);

        contentArea.getChildren().clear();
        contentArea.getChildren().add(placeholder);
        currentController = null;
    }

    private void loadPage(String fxmlPath, Button clickedButton) {
        try {
            stopCurrentPageMonitoring();

            if (currentActiveButton != null) {
                currentActiveButton.getStyleClass().remove("menu-button-active");
            }
            clickedButton.getStyleClass().add("menu-button-active");
            currentActiveButton = clickedButton;

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent page = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);

            currentController = loader.getController();
            startCurrentPageMonitoring();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load page: " + fxmlPath);

            Label errorLabel = new Label("Page not available yet: " + fxmlPath);
            errorLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #ff6b6b;");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(errorLabel);
            currentController = null;
        }
    }

    private void stopCurrentPageMonitoring() {
        if (currentController instanceof CpuPageController) {
            ((CpuPageController) currentController).stopMonitoring();
        } else if (currentController instanceof RamPageController) {
            ((RamPageController) currentController).stopMonitoring();
        }
    }

    private void startCurrentPageMonitoring() {
        if (currentController instanceof CpuPageController) {
            ((CpuPageController) currentController).startMonitoring();
        } else if (currentController instanceof RamPageController) {
            ((RamPageController) currentController).startMonitoring();
        }
    }

    public void shutdown() {
        if (uptimeTimeline != null) {
            uptimeTimeline.stop();
        }
        stopCurrentPageMonitoring();
    }
}
