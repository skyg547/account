package com.ho.account.reporting.adapter.in.web.dto;

import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryFiling;
import java.time.LocalDateTime;
import java.util.List;

/**
 * API response DTO for regulatory filing execution results.
 *
 * <p>외부 제출 결과는 접수증/상태/라인 목록이 API 계약입니다. core 도메인은 제출 규칙을 지키고,
 * Controller는 이 DTO로 외부 응답 모양을 고정합니다.
 */
public record RegulatoryFilingResponseDto(
        String filingId,
        String submissionId,
        FinancialStatement.StatementType statementType,
        LocalDateTime baseDate,
        int submissionVersion,
        String targetAgency,
        String submittedBy,
        LocalDateTime submittedAt,
        RegulatoryFiling.FilingStatus status,
        String regulatorReceiptId,
        String regulatorMessage,
        List<RegulatoryFilingLineResponseDto> lines) {

    public static RegulatoryFilingResponseDto from(RegulatoryFiling filing) {
        return new RegulatoryFilingResponseDto(
                filing.getFilingId(),
                filing.getSubmissionId(),
                filing.getStatementType(),
                filing.getBaseDate(),
                filing.getSubmissionVersion(),
                filing.getTargetAgency(),
                filing.getSubmittedBy(),
                filing.getSubmittedAt(),
                filing.getStatus(),
                filing.getRegulatorReceiptId(),
                filing.getRegulatorMessage(),
                filing.getLines().stream()
                        .map(RegulatoryFilingLineResponseDto::from)
                        .toList());
    }
}