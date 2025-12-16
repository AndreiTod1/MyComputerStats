package org.example.ui.cpu.model;

import org.example.core.cpu.CpuInfo;

public class SessionStatistics {

    // tracking accumulators
    private double totalLoad = 0;
    private double totalTemp = 0;
    private double totalFreq = 0;
    private double totalVolt = 0;
    private double totalPower = 0;
    private int sampleCount = 0;

    // detailed session stats tracking
    private double minLoad = Double.MAX_VALUE, maxLoad = 0;
    private double minTemp = Double.MAX_VALUE, maxTemp = 0;
    private double minFreq = Double.MAX_VALUE, maxFreq = 0;
    private double minVolt = Double.MAX_VALUE, maxVolt = 0;
    private double minPower = Double.MAX_VALUE, maxPower = 0;

    // throttling stats
    private double maxObservedTemp = 0;
    private int throttleEventCount = 0;
    private boolean wasThrottling = false;

    public void update(CpuInfo info, double load, double packagePower) {
        sampleCount++;

        // 1. LOAD
        double currentLoad = load * 100;
        totalLoad += currentLoad;
        if (currentLoad < minLoad)
            minLoad = currentLoad;
        if (currentLoad > maxLoad)
            maxLoad = currentLoad;

        // 2. TEMP
        double currentTemp = info.getTemperature();
        totalTemp += currentTemp;
        if (currentTemp < minTemp)
            minTemp = currentTemp;
        if (currentTemp > maxTemp)
            maxTemp = currentTemp;

        // Track max observed temp separately (often same as maxTemp, but explicit for
        // throttling context)
        if (currentTemp > maxObservedTemp)
            maxObservedTemp = currentTemp;

        // 3. FREQ
        double currentFreq = info.getClockSpeed();
        totalFreq += currentFreq;
        if (currentFreq < minFreq)
            minFreq = currentFreq;
        if (currentFreq > maxFreq)
            maxFreq = currentFreq;

        // 4. VOLTAGE
        double[] voltages = info.getPerCoreVoltages();
        double currentVolt = 0;
        if (voltages != null && voltages.length > 0) {
            for (double v : voltages)
                currentVolt += v;
            currentVolt /= voltages.length;
        }
        totalVolt += currentVolt;
        if (currentVolt < minVolt)
            minVolt = currentVolt;
        if (currentVolt > maxVolt)
            maxVolt = currentVolt;

        // 5. POWER
        totalPower += packagePower;
        if (packagePower < minPower)
            minPower = packagePower;
        if (packagePower > maxPower)
            maxPower = packagePower;
    }

    public void updateThrottling(boolean isThrottling) {
        if (isThrottling && !wasThrottling) {
            throttleEventCount++;
        }
        wasThrottling = isThrottling;
    }

    public void reset() {
        totalLoad = 0;
        totalTemp = 0;
        totalFreq = 0;
        totalVolt = 0;
        totalPower = 0;
        sampleCount = 0;

        minLoad = Double.MAX_VALUE;
        maxLoad = 0;
        minTemp = Double.MAX_VALUE;
        maxTemp = 0;
        minFreq = Double.MAX_VALUE;
        maxFreq = 0;
        minVolt = Double.MAX_VALUE;
        maxVolt = 0;
        minPower = Double.MAX_VALUE;
        maxPower = 0;

        maxObservedTemp = 0;
        throttleEventCount = 0;
        wasThrottling = false;
    }

    public double getMaxObservedTemp() {
        return maxObservedTemp;
    }

    public int getThrottleEventCount() {
        return throttleEventCount;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public double getAvgLoad() {
        return sampleCount == 0 ? 0 : totalLoad / sampleCount;
    }

    public double getMinLoad() {
        return minLoad == Double.MAX_VALUE ? 0 : minLoad;
    }

    public double getMaxLoad() {
        return maxLoad;
    }

    public double getAvgTemp() {
        return sampleCount == 0 ? 0 : totalTemp / sampleCount;
    }

    public double getMinTemp() {
        return minTemp == Double.MAX_VALUE ? 0 : minTemp;
    }

    public double getMaxTemp() {
        return maxTemp;
    }

    public double getAvgFreq() {
        return sampleCount == 0 ? 0 : totalFreq / sampleCount;
    }

    public double getMinFreq() {
        return minFreq == Double.MAX_VALUE ? 0 : minFreq;
    }

    public double getMaxFreq() {
        return maxFreq;
    }

    public double getAvgVolt() {
        return sampleCount == 0 ? 0 : totalVolt / sampleCount;
    }

    public double getMinVolt() {
        return minVolt == Double.MAX_VALUE ? 0 : minVolt;
    }

    public double getMaxVolt() {
        return maxVolt;
    }

    public double getAvgPower() {
        return sampleCount == 0 ? 0 : totalPower / sampleCount;
    }

    public double getMinPower() {
        return minPower == Double.MAX_VALUE ? 0 : minPower;
    }

    public double getMaxPower() {
        return maxPower;
    }
}
