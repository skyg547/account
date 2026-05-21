package com.ho.account.reporting.adapter.in.web;

import com.ho.account.reporting.application.port.in.ExportStatementDocumentUseCase;
import com.ho.account.reporting.application.port.in.GenerateStatementUseCase;
import com.ho.account.reporting.application.port.in.SubmitRegulatoryReportUseCase;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryReportSubmission;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound web adapter for reporting requests.
 */
@RestController
@RequestMapping("/api/v1/reporting")
@RequiredArgsConstructor
public class ReportingController {

    private final GenerateStatementUseCase generateStatementUseCase;
    private final ExportStatementDocumentUseCase exportStatementDocumentUseCase;
    private final SubmitRegulatoryReportUseCase submitRegulatoryReportUseCase;

    @PostMapping("/generate")
    public FinancialStatement generateStatement(
            @RequestParam("type") FinancialStatement.StatementType type,
            @RequestParam("baseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baseDate,
            @RequestHeader("X-User-ID") String userId) {
        GenerateStatementUseCase.GenerateCommand command = new GenerateStatementUseCase.GenerateCommand(
                type,
                baseDate,
                userId);
        return generateStatementUseCase.generate(command);
    }

    @PostMapping("/generate/document")
    public ResponseEntity<byte[]> generateStatementDocument(
            @RequestParam("type") FinancialStatement.StatementType type,
            @RequestParam("baseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baseDate,
            @RequestParam(value = "format", defaultValue = "PDF")
            ExportStatementDocumentUseCase.DocumentFormat format,
            @RequestHeader("X-User-ID") String userId) {
        ExportStatementDocumentUseCase.ExportCommand command =
                new ExportStatementDocumentUseCase.ExportCommand(type, baseDate, userId, format);
        ExportStatementDocumentUseCase.ExportedDocument document =
                exportStatementDocumentUseCase.export(command);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.fileName() + "\"")
                .contentLength(document.content().length)
                .body(document.content());
    }

    @PostMapping("/submissions/regulatory")
    public RegulatoryReportSubmission submitRegulatoryReport(
            @RequestParam("type") FinancialStatement.StatementType type,
            @RequestParam("baseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baseDate,
            @RequestParam(value = "correctionReason", required = false) String correctionReason,
            @RequestHeader("X-User-ID") String userId) {
        SubmitRegulatoryReportUseCase.SubmitCommand command =
                new SubmitRegulatoryReportUseCase.SubmitCommand(type, baseDate, userId, correctionReason);
        return submitRegulatoryReportUseCase.submit(command);
    }
}
