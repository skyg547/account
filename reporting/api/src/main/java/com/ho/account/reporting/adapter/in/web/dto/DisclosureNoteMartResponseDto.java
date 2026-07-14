package com.ho.account.reporting.adapter.in.web.dto;

import com.ho.account.reporting.domain.model.DisclosureNoteMart;
import com.ho.account.reporting.domain.model.FinancialStatement;
import java.time.LocalDateTime;
import java.util.List;

/**
 * API response DTO for disclosure note marts.
 *
 * <p>주석 마트는 공시 화면이나 감독보고 제출의 입력이 됩니다. API DTO를 두면,
 * 주석 분류 도메인 규칙은 core에 남기고 외부 JSON 필드는 API 계층에서 관리할 수 있습니다.
 */
public record DisclosureNoteMartResponseDto(
        String martId,
        String statementId,
        FinancialStatement.StatementType statementType,
        LocalDateTime baseDate,
        String generatedBy,
        LocalDateTime generatedAt,
        List<DisclosureNoteMartEntryResponseDto> entries) {

    public static DisclosureNoteMartResponseDto from(DisclosureNoteMart mart) {
        return new DisclosureNoteMartResponseDto(
                mart.getMartId(),
                mart.getStatementId(),
                mart.getStatementType(),
                mart.getBaseDate(),
                mart.getGeneratedBy(),
                mart.getGeneratedAt(),
                mart.getEntries().stream()
                        .map(DisclosureNoteMartEntryResponseDto::from)
                        .toList());
    }
}