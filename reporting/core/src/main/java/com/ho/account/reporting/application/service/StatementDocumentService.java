package com.ho.account.reporting.application.service;

import com.ho.account.reporting.application.port.in.ExportStatementDocumentUseCase;
import com.ho.account.reporting.application.port.in.GenerateStatementUseCase;
import com.ho.account.reporting.application.port.out.RenderStatementDocumentPort;
import com.ho.account.reporting.application.port.out.RenderStatementDocumentPort.RenderedDocument;
import com.ho.account.reporting.domain.model.FinancialStatement;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service that orchestrates statement generation and document rendering.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatementDocumentService implements ExportStatementDocumentUseCase {

    private final GenerateStatementUseCase generateStatementUseCase;
    private final RenderStatementDocumentPort renderStatementDocumentPort;

    @Override
    public ExportedDocument export(ExportCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Export command is required.");
        }

        DocumentFormat format = command.format() != null ? command.format() : DocumentFormat.PDF;
        FinancialStatement statement = generateStatementUseCase.generate(
                new GenerateStatementUseCase.GenerateCommand(
                        command.type(),
                        command.baseDate(),
                        command.requesterId()));

        if (statement.getLines().isEmpty()) {
            throw new IllegalStateException("Cannot export statement without report lines.");
        }

        RenderedDocument rendered = renderStatementDocumentPort.render(statement, format);
        String fileName = buildFileName(statement, rendered.fileExtension());

        return new ExportedDocument(fileName, rendered.contentType(), rendered.content());
    }

    private String buildFileName(FinancialStatement statement, String extension) {
        String typeToken = statement.getType().name().toLowerCase(Locale.ROOT);
        String dateToken = statement.getBaseDate().toLocalDate().toString();
        return typeToken + "_" + dateToken + "." + extension;
    }
}
