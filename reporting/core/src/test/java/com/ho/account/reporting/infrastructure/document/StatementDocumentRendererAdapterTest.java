package com.ho.account.reporting.infrastructure.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.reporting.application.port.in.ExportStatementDocumentUseCase.DocumentFormat;
import com.ho.account.reporting.application.port.out.RenderStatementDocumentPort.RenderedDocument;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.ReportLine;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class StatementDocumentRendererAdapterTest {

    private final StatementDocumentRendererAdapter adapter = new StatementDocumentRendererAdapter();

    @Test
    void render_excel_returnsCsvContent() {
        FinancialStatement statement = statement();

        RenderedDocument rendered = adapter.render(statement, DocumentFormat.EXCEL);

        String csv = new String(rendered.content(), StandardCharsets.UTF_8);
        assertThat(rendered.fileExtension()).isEqualTo("csv");
        assertThat(rendered.contentType()).isEqualTo("text/csv; charset=UTF-8");
        assertThat(csv).contains("lineCode,label,currentAmount,previousAmount,noteNumber,level");
        assertThat(csv).contains("ASSET_CASH,Cash,850000000,700000000,3,1");
    }

    @Test
    void render_pdf_returnsPdfHeader() {
        FinancialStatement statement = statement();

        RenderedDocument rendered = adapter.render(statement, DocumentFormat.PDF);

        String header = new String(rendered.content(), 0, 8, StandardCharsets.US_ASCII);
        assertThat(rendered.fileExtension()).isEqualTo("pdf");
        assertThat(rendered.contentType()).isEqualTo("application/pdf");
        assertThat(header).isEqualTo("%PDF-1.4");
    }

    private FinancialStatement statement() {
        FinancialStatement statement = new FinancialStatement(
                "ST-001",
                FinancialStatement.StatementType.BALANCE_SHEET,
                LocalDateTime.of(2026, 3, 31, 0, 0));
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
