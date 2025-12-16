package org.example.monitoring.cpu;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * wrapper for the native monitor bridge exe
 * reads temp, freq, voltage from stdout
 */
public class CpuTempProcessWrapper {

    private volatile boolean running;
    private volatile String status = "Stopped";

    private volatile double[] temperatures = new double[0];
    private volatile String[] coreTypes = new String[0];
    private volatile double[] usages = new double[0];
    private volatile double[] frequencies = new double[0];
    private volatile double[] voltages = new double[0];

    private int coreCount = 0;
    private Thread monitorThread;
    private Process bridgeProcess;

    public void start() {
        if (running)
            return;

        running = true;
        status = "Initializing...";

        monitorThread = new Thread(this::monitorLoop, "CpuBridgeMonitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    public void stop() {
        running = false;
        if (bridgeProcess != null && bridgeProcess.isAlive()) {
            bridgeProcess.destroy();
        }
    }

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

    public double[] getVoltages() {
        return voltages.clone();
    }

    public String getStatus() {
        return status;
    }

    private void monitorLoop() {
        try {
            File userDir = new File(System.getProperty("user.dir"));

            // try sibling nativebridge folder first
            File nativeDir = new File(userDir.getParentFile(), "NativeBridge");
            File exeFile = new File(nativeDir, "MonitorBridge.exe");
            System.out.println("[CpuBridge] checking sibling: " + exeFile.getAbsolutePath());

            if (!exeFile.exists()) {
                // fallback to local native folder
                nativeDir = new File(userDir, "native");
                exeFile = new File(nativeDir, "MonitorBridge.exe");
                System.out.println("[CpuBridge] fallback local native: " + exeFile.getAbsolutePath());
            }

            if (!exeFile.exists()) {
                // last resort, check root
                nativeDir = userDir;
                exeFile = new File(nativeDir, "MonitorBridge.exe");
                System.out.println("[CpuBridge] fallback root: " + exeFile.getAbsolutePath());
            }

            if (!exeFile.exists()) {
                status = "Error: MonitorBridge.exe not found";
                System.err.println("[CpuBridge] " + status);
                return;
            }

            System.out.println("[CpuBridge] found: " + exeFile.getAbsolutePath());

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
                System.err.println("[CpuBridge] process exited unexpectedly");
            }

        } catch (Exception e) {
            status = "Error: " + e.getMessage();
            System.err.println("[CpuBridge] error: " + e.getMessage());
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
                // format: INIT,coreCount,tjMax,cpuName
                coreCount = Integer.parseInt(parts[1]);
                int tjMax = Integer.parseInt(parts[2]);
                String cpuName = parts.length > 3 ? parts[3] : "Unknown CPU";

                System.out.println(
                        "[CpuBridge] initialized: " + cpuName + " (" + coreCount + " cores, tjmax=" + tjMax + ")");

                temperatures = new double[coreCount];
                frequencies = new double[coreCount];
                usages = new double[coreCount];
                coreTypes = new String[coreCount];

                // assume p cores for now
                for (int i = 0; i < coreCount; i++)
                    coreTypes[i] = "P";

                status = "Monitoring (" + cpuName + ")";

            } else if (parts[0].equals("DATA")) {
                // format: DATA,pkgTemp, c0T,c0F,c0V, c1T,c1F,c1V, ...
                double[] newTemps = new double[coreCount];
                double[] newFreqs = new double[coreCount];
                double[] newVolts = new double[coreCount];

                for (int i = 0; i < coreCount; i++) {
                    int baseIdx = 2 + (i * 3);
                    if (baseIdx + 2 < parts.length) {
                        try {
                            newTemps[i] = Double.parseDouble(parts[baseIdx]);
                            newFreqs[i] = Double.parseDouble(parts[baseIdx + 1]);
                            newVolts[i] = Double.parseDouble(parts[baseIdx + 2]);
                        } catch (NumberFormatException e) {
                            // skip malformed
                        }
                    }
                }

                this.temperatures = newTemps;
                this.frequencies = newFreqs;
                this.voltages = newVolts;
            } else {
                System.out.println("[CpuBridge] raw: " + line);
            }
        } catch (Exception e) {
            System.err.println("[CpuBridge] parse error: " + e.getMessage());
        }
    }
}
