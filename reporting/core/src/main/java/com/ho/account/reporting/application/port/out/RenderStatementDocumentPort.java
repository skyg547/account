package com.ho.account.reporting.application.port.out;

import com.ho.account.reporting.application.port.in.ExportStatementDocumentUseCase.DocumentFormat;
import com.ho.account.reporting.domain.model.FinancialStatement;

/**
 * Outbound port for rendering a statement into bytes for a specific format.
 */
public interface RenderStatementDocumentPort {

    RenderedDocument render(FinancialStatement statement, DocumentFormat format);

    record RenderedDocument(
            byte[] content,
            String contentType,
            String fileExtension) {
    }
}
