package com.ho.account.reporting.adapter.in.web;

import com.ho.account.reporting.adapter.in.web.dto.DisclosureNoteMartResponseDto;
import com.ho.account.reporting.adapter.in.web.dto.FinancialStatementResponseDto;
import com.ho.account.reporting.adapter.in.web.dto.JournalDetailSummaryResponseDto;
import com.ho.account.reporting.adapter.in.web.dto.RegulatoryFilingResponseDto;
import com.ho.account.reporting.adapter.in.web.dto.RegulatoryReportSubmissionResponseDto;
import com.ho.account.reporting.application.port.in.DisclosureNoteMartUseCase;
import com.ho.account.reporting.application.port.in.ExportStatementDocumentUseCase;
import com.ho.account.reporting.application.port.in.GenerateStatementUseCase;
import com.ho.account.reporting.application.port.in.SubmitRegulatoryFilingUseCase;
import com.ho.account.reporting.application.port.in.SubmitRegulatoryReportUseCase;
import com.ho.account.reporting.domain.model.FinancialStatement;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound web adapter for reporting requests.
 *
 * <p>초보자 가이드: Controller는 HTTP 요청을 core command로 바꾸고,
 * core가 돌려준 도메인 결과를 API response DTO로 변환하는 입구입니다.
 * 보고서 합산, 제출 검증, 주석 마트 분류 같은 업무 판단은 core use case가 담당합니다.
 */
@RestController
@RequestMapping("/api/v1/reporting")
@RequiredArgsConstructor
public class ReportingController {

    private final GenerateStatementUseCase generateStatementUseCase;
    private final ExportStatementDocumentUseCase exportStatementDocumentUseCase;
    private final SubmitRegulatoryReportUseCase submitRegulatoryReportUseCase;
    private final DisclosureNoteMartUseCase disclosureNoteMartUseCase;
    private final SubmitRegulatoryFilingUseCase submitRegulatoryFilingUseCase;

    @PostMapping("/generate")
    public FinancialStatementResponseDto generateStatement(
            @RequestParam("type") FinancialStatement.StatementType type,
            @RequestParam("baseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baseDate,
            @RequestHeader("X-User-ID") String userId) {
        GenerateStatementUseCase.GenerateCommand command = new GenerateStatementUseCase.GenerateCommand(
                type,
                baseDate,
                userId);
        return FinancialStatementResponseDto.from(generateStatementUseCase.generate(command));
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
    public RegulatoryReportSubmissionResponseDto submitRegulatoryReport(
            @RequestParam("type") FinancialStatement.StatementType type,
            @RequestParam("baseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baseDate,
            @RequestParam(value = "correctionReason", required = false) String correctionReason,
            @RequestHeader("X-User-ID") String userId) {
        SubmitRegulatoryReportUseCase.SubmitCommand command =
                new SubmitRegulatoryReportUseCase.SubmitCommand(type, baseDate, userId, correctionReason);
        return RegulatoryReportSubmissionResponseDto.from(submitRegulatoryReportUseCase.submit(command));
    }

    @PostMapping("/disclosure-notes/generate")
    public DisclosureNoteMartResponseDto generateDisclosureNoteMart(
            @RequestParam("type") FinancialStatement.StatementType type,
            @RequestParam("baseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baseDate,
            @RequestHeader("X-User-ID") String userId) {
        DisclosureNoteMartUseCase.GenerateCommand command =
                new DisclosureNoteMartUseCase.GenerateCommand(type, baseDate, userId);
        return DisclosureNoteMartResponseDto.from(disclosureNoteMartUseCase.generate(command));
    }

    @GetMapping("/disclosure-notes")
    public ResponseEntity<DisclosureNoteMartResponseDto> findDisclosureNoteMart(
            @RequestParam("type") FinancialStatement.StatementType type,
            @RequestParam("baseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baseDate) {
        DisclosureNoteMartUseCase.FindQuery query = new DisclosureNoteMartUseCase.FindQuery(type, baseDate);
        return disclosureNoteMartUseCase.find(query)
                .map(DisclosureNoteMartResponseDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/disclosure-notes/drill-down")
    public ResponseEntity<List<JournalDetailSummaryResponseDto>> drillDownDisclosureNote(
            @RequestParam("type") FinancialStatement.StatementType type,
            @RequestParam("baseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baseDate,
            @RequestParam("entryId") String entryId) {
        DisclosureNoteMartUseCase.DrillDownQuery query = new DisclosureNoteMartUseCase.DrillDownQuery(type, baseDate, entryId);
        try {
            return ResponseEntity.ok(disclosureNoteMartUseCase.drillDown(query).stream()
                    .map(JournalDetailSummaryResponseDto::from)
                    .toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/regulatory-filings/submit")
    public RegulatoryFilingResponseDto submitRegulatoryFiling(
            @RequestParam("type") FinancialStatement.StatementType type,
            @RequestParam("baseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baseDate,
            @RequestParam(value = "targetAgency", defaultValue = "FSS") String targetAgency,
            @RequestHeader("X-User-ID") String userId) {
        SubmitRegulatoryFilingUseCase.SubmitCommand command =
                new SubmitRegulatoryFilingUseCase.SubmitCommand(type, baseDate, userId, targetAgency);
        return RegulatoryFilingResponseDto.from(submitRegulatoryFilingUseCase.submit(command));
    }

    @GetMapping("/regulatory-filings/latest")
    public ResponseEntity<RegulatoryFilingResponseDto> findLatestRegulatoryFiling(
            @RequestParam("type") FinancialStatement.StatementType type,
            @RequestParam("baseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baseDate) {
        SubmitRegulatoryFilingUseCase.FindLatestQuery query =
                new SubmitRegulatoryFilingUseCase.FindLatestQuery(type, baseDate);
        return submitRegulatoryFilingUseCase.findLatest(query)
                .map(RegulatoryFilingResponseDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}