package com.ho.account.reporting.adapter.in.web.dto;

import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryReportSubmission;
import java.time.LocalDateTime;
import java.util.List;

/**
 * API response DTO for regulatory report submissions.
 *
 * <p>정정 제출 사유, 제출 버전, 검증 메시지는 외부 감사자가 보는 계약 정보입니다.
 * 도메인 객체를 그대로 노출하지 않고 필요한 필드만 명시적으로 내보냅니다.
 */
public record RegulatoryReportSubmissionResponseDto(
        String submissionId,
        String statementId,
        FinancialStatement.StatementType statementType,
        LocalDateTime baseDate,
        int version,
        String submittedBy,
        LocalDateTime submittedAt,
        String correctionReason,
        RegulatoryReportSubmission.SubmissionStatus status,
        List<String> validationMessages) {

    public static RegulatoryReportSubmissionResponseDto from(RegulatoryReportSubmission submission) {
        return new RegulatoryReportSubmissionResponseDto(
                submission.getSubmissionId(),
                submission.getStatementId(),
                submission.getStatementType(),
                submission.getBaseDate(),
                submission.getVersion(),
                submission.getSubmittedBy(),
                submission.getSubmittedAt(),
                submission.getCorrectionReason(),
                submission.getStatus(),
                submission.getValidationMessages());
    }
}