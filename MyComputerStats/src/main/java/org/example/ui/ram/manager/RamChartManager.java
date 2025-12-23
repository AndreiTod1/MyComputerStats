package org.example.ui.ram.manager;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ToggleButton;
import org.example.core.memory.RamInfo;
import org.example.core.settings.AppSettings;

public class RamChartManager {

    private final LineChart<Number, Number> chart;
    private final NumberAxis xAxis;
    private final NumberAxis yAxis;
    private final ToggleButton btnUsage;

    private XYChart.Series<Number, Number> usageSeries = new XYChart.Series<>();

    private long startTime = 0;
    private int maxDataPoints = 60;
    private double totalRamGB = 0;

    public RamChartManager(LineChart<Number, Number> chart, NumberAxis xAxis, NumberAxis yAxis,
            ToggleButton btnUsage, long totalRamBytes) {
        this.chart = chart;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.btnUsage = btnUsage;
        this.totalRamGB = totalRamBytes / (1024.0 * 1024 * 1024);

        initialize();
    }

    private void initialize() {
        usageSeries.setName("Used (GB)");
        chart.getData().add(usageSeries);

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

        setupChart();
    }

    private void setupChart() {
        btnUsage.setSelected(true);

        yAxis.setLabel("Used (GB)");
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(Math.ceil(totalRamGB));
        yAxis.setTickUnit(Math.ceil(totalRamGB) / 4);
        yAxis.setStyle("-fx-tick-label-fill: #00f2ff;");

        xAxis.setLabel("Time (s)");
        xAxis.setStyle("-fx-tick-label-fill: #00f2ff;");
    }

    public void updateSettings(AppSettings settings) {
        chart.setVisible(settings.isShowCpuChart());
        chart.setManaged(settings.isShowCpuChart());
        this.maxDataPoints = settings.getChartHistorySeconds();
        xAxis.setUpperBound(maxDataPoints);
    }

    public void update(RamInfo info) {
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
            xAxis.setAutoRanging(false);
        }

        double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0;

        double usedGB = info.getUsedBytes() / (1024.0 * 1024 * 1024);
        usageSeries.getData().add(new XYChart.Data<>(elapsedSeconds, usedGB));

        xAxis.setLowerBound(Math.max(0, elapsedSeconds - maxDataPoints));
        xAxis.setUpperBound(Math.max(elapsedSeconds, maxDataPoints));

        trimOldData(elapsedSeconds - maxDataPoints);
        updateLineColor(info.getUsagePercent());
    }

    private void trimOldData(double threshold) {
        var data = usageSeries.getData();
        while (!data.isEmpty() && data.get(0).getXValue().doubleValue() < threshold) {
            data.remove(0);
        }
    }

    public void reset() {
        usageSeries.getData().clear();
        startTime = 0;
    }

    private void updateLineColor(double percent) {
        chart.getStyleClass().removeAll("chart-low", "chart-medium", "chart-high", "chart-critical");

        if (percent < 40)
            chart.getStyleClass().add("chart-low");
        else if (percent < 65)
            chart.getStyleClass().add("chart-medium");
        else if (percent < 85)
            chart.getStyleClass().add("chart-high");
        else
            chart.getStyleClass().add("chart-critical");
    }
}
