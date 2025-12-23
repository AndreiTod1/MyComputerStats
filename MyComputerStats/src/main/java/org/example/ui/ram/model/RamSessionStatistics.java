package org.example.ui.ram.model;

import org.example.core.memory.RamInfo;

public class RamSessionStatistics {

    private double totalUsagePercent = 0;
    private double totalSwapPercent = 0;
    private double totalUsedMB = 0;
    private int sampleCount = 0;

    private double minUsagePercent = Double.MAX_VALUE;
    private double maxUsagePercent = 0;
    private double minSwapPercent = Double.MAX_VALUE;
    private double maxSwapPercent = 0;
    private double minUsedMB = Double.MAX_VALUE;
    private double maxUsedMB = 0;

    public void update(RamInfo info) {
        sampleCount++;

        double usagePercent = info.getUsagePercent();
        double swapPercent = info.getSwapUsagePercent();
        double usedMB = info.getUsedBytes() / (1024.0 * 1024);

        totalUsagePercent += usagePercent;
        totalSwapPercent += swapPercent;
        totalUsedMB += usedMB;

        if (usagePercent < minUsagePercent)
            minUsagePercent = usagePercent;
        if (usagePercent > maxUsagePercent)
            maxUsagePercent = usagePercent;

        if (swapPercent < minSwapPercent)
            minSwapPercent = swapPercent;
        if (swapPercent > maxSwapPercent)
            maxSwapPercent = swapPercent;

        if (usedMB < minUsedMB)
            minUsedMB = usedMB;
        if (usedMB > maxUsedMB)
            maxUsedMB = usedMB;
    }

    public void reset() {
        totalUsagePercent = 0;
        totalSwapPercent = 0;
        totalUsedMB = 0;
        sampleCount = 0;

        minUsagePercent = Double.MAX_VALUE;
        maxUsagePercent = 0;
        minSwapPercent = Double.MAX_VALUE;
        maxSwapPercent = 0;
        minUsedMB = Double.MAX_VALUE;
        maxUsedMB = 0;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public double getAvgUsagePercent() {
        return sampleCount == 0 ? 0 : totalUsagePercent / sampleCount;
    }

    public double getMinUsagePercent() {
        return minUsagePercent == Double.MAX_VALUE ? 0 : minUsagePercent;
    }

    public double getMaxUsagePercent() {
        return maxUsagePercent;
    }

    public double getAvgSwapPercent() {
        return sampleCount == 0 ? 0 : totalSwapPercent / sampleCount;
    }

    public double getMinSwapPercent() {
        return minSwapPercent == Double.MAX_VALUE ? 0 : minSwapPercent;
    }

    public double getMaxSwapPercent() {
        return maxSwapPercent;
    }

    public double getAvgUsedMB() {
        return sampleCount == 0 ? 0 : totalUsedMB / sampleCount;
    }

    public double getMinUsedMB() {
        return minUsedMB == Double.MAX_VALUE ? 0 : minUsedMB;
    }

    public double getMaxUsedMB() {
        return maxUsedMB;
    }
}
