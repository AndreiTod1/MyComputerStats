package org.example.monitoring.cpu;

import org.example.core.cpu.CpuInfo;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;

import java.util.Arrays;

public class CpuMonitoringService {

    private SystemInfo systemInfo;
    private CentralProcessor processor;
    private long[] prevTicks;

    public CpuMonitoringService() {
        this.systemInfo = new SystemInfo();
        this.processor = systemInfo.getHardware().getProcessor();
        this.prevTicks = processor.getSystemCpuLoadTicks();
    }

    public CpuInfo readCpuInfo() {
        double load = processor.getSystemCpuLoadBetweenTicks(prevTicks);
        this.prevTicks = processor.getSystemCpuLoadTicks();

        ProcessorIdentifier id = processor.getProcessorIdentifier();
        String brand = id.getVendor();
        String model = id.getName();

        int physical = processor.getPhysicalProcessorCount();
        int logical = processor.getLogicalProcessorCount();

        long[] freqs = processor.getCurrentFreq();
        double avgHz = freqs != null && freqs.length > 0
                ? Arrays.stream(freqs).average().orElse(0)
                : 0;
        double clockSpeedGHz = avgHz / 1_000_000_000.0;

        long maxHz = processor.getMaxFreq();
        double maxClockSpeedGHz = maxHz > 0 ? maxHz / 1_000_000_000.0 : 0.0;

        return new CpuInfo(brand, model, physical, logical, clockSpeedGHz, maxClockSpeedGHz, load);
    }

}
