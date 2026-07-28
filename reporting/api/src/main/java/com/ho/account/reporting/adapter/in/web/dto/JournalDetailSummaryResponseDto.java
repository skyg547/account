package com.ho.account.reporting.adapter.in.web.dto;

import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalSide;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * API response DTO for disclosure note drill-down rows.
 *
 * <p>드릴다운은 보고서 숫자를 만든 전표 상세를 보여주는 화면용 기능입니다.
 * contracts 객체를 그대로 직렬화하지 않고 API DTO로 감싸면, journal-ledger 계약 변경이
 * reporting HTTP 응답에 바로 새는 일을 줄일 수 있습니다.
 */
public record JournalDetailSummaryResponseDto(
        Long id,
        JournalSide side,
        String accountCode,
        String accountName,
        String accountCategory,
        LocalDate accountingDate,
        BigDecimal amount,
        BigDecimal baseAmount,
        String slipNo,
        String detailDescription,
        String headerDescription,
        String businessPartnerCode,
        String accountNo) {

    public static JournalDetailSummaryResponseDto from(JournalDetailSummary summary) {
        return new JournalDetailSummaryResponseDto(
                summary.getId(),
                summary.getSide(),
                summary.getAccountCode(),
                summary.getAccountName(),
                summary.getAccountCategory(),
                summary.getAccountingDate(),
                summary.getAmount(),
                summary.getBaseAmount(),
                summary.getSlipNo(),
                summary.getDetailDescription(),
                summary.getHeaderDescription(),
                summary.getBusinessPartnerCode(),
                summary.getAccountNo());
    }
}