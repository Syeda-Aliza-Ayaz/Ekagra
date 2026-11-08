package com.ekagra.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A printable/exportable report made of sections (COMPOSITION: sections exist only inside their report).
 * Built with {@link com.ekagra.pattern.builder.ReportBuilder}, written by an Exporter (PDF or CSV).
 */
public record Report(String title, String subtitle, LocalDateTime generatedAt, List<Section> sections) {

    /** A headline number, e.g. "Focus time: 6h 20m". */
    public record Stat(String label, String value) {
    }

    /** One horizontal bar, e.g. Monday 2.5 h out of a max of 4 h. {@code colorHex} may be null. */
    public record Bar(String label, double value, double max, String valueText, String colorHex) {
    }

    public record Table(List<String> headers, List<List<String>> rows) {
    }

    public record Section(String heading, List<String> paragraphs, List<Stat> stats, List<Bar> bars, Table table) {
    }
}
