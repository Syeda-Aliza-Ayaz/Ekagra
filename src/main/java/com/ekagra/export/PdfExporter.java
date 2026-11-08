package com.ekagra.export;

import com.ekagra.model.Report;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Renders a {@link Report} as a clean A4 PDF with Apache PDFBox (FR-36). */
public class PdfExporter implements Exporter {

    private static final float MARGIN = 48;
    private static final Color BLUE = new Color(0x22, 0x61, 0x92);
    private static final Color CORAL = new Color(0xEF, 0x85, 0x57);
    private static final Color INK = new Color(0x1D, 0x2B, 0x38);
    private static final Color MUTED = new Color(0x5C, 0x66, 0x71);
    private static final Color LINE = new Color(0xDC, 0xD5, 0xC8);
    private static final Color TINT = new Color(0xF3, 0xF0, 0xEA);

    private final PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private PDDocument doc;
    private PDPageContentStream cs;
    private float y;
    private float width;
    private int pageNo;

    @Override
    public String extension() {
        return "pdf";
    }

    @Override
    public void export(Report report, Path file) throws IOException {
        try (PDDocument document = new PDDocument()) {
            doc = document;
            pageNo = 0;
            newPage();
            header(report);
            for (Report.Section s : report.sections()) {
                section(s);
            }
            footer();
            cs.close();
            doc.save(file.toFile());
        }
    }

    // ---------- page handling ----------

    private void newPage() throws IOException {
        if (cs != null) {
            footer();
            cs.close();
        }
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        cs = new PDPageContentStream(doc, page);
        width = page.getMediaBox().getWidth() - 2 * MARGIN;
        y = page.getMediaBox().getHeight() - MARGIN;
        pageNo++;
    }

    private void ensure(float needed) throws IOException {
        if (y - needed < MARGIN + 20) {
            newPage();
        }
    }

    private void footer() throws IOException {
        text("Ekagra  ·  page " + pageNo, MARGIN, 28, regular, 8, MUTED);
    }

    // ---------- blocks ----------

    private void header(Report r) throws IOException {
        float h = 78;
        rect(MARGIN, y - h, width, h, BLUE);
        rect(MARGIN, y - h, 6, h, CORAL);
        text(r.title(), MARGIN + 22, y - 32, bold, 22, Color.WHITE);
        text(r.subtitle(), MARGIN + 22, y - 52, regular, 11, new Color(0xDC, 0xE8, 0xF2));
        text("Generated " + r.generatedAt().format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")),
            MARGIN + 22, y - 67, regular, 8, new Color(0xA9, 0xC6, 0xDD));
        y -= h + 22;
    }

    private void section(Report.Section s) throws IOException {
        ensure(60);
        text(s.heading(), MARGIN, y, bold, 14, BLUE);
        y -= 8;
        line(MARGIN, y, MARGIN + width, LINE);
        y -= 16;
        for (String p : s.paragraphs()) {
            paragraph(p);
        }
        if (!s.stats().isEmpty()) {
            stats(s.stats());
        }
        for (Report.Bar b : s.bars()) {
            bar(b);
        }
        if (s.table() != null) {
            table(s.table());
        }
        y -= 12;
    }

    private void paragraph(String p) throws IOException {
        for (String line : wrap("•  " + p, regular, 10.5f, width)) {
            ensure(16);
            text(line, MARGIN, y, regular, 10.5f, INK);
            y -= 15;
        }
        y -= 3;
    }

    private void stats(List<Report.Stat> stats) throws IOException {
        int perRow = 4;
        float gap = 10;
        float boxW = (width - gap * (perRow - 1)) / perRow;
        float boxH = 46;
        for (int i = 0; i < stats.size(); i += perRow) {
            ensure(boxH + 10);
            for (int j = 0; j < perRow && i + j < stats.size(); j++) {
                Report.Stat st = stats.get(i + j);
                float x = MARGIN + j * (boxW + gap);
                rect(x, y - boxH, boxW, boxH, TINT);
                text(fit(st.label(), regular, 8.5f, boxW - 16), x + 8, y - 15, regular, 8.5f, MUTED);
                text(fit(st.value(), bold, 15, boxW - 16), x + 8, y - 35, bold, 15, INK);
            }
            y -= boxH + 10;
        }
    }

    private void bar(Report.Bar b) throws IOException {
        ensure(18);
        float labelW = 120;
        float valueW = 80;
        float trackW = width - labelW - valueW - 10;
        text(fit(b.label(), regular, 9.5f, labelW - 6), MARGIN, y - 9, regular, 9.5f, INK);
        rect(MARGIN + labelW, y - 11, trackW, 9, TINT);
        double frac = b.max() <= 0 ? 0 : Math.max(0, Math.min(1, b.value() / b.max()));
        Color c = b.colorHex() == null ? BLUE : Color.decode(b.colorHex());
        if (frac > 0) {
            rect(MARGIN + labelW, y - 11, (float) (trackW * frac), 9, c);
        }
        text(b.valueText(), MARGIN + labelW + trackW + 10, y - 9, bold, 9.5f, INK);
        y -= 18;
    }

    private void table(Report.Table t) throws IOException {
        int cols = t.headers().size();
        float colW = width / cols;
        float rowH = 18;
        ensure(rowH * 2);
        tableHeader(t, colW, rowH);
        boolean shade = false;
        for (List<String> row : t.rows()) {
            if (y - rowH < MARGIN + 20) {
                newPage();
                tableHeader(t, colW, rowH);
            }
            if (shade) {
                rect(MARGIN, y - rowH, width, rowH, new Color(0xFB, 0xFA, 0xF7));
            }
            for (int c = 0; c < cols && c < row.size(); c++) {
                text(fit(row.get(c), regular, 9, colW - 8), MARGIN + c * colW + 5, y - 12.5f, regular, 9, INK);
            }
            y -= rowH;
            shade = !shade;
        }
        line(MARGIN, y, MARGIN + width, LINE);
        y -= 8;
    }

    private void tableHeader(Report.Table t, float colW, float rowH) throws IOException {
        rect(MARGIN, y - rowH, width, rowH, TINT);
        for (int c = 0; c < t.headers().size(); c++) {
            text(fit(t.headers().get(c), bold, 9, colW - 8), MARGIN + c * colW + 5, y - 12.5f, bold, 9, MUTED);
        }
        y -= rowH;
    }

    // ---------- drawing primitives ----------

    private void text(String s, float x, float baseline, PDFont font, float size, Color color) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, baseline);
        cs.showText(safe(s, font));
        cs.endText();
    }

    private void rect(float x, float yBottom, float w, float h, Color color) throws IOException {
        cs.setNonStrokingColor(color);
        cs.addRect(x, yBottom, w, h);
        cs.fill();
    }

    private void line(float x1, float yy, float x2, Color color) throws IOException {
        cs.setStrokingColor(color);
        cs.setLineWidth(0.8f);
        cs.moveTo(x1, yy);
        cs.lineTo(x2, yy);
        cs.stroke();
    }

    // ---------- text helpers ----------

    /** Standard PDF fonts only cover Western characters; anything else becomes '?'. */
    private static String safe(String s, PDFont font) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (char ch : s.toCharArray()) {
            String c = String.valueOf(ch);
            try {
                font.encode(c);
                out.append(c);
            } catch (IllegalArgumentException | IOException e) {
                out.append(switch (ch) {
                    case '▲' -> "+";
                    case '▼' -> "-";
                    case '✓' -> "ok";
                    default -> "?";
                });
            }
        }
        return out.toString();
    }

    private static float widthOf(String s, PDFont font, float size) {
        try {
            return font.getStringWidth(safe(s, font)) / 1000f * size;
        } catch (IOException e) {
            return s.length() * size * 0.5f;
        }
    }

    private static String fit(String s, PDFont font, float size, float max) {
        if (s == null) {
            return "";
        }
        if (widthOf(s, font, size) <= max) {
            return s;
        }
        String t = s;
        while (t.length() > 1 && widthOf(t + "...", font, size) > max) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "...";
    }

    private static List<String> wrap(String text, PDFont font, float size, float max) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (widthOf(candidate, font, size) > max && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder("    " + word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }
}
