package org.example.ui.ram;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;
import org.example.core.memory.RamInfo;
import org.example.core.settings.AppSettings;
import org.example.core.settings.SettingsChangeListener;
import org.example.core.settings.SettingsManager;
import org.example.monitoring.memory.RamMonitoringService;
import org.example.ui.ram.manager.RamChartManager;
import org.example.ui.ram.model.RamSessionStatistics;

public class RamPageController {

    @FXML
    private Label totalMemoryLabel;
    @FXML
    private Label memoryTypeLabel;
    @FXML
    private Label memorySpeedLabel;

    @FXML
    private Label usedMemoryLabel;
    @FXML
    private Label availableMemoryLabel;
    @FXML
    private Label usagePercentLabel;
    @FXML
    private ProgressBar usageBar;

    @FXML
    private LineChart<Number, Number> ramChart;
    @FXML
    private NumberAxis xAxis;
    @FXML
    private NumberAxis yAxis;
    @FXML
    private javafx.scene.control.ToggleButton chartModeUsage;

    @FXML
    private Label avgUsageLabel;
    @FXML
    private Label sampleCountLabel;

    @FXML
    private javafx.scene.control.ToggleButton showDetailsToggle;
    @FXML
    private javafx.scene.layout.GridPane detailsGrid;

    @FXML
    private Label minUsageLabel, avgUsageDetailLabel, maxUsageLabel;
    @FXML
    private Label minMBLabel, avgMBDetailLabel, maxMBLabel;

    private RamChartManager chartManager;
    private RamMonitoringService ramService;
    private RamSessionStatistics sessionStats = new RamSessionStatistics();
    private Timeline timeline;

    @FXML
    public void initialize() {
        ramService = new RamMonitoringService();

        RamInfo info = ramService.readRamInfo();
        long totalRamBytes = info.getTotalBytes();

        chartManager = new RamChartManager(ramChart, xAxis, yAxis, chartModeUsage, totalRamBytes);

        AppSettings settings = SettingsManager.getInstance().getSettings();
        applySettings(settings);

        updateStaticInfo(info);
        updateDynamicInfo(info);

        startMonitoring();
        SettingsChangeListener.getInstance().addListener(this::onSettingsChanged);
    }

    public void startMonitoring() {
        if (timeline != null && timeline.getStatus() == Animation.Status.RUNNING) {
            return;
        }

        AppSettings settings = SettingsManager.getInstance().getSettings();
        double refreshInterval = settings.getCpuRefreshInterval();

        timeline = new Timeline(
                new KeyFrame(Duration.seconds(refreshInterval), event -> {
                    RamInfo updated = ramService.readRamInfo();
                    updateDynamicInfo(updated);
                }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    public void stopMonitoring() {
        if (timeline != null) {
            timeline.stop();
        }
    }

    @FXML
    private void handleToggleDetails() {
        boolean visible = showDetailsToggle.isSelected();
        detailsGrid.setVisible(visible);
        detailsGrid.setManaged(visible);
        showDetailsToggle.setText(visible ? "Hide Details ▲" : "Show Details ▼");
    }

    @FXML
    private void handleResetStats() {
        sessionStats.reset();
        if (chartManager != null) {
            chartManager.reset();
        }
        updateSessionStatsUI();
    }

    private void onSettingsChanged(AppSettings settings) {
        applySettings(settings);
        restartMonitoring();
    }

    private void applySettings(AppSettings settings) {
        if (chartManager != null) {
            chartManager.updateSettings(settings);
        }
    }

    private void restartMonitoring() {
        if (timeline != null) {
            timeline.stop();
        }
        startMonitoring();
    }

    private void updateStaticInfo(RamInfo info) {
        totalMemoryLabel.setText(info.getFormattedTotal());
        memoryTypeLabel.setText(info.getMemoryType());

        long speed = info.getMemorySpeed();
        memorySpeedLabel.setText(speed > 0 ? speed + " MHz" : "N/A");
    }

    private void updateDynamicInfo(RamInfo info) {
        usedMemoryLabel.setText(info.getFormattedUsed());
        availableMemoryLabel.setText(info.getFormattedAvailable());
        usagePercentLabel.setText(String.format("%.1f%%", info.getUsagePercent()));
        usageBar.setProgress(info.getUsagePercent() / 100.0);

        if (chartManager != null) {
            chartManager.update(info);
        }

        sessionStats.update(info);
        updateSessionStatsUI();
    }

    private void updateSessionStatsUI() {
        avgUsageLabel.setText(String.format("%.1f%%", sessionStats.getAvgUsagePercent()));
        sampleCountLabel.setText(String.valueOf(sessionStats.getSampleCount()));

        minUsageLabel.setText(String.format("%.1f%%", sessionStats.getMinUsagePercent()));
        avgUsageDetailLabel.setText(String.format("%.1f%%", sessionStats.getAvgUsagePercent()));
        maxUsageLabel.setText(String.format("%.1f%%", sessionStats.getMaxUsagePercent()));

        minMBLabel.setText(String.format("%.2f GB", sessionStats.getMinUsedMB() / 1024.0));
        avgMBDetailLabel.setText(String.format("%.2f GB", sessionStats.getAvgUsedMB() / 1024.0));
        maxMBLabel.setText(String.format("%.2f GB", sessionStats.getMaxUsedMB() / 1024.0));
    }
}
