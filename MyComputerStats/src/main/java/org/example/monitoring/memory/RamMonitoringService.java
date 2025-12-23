package org.example.monitoring.memory;

import org.example.core.memory.RamInfo;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.PhysicalMemory;

import java.util.List;

public class RamMonitoringService {

    private final SystemInfo systemInfo;
    private final GlobalMemory memory;

    public RamMonitoringService() {
        this.systemInfo = new SystemInfo();
        this.memory = systemInfo.getHardware().getMemory();
    }

    public RamInfo readRamInfo() {
        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;

        long swapTotal = memory.getVirtualMemory().getSwapTotal();
        long swapUsed = memory.getVirtualMemory().getSwapUsed();

        String memType = detectMemoryType();
        long memSpeed = detectMemorySpeed();

        return new RamInfo(total, available, used, swapTotal, swapUsed, memType, memSpeed);
    }

    private String detectMemoryType() {
        List<PhysicalMemory> modules = memory.getPhysicalMemory();
        if (modules != null && !modules.isEmpty()) {
            String type = modules.get(0).getMemoryType();
            return type != null && !type.isEmpty() ? type : "Unknown";
        }
        return "Unknown";
    }

    private long detectMemorySpeed() {
        List<PhysicalMemory> modules = memory.getPhysicalMemory();
        if (modules != null && !modules.isEmpty()) {
            long speedHz = modules.get(0).getClockSpeed();
            return speedHz / 1_000_000;
        }
        return 0;
    }
}
