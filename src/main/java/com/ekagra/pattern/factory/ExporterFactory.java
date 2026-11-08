package com.ekagra.pattern.factory;

import com.ekagra.export.CsvExporter;
import com.ekagra.export.Exporter;
import com.ekagra.export.PdfExporter;

/** FACTORY pattern: picks the right writer for the chosen file format. */
public final class ExporterFactory {

    public enum Format {
        PDF("PDF document", "*.pdf"),
        CSV("CSV spreadsheet", "*.csv");

        private final String description;
        private final String pattern;

        Format(String description, String pattern) {
            this.description = description;
            this.pattern = pattern;
        }

        public String getDescription() { return description; }
        public String getPattern() { return pattern; }
    }

    private ExporterFactory() {
    }

    public static Exporter create(Format format) {
        return switch (format) {
            case PDF -> new PdfExporter();
            case CSV -> new CsvExporter();
        };
    }
}
