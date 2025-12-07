package org.example.core.cpu;

public class CpuInfo {
    private final String brand;
    private final String model;
    private final int physicalCores;
    private final int logicalCores;
    private final double currentClockSpeed;
    private final double maxClockSpeed;
    private final double loadPercentage;
    private final double[] perCoreLoads;
    private final double temperature;
    private final double[] perCoreTemperatures;
    private final double[] perCoreFrequencies;
    private final double averageTemperature;
    private final String temperatureSource;
    private final String[] coreTypes; // P = Performance, E = Efficiency

    public CpuInfo(String brand, String model, int physicalCores, int logicalCores,
            double currentClockSpeed, double maxClockSpeed, double loadPercentage,
            double[] perCoreLoads, double temperature, double[] perCoreTemperatures) {
        this(brand, model, physicalCores, logicalCores, currentClockSpeed, maxClockSpeed,
                loadPercentage, perCoreLoads, temperature, perCoreTemperatures, null, 0.0, "N/A", null);
    }

    public CpuInfo(String brand, String model, int physicalCores, int logicalCores,
            double currentClockSpeed, double maxClockSpeed, double loadPercentage,
            double[] perCoreLoads, double temperature, double[] perCoreTemperatures,
            double[] perCoreFrequencies, double averageTemperature, String temperatureSource) {
        this(brand, model, physicalCores, logicalCores, currentClockSpeed, maxClockSpeed,
                loadPercentage, perCoreLoads, temperature, perCoreTemperatures,
                perCoreFrequencies, averageTemperature, temperatureSource, null);
    }

    public CpuInfo(String brand, String model, int physicalCores, int logicalCores,
            double currentClockSpeed, double maxClockSpeed, double loadPercentage,
            double[] perCoreLoads, double temperature, double[] perCoreTemperatures,
            double[] perCoreFrequencies, double averageTemperature, String temperatureSource,
            String[] coreTypes) {
        this.brand = brand != null ? brand : "Unknown";
        this.model = model != null ? model : "Unknown";
        this.physicalCores = Math.max(1, physicalCores);
        this.logicalCores = Math.max(1, logicalCores);
        this.currentClockSpeed = Math.max(0, currentClockSpeed);
        this.maxClockSpeed = Math.max(0, maxClockSpeed);
        this.loadPercentage = clamp(loadPercentage, 0.0, 1.0);
        this.perCoreLoads = perCoreLoads != null ? perCoreLoads : new double[0];
        this.temperature = temperature;
        this.perCoreTemperatures = perCoreTemperatures != null ? perCoreTemperatures : new double[0];
        this.perCoreFrequencies = perCoreFrequencies != null ? perCoreFrequencies : new double[0];
        this.averageTemperature = averageTemperature;
        this.temperatureSource = temperatureSource != null ? temperatureSource : "N/A";
        this.coreTypes = coreTypes != null ? coreTypes : new String[0];
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public int getPhysicalCores() {
        return physicalCores;
    }

    public int getLogicalCores() {
        return logicalCores;
    }

    public double getClockSpeed() {
        return currentClockSpeed;
    }

    public double getMaxClockSpeed() {
        return maxClockSpeed;
    }

    public double getLoad() {
        return loadPercentage;
    }

    public double getLoadPercentage() {
        return loadPercentage * 100.0;
    }

    public double[] getPerCoreLoads() {
        return perCoreLoads;
    }

    public double getTemperature() {
        return temperature;
    }

    public double[] getPerCoreTemperatures() {
        return perCoreTemperatures;
    }

    public double[] getPerCoreFrequencies() {
        return perCoreFrequencies;
    }

    public double getAverageTemperature() {
        return averageTemperature;
    }

    public String getTemperatureSource() {
        return temperatureSource;
    }

    public String[] getCoreTypes() {
        return coreTypes;
    }

    public String getFormattedTemperature() {
        if (temperature > 0) {
            return String.format("%.1f°C", temperature);
        } else {
            return "N/A";
        }
    }

    public String getFormattedAverageTemperature() {
        if (averageTemperature > 0) {
            return String.format("%.1f°C", averageTemperature);
        } else {
            return "N/A";
        }
    }

    public String getFormattedClockSpeed() {
        return currentClockSpeed > 0
                ? String.format("%.2f GHz", currentClockSpeed)
                : "N/A";
    }

    public String getFormattedMaxClockSpeed() {
        return maxClockSpeed > 0
                ? String.format("%.2f GHz", maxClockSpeed)
                : "N/A";
    }

    public String getFormattedLoad() {
        return String.format("%.1f%%", getLoadPercentage());
    }

    public String getCoreDescription() {
        return String.format("%d cores • %d threads", physicalCores, logicalCores);
    }

    public LoadLevel getLoadLevel() {
        if (loadPercentage < 0.3)
            return LoadLevel.LOW;
        if (loadPercentage < 0.6)
            return LoadLevel.MODERATE;
        if (loadPercentage < 0.85)
            return LoadLevel.HIGH;
        return LoadLevel.CRITICAL;
    }

    public boolean hasHyperthreading() {
        return logicalCores > physicalCores;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String toString() {
        return String.format("CpuInfo[%s %s, %s, Load: %s]",
                brand, model, getCoreDescription(), getFormattedLoad());
    }

    public enum LoadLevel {
        LOW, MODERATE, HIGH, CRITICAL
    }
}
