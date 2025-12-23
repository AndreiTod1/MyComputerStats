package org.example.monitoring.disk;

import org.example.core.disk.DiskInfo;
import oshi.SystemInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

import java.util.ArrayList;
import java.util.List;

public class DiskMonitoringService {

    private final SystemInfo systemInfo;
    private final FileSystem fileSystem;

    public DiskMonitoringService() {
        this.systemInfo = new SystemInfo();
        this.fileSystem = systemInfo.getOperatingSystem().getFileSystem();
    }

    public List<DiskInfo> readDiskInfo() {
        List<DiskInfo> disks = new ArrayList<>();

        for (OSFileStore store : fileSystem.getFileStores()) {
            long total = store.getTotalSpace();
            long free = store.getUsableSpace();
            long used = total - free;
            double usagePercent = total > 0 ? (used * 100.0 / total) : 0;

            String type = store.getType();
            if (type == null || type.isEmpty()) {
                type = "Unknown";
            }

            disks.add(new DiskInfo(
                    store.getName(),
                    store.getDescription(),
                    total,
                    used,
                    free,
                    usagePercent,
                    type,
                    store.getMount()));
        }

        return disks;
    }

    public DiskInfo getTotalDiskInfo() {
        List<DiskInfo> disks = readDiskInfo();

        long totalBytes = 0;
        long usedBytes = 0;
        long freeBytes = 0;

        for (DiskInfo disk : disks) {
            totalBytes += disk.getTotalBytes();
            usedBytes += disk.getUsedBytes();
            freeBytes += disk.getFreeBytes();
        }

        double usagePercent = totalBytes > 0 ? (usedBytes * 100.0 / totalBytes) : 0;

        return new DiskInfo("All Drives", "Combined", totalBytes, usedBytes, freeBytes,
                usagePercent, "Combined", "All");
    }

    public List<String> getPhysicalDisks() {
        List<String> physicalDisks = new ArrayList<>();
        for (oshi.hardware.HWDiskStore disk : systemInfo.getHardware().getDiskStores()) {
            String model = disk.getModel();
            long sizeGB = disk.getSize() / (1024L * 1024 * 1024);
            if (model != null && !model.isEmpty() && sizeGB > 0) {
                physicalDisks.add(model + " (" + sizeGB + " GB)");
            }
        }
        return physicalDisks;
    }

    public java.util.Map<String, String> getPartitionDeviceMap() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        for (oshi.hardware.HWDiskStore disk : systemInfo.getHardware().getDiskStores()) {
            String deviceName = disk.getModel();
            for (oshi.hardware.HWPartition partition : disk.getPartitions()) {
                String mount = partition.getMountPoint();
                if (mount != null && !mount.isEmpty()) {
                    map.put(mount, deviceName);
                }
            }
        }
        return map;
    }
}
