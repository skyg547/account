package com.ho.account.reporting.adapter.in.web.dto;

import com.ho.account.reporting.domain.model.ReportLine;
import java.math.BigDecimal;

/**
 * API response DTO for one financial statement line.
 *
 * <p>초보자 가이드: core의 `ReportLine`은 재무제표 한 줄을 표현하는 업무 객체입니다.
 * HTTP 응답은 이 DTO로 한 번 감싸서, 화면/API 계약이 도메인 객체 내부 구조에 직접 묶이지 않게 합니다.
 */
public record ReportLineResponseDto(
        String lineCode,
        String label,
        BigDecimal currentAmount,
        BigDecimal previousAmount,
        String noteNumber,
        int level) {

    public static ReportLineResponseDto from(ReportLine line) {
        return new ReportLineResponseDto(
                line.getLineCode(),
                line.getLabel(),
                line.getCurrentAmount(),
                line.getPreviousAmount(),
                line.getNoteNumber(),
                line.getLevel());
    }
}