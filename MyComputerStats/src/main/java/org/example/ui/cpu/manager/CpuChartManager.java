package org.example.ui.cpu.manager;

import javafx.application.Platform;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ToggleButton;
import org.example.core.cpu.CpuInfo;
import org.example.core.settings.AppSettings;

public class CpuChartManager {

    private final LineChart<Number, Number> chart;
    private final NumberAxis xAxis;
    private final NumberAxis yAxis;
    private final ToggleButton btnLoad;
    private final ToggleButton btnTemp;
    private final ToggleButton btnVolt;
    private final ToggleButton btnPower;

    public enum ChartMode {
        LOAD, TEMP, VOLTAGE, POWER
    }

    private ChartMode currentMode = ChartMode.LOAD;
    private XYChart.Series<Number, Number> loadSeries = new XYChart.Series<>();
    private XYChart.Series<Number, Number> tempSeries = new XYChart.Series<>();
    private XYChart.Series<Number, Number> voltageSeries = new XYChart.Series<>();
    private XYChart.Series<Number, Number> powerSeries = new XYChart.Series<>();

    // currently displayed series
    private XYChart.Series<Number, Number> activeSeries;

    private long startTime = 0;
    private int maxDataPoints = 60;

    public CpuChartManager(LineChart<Number, Number> chart, NumberAxis xAxis, NumberAxis yAxis,
            ToggleButton btnLoad, ToggleButton btnTemp, ToggleButton btnVolt, ToggleButton btnPower) {
        this.chart = chart;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.btnLoad = btnLoad;
        this.btnTemp = btnTemp;
        this.btnVolt = btnVolt;
        this.btnPower = btnPower;

        initialize();
    }

    private void initialize() {
        loadSeries.setName("Load");
        tempSeries.setName("Temp");
        voltageSeries.setName("Voltage");
        powerSeries.setName("Power");

        // default to load
        activeSeries = loadSeries;
        chart.getData().add(loadSeries);

        // x-axis formatter
        xAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number n) {
                return String.valueOf(n.intValue());
            }

            @Override
            public Number fromString(String s) {
                return Integer.parseInt(s);
            }
        });

        setupToggleButtons();
        switchMode(ChartMode.LOAD);
    }

    private void setupToggleButtons() {
        btnLoad.setOnAction(e -> switchMode(ChartMode.LOAD));
        btnTemp.setOnAction(e -> switchMode(ChartMode.TEMP));
        btnVolt.setOnAction(e -> switchMode(ChartMode.VOLTAGE));
        btnPower.setOnAction(e -> switchMode(ChartMode.POWER));
    }

    public void updateSettings(AppSettings settings) {
        chart.setVisible(settings.isShowCpuChart());
        chart.setManaged(settings.isShowCpuChart());
        this.maxDataPoints = settings.getChartHistorySeconds();
        xAxis.setUpperBound(maxDataPoints);
    }

    public void update(CpuInfo info, double smoothedLoad) {
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
            xAxis.setAutoRanging(false);
        }

        double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0;

        // add data to ALL series so history is preserved when switching
        loadSeries.getData().add(new XYChart.Data<>(elapsedSeconds, smoothedLoad));
        tempSeries.getData().add(new XYChart.Data<>(elapsedSeconds, info.getTemperature()));

        // voltage average
        double[] volts = info.getPerCoreVoltages();
        double avgVolt = 0;
        if (volts != null && volts.length > 0) {
            for (double v : volts)
                avgVolt += v;
            avgVolt /= volts.length;
        }
        voltageSeries.getData().add(new XYChart.Data<>(elapsedSeconds, avgVolt));

        // power (package)
        // Note: We need package power passed in or read from info if available.
        // Assuming CpuInfo or Service provides it. Controller passes it usually.
        // For now, we will assume 0 if not accessible, or update caller to pass it.
        // Wait, CpuInfo doesn't have package power directly?
        // Controller used cpuService.getPackagePower().
        // I will add a method to update power separately or pass it in update.
    }

    public void updatePower(double power, double elapsedSeconds) {
        powerSeries.getData().add(new XYChart.Data<>(elapsedSeconds, power));
    }

    // Overloaded update to allow passing power directly
    public void update(CpuInfo info, double smoothedLoad, double packagePower) {
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
            xAxis.setAutoRanging(false);
        }

        double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0;

        loadSeries.getData().add(new XYChart.Data<>(elapsedSeconds, smoothedLoad));
        tempSeries.getData().add(new XYChart.Data<>(elapsedSeconds, info.getTemperature()));

        double[] volts = info.getPerCoreVoltages();
        double avgVolt = 0;
        if (volts != null && volts.length > 0) {
            for (double v : volts)
                avgVolt += v;
            avgVolt /= volts.length;
        }
        voltageSeries.getData().add(new XYChart.Data<>(elapsedSeconds, avgVolt));

        powerSeries.getData().add(new XYChart.Data<>(elapsedSeconds, packagePower));

        // update axis range
        xAxis.setLowerBound(Math.max(0, elapsedSeconds - maxDataPoints));
        xAxis.setUpperBound(Math.max(elapsedSeconds, maxDataPoints));

        trimOldData(elapsedSeconds - maxDataPoints);

        // update line color based on active series last value
        if (!activeSeries.getData().isEmpty()) {
            double val = activeSeries.getData().get(activeSeries.getData().size() - 1).getYValue().doubleValue();
            updateLineColor(val);
        }
    }

    private void trimOldData(double threshold) {
        trimSeries(loadSeries, threshold);
        trimSeries(tempSeries, threshold);
        trimSeries(voltageSeries, threshold);
        trimSeries(powerSeries, threshold);
    }

    private void trimSeries(XYChart.Series<Number, Number> series, double threshold) {
        var data = series.getData();
        while (!data.isEmpty() && data.get(0).getXValue().doubleValue() < threshold) {
            data.remove(0);
        }
    }

    public void reset() {
        loadSeries.getData().clear();
        tempSeries.getData().clear();
        voltageSeries.getData().clear();
        powerSeries.getData().clear();
        // keep start time or reset? Better to keep relative time or reset everything.
        // If we reset stats, we usually want fresh chart.
        // But removing data is enough.
    }

    private void switchMode(ChartMode mode) {
        this.currentMode = mode;
        updateButtonStyles();

        chart.getData().clear();
        chart.getStyleClass().removeAll("chart-low", "chart-medium", "chart-high", "chart-critical");

        String axisColorStyle = "-fx-tick-label-fill: -color-text-secondary;";

        switch (mode) {
            case LOAD:
                activeSeries = loadSeries;
                yAxis.setLabel("Load (%)");
                yAxis.setUpperBound(100);
                yAxis.setLowerBound(0);
                yAxis.setTickUnit(20);
                axisColorStyle = "-fx-tick-label-fill: #00f2ff;";
                break;
            case TEMP:
                activeSeries = tempSeries;
                yAxis.setLabel("Temp (°C)");
                yAxis.setUpperBound(100);
                yAxis.setLowerBound(0);
                yAxis.setTickUnit(20);
                axisColorStyle = "-fx-tick-label-fill: #ff5555;";
                break;
            case VOLTAGE:
                activeSeries = voltageSeries;
                yAxis.setLabel("Voltage (V)");
                yAxis.setUpperBound(1.6);
                yAxis.setLowerBound(0.6);
                yAxis.setTickUnit(0.2);
                axisColorStyle = "-fx-tick-label-fill: #ffb800;";
                break;
            case POWER:
                activeSeries = powerSeries;
                yAxis.setLabel("Power (W)");
                yAxis.setUpperBound(200);
                yAxis.setLowerBound(0);
                yAxis.setTickUnit(40);
                axisColorStyle = "-fx-tick-label-fill: #00ff9d;";
                break;
        }

        chart.getData().add(activeSeries);
        yAxis.setStyle(axisColorStyle);

        // force color update
        if (!activeSeries.getData().isEmpty()) {
            double val = activeSeries.getData().get(activeSeries.getData().size() - 1).getYValue().doubleValue();
            updateLineColor(val);
        }
    }

    private void updateButtonStyles() {
        String base = "-fx-background-radius: 15; -fx-padding: 5 15; -fx-font-size: 11px; -fx-cursor: hand;";
        String inactive = "-fx-background-color: #333; -fx-text-fill: #888; " + base;

        btnLoad.setStyle(currentMode == ChartMode.LOAD ? "-fx-background-color: #00f2ff; -fx-text-fill: black; " + base
                : inactive);
        btnTemp.setStyle(currentMode == ChartMode.TEMP ? "-fx-background-color: #ff5555; -fx-text-fill: white; " + base
                : inactive);
        btnVolt.setStyle(
                currentMode == ChartMode.VOLTAGE ? "-fx-background-color: #ffb800; -fx-text-fill: black; " + base
                        : inactive);
        btnPower.setStyle(
                currentMode == ChartMode.POWER ? "-fx-background-color: #00ff9d; -fx-text-fill: black; " + base
                        : inactive);
    }

    private void updateLineColor(double value) {
        // Only dynamic for LOAD usually, but maybe for others too?
        // Original code only had dynamic color logic.
        // Whatever, let's keep the logic.

        chart.getStyleClass().removeAll("chart-low", "chart-medium", "chart-high", "chart-critical");

        // For other modes, maybe we want fixed colors?
        // In the original code (lines 374+), the AXIS color changed, but the LINE color
        // was set by CSS classes .chart-low etc.
        // but those classes probably assume Load coloring (Green->Yellow->Red).
        // For Temp: Green->Red is fine.
        // For Voltage: maybe not appropriate?
        // For Power: maybe not appropriate?

        // However, the original code had:
        // switch(mode) { case LOAD: ... // Load line color is dynamic, handled by
        // updateChartLineColor }
        // case TEMP: ...

        // Let's defer to CSS if possible, but the original code enforced it via
        // 'updateChartLineColor' calling style classes.
        // I will keep it for now.

        if (value < 30)
            chart.getStyleClass().add("chart-low");
        else if (value < 60)
            chart.getStyleClass().add("chart-medium");
        else if (value < 85)
            chart.getStyleClass().add("chart-high");
        else
            chart.getStyleClass().add("chart-critical");
    }
}
