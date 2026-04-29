package com.ho.account.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.reporting.application.port.in.ExportStatementDocumentUseCase.DocumentFormat;
import com.ho.account.reporting.application.port.in.ExportStatementDocumentUseCase.ExportCommand;
import com.ho.account.reporting.application.port.in.GenerateStatementUseCase;
import com.ho.account.reporting.application.port.out.RenderStatementDocumentPort;
import com.ho.account.reporting.application.port.out.RenderStatementDocumentPort.RenderedDocument;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.ReportLine;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class StatementDocumentServiceTest {

    @Test
    void export_pdf_returnsRenderedPayloadWithExpectedFileName() {
        GenerateStatementUseCase generateUseCase = command -> finalizedStatement(command.baseDate());
        RenderStatementDocumentPort renderPort = (statement, format) -> new RenderedDocument(
                "pdf-bytes".getBytes(StandardCharsets.UTF_8),
                "application/pdf",
                "pdf");

        StatementDocumentService service = new StatementDocumentService(generateUseCase, renderPort);

        var result = service.export(new ExportCommand(
                FinancialStatement.StatementType.BALANCE_SHEET,
                LocalDateTime.of(2026, 3, 31, 0, 0),
                "tester",
                DocumentFormat.PDF));

        assertThat(result.fileName()).isEqualTo("balance_sheet_2026-03-31.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(new String(result.content(), StandardCharsets.UTF_8)).isEqualTo("pdf-bytes");
    }

    @Test
    void export_excel_passesFormatAndUsesRendererExtension() {
        AtomicReference<DocumentFormat> requestedFormat = new AtomicReference<>();
        GenerateStatementUseCase generateUseCase = command -> finalizedStatement(command.baseDate());
        RenderStatementDocumentPort renderPort = (statement, format) -> {
            requestedFormat.set(format);
            return new RenderedDocument(
                    "csv".getBytes(StandardCharsets.UTF_8),
                    "text/csv; charset=UTF-8",
                    "csv");
        };

        StatementDocumentService service = new StatementDocumentService(generateUseCase, renderPort);

        var result = service.export(new ExportCommand(
                FinancialStatement.StatementType.BALANCE_SHEET,
                LocalDateTime.of(2026, 3, 31, 0, 0),
                "tester",
                DocumentFormat.EXCEL));

        assertThat(requestedFormat.get()).isEqualTo(DocumentFormat.EXCEL);
        assertThat(result.fileName()).isEqualTo("balance_sheet_2026-03-31.csv");
    }

    @Test
    void export_throwsWhenGeneratedStatementHasNoLines() {
        GenerateStatementUseCase generateUseCase = command ->
                new FinancialStatement("EMPTY-1", command.type(), command.baseDate());
        RenderStatementDocumentPort renderPort = (statement, format) -> new RenderedDocument(
                new byte[0],
                "application/pdf",
                "pdf");

        StatementDocumentService service = new StatementDocumentService(generateUseCase, renderPort);

        assertThatThrownBy(() -> service.export(new ExportCommand(
                FinancialStatement.StatementType.BALANCE_SHEET,
                LocalDateTime.of(2026, 3, 31, 0, 0),
                "tester",
                DocumentFormat.PDF)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("without report lines");
    }

    private FinancialStatement finalizedStatement(LocalDateTime baseDate) {
        FinancialStatement statement = new FinancialStatement(
                "ST-001",
                FinancialStatement.StatementType.BALANCE_SHEET,
                baseDate);
        statement.addLine(new ReportLine(
                "ASSET_CASH",
                "Cash",
                new BigDecimal("850000000"),
                new BigDecimal("700000000"),
                "3",
                1));
        statement.finalizeStatement();
        return statement;
    }
}
