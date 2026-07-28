package com.ho.account.reporting.adapter.in.web.dto;

import com.ho.account.reporting.domain.model.FinancialStatement;
import java.time.LocalDateTime;
import java.util.List;

/**
 * API response DTO for generated financial statements.
 *
 * <p>업무 흐름: Controller -> use case -> domain result -> response DTO.
 * core는 재무제표 생성 규칙을 지키고, API는 외부에 보여줄 JSON 모양만 책임집니다.
 */
public record FinancialStatementResponseDto(
        String statementId,
        FinancialStatement.StatementType type,
        LocalDateTime baseDate,
        List<ReportLineResponseDto> lines,
        FinancialStatement.StatementStatus status) {

    public static FinancialStatementResponseDto from(FinancialStatement statement) {
        return new FinancialStatementResponseDto(
                statement.getStatementId(),
                statement.getType(),
                statement.getBaseDate(),
                statement.getLines().stream()
                        .map(ReportLineResponseDto::from)
                        .toList(),
                statement.getStatus());
    }
}