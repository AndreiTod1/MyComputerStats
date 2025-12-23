package org.example.ui.cpu;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.core.cpu.CpuInfo;

import java.util.ArrayList;
import java.util.List;

public class CpuCoreTable extends VBox {

    private final VBox rowsContainer;
    private final List<CoreRow> rows = new ArrayList<>();
    private SortField currentSortField = SortField.CORE;
    private boolean sortAscending = true;

    private enum SortField {
        CORE, TYPE, TEMP, MAX, LOAD, FREQ, VOLT
    }

    public CpuCoreTable() {
        this.setSpacing(0);
        this.setPadding(new Insets(10));

        HBox header = new HBox(0);
        header.setPadding(new Insets(10, 0, 10, 0));
        header.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 5 5 0 0;");

        // define column percentages
        double[] widths = { 0.1, 0.1, 0.15, 0.15, 0.15, 0.2, 0.15 };

        header.getChildren().addAll(
                createHeaderLabel("Core", SortField.CORE, widths[0]),
                createHeaderLabel("Type", SortField.TYPE, widths[1]),
                createHeaderLabel("Temp", SortField.TEMP, widths[2]),
                createHeaderLabel("Max", SortField.MAX, widths[3]),
                createHeaderLabel("Load", SortField.LOAD, widths[4]),
                createHeaderLabel("Freq", SortField.FREQ, widths[5]),
                createHeaderLabel("Volt", SortField.VOLT, widths[6]));

        rowsContainer = new VBox(0);
        this.getChildren().addAll(header, rowsContainer);
    }

    private Label createHeaderLabel(String text, SortField field, double percentWidth) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.prefWidthProperty().bind(this.widthProperty().multiply(percentWidth));
        label.setAlignment(Pos.CENTER); // center align headers
        label.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: -color-text-secondary; -fx-cursor: hand;");
        label.setOnMouseClicked(e -> handleSort(field));
        return label;
    }

    private void handleSort(SortField field) {
        if (currentSortField == field) {
            sortAscending = !sortAscending;
        } else {
            currentSortField = field;
            sortAscending = true;
        }
        sortRows();
    }

    public void update(CpuInfo info) {
        int coreCount = info.getLogicalCores();

        if (rows.size() != coreCount) {
            rows.clear();
            rowsContainer.getChildren().clear();
            for (int i = 0; i < coreCount; i++) {
                CoreRow row = new CoreRow(i, this);
                rows.add(row);
                rowsContainer.getChildren().add(row);
            }
        }

        double[] loads = info.getPerCoreLoads();
        double[] temps = info.getPerCoreTemperatures();
        double[] freqs = info.getPerCoreFrequencies();
        double[] volts = info.getPerCoreVoltages();
        double[] maxTemps = info.getPerCoreMaxTemps();
        String[] types = info.getCoreTypes();

        for (int i = 0; i < coreCount; i++) {
            CoreRow row = rows.get(i);
            row.index = i;
            row.type = (types != null && i < types.length) ? types[i] : "?";
            row.load = (loads != null && i < loads.length) ? loads[i] : 0;
            row.temp = (temps != null && i < temps.length) ? temps[i] : 0;
            row.maxTemp = (maxTemps != null && i < maxTemps.length) ? maxTemps[i] : 0;
            row.freq = (freqs != null && i < freqs.length) ? freqs[i] : 0;
            row.volt = (volts != null && i < volts.length) ? volts[i] : 0;

            row.updateDisplay();
        }

        sortRows();
    }

    private void sortRows() {
        rows.sort((r1, r2) -> {
            int result = 0;
            switch (currentSortField) {
                case CORE:
                    result = Integer.compare(r1.index, r2.index);
                    break;
                case TYPE:
                    result = r1.type.compareTo(r2.type);
                    break;
                case TEMP:
                    result = Double.compare(r1.temp, r2.temp);
                    break;
                case MAX:
                    result = Double.compare(r1.maxTemp, r2.maxTemp);
                    break;
                case LOAD:
                    result = Double.compare(r1.load, r2.load);
                    break;
                case FREQ:
                    result = Double.compare(r1.freq, r2.freq);
                    break;
                case VOLT:
                    result = Double.compare(r1.volt, r2.volt);
                    break;
            }
            return sortAscending ? result : -result;
        });

        rowsContainer.getChildren().clear();
        rowsContainer.getChildren().addAll(rows);
    }

    public String getColorForTemp(double temp) {
        if (temp < 60) {
            return "-fx-text-fill: #00f2ff;"; // normal
        } else if (temp < 75) {
            return "-fx-text-fill: #ffff00;"; // yellow (warm)
        } else if (temp < 85) {
            return "-fx-text-fill: #ff8800;"; // orange (hot)
        } else if (temp < 95) {
            return "-fx-text-fill: #ff4400;"; // red-orange (danger)
        } else {
            return "-fx-text-fill: #ff0000; -fx-font-weight: bold;"; // red (critical)
        }
    }

    private static class CoreRow extends HBox {
        int index;
        String type;
        double load, temp, maxTemp, freq, volt;
        CpuCoreTable parentTable;

        private final Label lblCore, lblType, lblTemp, lblMax, lblLoad, lblFreq, lblVolt;

        private final double[] widths = { 0.1, 0.1, 0.15, 0.15, 0.15, 0.2, 0.15 };

        public CoreRow(int index, CpuCoreTable parent) {
            this.index = index;
            this.parentTable = parent;
            this.setSpacing(0);
            this.setPadding(new Insets(12, 0, 12, 0));
            this.setStyle("-fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 0 0 1 0;");
            this.setAlignment(Pos.CENTER_LEFT);

            String valueStyle = "-fx-font-family: 'Consolas', 'Monospaced'; -fx-font-size: 16px;";
            String labelStyle = "-fx-font-size: 16px;";

            lblCore = createLabel("-color-text-secondary", widths[0], parent, labelStyle);
            lblType = createLabel("-color-text-secondary", widths[1], parent, labelStyle);
            lblTemp = createLabel("-color-text-primary", widths[2], parent, valueStyle);
            lblMax = createLabel("-color-accent-warning", widths[3], parent, valueStyle);
            lblLoad = createLabel("-color-text-primary", widths[4], parent, valueStyle);
            lblFreq = createLabel("-color-accent-primary", widths[5], parent, valueStyle);
            lblVolt = createLabel("-color-accent-warning", widths[6], parent, valueStyle);

            this.getChildren().addAll(lblCore, lblType, lblTemp, lblMax, lblLoad, lblFreq, lblVolt);
        }

        private Label createLabel(String colorVar, double percent, CpuCoreTable parent, String extraStyle) {
            Label l = new Label();
            l.setMaxWidth(Double.MAX_VALUE);
            // bind width to parent width * percent
            l.prefWidthProperty().bind(parent.widthProperty().multiply(percent));
            l.setAlignment(Pos.CENTER); // center align values
            l.setStyle(extraStyle + " -fx-text-fill: " + colorVar + ";");
            return l;
        }

        public void updateDisplay() {
            lblCore.setText("C" + index);
            lblType.setText(type);

            // temp with gradient
            lblTemp.setText(String.format("%.0f°C", temp));
            lblTemp.setStyle(parentTable.getColorForTemp(temp)
                    + " -fx-font-family: 'Consolas', 'Monospaced'; -fx-font-size: 16px;");

            // max temp with warning
            String warning = (maxTemp > 90) ? " (!)" : "";
            lblMax.setText(String.format("%.0f°C%s", maxTemp, warning));

            // specific style for max temp warning
            if (maxTemp > 90) {
                lblMax.setStyle(
                        "-fx-text-fill: #ff0000; -fx-font-weight: bold; -fx-font-family: 'Consolas', 'Monospaced'; -fx-font-size: 16px;");
            } else {
                lblMax.setStyle(parentTable.getColorForTemp(maxTemp)
                        + " -fx-font-family: 'Consolas', 'Monospaced'; -fx-font-size: 16px;");
            }

            lblLoad.setText(String.format("%.0f%%", load * 100));
            lblFreq.setText(String.format("%.2f GHz", freq));
            lblVolt.setText(String.format("%.3fV", volt));
        }
    }
}
