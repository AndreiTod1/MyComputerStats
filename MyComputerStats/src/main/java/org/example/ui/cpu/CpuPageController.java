package org.example.ui.cpu;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.core.cpu.CpuInfo;
import org.example.monitoring.cpu.CpuMonitoringService;
import org.example.core.settings.AppSettings;
import org.example.core.settings.SettingsManager;
import org.example.core.settings.SettingsChangeListener;
import org.example.ui.cpu.manager.CpuChartManager;
import org.example.ui.cpu.model.SessionStatistics;

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

    // Chart components injected from FXML
    @FXML
    private LineChart<Number, Number> cpuChart;
    @FXML
    private NumberAxis xAxis;
    @FXML
    private NumberAxis yAxis;
    @FXML
    private javafx.scene.control.ToggleButton chartModeLoad;
    @FXML
    private javafx.scene.control.ToggleButton chartModeTemp;
    @FXML
    private javafx.scene.control.ToggleButton chartModeVoltage;
    @FXML
    private javafx.scene.control.ToggleButton chartModePower;

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

    // average stats labels
    @FXML
    private Label avgLoadLabel;
    @FXML
    private Label sessionAvgTempLabel;
    @FXML
    private Label sessionAvgPowerLabel;

    @FXML
    private Label sampleCountLabel;

    // throttling labels
    @FXML
    private Label thermalThrottleLabel;
    @FXML
    private Label powerLimitLabel;
    @FXML
    private Label packagePowerLabel;
    @FXML
    private Label maxObservedTempLabel;
    @FXML
    private Label throttleCountLabel;

    // system activity labels
    @FXML
    private Label contextSwitchesLabel;
    @FXML
    private Label interruptsLabel;
    @FXML
    private Label processCountLabel;
    @FXML
    private Label threadCountLabel;

    // top processes
    @FXML
    private VBox topProcessesContainer;
    @FXML
    private javafx.scene.control.ToggleButton topProcessesToggle;
    @FXML
    private javafx.scene.control.ComboBox<Integer> processCountCombo;
    private boolean topProcessesEnabled = false;
    private int topProcessCount = 5;

    // Delegates
    private CpuChartManager chartManager;
    private SessionStatistics sessionStats = new SessionStatistics();

    // Services
    private CpuMonitoringService cpuService;
    private org.example.monitoring.cpu.CpuStressTestManager stressManager;
    private Timeline timeline;
    private CpuCoreTable cpuTable;

    private double previousOverallLoad = 0.0;

    // FXML fields for detailed stats
    @FXML
    private javafx.scene.control.ToggleButton showDetailsToggle;
    @FXML
    private javafx.scene.layout.GridPane detailsGrid;

    @FXML
    private Label minLoadLabel, avgLoadDetailLabel, maxLoadLabel;
    @FXML
    private Label minTempLabel, avgTempDetailLabel, maxTempLabel;
    @FXML
    private Label minFreqLabel, avgFreqDetailLabel, sessionMaxFreqLabel;
    @FXML
    private Label minVoltLabel, avgVoltDetailLabel, maxVoltLabel;
    @FXML
    private Label minPowerLabel, avgPowerDetailLabel, maxPowerLabel;

    @FXML
    private void handleToggleDetails() {
        boolean visible = showDetailsToggle.isSelected();
        detailsGrid.setVisible(visible);
        detailsGrid.setManaged(visible);
        if (visible) {
            showDetailsToggle.setText("Hide Details ▲");
        } else {
            showDetailsToggle.setText("Show Details ▼");
        }
    }

    @FXML
    private void handleResetStats() {
        if (cpuService != null) {
            cpuService.resetStats();
        }

        sessionStats.reset();

        if (chartManager != null) {
            chartManager.reset();
        }

        // Update UI immediately
        updateSessionStatsUI();

        maxObservedTempLabel.setText("0°C");
        maxObservedTempLabel.setStyle("");
        throttleCountLabel.setText("0");

        thermalThrottleLabel.setText("No");
        thermalThrottleLabel.setStyle("-fx-text-fill: #00ff00;");
        powerLimitLabel.setText("Normal");
        powerLimitLabel.setStyle("-fx-text-fill: #00ff00;");
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

        chartManager = new CpuChartManager(cpuChart, xAxis, yAxis,
                chartModeLoad, chartModeTemp, chartModeVoltage, chartModePower);

        updateStressStatus(false);
        initializeTopProcesses();

        AppSettings settings = SettingsManager.getInstance().getSettings();
        applySettings(settings);

        CpuInfo info = cpuService.readCpuInfo();
        updateStaticInfo(info);
        initializePerCoreDisplay(info.getLogicalCores());
        updateDynamicInfo(info);

        startMonitoring();
        SettingsChangeListener.getInstance().addListener(this::onSettingsChanged);
    }

    private void initializeTopProcesses() {
        // setup combo box
        processCountCombo.getItems().addAll(5, 10, 15, 20);
        processCountCombo.setValue(5);
        processCountCombo.setOnAction(e -> topProcessCount = processCountCombo.getValue());

        // setup toggle button
        topProcessesToggle.setOnAction(e -> {
            topProcessesEnabled = topProcessesToggle.isSelected();
            topProcessesContainer.setVisible(topProcessesEnabled);
            topProcessesContainer.setManaged(topProcessesEnabled);

            if (topProcessesEnabled) {
                topProcessesToggle.setText("▼ ON");
                topProcessesToggle.setStyle(
                        "-fx-background-color: rgba(0,170,0,0.3); -fx-text-fill: #00ff00; -fx-font-size: 11px; -fx-cursor: hand;");
            } else {
                topProcessesToggle.setText("▶ OFF");
                topProcessesToggle.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: #888; -fx-font-size: 11px; -fx-cursor: hand;");
                topProcessesContainer.getChildren().clear();
            }
        });
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

        if (chartManager != null) {
            chartManager.update(info, smoothedOverallLoad * 100, cpuService.getPackagePower());
        }

        // update session averages
        updateSessionStats(info, smoothedOverallLoad);

        // update throttling status
        updateThrottlingStatus(info);

        // update system activity
        updateSystemActivity();

        // update top processes
        updateTopProcesses();
    }

    private void updateSystemActivity() {
        cpuService.updateSystemActivity();

        contextSwitchesLabel.setText(formatNumber(cpuService.getContextSwitchesPerSec()));
        interruptsLabel.setText(formatNumber(cpuService.getInterruptsPerSec()));
        processCountLabel.setText(String.valueOf(cpuService.getProcessCount()));
        threadCountLabel.setText(String.valueOf(cpuService.getThreadCount()));
    }

    private String formatNumber(double value) {
        if (value >= 1_000_000)
            return String.format("%.1fM", value / 1_000_000);
        if (value >= 1_000)
            return String.format("%.1fK", value / 1_000);
        return String.format("%.0f", value);
    }

    private void updateTopProcesses() {
        // only update if enabled
        if (!topProcessesEnabled)
            return;

        var processes = cpuService.getTopProcesses(topProcessCount);
        topProcessesContainer.getChildren().clear();

        for (var proc : processes) {
            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(12);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.getStyleClass().add("process-row");

            // process name
            Label nameLabel = new Label(proc.name);
            nameLabel.setMinWidth(200);
            nameLabel.setMaxWidth(200);
            nameLabel.getStyleClass().add("process-name");

            // progress bar
            ProgressBar bar = new ProgressBar(Math.min(proc.cpuPercent / 100.0, 1.0));
            bar.setPrefWidth(400);
            bar.setMinHeight(14);
            javafx.scene.layout.HBox.setHgrow(bar, javafx.scene.layout.Priority.ALWAYS);

            // color based on usage
            if (proc.cpuPercent > 50) {
                bar.setStyle("-fx-accent: #ff3333;");
            } else if (proc.cpuPercent > 20) {
                bar.setStyle("-fx-accent: #ff9900;");
            } else if (proc.cpuPercent > 5) {
                bar.setStyle("-fx-accent: #00ccff;");
            } else {
                bar.setStyle("-fx-accent: #00aa00;");
            }

            // percentage label
            Label percentLabel = new Label(String.format("%.1f%%", proc.cpuPercent));
            percentLabel.setMinWidth(65);
            percentLabel.getStyleClass().add("process-percent");

            row.getChildren().addAll(nameLabel, bar, percentLabel);
            topProcessesContainer.getChildren().add(row);
        }
    }

    private void updateSessionStats(CpuInfo info, double load) {
        sessionStats.update(info, load, cpuService.getPackagePower());
        updateSessionStatsUI();
    }

    private void updateSessionStatsUI() {
        // --- UPDATE UI SUMMARY ---
        avgLoadLabel.setText(String.format("%.1f%%", sessionStats.getAvgLoad()));
        sessionAvgTempLabel.setText(String.format("%.0f°C", sessionStats.getAvgTemp()));
        sessionAvgPowerLabel.setText(String.format("%.1f W", sessionStats.getAvgPower()));
        sampleCountLabel.setText(String.valueOf(sessionStats.getSampleCount()));

        // --- UPDATE DETAILED GRID UI ---
        // Load
        minLoadLabel.setText(String.format("%.1f%%", sessionStats.getMinLoad()));
        avgLoadDetailLabel.setText(String.format("%.1f%%", sessionStats.getAvgLoad()));
        maxLoadLabel.setText(String.format("%.1f%%", sessionStats.getMaxLoad()));

        // Temp
        minTempLabel.setText(String.format("%.0f°C", sessionStats.getMinTemp()));
        avgTempDetailLabel.setText(String.format("%.0f°C", sessionStats.getAvgTemp()));
        maxTempLabel.setText(String.format("%.0f°C", sessionStats.getMaxTemp()));

        // Freq
        minFreqLabel.setText(String.format("%.2f GHz", sessionStats.getMinFreq()));
        avgFreqDetailLabel.setText(String.format("%.2f GHz", sessionStats.getAvgFreq()));
        sessionMaxFreqLabel.setText(String.format("%.2f GHz", sessionStats.getMaxFreq()));

        // Volt
        minVoltLabel.setText(String.format("%.3f V", sessionStats.getMinVolt()));
        avgVoltDetailLabel.setText(String.format("%.3f V", sessionStats.getAvgVolt()));
        maxVoltLabel.setText(String.format("%.3f V", sessionStats.getMaxVolt()));

        // Power
        minPowerLabel.setText(String.format("%.1f W", sessionStats.getMinPower()));
        avgPowerDetailLabel.setText(String.format("%.1f W", sessionStats.getAvgPower()));
        maxPowerLabel.setText(String.format("%.1f W", sessionStats.getMaxPower()));
    }

    private void updateThrottlingStatus(CpuInfo info) {
        // update throttling logic in session stats
        sessionStats.updateThrottling(cpuService.isThermalThrottle());

        double temp = info.getTemperature();

        // track max temp (display tracking)
        maxObservedTempLabel.setText(String.format("%.0f°C", sessionStats.getMaxObservedTemp()));
        colorTemperatureLabel(maxObservedTempLabel, sessionStats.getMaxObservedTemp());

        // get throttle status from native bridge (reads actual MSR bits)
        boolean isThrottling = cpuService.isThermalThrottle();
        boolean isPowerLimited = cpuService.isPowerThrottle();
        double packagePower = cpuService.getPackagePower();

        // update throttle count label
        throttleCountLabel.setText(String.valueOf(sessionStats.getThrottleEventCount()));

        // update thermal throttle display
        if (isThrottling) {
            thermalThrottleLabel.setText("YES");
            thermalThrottleLabel.setStyle("-fx-text-fill: #ff0000; -fx-font-weight: bold;");
        } else {
            thermalThrottleLabel.setText("No");
            thermalThrottleLabel.setStyle("-fx-text-fill: #00ff00;");
        }

        // update power limit display
        if (isPowerLimited) {
            powerLimitLabel.setText("Limited");
            powerLimitLabel.setStyle("-fx-text-fill: #ff9900;");
        } else {
            powerLimitLabel.setText("Normal");
            powerLimitLabel.setStyle("-fx-text-fill: #00ff00;");
        }

        // update package power display
        packagePowerLabel.setText(String.format("%.1fW", packagePower));
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
}
