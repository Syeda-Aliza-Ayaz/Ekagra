package com.ekagra.pattern.builder;

import com.ekagra.model.Report;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * BUILDER pattern: a report has many optional parts (stats, bars, tables, text) in any combination.
 * Instead of a constructor with a dozen parameters, the report is assembled step by step:
 *
 * <pre>
 * Report r = new ReportBuilder("Weekly report")
 *     .subtitle("Syeda · 3–9 Nov")
 *     .section("Summary").stat("Focus", "6h").stat("Score", "72")
 *     .section("Daily breakdown").table(headers, rows)
 *     .build();
 * </pre>
 */
public class ReportBuilder {

    private final String title;
    private String subtitle = "";
    private final List<Report.Section> sections = new ArrayList<>();

    // the section currently being filled
    private String heading;
    private List<String> paragraphs;
    private List<Report.Stat> stats;
    private List<Report.Bar> bars;
    private Report.Table table;

    public ReportBuilder(String title) {
        this.title = title;
    }

    public ReportBuilder subtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    /** Starts a new section; the previous one is finished automatically. */
    public ReportBuilder section(String heading) {
        flush();
        this.heading = heading;
        this.paragraphs = new ArrayList<>();
        this.stats = new ArrayList<>();
        this.bars = new ArrayList<>();
        this.table = null;
        return this;
    }

    public ReportBuilder paragraph(String text) {
        requireSection();
        paragraphs.add(text);
        return this;
    }

    public ReportBuilder stat(String label, String value) {
        requireSection();
        stats.add(new Report.Stat(label, value));
        return this;
    }

    public ReportBuilder bar(String label, double value, double max, String valueText, String colorHex) {
        requireSection();
        bars.add(new Report.Bar(label, value, max, valueText, colorHex));
        return this;
    }

    public ReportBuilder table(List<String> headers, List<List<String>> rows) {
        requireSection();
        this.table = new Report.Table(List.copyOf(headers), rows.stream().map(List::copyOf).toList());
        return this;
    }

    public Report build() {
        flush();
        if (sections.isEmpty()) {
            throw new IllegalStateException("A report needs at least one section");
        }
        return new Report(title, subtitle, LocalDateTime.now(), List.copyOf(sections));
    }

    private void requireSection() {
        if (heading == null) {
            throw new IllegalStateException("Call section(...) first");
        }
    }

    private void flush() {
        if (heading != null) {
            sections.add(new Report.Section(heading, List.copyOf(paragraphs), List.copyOf(stats), List.copyOf(bars), table));
            heading = null;
        }
    }
}
