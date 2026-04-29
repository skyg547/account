package com.ho.account.reporting.application.port.in;

import com.ho.account.reporting.domain.model.FinancialStatement;
import java.time.LocalDateTime;

/**
 * Use case for exporting a financial statement as a downloadable document.
 */
public interface ExportStatementDocumentUseCase {

    ExportedDocument export(ExportCommand command);

    record ExportCommand(
            FinancialStatement.StatementType type,
            LocalDateTime baseDate,
            String requesterId,
            DocumentFormat format) {
    }

    record ExportedDocument(
            String fileName,
            String contentType,
            byte[] content) {
    }

    enum DocumentFormat {
        PDF,
        EXCEL
    }
}
