package com.ekagra.export;

import com.ekagra.model.Report;

import java.io.IOException;
import java.nio.file.Path;

/** Writes a {@link Report} to a file. Implementations: {@link PdfExporter}, {@link CsvExporter}. */
public interface Exporter {

    void export(Report report, Path file) throws IOException;

    /** File extension without the dot, e.g. "pdf". */
    String extension();
}
