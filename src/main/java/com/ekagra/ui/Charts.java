package com.ekagra.ui;

import javafx.collections.FXCollections;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;

import java.util.List;

/**
 * Builds the app's charts with one consistent style: thin marks, recessive dashed grid, no vertical
 * grid lines, hover tooltip on every point/bar. Single-series charts hide the legend (the card title names it).
 */
public final class Charts {

    private Charts() {
    }

    /** One value per category label (e.g. per day). */
    public record Point(String label, double value, String tooltip) {
    }

    public static LineChart<String, Number> line(List<Point> points, double yMax, String styleClass) {
        CategoryAxis x = new CategoryAxis(FXCollections.observableArrayList(points.stream().map(Point::label).toList()));
        NumberAxis y = yMax > 0 ? new NumberAxis(0, yMax, yMax / 4) : new NumberAxis();
        y.setMinorTickVisible(false);
        LineChart<String, Number> chart = new LineChart<>(x, y);
        style(chart, styleClass);
        chart.setCreateSymbols(true);
        chart.getData().add(series(points));
        return chart;
    }

    public static BarChart<String, Number> bar(List<Point> points, String yLabel, String styleClass) {
        CategoryAxis x = new CategoryAxis(FXCollections.observableArrayList(points.stream().map(Point::label).toList()));
        NumberAxis y = new NumberAxis();
        y.setLabel(yLabel);
        y.setMinorTickVisible(false);
        BarChart<String, Number> chart = new BarChart<>(x, y);
        style(chart, styleClass);
        chart.setCategoryGap(Math.max(8, 60 - points.size() * 3));
        chart.getData().add(series(points));
        return chart;
    }

    private static XYChart.Series<String, Number> series(List<Point> points) {
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        for (Point p : points) {
            XYChart.Data<String, Number> d = new XYChart.Data<>(p.label(), p.value());
            tooltip(d, p.tooltip());
            s.getData().add(d);
        }
        return s;
    }

    public static void tooltip(XYChart.Data<String, Number> d, String text) {
        d.nodeProperty().addListener((o, a, node) -> {
            if (node != null && text != null) {
                Tooltip t = new Tooltip(text);
                t.setShowDelay(javafx.util.Duration.millis(80));
                Tooltip.install(node, t);
            }
        });
    }

    private static void style(XYChart<String, Number> chart, String styleClass) {
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setVerticalGridLinesVisible(false);
        chart.getStyleClass().add("ek-chart");
        if (styleClass != null) {
            chart.getStyleClass().add(styleClass);
        }
    }
}
