package org.example.monitoring.cpu;

import org.example.core.cpu.CpuInfo;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.Arrays;

/**
 * cpu metric aggregator
 * uses oshi for load/freq and native bridge for temp/voltage
 */
public class CpuMonitoringService {

    private static final long LOG_INTERVAL_MS = 30_000;

    private final CentralProcessor processor;
    private final oshi.software.os.OperatingSystem os;
    private final CpuTempProcessWrapper temperatureWrapper;

    private long[] previousTicks;
    private long[][] previousCoreTicks;
    private long lastLogTime = 0;

    private double[] maxCoreTemps;

    // for per-second calculations
    private long lastContextSwitches = 0;
    private long lastInterrupts = 0;
    private long lastMeasureTime = 0;
    private double contextSwitchesPerSec = 0;
    private double interruptsPerSec = 0;

    public CpuMonitoringService() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        this.processor = hardware.getProcessor();
        this.os = systemInfo.getOperatingSystem();

        this.previousTicks = processor.getSystemCpuLoadTicks();
        this.previousCoreTicks = processor.getProcessorCpuLoadTicks();

        this.maxCoreTemps = new double[processor.getLogicalProcessorCount()];

        this.temperatureWrapper = new CpuTempProcessWrapper();
        this.temperatureWrapper.start();

        // init counters
        this.lastContextSwitches = processor.getContextSwitches();
        this.lastInterrupts = processor.getInterrupts();
        this.lastMeasureTime = System.currentTimeMillis();

        String cpuName = processor.getProcessorIdentifier().getName();
        int physical = processor.getPhysicalProcessorCount();
        int logical = processor.getLogicalProcessorCount();
        System.out.printf("[cpu] init: %s (%dp / %dl)%n", cpuName, physical, logical);
    }

    public void shutdown() {
        temperatureWrapper.stop();
    }

    public void resetStats() {
        Arrays.fill(maxCoreTemps, 0.0);
        System.out.println("[cpu] stats reset");
    }

    public CpuInfo readCpuInfo() {
        ProcessorIdentifier id = processor.getProcessorIdentifier();
        int physicalCores = processor.getPhysicalProcessorCount();
        int logicalCores = processor.getLogicalProcessorCount();

        double[] perCoreFreqs = calculatePerCoreFrequencies(logicalCores);
        double currentFreqGHz = calculateAverageFrequency(perCoreFreqs);
        double maxFreqGHz = processor.getMaxFreq() / 1_000_000_000.0;

        double systemLoad = processor.getSystemCpuLoadBetweenTicks(previousTicks);
        double[] perCoreLoads = processor.getProcessorCpuLoadBetweenTicks(previousCoreTicks);
        previousTicks = processor.getSystemCpuLoadTicks();
        previousCoreTicks = processor.getProcessorCpuLoadTicks();

        double[] perCoreTemps = getPerCoreTemperatures(logicalCores);
        String[] coreTypes = temperatureWrapper.getCoreTypes();

        updateMaxTemps(perCoreTemps);

        // use native freqs if available
        double[] nativeFreqs = temperatureWrapper.getFrequencies();
        if (nativeFreqs.length > 0) {
            perCoreFreqs = new double[nativeFreqs.length];
            for (int i = 0; i < nativeFreqs.length; i++) {
                perCoreFreqs[i] = nativeFreqs[i] / 1000.0;
            }
        }

        double[] voltages = temperatureWrapper.getVoltages();

        String tempStatus = temperatureWrapper.getStatus();
        double maxTemp = calculateMax(perCoreTemps);
        double avgTemp = calculateAverage(perCoreTemps);

        logIfNeeded(systemLoad, currentFreqGHz, maxTemp, tempStatus);

        return new CpuInfo(
                id.getVendor(), id.getName(),
                physicalCores, logicalCores,
                currentFreqGHz, maxFreqGHz,
                systemLoad, perCoreLoads,
                maxTemp, perCoreTemps,
                perCoreFreqs, avgTemp,
                tempStatus, coreTypes, voltages, maxCoreTemps.clone());
    }

    private void updateMaxTemps(double[] currentTemps) {
        if (maxCoreTemps.length != currentTemps.length) {
            maxCoreTemps = new double[currentTemps.length];
        }
        for (int i = 0; i < currentTemps.length; i++) {
            if (currentTemps[i] > maxCoreTemps[i]) {
                maxCoreTemps[i] = currentTemps[i];
            }
        }
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
            System.out.printf("[cpu] load: %.1f%% | freq: %.2f ghz | temp: %.0fc [%s]%n",
                    load * 100, freqGHz, temp, status);
            lastLogTime = now;
        }
    }

    // throttle and power from native bridge
    public boolean isThermalThrottle() {
        return temperatureWrapper.isThermalThrottle();
    }

    public boolean isPowerThrottle() {
        return temperatureWrapper.isPowerThrottle();
    }

    public double getPackagePower() {
        return temperatureWrapper.getPackagePower();
    }

    // system activity metrics
    public void updateSystemActivity() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastMeasureTime;

        if (elapsed > 0) {
            long currentCs = processor.getContextSwitches();
            long currentInt = processor.getInterrupts();

            double timeSec = elapsed / 1000.0;
            contextSwitchesPerSec = (currentCs - lastContextSwitches) / timeSec;
            interruptsPerSec = (currentInt - lastInterrupts) / timeSec;

            lastContextSwitches = currentCs;
            lastInterrupts = currentInt;
            lastMeasureTime = now;
        }
    }

    public double getContextSwitchesPerSec() {
        return contextSwitchesPerSec;
    }

    public double getInterruptsPerSec() {
        return interruptsPerSec;
    }

    public int getProcessCount() {
        return os.getProcessCount();
    }

    public int getThreadCount() {
        return os.getThreadCount();
    }

    // top cpu processes with accurate cpu load calculation
    private java.util.Map<Integer, oshi.software.os.OSProcess> previousProcesses = new java.util.HashMap<>();
    private long lastProcessUpdate = 0;

    public java.util.List<ProcessInfo> getTopProcesses(int count) {
        java.util.List<ProcessInfo> all = new java.util.ArrayList<>();
        int logicalCores = processor.getLogicalProcessorCount();

        // get more processes to have good selection after filtering
        java.util.List<oshi.software.os.OSProcess> procs = os.getProcesses(
                null,
                oshi.software.os.OperatingSystem.ProcessSorting.CPU_DESC,
                50);

        for (oshi.software.os.OSProcess p : procs) {
            String name = p.getName();

            // skip system processes
            if (name.equalsIgnoreCase("Idle") ||
                    name.equalsIgnoreCase("System Idle Process") ||
                    name.equalsIgnoreCase("Registry") ||
                    name.equalsIgnoreCase("Memory Compression") ||
                    name.equalsIgnoreCase("System")) {
                continue;
            }

            double cpuPercent = 0;
            oshi.software.os.OSProcess prev = previousProcesses.get(p.getProcessID());

            if (prev != null) {
                // get CPU load and divide by logical cores to match Task Manager
                cpuPercent = 100.0 * p.getProcessCpuLoadBetweenTicks(prev) / logicalCores;

                // sanity check
                if (cpuPercent < 0 || Double.isNaN(cpuPercent) || Double.isInfinite(cpuPercent)) {
                    cpuPercent = 0;
                }
                // cap at 100%
                cpuPercent = Math.min(100.0, cpuPercent);
            }

            previousProcesses.put(p.getProcessID(), p);
            all.add(new ProcessInfo(name, cpuPercent, p.getProcessID(), p.getPath()));
        }

        // sort by cpu descending AFTER calculation
        all.sort((a, b) -> Double.compare(b.cpuPercent, a.cpuPercent));

        // return top N
        if (all.size() > count) {
            return new java.util.ArrayList<>(all.subList(0, count));
        }
        return all;
    }

    public static class ProcessInfo {
        public final String name;
        public final double cpuPercent;
        public final int pid;
        public final String path;

        public ProcessInfo(String name, double cpuPercent, int pid, String path) {
            this.name = name;
            this.cpuPercent = cpuPercent;
            this.pid = pid;
            this.path = path != null ? path : "";
        }
    }
}
