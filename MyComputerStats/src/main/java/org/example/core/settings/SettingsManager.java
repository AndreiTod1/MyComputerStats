package org.example.core.settings;

import java.io.*;
import java.util.Properties;

public class SettingsManager {

    private static final String SETTINGS_FILE = "mycomputerstats.properties";
    private static SettingsManager instance;
    private AppSettings settings;
    private Properties properties;

    private SettingsManager() {
        properties = new Properties();
        loadSettings();
    }

    public static SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }

    public AppSettings getSettings() {
        return settings;
    }

    private void loadSettings() {
        settings = new AppSettings();
        File settingsFile = new File(SETTINGS_FILE);

        if (settingsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(settingsFile)) {
                properties.load(fis);
                applyPropertiesToSettings();
            } catch (IOException e) {
                System.err.println("Failed to load settings: " + e.getMessage());
            }
        }
    }

    private void applyPropertiesToSettings() {
        settings.setCpuRefreshInterval(getInt("cpu.refresh.interval", 1));
        settings.setChartHistorySeconds(getInt("chart.history.seconds", 60));
        settings.setShowCpuChart(getBoolean("display.show.cpu.chart", true));
        settings.setTemperatureUnit(getString("display.temperature.unit", "Celsius"));
        settings.setTheme(getString("theme", "Dark"));
        settings.setAlwaysOnTop(getBoolean("window.always.on.top", false));
    }

    public void saveSettings() {
        properties.setProperty("cpu.refresh.interval", String.valueOf(settings.getCpuRefreshInterval()));
        properties.setProperty("chart.history.seconds", String.valueOf(settings.getChartHistorySeconds()));
        properties.setProperty("display.show.cpu.chart", String.valueOf(settings.isShowCpuChart()));
        properties.setProperty("display.temperature.unit", settings.getTemperatureUnit());
        properties.setProperty("theme", settings.getTheme());
        properties.setProperty("window.always.on.top", String.valueOf(settings.isAlwaysOnTop()));

        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            properties.store(fos, "MyComputerStats Settings");
        } catch (IOException e) {
            System.err.println("Failed to save settings: " + e.getMessage());
        }
    }

    private String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(defaultValue)));
    }
}

