package com.ekagra.export;

import com.ekagra.model.Report;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * CSV export (FR-37). A report with a single table becomes a plain CSV that opens cleanly in Excel;
 * multi-section reports get a title row per section.
 */
public class CsvExporter implements Exporter {

    @Override
    public void export(Report report, Path file) throws IOException {
        List<Report.Section> withData = report.sections().stream()
            .filter(s -> s.table() != null || !s.stats().isEmpty()).toList();
        boolean single = withData.size() == 1 && withData.get(0).table() != null;
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write('﻿');   // BOM so Excel detects UTF-8
            for (Report.Section s : withData) {
                if (!single) {
                    line(w, List.of(s.heading()));
                }
                for (Report.Stat st : s.stats()) {
                    line(w, List.of(st.label(), st.value()));
                }
                if (s.table() != null) {
                    line(w, s.table().headers());
                    for (List<String> row : s.table().rows()) {
                        line(w, row);
                    }
                }
                if (!single) {
                    w.write("\r\n");
                }
            }
        }
    }

    @Override
    public String extension() {
        return "csv";
    }

    private static void line(Writer w, List<String> cells) throws IOException {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                w.write(',');
            }
            w.write(escape(cells.get(i)));
        }
        w.write("\r\n");
    }

    /** RFC 4180: quote fields containing comma, quote or newline; double any quotes. */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
