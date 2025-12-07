package org.example.monitoring.cpu;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Monitoring via external process (NativeBridge).
 * Parses stdout from MonitorBridge.exe to avoid JNA/Driver issues in JVM.
 */
public class CpuTempProcessWrapper {

    private volatile boolean running;
    private volatile String status = "Stopped";

    // Cached data arrays
    private volatile double[] temperatures = new double[0];
    private volatile String[] coreTypes = new String[0];
    private volatile double[] usages = new double[0];
    private volatile double[] frequencies = new double[0];

    private int coreCount = 0;
    private Thread monitorThread;
    private Process bridgeProcess;

    // start monitoring
    public void start() {
        if (running)
            return;

        running = true;
        status = "Initializing...";

        monitorThread = new Thread(this::monitorLoop, "CpuBridgeMonitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    // stop monitoring
    public void stop() {
        running = false;
        if (bridgeProcess != null && bridgeProcess.isAlive()) {
            bridgeProcess.destroy();
        }
    }

    // Getters
    public double[] getTemperatures() {
        return temperatures.clone();
    }

    public String[] getCoreTypes() {
        return coreTypes.clone();
    }

    public double[] getUsages() {
        return usages.clone();
    }

    public double[] getFrequencies() {
        return frequencies.clone();
    }

    public String getStatus() {
        return status;
    }

    private void monitorLoop() {
        try {
            // look for native bridge sibling folder
            File userDir = new File(System.getProperty("user.dir"));

            // check sibling (preferred)
            File nativeDir = new File(userDir.getParentFile(), "NativeBridge");
            File exeFile = new File(nativeDir, "MonitorBridge.exe");

            System.out.println("[CpuBridge] Checking path: " + exeFile.getAbsolutePath());

            if (!exeFile.exists()) {
                // check internal native folder (fallback)
                nativeDir = new File(userDir, "native");
                exeFile = new File(nativeDir, "MonitorBridge.exe");
                System.out.println("[CpuBridge] Fallback checking: " + exeFile.getAbsolutePath());
            }

            if (!exeFile.exists()) {
                status = "Error: MonitorBridge.exe not found at " + exeFile.getAbsolutePath();
                System.err.println("[CpuBridge] " + status);
                return;
            }

            System.out.println("[CpuBridge] Found: " + exeFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(exeFile.getAbsolutePath());
            pb.directory(nativeDir);
            pb.redirectErrorStream(true);

            bridgeProcess = pb.start();
            status = "Bridge Started";

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(bridgeProcess.getInputStream()))) {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    processLine(line);
                }
            }

            if (running) {
                status = "Bridge Process Exited";
                System.err.println("[CpuBridge] Process exited unexpectedly");
            }

        } catch (Exception e) {
            status = "Error: " + e.getMessage();
            System.err.println("[CpuBridge] Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (bridgeProcess != null) {
                bridgeProcess.destroy();
            }
        }
    }

    private void processLine(String line) {
        try {
            String[] parts = line.split(",");
            if (parts.length == 0)
                return;

            if (parts[0].equals("INIT")) {
                // INIT,coreCount,tjMax,cpuName
                coreCount = Integer.parseInt(parts[1]);
                int tjMax = Integer.parseInt(parts[2]);
                String cpuName = parts.length > 3 ? parts[3] : "Unknown CPU";

                System.out.println(
                        "[CpuBridge] Initialized: " + cpuName + " (" + coreCount + " cores, TjMax=" + tjMax + "C)");

                // Init arrays
                temperatures = new double[coreCount];
                frequencies = new double[coreCount];
                usages = new double[coreCount]; // Not provided by bridge yet
                coreTypes = new String[coreCount];

                // Assume all P-cores for now or update bridge to send types
                for (int i = 0; i < coreCount; i++)
                    coreTypes[i] = "P";

                status = "Monitoring (" + cpuName + ")";

            } else if (parts[0].equals("DATA")) {
                // DATA,pkgTemp, c0T,c0F,c0V, c1T,c1F,c1V, ...

                double[] newTemps = new double[coreCount];
                double[] newFreqs = new double[coreCount];

                for (int i = 0; i < coreCount; i++) {
                    int baseIdx = 2 + (i * 3);
                    if (baseIdx + 2 < parts.length) {
                        newTemps[i] = Integer.parseInt(parts[baseIdx]);
                        newFreqs[i] = Integer.parseInt(parts[baseIdx + 1]); // MHz
                        // Voltage (baseIdx+2) is ignored
                    }
                }

                this.temperatures = newTemps;
                this.frequencies = newFreqs;
            }
        } catch (Exception e) {
            System.err.println("[CpuBridge] Parse error: " + e.getMessage() + " for line: " + line);
        }
    }
}
