package org.example.core.settings;

public class AppSettings {

    // Refresh intervals (in seconds)
    private int cpuRefreshInterval = 1;
    private int chartHistorySeconds = 60;

    // Display preferences
    private boolean showCpuChart = true;
    private String temperatureUnit = "Celsius";

    // Theme settings
    private String theme = "Dark";

    // Window settings
    private boolean alwaysOnTop = false;

    // Getters and Setters
    public int getCpuRefreshInterval() {
        return cpuRefreshInterval;
    }

    public void setCpuRefreshInterval(int cpuRefreshInterval) {
        this.cpuRefreshInterval = cpuRefreshInterval;
    }

    public int getChartHistorySeconds() {
        return chartHistorySeconds;
    }

    public void setChartHistorySeconds(int chartHistorySeconds) {
        this.chartHistorySeconds = chartHistorySeconds;
    }

    public boolean isShowCpuChart() {
        return showCpuChart;
    }

    public void setShowCpuChart(boolean showCpuChart) {
        this.showCpuChart = showCpuChart;
    }

    public String getTemperatureUnit() {
        return temperatureUnit;
    }

    public void setTemperatureUnit(String temperatureUnit) {
        this.temperatureUnit = temperatureUnit;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
    }
}

