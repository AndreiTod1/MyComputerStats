package org.example.monitoring.cpu;

import java.util.concurrent.TimeUnit;

/**
 * Manages CPU Stress Testing.
 * Spawns threads to consume 100% CPU cycles.
 */
public class CpuStressTestManager {

    private Process stressProcess;

    /**
     * Starts the stress test using external executable.
     */
    public void startStressTest() {
        if (isRunning()) {
            return;
        }

        try {
            System.out.println("[StressTest] Starting external stress process...");
            ProcessBuilder pb = new ProcessBuilder("native/StressTest.exe");
            pb.redirectErrorStream(true);
            stressProcess = pb.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start StressTest.exe");
        }
    }

    /**
     * Stops the stress test.
     */
    public void stopStressTest() {
        if (stressProcess != null && stressProcess.isAlive()) {
            System.out.println("[StressTest] Stopping external stress process...");
            stressProcess.destroy(); // Try polite termination
            try {
                // Give it a moment, then force kill
                if (!stressProcess.waitFor(1, TimeUnit.SECONDS)) {
                    stressProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                stressProcess.destroyForcibly();
            }
            stressProcess = null;
            System.out.println("[StressTest] Stopped.");
        }
    }

    public boolean isRunning() {
        return stressProcess != null && stressProcess.isAlive();
    }
}
