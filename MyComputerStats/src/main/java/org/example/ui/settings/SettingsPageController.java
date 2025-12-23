package org.example.ui.settings;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import org.example.core.settings.AppSettings;
import org.example.core.settings.SettingsManager;
import org.example.core.settings.SettingsChangeListener;

public class SettingsPageController {

    @FXML private Spinner<Double> cpuRefreshSpinner;
    @FXML private Spinner<Integer> chartHistorySpinner;
    @FXML private CheckBox showCpuChartCheckbox;
    @FXML private ComboBox<String> temperatureUnitCombo;
    @FXML private ComboBox<String> themeCombo;
    @FXML private CheckBox alwaysOnTopCheckbox;

    private SettingsManager settingsManager;

    @FXML
    public void initialize() {
        settingsManager = SettingsManager.getInstance();
        initializeSpinners();
        initializeComboBoxes();
        loadCurrentSettings();
    }

    private void initializeSpinners() {
        cpuRefreshSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.3, 10.0, 0.5, 0.1));
        chartHistorySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 300, 60));
    }

    private void initializeComboBoxes() {
        temperatureUnitCombo.getItems().addAll("Celsius", "Fahrenheit");
        temperatureUnitCombo.setValue("Celsius");

        themeCombo.getItems().addAll("Dark", "Light");
        themeCombo.setValue("Dark");
    }

    private void loadCurrentSettings() {
        AppSettings settings = settingsManager.getSettings();
        cpuRefreshSpinner.getValueFactory().setValue(settings.getCpuRefreshInterval());
        chartHistorySpinner.getValueFactory().setValue(settings.getChartHistorySeconds());
        showCpuChartCheckbox.setSelected(settings.isShowCpuChart());
        temperatureUnitCombo.setValue(settings.getTemperatureUnit());
        themeCombo.setValue(settings.getTheme());
        alwaysOnTopCheckbox.setSelected(settings.isAlwaysOnTop());
    }

    @FXML
    private void saveSettings() {
        AppSettings settings = settingsManager.getSettings();
        settings.setCpuRefreshInterval(cpuRefreshSpinner.getValue());
        settings.setChartHistorySeconds(chartHistorySpinner.getValue());
        settings.setShowCpuChart(showCpuChartCheckbox.isSelected());
        settings.setTemperatureUnit(temperatureUnitCombo.getValue());
        settings.setTheme(themeCombo.getValue());
        settings.setAlwaysOnTop(alwaysOnTopCheckbox.isSelected());

        settingsManager.saveSettings();

        // Notify all listeners that settings have changed
        SettingsChangeListener.getInstance().notifySettingsChanged(settings);

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Settings Saved");
        alert.setHeaderText(null);
        alert.setContentText("Settings applied successfully!");
        alert.showAndWait();
    }

    @FXML
    private void resetToDefaults() {
        AppSettings defaults = new AppSettings();
        cpuRefreshSpinner.getValueFactory().setValue(defaults.getCpuRefreshInterval());
        chartHistorySpinner.getValueFactory().setValue(defaults.getChartHistorySeconds());
        showCpuChartCheckbox.setSelected(defaults.isShowCpuChart());
        temperatureUnitCombo.setValue(defaults.getTemperatureUnit());
        themeCombo.setValue(defaults.getTheme());
        alwaysOnTopCheckbox.setSelected(defaults.isAlwaysOnTop());

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Reset to Defaults");
        alert.setHeaderText(null);
        alert.setContentText("Settings reset to defaults.\nClick 'Save Settings' to persist.");
        alert.showAndWait();
    }
}

