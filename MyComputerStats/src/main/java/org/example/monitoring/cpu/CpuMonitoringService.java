package org.example.monitoring.cpu;

import org.example.core.cpu.CpuInfo;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.Arrays;

/**
 * Aggregates CPU metrics.
 * Combines OSHI data (Load/Freq) with NativeBridge data (Temp/CoreType).
 */
public class CpuMonitoringService {

    private static final long LOG_INTERVAL_MS = 30_000; // Log every 30 seconds

    private final CentralProcessor processor;
    private final CpuTempProcessWrapper temperatureWrapper;

    private long[] previousTicks;
    private long[][] previousCoreTicks;
    private long lastLogTime = 0;

    public CpuMonitoringService() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        this.processor = hardware.getProcessor();

        // init usage counters
        this.previousTicks = processor.getSystemCpuLoadTicks();
        this.previousCoreTicks = processor.getProcessorCpuLoadTicks();

        // Start temperature subprocess
        this.temperatureWrapper = new CpuTempProcessWrapper();
        this.temperatureWrapper.start();

        // Log initialization
        String cpuName = processor.getProcessorIdentifier().getName();
        int physical = processor.getPhysicalProcessorCount();
        int logical = processor.getLogicalProcessorCount();
        System.out.printf("[CPU] Initialized: %s (%dP / %dL)%n", cpuName, physical, logical);
    }

    /**
     * Shuts down the temperature monitoring subprocess.
     */
    public void shutdown() {
        temperatureWrapper.stop();
    }

    // polls current cpu stats
    public CpuInfo readCpuInfo() {
        ProcessorIdentifier id = processor.getProcessorIdentifier();
        int physicalCores = processor.getPhysicalProcessorCount();
        int logicalCores = processor.getLogicalProcessorCount();

        // Calculate frequencies from OSHI
        double[] perCoreFreqs = calculatePerCoreFrequencies(logicalCores);
        double currentFreqGHz = calculateAverageFrequency(perCoreFreqs);
        double maxFreqGHz = processor.getMaxFreq() / 1_000_000_000.0;

        // Calculate CPU load from OSHI
        double systemLoad = processor.getSystemCpuLoadBetweenTicks(previousTicks);
        double[] perCoreLoads = processor.getProcessorCpuLoadBetweenTicks(previousCoreTicks);
        previousTicks = processor.getSystemCpuLoadTicks();
        previousCoreTicks = processor.getProcessorCpuLoadTicks();

        // native data
        double[] perCoreTemps = getPerCoreTemperatures(logicalCores);
        String[] coreTypes = temperatureWrapper.getCoreTypes();

        // use native frequencies if available
        double[] nativeFreqs = temperatureWrapper.getFrequencies();
        if (nativeFreqs.length > 0) {
            perCoreFreqs = new double[nativeFreqs.length];
            for (int i = 0; i < nativeFreqs.length; i++) {
                perCoreFreqs[i] = nativeFreqs[i] / 1000.0; // MHz to GHz
            }
        }

        String tempStatus = temperatureWrapper.getStatus();
        double maxTemp = calculateMax(perCoreTemps);
        double avgTemp = calculateAverage(perCoreTemps);

        // Periodic logging
        logIfNeeded(systemLoad, currentFreqGHz, maxTemp, tempStatus);

        return new CpuInfo(
                id.getVendor(), id.getName(),
                physicalCores, logicalCores,
                currentFreqGHz, maxFreqGHz,
                systemLoad, perCoreLoads,
                maxTemp, perCoreTemps,
                perCoreFreqs, avgTemp,
                tempStatus, coreTypes);
    }

    private double[] calculatePerCoreFrequencies(int logicalCores) {
        long[] freqs = processor.getCurrentFreq();
        double[] result = new double[logicalCores];

        if (freqs != null) {
            for (int i = 0; i < Math.min(freqs.length, logicalCores); i++) {
                result[i] = freqs[i] / 1_000_000_000.0;
            }
        }
        return result;
    }

    private double calculateAverageFrequency(double[] frequencies) {
        return Arrays.stream(frequencies).average().orElse(0.0);
    }

    private double[] getPerCoreTemperatures(int logicalCores) {
        double[] wrapperTemps = temperatureWrapper.getTemperatures();
        double[] result = new double[logicalCores];

        // Copy available temps, rest remain 0
        System.arraycopy(wrapperTemps, 0, result, 0, Math.min(wrapperTemps.length, logicalCores));
        return result;
    }

    private double calculateMax(double[] values) {
        double max = 0;
        for (double v : values) {
            if (v > max)
                max = v;
        }
        return max;
    }

    private double calculateAverage(double[] values) {
        double sum = 0;
        int count = 0;
        for (double v : values) {
            if (v > 0) {
                sum += v;
                count++;
            }
        }
        return count > 0 ? sum / count : 0.0;
    }

    private void logIfNeeded(double load, double freqGHz, double temp, String status) {
        long now = System.currentTimeMillis();
        if (now - lastLogTime >= LOG_INTERVAL_MS) {
            System.out.printf("[CPU] Load: %.1f%% | Freq: %.2f GHz | Temp: %.0f°C [%s]%n",
                    load * 100, freqGHz, temp, status);
            lastLogTime = now;
        }
    }
}
