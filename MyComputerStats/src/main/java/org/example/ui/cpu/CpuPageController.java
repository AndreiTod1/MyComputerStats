package org.example.ui.cpu;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.core.cpu.CpuInfo;
import org.example.monitoring.cpu.CpuMonitoringService;
import org.example.core.settings.AppSettings;
import org.example.core.settings.SettingsManager;
import org.example.core.settings.SettingsChangeListener;

import org.example.ui.cpu.CpuCoreTable;

public class CpuPageController {

    @FXML
    private Label cpuBrandLabel;
    @FXML
    private Label cpuModelLabel;
    @FXML
    private Label coreInfoLabel;
    @FXML
    private Label freqLabel;
    @FXML
    private Label maxFreqLabel;

    @FXML
    private Label temperatureLabel;
    @FXML
    private Label avgTemperatureLabel;
    @FXML
    private Label tempSourceLabel;
    @FXML
    private Label loadLabel;
    @FXML
    private ProgressBar loadBar;
    @FXML
    private LineChart<Number, Number> cpuChart;
    @FXML
    private NumberAxis xAxis;
    @FXML
    private NumberAxis yAxis;
    @FXML
    private VBox perCoreContainer;

    @FXML
    private javafx.scene.control.Button resetStatsButton;

    // Stress Test Controls
    @FXML
    private VBox stressTestContainer;
    @FXML
    private javafx.scene.control.Button startStressButton;
    @FXML
    private javafx.scene.control.Button stopStressButton;
    @FXML
    private Label stressStatusLabel;

    private CpuMonitoringService cpuService;
    private org.example.monitoring.cpu.CpuStressTestManager stressManager;
    private Timeline timeline;
    private XYChart.Series<Number, Number> cpuSeries;
    private int maxDataPoints = 60;
    private CpuCoreTable cpuTable;
    private double previousOverallLoad = 0.0;

    @FXML
    private void handleResetStats() {
        if (cpuService != null) {
            cpuService.resetStats();
        }
    }

    @FXML
    private void handleStartStress() {
        if (stressManager != null && !stressManager.isRunning()) {
            stressManager.startStressTest();
            updateStressStatus(true);
        }
    }

    @FXML
    private void handleStopStress() {
        if (stressManager != null && stressManager.isRunning()) {
            stressManager.stopStressTest();
            updateStressStatus(false);
        }
    }

    private void updateStressStatus(boolean running) {
        if (running) {
            stressStatusLabel.setText("Running...");
            stressStatusLabel.setStyle("-fx-text-fill: #ff0000; -fx-font-weight: bold;");
            startStressButton.setDisable(true);
            stopStressButton.setDisable(false);
            stressTestContainer.setStyle(
                    "-fx-border-color: #ff0000; -fx-border-width: 1; -fx-background-color: rgba(255, 0, 0, 0.05);");
        } else {
            stressStatusLabel.setText("Inactive");
            stressStatusLabel.setStyle("-fx-text-fill: #00ff00;");
            startStressButton.setDisable(false);
            stopStressButton.setDisable(true);
            stressTestContainer.setStyle("");
        }
    }

    @FXML
    public void initialize() {
        cpuService = new CpuMonitoringService();
        stressManager = new org.example.monitoring.cpu.CpuStressTestManager();
        initializeChart();
        updateStressStatus(false);
        cpuService = new CpuMonitoringService();
        initializeChart();

        AppSettings settings = SettingsManager.getInstance().getSettings();
        applySettings(settings);

        CpuInfo info = cpuService.readCpuInfo();
        updateStaticInfo(info);
        initializePerCoreDisplay(info.getLogicalCores());
        updateDynamicInfo(info);

        startMonitoring();
        SettingsChangeListener.getInstance().addListener(this::onSettingsChanged);
    }

    private void onSettingsChanged(AppSettings settings) {
        applySettings(settings);
        restartMonitoring();
    }

    private void applySettings(AppSettings settings) {
        cpuChart.setVisible(settings.isShowCpuChart());
        cpuChart.setManaged(settings.isShowCpuChart());
        maxDataPoints = settings.getChartHistorySeconds();
        xAxis.setUpperBound(maxDataPoints);
    }

    private void startMonitoring() {
        AppSettings settings = SettingsManager.getInstance().getSettings();
        double refreshInterval = settings.getCpuRefreshInterval();

        timeline = new Timeline(
                new KeyFrame(Duration.seconds(refreshInterval), event -> {
                    CpuInfo updated = cpuService.readCpuInfo();
                    updateDynamicInfo(updated);
                }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void restartMonitoring() {
        if (timeline != null) {
            timeline.stop();
        }
        startMonitoring();
    }

    private void initializeChart() {
        cpuSeries = new XYChart.Series<>();
        cpuSeries.setName("CPU Usage");
        cpuChart.getData().add(cpuSeries);
        cpuSeries.getData().add(new XYChart.Data<>(0, 0));
    }

    private void initializePerCoreDisplay(int coreCount) {
        cpuTable = new CpuCoreTable();

        perCoreContainer.getChildren().clear();
        perCoreContainer.getChildren().add(cpuTable);
    }

    private void updateStaticInfo(CpuInfo info) {
        String brand = formatBrandName(info.getBrand());
        cpuBrandLabel.setText(brand);
        cpuModelLabel.setText(info.getModel());
        coreInfoLabel.setText(info.getCoreDescription());
        maxFreqLabel.setText(info.getFormattedMaxClockSpeed());
    }

    private String formatBrandName(String rawBrand) {
        if (rawBrand == null || rawBrand.isEmpty()) {
            return "Unknown";
        }

        switch (rawBrand) {
            case "GenuineIntel":
                return "Intel";
            case "AuthenticAMD":
                return "AMD";
            case "CentaurHauls":
                return "Centaur";
            case "CyrixInstead":
                return "Cyrix";
            case "Qualcomm Technologies, Inc.":
                return "Qualcomm";
            default:
                return rawBrand;
        }
    }

    private void updateDynamicInfo(CpuInfo info) {
        freqLabel.setText(info.getFormattedClockSpeed());
        temperatureLabel.setText(info.getFormattedTemperature());

        // update average temperature
        avgTemperatureLabel.setText(info.getFormattedAverageTemperature());

        // update temperature source
        tempSourceLabel.setText(info.getTemperatureSource());

        // color temperature labels based on value
        colorTemperatureLabel(temperatureLabel, info.getTemperature());
        colorTemperatureLabel(avgTemperatureLabel, info.getAverageTemperature());

        double rawLoad = info.getLoad();
        double smoothedOverallLoad = previousOverallLoad * 0.2 + rawLoad * 0.8;
        previousOverallLoad = smoothedOverallLoad;

        loadLabel.setText(String.format("%.1f%%", smoothedOverallLoad * 100));
        loadBar.setProgress(smoothedOverallLoad);

        CpuInfo.LoadLevel level = info.getLoadLevel();
        loadBar.getStyleClass().removeAll("progress-bar-low", "progress-bar-medium", "progress-bar-high");

        switch (level) {
            case LOW:
                loadBar.getStyleClass().add("progress-bar-low");
                break;
            case MODERATE:
                loadBar.getStyleClass().add("progress-bar-medium");
                break;
            case HIGH:
            case CRITICAL:
                loadBar.getStyleClass().add("progress-bar-high");
                break;
        }

        if (cpuTable != null) {
            cpuTable.update(info);
        }
        updateChart(smoothedOverallLoad * 100);
    }

    private void colorTemperatureLabel(Label label, double temp) {
        if (temp <= 0) {
            label.setStyle("-fx-text-fill: #888;");
        } else if (temp > 80) {
            label.setStyle("-fx-text-fill: #ff0000; -fx-font-weight: bold;");
        } else if (temp > 70) {
            label.setStyle("-fx-text-fill: #ff9900;");
        } else if (temp > 60) {
            label.setStyle("-fx-text-fill: #ffcc00;");
        } else {
            label.setStyle("-fx-text-fill: #00ff00;");
        }
    }

    // Old updatePerCoreLoads removed

    private long chartStartTime = 0;

    private void updateChart(double cpuUsage) {
        if (chartStartTime == 0) {
            chartStartTime = System.currentTimeMillis();
            xAxis.setAutoRanging(false); // Disable auto-ranging explicitly
        }

        double elapsedSeconds = (System.currentTimeMillis() - chartStartTime) / 1000.0;

        cpuSeries.getData().add(new XYChart.Data<>(elapsedSeconds, cpuUsage));

        double windowStart = elapsedSeconds - maxDataPoints;
        if (windowStart < 0)
            windowStart = 0;

        // Remove old points
        var data = cpuSeries.getData();
        while (!data.isEmpty() && data.get(0).getXValue().doubleValue() < windowStart) {
            data.remove(0);
        }

        // Update Axis
        xAxis.setLowerBound(windowStart);
        xAxis.setUpperBound(Math.max(elapsedSeconds, maxDataPoints)); // Keep full window visible or grow until full

        updateChartLineColor(cpuUsage);
    }

    private void updateChartLineColor(double cpuUsage) {
        cpuChart.getStyleClass().removeAll("chart-low", "chart-medium", "chart-high", "chart-critical");

        if (cpuUsage < 30) {
            cpuChart.getStyleClass().add("chart-low");
        } else if (cpuUsage < 60) {
            cpuChart.getStyleClass().add("chart-medium");
        } else if (cpuUsage < 85) {
            cpuChart.getStyleClass().add("chart-high");
        } else {
            cpuChart.getStyleClass().add("chart-critical");
        }
    }
}
