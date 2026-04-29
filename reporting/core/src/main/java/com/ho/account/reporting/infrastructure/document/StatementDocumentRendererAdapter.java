package com.ho.account.reporting.infrastructure.document;

import com.ho.account.reporting.application.port.in.ExportStatementDocumentUseCase.DocumentFormat;
import com.ho.account.reporting.application.port.out.RenderStatementDocumentPort;
import com.ho.account.reporting.application.port.out.RenderStatementDocumentPort.RenderedDocument;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.ReportLine;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Technical adapter for rendering statement snapshots as PDF or Excel-friendly CSV.
 */
@Component
public class StatementDocumentRendererAdapter implements RenderStatementDocumentPort {

    @Override
    public RenderedDocument render(FinancialStatement statement, DocumentFormat format) {
        if (statement == null) {
            throw new IllegalArgumentException("Statement is required.");
        }

        DocumentFormat targetFormat = format != null ? format : DocumentFormat.PDF;
        return switch (targetFormat) {
            case PDF -> new RenderedDocument(renderPdf(statement), "application/pdf", "pdf");
            case EXCEL -> new RenderedDocument(
                    renderCsv(statement),
                    "text/csv; charset=UTF-8",
                    "csv");
        };
    }

    private byte[] renderCsv(FinancialStatement statement) {
        StringBuilder csv = new StringBuilder();
        csv.append("lineCode,label,currentAmount,previousAmount,noteNumber,level\n");

        for (ReportLine line : statement.getLines()) {
            csv.append(escapeCsv(line.getLineCode())).append(',')
                    .append(escapeCsv(line.getLabel())).append(',')
                    .append(escapeCsv(formatAmount(line.getCurrentAmount()))).append(',')
                    .append(escapeCsv(formatAmount(line.getPreviousAmount()))).append(',')
                    .append(escapeCsv(line.getNoteNumber())).append(',')
                    .append(line.getLevel())
                    .append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] renderPdf(FinancialStatement statement) {
        List<String> rows = new ArrayList<>();
        rows.add("Financial Statement");
        rows.add("Type: " + statement.getType().name());
        rows.add("Base date: " + statement.getBaseDate().toLocalDate());
        rows.add("Status: " + statement.getStatus().name());
        rows.add("");
        rows.add("Code | Label | Current | Previous | Note");

        for (ReportLine line : statement.getLines()) {
            rows.add(String.format(
                    Locale.ROOT,
                    "%s | %s | %s | %s | %s",
                    safePdfText(line.getLineCode()),
                    safePdfText(line.getLabel()),
                    formatAmount(line.getCurrentAmount()),
                    formatAmount(line.getPreviousAmount()),
                    safePdfText(line.getNoteNumber())));
        }

        StringBuilder stream = new StringBuilder();
        stream.append("BT\n");
        stream.append("/F1 10 Tf\n");
        stream.append("50 780 Td\n");

        for (int i = 0; i < rows.size(); i++) {
            String line = escapePdfLiteral(rows.get(i));
            stream.append('(').append(line).append(") Tj\n");
            if (i < rows.size() - 1) {
                stream.append("0 -14 Td\n");
            }
        }
        stream.append("ET\n");

        return buildSimplePdf(stream.toString());
    }

    private byte[] buildSimplePdf(String contentStream) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0); // object 0 is always free

        writeAscii(out, "%PDF-1.4\n");

        offsets.add(out.size());
        writeObject(out, 1, "<< /Type /Catalog /Pages 2 0 R >>");

        offsets.add(out.size());
        writeObject(out, 2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>");

        offsets.add(out.size());
        writeObject(out, 3, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>");

        byte[] streamBytes = contentStream.getBytes(StandardCharsets.US_ASCII);
        String streamObject = "<< /Length " + streamBytes.length + " >>\n"
                + "stream\n"
                + contentStream
                + "endstream";
        offsets.add(out.size());
        writeObject(out, 4, streamObject);

        offsets.add(out.size());
        writeObject(out, 5, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        int xrefOffset = out.size();
        writeAscii(out, "xref\n");
        writeAscii(out, "0 6\n");
        writeAscii(out, "0000000000 65535 f \n");
        for (int i = 1; i < offsets.size(); i++) {
            writeAscii(out, String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(i)));
        }
        writeAscii(out, "trailer\n");
        writeAscii(out, "<< /Size 6 /Root 1 0 R >>\n");
        writeAscii(out, "startxref\n");
        writeAscii(out, Integer.toString(xrefOffset));
        writeAscii(out, "\n%%EOF\n");

        return out.toByteArray();
    }

    private void writeObject(ByteArrayOutputStream out, int objectNumber, String body) {
        writeAscii(out, objectNumber + " 0 obj\n");
        writeAscii(out, body);
        if (!body.endsWith("\n")) {
            writeAscii(out, "\n");
        }
        writeAscii(out, "endobj\n");
    }

    private void writeAscii(ByteArrayOutputStream out, String value) {
        out.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
    }

    private String safePdfText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        StringBuilder ascii = new StringBuilder();
        for (char ch : value.toCharArray()) {
            if (ch >= 32 && ch <= 126) {
                ascii.append(ch);
            } else {
                ascii.append('?');
            }
        }
        if (ascii.length() > 100) {
            return ascii.substring(0, 100) + "...";
        }
        return ascii.toString();
    }

    private String escapePdfLiteral(String input) {
        StringBuilder escaped = new StringBuilder();
        for (char ch : input.toCharArray()) {
            if (ch == '\\' || ch == '(' || ch == ')') {
                escaped.append('\\');
            }
            escaped.append(ch);
        }
        return escaped.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatAmount(BigDecimal amount) {
        return amount != null ? amount.toPlainString() : BigDecimal.ZERO.toPlainString();
    }
}
