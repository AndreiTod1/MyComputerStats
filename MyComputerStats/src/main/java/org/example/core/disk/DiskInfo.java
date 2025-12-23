package org.example.core.disk;

import java.util.List;

public class DiskInfo {
    private final String name;
    private final String model;
    private final long totalBytes;
    private final long usedBytes;
    private final long freeBytes;
    private final double usagePercent;
    private final String type;
    private final String mountPoint;

    public DiskInfo(String name, String model, long totalBytes, long usedBytes, long freeBytes,
            double usagePercent, String type, String mountPoint) {
        this.name = name;
        this.model = model;
        this.totalBytes = totalBytes;
        this.usedBytes = usedBytes;
        this.freeBytes = freeBytes;
        this.usagePercent = usagePercent;
        this.type = type;
        this.mountPoint = mountPoint;
    }

    public String getName() {
        return name;
    }

    public String getModel() {
        return model;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public long getFreeBytes() {
        return freeBytes;
    }

    public double getUsagePercent() {
        return usagePercent;
    }

    public String getType() {
        return type;
    }

    public String getMountPoint() {
        return mountPoint;
    }

    public String getFormattedTotal() {
        return formatBytes(totalBytes);
    }

    public String getFormattedUsed() {
        return formatBytes(usedBytes);
    }

    public String getFormattedFree() {
        return formatBytes(freeBytes);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        if (bytes < 1024L * 1024 * 1024 * 1024)
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        return String.format("%.2f TB", bytes / (1024.0 * 1024 * 1024 * 1024));
    }
}
