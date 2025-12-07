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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.core.cpu.CpuInfo;
import org.example.monitoring.cpu.CpuMonitoringService;
import org.example.core.settings.AppSettings;
import org.example.core.settings.SettingsManager;
import org.example.core.settings.SettingsChangeListener;

import java.util.ArrayList;
import java.util.List;

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
    private Label turboFreqLabel;
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

    private CpuMonitoringService cpuService;
    private Timeline timeline;
    private XYChart.Series<Number, Number> cpuSeries;
    private int timeCounter = 0;
    private int maxDataPoints = 60;
    private List<ProgressBar> coreProgressBars;
    private List<Label> coreLoadLabels;
    private List<Label> coreTemperatureLabels;
    private List<Label> coreFrequencyLabels;
    private List<VBox> coreBars;
    private double[] previousCoreLoads;
    private List<Transition> activeTransitions;
    private double previousOverallLoad = 0.0;

    @FXML
    public void initialize() {
        cpuService = new CpuMonitoringService();
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
        coreProgressBars = new ArrayList<>();
        coreLoadLabels = new ArrayList<>();
        coreTemperatureLabels = new ArrayList<>();
        coreFrequencyLabels = new ArrayList<>();
        coreBars = new ArrayList<>();
        previousCoreLoads = new double[coreCount];
        activeTransitions = new ArrayList<>();

        HBox coresContainer = new HBox(10);
        coresContainer.setAlignment(Pos.BOTTOM_LEFT);
        coresContainer.setPadding(new Insets(10, 0, 10, 0));

        for (int i = 0; i < coreCount; i++) {
            VBox coreColumn = new VBox(5);
            coreColumn.setAlignment(Pos.BOTTOM_CENTER);
            coreColumn.setPrefWidth(60);

            Label loadLabel = new Label("0%");
            loadLabel.getStyleClass().add("core-load-label");
            loadLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

            Label tempLabel = new Label("--°C");
            tempLabel.getStyleClass().add("core-temp-label");
            tempLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #888;");

            Label freqLabel = new Label("-- GHz");
            freqLabel.getStyleClass().add("core-freq-label");
            freqLabel.setStyle("-fx-font-size: 8px; -fx-text-fill: #aaa;");

            VBox barContainer = new VBox();
            barContainer.setPrefWidth(40);
            barContainer.setPrefHeight(150);
            barContainer.setStyle("-fx-background-color: #2b2b2b; -fx-background-radius: 3;");
            barContainer.setAlignment(Pos.BOTTOM_CENTER);

            VBox bar = new VBox();
            bar.setPrefWidth(40);
            bar.setMaxHeight(0);
            bar.setPrefHeight(0);
            bar.setStyle("-fx-background-color: #00ff00; -fx-background-radius: 3;");

            barContainer.getChildren().add(bar);

            Label coreLabel = new Label("C" + i);
            coreLabel.getStyleClass().add("info-label");
            coreLabel.setStyle("-fx-font-size: 10px;");

            coreColumn.getChildren().addAll(loadLabel, tempLabel, freqLabel, barContainer, coreLabel);

            coresContainer.getChildren().add(coreColumn);

            coreProgressBars.add(null);
            coreLoadLabels.add(loadLabel);
            coreTemperatureLabels.add(tempLabel);
            coreFrequencyLabels.add(freqLabel);
            coreBars.add(bar);
            previousCoreLoads[i] = 0.0;
            activeTransitions.add(null);
        }

        perCoreContainer.getChildren().add(coresContainer);
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

        // update turbo frequency (max current frequency from all cores)
        double[] perCoreFreqs = info.getPerCoreFrequencies();
        if (perCoreFreqs != null && perCoreFreqs.length > 0) {
            double maxCurrentFreq = 0;
            for (double freq : perCoreFreqs) {
                if (freq > maxCurrentFreq) {
                    maxCurrentFreq = freq;
                }
            }
            turboFreqLabel.setText(maxCurrentFreq > 0 ? String.format("%.2f GHz", maxCurrentFreq) : "N/A");
        } else {
            turboFreqLabel.setText("N/A");
        }

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

        updatePerCoreLoads(info.getPerCoreLoads(), info.getPerCoreTemperatures(), info.getPerCoreFrequencies());
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

    private void updatePerCoreLoads(double[] loads, double[] temperatures, double[] frequencies) {
        for (int i = 0; i < loads.length && i < coreBars.size(); i++) {
            double rawLoad = loads[i];

            double smoothedLoad = previousCoreLoads[i] * 0.2 + rawLoad * 0.8;
            previousCoreLoads[i] = smoothedLoad;

            VBox bar = coreBars.get(i);
            Label loadLabel = coreLoadLabels.get(i);
            Label tempLabel = coreTemperatureLabels.get(i);
            Label freqLabel = coreFrequencyLabels.get(i);

            loadLabel.setText(String.format("%.0f%%", smoothedLoad * 100));

            // update temperature
            if (i < temperatures.length && temperatures[i] > 0) {
                double temp = temperatures[i];
                tempLabel.setText(String.format("%.0f°C", temp));
                tempLabel.setVisible(true);
                tempLabel.setManaged(true);

                if (temp > 80) {
                    tempLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #ff0000; -fx-font-weight: bold;");
                } else if (temp > 70) {
                    tempLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #ff9900;");
                } else if (temp > 60) {
                    tempLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #ffcc00;");
                } else {
                    tempLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #00ff00;");
                }
            } else {
                tempLabel.setText("--°C");
                tempLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #888;");
            }

            // update frequency
            if (frequencies != null && i < frequencies.length && frequencies[i] > 0) {
                freqLabel.setText(String.format("%.1fG", frequencies[i]));
                freqLabel.setVisible(true);
                freqLabel.setManaged(true);

                // color based on frequency (turbo if above base)
                if (frequencies[i] > 3.5) {
                    freqLabel.setStyle("-fx-font-size: 8px; -fx-text-fill: #00ffff;");
                } else {
                    freqLabel.setStyle("-fx-font-size: 8px; -fx-text-fill: #aaa;");
                }
            } else {
                freqLabel.setText("--G");
                freqLabel.setStyle("-fx-font-size: 8px; -fx-text-fill: #888;");
            }

            double currentHeight = bar.getPrefHeight();
            double targetHeight = 150 * smoothedLoad;

            if (activeTransitions.get(i) != null) {
                activeTransitions.get(i).stop();
            }

            final int index = i;
            Transition transition = new Transition() {
                {
                    setCycleDuration(Duration.millis(150));
                }

                @Override
                protected void interpolate(double frac) {
                    double newHeight = currentHeight + (targetHeight - currentHeight) * frac;
                    bar.setMaxHeight(newHeight);
                    bar.setPrefHeight(newHeight);
                }
            };

            transition.setOnFinished(e -> activeTransitions.set(index, null));
            activeTransitions.set(i, transition);

            String color;
            if (smoothedLoad < 0.3) {
                color = "#00ff00";
            } else if (smoothedLoad < 0.6) {
                color = "#ffff00";
            } else if (smoothedLoad < 0.85) {
                color = "#ff9900";
            } else {
                color = "#ff0000";
            }

            bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3;");
            transition.play();
        }
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
