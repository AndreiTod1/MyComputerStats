package org.example.core.memory;

public class RamInfo {

    private final long totalBytes;
    private final long availableBytes;
    private final long usedBytes;

    private final long swapTotalBytes;
    private final long swapUsedBytes;

    private final String memoryType;
    private final long memorySpeed;

    public RamInfo(long totalBytes, long availableBytes, long usedBytes,
            long swapTotalBytes, long swapUsedBytes,
            String memoryType, long memorySpeed) {
        this.totalBytes = totalBytes;
        this.availableBytes = availableBytes;
        this.usedBytes = usedBytes;
        this.swapTotalBytes = swapTotalBytes;
        this.swapUsedBytes = swapUsedBytes;
        this.memoryType = memoryType;
        this.memorySpeed = memorySpeed;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getAvailableBytes() {
        return availableBytes;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public double getUsagePercent() {
        return totalBytes > 0 ? (usedBytes * 100.0) / totalBytes : 0;
    }

    public long getSwapTotalBytes() {
        return swapTotalBytes;
    }

    public long getSwapUsedBytes() {
        return swapUsedBytes;
    }

    public long getSwapFreeBytes() {
        return swapTotalBytes - swapUsedBytes;
    }

    public double getSwapUsagePercent() {
        return swapTotalBytes > 0 ? (swapUsedBytes * 100.0) / swapTotalBytes : 0;
    }

    public String getMemoryType() {
        return memoryType;
    }

    public long getMemorySpeed() {
        return memorySpeed;
    }

    public String getFormattedTotal() {
        return formatBytes(totalBytes);
    }

    public String getFormattedUsed() {
        return formatBytes(usedBytes);
    }

    public String getFormattedAvailable() {
        return formatBytes(availableBytes);
    }

    public String getFormattedSwapTotal() {
        return formatBytes(swapTotalBytes);
    }

    public String getFormattedSwapUsed() {
        return formatBytes(swapUsedBytes);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
