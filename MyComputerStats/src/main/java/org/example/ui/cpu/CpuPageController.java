package org.example.ui.cpu;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;
import org.example.core.cpu.CpuInfo;
import org.example.monitoring.cpu.CpuMonitoringService;
import org.example.core.settings.AppSettings;
import org.example.core.settings.SettingsManager;
import org.example.core.settings.SettingsChangeListener;

public class CpuPageController {

    @FXML private Label cpuBrandLabel;
    @FXML private Label cpuModelLabel;
    @FXML private Label coreInfoLabel;
    @FXML private Label freqLabel;
    @FXML private Label maxFreqLabel;
    @FXML private Label loadLabel;
    @FXML private ProgressBar loadBar;
    @FXML private LineChart<Number, Number> cpuChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;

    private CpuMonitoringService cpuService;
    private Timeline timeline;
    private XYChart.Series<Number, Number> cpuSeries;
    private int timeCounter = 0;
    private int maxDataPoints = 60;

    @FXML
    public void initialize() {
        cpuService = new CpuMonitoringService();
        initializeChart();

        AppSettings settings = SettingsManager.getInstance().getSettings();
        applySettings(settings);

        CpuInfo info = cpuService.readCpuInfo();
        updateStaticInfo(info);
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
        int refreshInterval = settings.getCpuRefreshInterval();

        timeline = new Timeline(
                new KeyFrame(Duration.seconds(refreshInterval), event -> {
                    CpuInfo updated = cpuService.readCpuInfo();
                    updateDynamicInfo(updated);
                })
        );
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
            case "GenuineIntel": return "Intel";
            case "AuthenticAMD": return "AMD";
            case "CentaurHauls": return "Centaur";
            case "CyrixInstead": return "Cyrix";
            case "Qualcomm Technologies, Inc.": return "Qualcomm";
            default: return rawBrand;
        }
    }

    private void updateDynamicInfo(CpuInfo info) {
        freqLabel.setText(info.getFormattedClockSpeed());
        loadLabel.setText(info.getFormattedLoad());
        loadBar.setProgress(info.getLoad());

        CpuInfo.LoadLevel level = info.getLoadLevel();
        loadBar.getStyleClass().removeAll("progress-bar-low", "progress-bar-medium", "progress-bar-high");

        switch (level) {
            case LOW: loadBar.getStyleClass().add("progress-bar-low"); break;
            case MODERATE: loadBar.getStyleClass().add("progress-bar-medium"); break;
            case HIGH:
            case CRITICAL: loadBar.getStyleClass().add("progress-bar-high"); break;
        }

        updateChart(info.getLoadPercentage());
    }

    private void updateChart(double cpuUsage) {
        timeCounter++;
        cpuSeries.getData().add(new XYChart.Data<>(timeCounter, cpuUsage));

        if (cpuSeries.getData().size() > maxDataPoints) {
            cpuSeries.getData().remove(0);
        }

        if (timeCounter > maxDataPoints) {
            xAxis.setLowerBound(timeCounter - maxDataPoints);
            xAxis.setUpperBound(timeCounter);
        }

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
