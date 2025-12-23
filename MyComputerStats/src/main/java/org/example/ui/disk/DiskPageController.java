package org.example.ui.disk;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import org.example.core.disk.DiskInfo;
import org.example.monitoring.disk.DiskMonitoringService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class DiskPageController {

    @FXML
    private Label totalSpaceLabel;
    @FXML
    private Label usedSpaceLabel;
    @FXML
    private Label freeSpaceLabel;
    @FXML
    private Label usagePercentLabel;
    @FXML
    private ProgressBar totalUsageBar;
    @FXML
    private VBox drivesContainer;

    private DiskMonitoringService diskService;
    private Timeline timeline;

    // remember expanded state by mount point
    private Set<String> expandedDisks = new HashSet<>();
    private Map<String, VBox> diskCards = new HashMap<>();
    private Map<String, String> partitionDeviceMap = new HashMap<>();

    @FXML
    public void initialize() {
        diskService = new DiskMonitoringService();
        updateDiskInfo();
        startLiveUpdates();
    }

    private void startLiveUpdates() {
        timeline = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> refreshData()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    public void stopMonitoring() {
        if (timeline != null) {
            timeline.stop();
        }
    }

    private void updateDiskInfo() {
        List<DiskInfo> disks = diskService.readDiskInfo();
        DiskInfo total = diskService.getTotalDiskInfo();
        List<String> physicalDisks = diskService.getPhysicalDisks();

        totalSpaceLabel.setText(total.getFormattedTotal());
        usedSpaceLabel.setText(total.getFormattedUsed());
        freeSpaceLabel.setText(total.getFormattedFree());
        usagePercentLabel.setText(String.format("%.1f%%", total.getUsagePercent()));
        totalUsageBar.setProgress(total.getUsagePercent() / 100.0);

        buildDrivesList(disks, physicalDisks);
    }

    private void refreshData() {
        List<DiskInfo> disks = diskService.readDiskInfo();
        DiskInfo total = diskService.getTotalDiskInfo();

        totalSpaceLabel.setText(total.getFormattedTotal());
        usedSpaceLabel.setText(total.getFormattedUsed());
        freeSpaceLabel.setText(total.getFormattedFree());
        usagePercentLabel.setText(String.format("%.1f%%", total.getUsagePercent()));
        totalUsageBar.setProgress(total.getUsagePercent() / 100.0);

        // update existing cards without rebuilding
        for (DiskInfo disk : disks) {
            if (disk.getTotalBytes() == 0)
                continue;
            VBox card = diskCards.get(disk.getMountPoint());
            if (card != null) {
                updateDriveCard(card, disk);
            }
        }
    }

    private void buildDrivesList(List<DiskInfo> disks, List<String> physicalDisks) {
        if (drivesContainer.getChildren().size() > 1) {
            drivesContainer.getChildren().subList(1, drivesContainer.getChildren().size()).clear();
        }
        diskCards.clear();
        partitionDeviceMap = diskService.getPartitionDeviceMap();

        // group partitions by device
        java.util.Map<String, java.util.List<DiskInfo>> disksByDevice = new java.util.LinkedHashMap<>();

        for (DiskInfo disk : disks) {
            if (disk.getTotalBytes() == 0)
                continue;
            String deviceName = partitionDeviceMap.get(disk.getMountPoint());
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = "Unknown Device";
            }
            disksByDevice.computeIfAbsent(deviceName, k -> new java.util.ArrayList<>()).add(disk);
        }

        for (java.util.Map.Entry<String, java.util.List<DiskInfo>> entry : disksByDevice.entrySet()) {
            String deviceName = entry.getKey();
            java.util.List<DiskInfo> deviceDisks = entry.getValue();

            // device header
            VBox deviceSection = new VBox(8);
            deviceSection.setPadding(new Insets(5, 0, 10, 0));

            Label deviceHeader = new Label(deviceName);
            deviceHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #00f2ff; " +
                    "-fx-background-color: rgba(0,242,255,0.08); -fx-padding: 8 15; -fx-background-radius: 8;");
            deviceSection.getChildren().add(deviceHeader);

            for (DiskInfo disk : deviceDisks) {
                VBox driveCard = createDriveCard(disk);
                diskCards.put(disk.getMountPoint(), driveCard);
                deviceSection.getChildren().add(driveCard);
            }

            drivesContainer.getChildren().add(deviceSection);
        }
    }

    private VBox createDriveCard(DiskInfo disk) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 12;");
        card.setUserData(disk.getMountPoint());

        VBox mainRow = new VBox(10);
        mainRow.setPadding(new Insets(18, 20, 18, 20));
        mainRow.setStyle("-fx-cursor: hand;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(disk.getMountPoint() + "  " + disk.getName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00f2ff;");
        nameLabel.setId("nameLabel");

        Label typeLabel = new Label("[" + disk.getType() + "]");
        typeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #777;");

        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        double freeGB = disk.getFreeBytes() / (1024.0 * 1024 * 1024);
        String percentColor = freeGB < 15 ? "#ff3333" : "#00f2ff";

        Label percentLabel = new Label(String.format("%.1f%%", disk.getUsagePercent()));
        percentLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + percentColor + ";");
        percentLabel.setId("percentLabel");

        header.getChildren().addAll(nameLabel, typeLabel, percentLabel);

        ProgressBar bar = new ProgressBar(disk.getUsagePercent() / 100.0);
        bar.setPrefWidth(Double.MAX_VALUE);
        bar.setPrefHeight(12);
        bar.setStyle(freeGB < 15 ? "-fx-accent: #ff3333;" : "-fx-accent: #00f2ff;");
        bar.setId("progressBar");

        HBox usageRow = new HBox(25);
        usageRow.setAlignment(Pos.CENTER_LEFT);

        Label usedLabel = new Label(disk.getFormattedUsed() + " used");
        usedLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #aaa;");
        usedLabel.setId("usedLabel");

        Label freeLabel = new Label(disk.getFormattedFree() + " free");
        freeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #00ff9d;");
        freeLabel.setId("freeLabel");

        Label expandHint = new Label("[ + ] Expand");
        expandHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        HBox.setHgrow(expandHint, Priority.ALWAYS);
        expandHint.setAlignment(Pos.CENTER_RIGHT);
        expandHint.setMaxWidth(Double.MAX_VALUE);

        usageRow.getChildren().addAll(usedLabel, freeLabel, expandHint);

        mainRow.getChildren().addAll(header, bar, usageRow);

        HBox expandedPanel = createExpandedPanel(disk);
        boolean isExpanded = expandedDisks.contains(disk.getMountPoint());
        expandedPanel.setVisible(isExpanded);
        expandedPanel.setManaged(isExpanded);
        expandHint.setText(isExpanded ? "[ - ] Collapse" : "[ + ] Expand");

        mainRow.setOnMouseClicked(e -> {
            boolean isVisible = expandedPanel.isVisible();
            expandedPanel.setVisible(!isVisible);
            expandedPanel.setManaged(!isVisible);
            expandHint.setText(isVisible ? "[ + ] Expand" : "[ - ] Collapse");

            if (!isVisible) {
                expandedDisks.add(disk.getMountPoint());
            } else {
                expandedDisks.remove(disk.getMountPoint());
            }
        });

        card.getChildren().addAll(mainRow, expandedPanel);
        return card;
    }

    private void updateDriveCard(VBox card, DiskInfo disk) {
        // update labels without rebuilding
        VBox mainRow = (VBox) card.getChildren().get(0);
        HBox header = (HBox) mainRow.getChildren().get(0);
        ProgressBar bar = (ProgressBar) mainRow.getChildren().get(1);
        HBox usageRow = (HBox) mainRow.getChildren().get(2);

        double freeGB = disk.getFreeBytes() / (1024.0 * 1024 * 1024);
        String percentColor = freeGB < 15 ? "#ff3333" : "#00f2ff";

        Label percentLabel = (Label) header.getChildren().get(2);
        percentLabel.setText(String.format("%.1f%%", disk.getUsagePercent()));
        percentLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + percentColor + ";");

        bar.setProgress(disk.getUsagePercent() / 100.0);
        bar.setStyle(freeGB < 15 ? "-fx-accent: #ff3333;" : "-fx-accent: #00f2ff;");

        Label usedLabel = (Label) usageRow.getChildren().get(0);
        usedLabel.setText(disk.getFormattedUsed() + " used");

        Label freeLabel = (Label) usageRow.getChildren().get(1);
        freeLabel.setText(disk.getFormattedFree() + " free");
    }

    private HBox createExpandedPanel(DiskInfo disk) {
        HBox panel = new HBox(30);
        panel.setPadding(new Insets(20, 25, 25, 25));
        panel.setAlignment(Pos.CENTER);
        panel.setStyle("-fx-background-color: rgba(0,0,0,0.25); -fx-background-radius: 0 0 12 12;");

        double freeGB = disk.getFreeBytes() / (1024.0 * 1024 * 1024);
        boolean isLowSpace = freeGB < 15;

        VBox statsBox = new VBox(14);
        statsBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(statsBox, Priority.ALWAYS);

        addStatRow(statsBox, "Mount Point:", disk.getMountPoint(), "#00f2ff");
        addStatRow(statsBox, "File System:", disk.getType(), "#ff9900");
        addStatRow(statsBox, "Total Space:", disk.getFormattedTotal(), "#ffffff");
        addStatRow(statsBox, "Used Space:", disk.getFormattedUsed(), isLowSpace ? "#ff3333" : "#ffcc00");
        addStatRow(statsBox, "Free Space:", disk.getFormattedFree(), isLowSpace ? "#ff3333" : "#00ff9d");

        // pie chart using JavaFX PieChart
        VBox chartSection = createPieChart(disk);
        HBox.setHgrow(chartSection, Priority.ALWAYS);

        panel.getChildren().addAll(statsBox, chartSection);
        return panel;
    }

    private void addStatRow(VBox container, String label, String value, String valueColor) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);

        Label labelText = new Label(label);
        labelText.setStyle("-fx-font-size: 14px; -fx-text-fill: #888; -fx-min-width: 100;");
        labelText.setMinWidth(110);

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + valueColor + ";");

        row.getChildren().addAll(labelText, valueLabel);
        container.getChildren().add(row);
    }

    private VBox createPieChart(DiskInfo disk) {
        VBox container = new VBox(10);
        container.setAlignment(Pos.CENTER);

        double freeGB = disk.getFreeBytes() / (1024.0 * 1024 * 1024);
        double usedGB = disk.getUsedBytes() / (1024.0 * 1024 * 1024);
        boolean isLowSpace = freeGB < 15;

        PieChart pieChart = new PieChart();
        pieChart.setMaxSize(160, 160);
        pieChart.setPrefSize(160, 160);
        pieChart.setMinSize(160, 160);
        pieChart.setLabelsVisible(false);
        pieChart.setLegendVisible(false);
        pieChart.setStartAngle(90);

        PieChart.Data usedData = new PieChart.Data("Used", usedGB);
        PieChart.Data freeData = new PieChart.Data("Free", freeGB);

        pieChart.getData().addAll(usedData, freeData);

        pieChart.setStyle("-fx-background-color: transparent;");

        javafx.application.Platform.runLater(() -> {
            String usedColor = isLowSpace ? "#ff3333" : "#00f2ff";
            if (usedData.getNode() != null) {
                usedData.getNode().setStyle("-fx-pie-color: " + usedColor + ";");
                Tooltip.install(usedData.getNode(), new Tooltip(String.format("Used: %.1f GB", usedGB)));
            }
            if (freeData.getNode() != null) {
                freeData.getNode().setStyle("-fx-pie-color: #1e3a5f;");
                Tooltip.install(freeData.getNode(), new Tooltip(String.format("Free: %.1f GB", freeGB)));
            }
            // subtle border
            pieChart.lookupAll(".chart-pie").forEach(node -> {
                node.setStyle(node.getStyle() + " -fx-border-color: #0a0a14; -fx-border-width: 1;");
            });
        });

        Label percentOverlay = new Label(String.format("%.0f%%", disk.getUsagePercent()));
        percentOverlay.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        StackPane chartWithLabel = new StackPane();
        chartWithLabel.getChildren().addAll(pieChart, percentOverlay);

        // legend
        HBox legend = new HBox(15);
        legend.setAlignment(Pos.CENTER);

        HBox usedLegend = new HBox(6);
        usedLegend.setAlignment(Pos.CENTER_LEFT);
        Region usedColorBox = new Region();
        usedColorBox.setMinSize(10, 10);
        usedColorBox.setMaxSize(10, 10);
        usedColorBox.setStyle(
                "-fx-background-color: " + (isLowSpace ? "#ff3333" : "#00f2ff") + "; -fx-background-radius: 2;");
        Label usedText = new Label(String.format("%.1f GB", usedGB));
        usedText.setStyle("-fx-font-size: 11px; -fx-text-fill: #ccc;");
        usedLegend.getChildren().addAll(usedColorBox, usedText);

        HBox freeLegend = new HBox(6);
        freeLegend.setAlignment(Pos.CENTER_LEFT);
        Region freeColorBox = new Region();
        freeColorBox.setMinSize(10, 10);
        freeColorBox.setMaxSize(10, 10);
        freeColorBox.setStyle("-fx-background-color: #2a4a6a; -fx-background-radius: 2;");
        Label freeText = new Label(String.format("%.1f GB", freeGB));
        freeText.setStyle("-fx-font-size: 11px; -fx-text-fill: #ccc;");
        freeLegend.getChildren().addAll(freeColorBox, freeText);

        legend.getChildren().addAll(usedLegend, freeLegend);

        container.getChildren().addAll(pieChart, legend);
        return container;
    }
}
