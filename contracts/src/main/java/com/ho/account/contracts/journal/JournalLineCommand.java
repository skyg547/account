package com.ho.account.contracts.journal;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/**
 * 전표 한 줄을 전달하는 계약입니다.
 *
 * <p>금액의 부호로 차대변을 추측하지 않고 {@code drcrType}을 명시합니다. 계약 단계에서는 필수
 * 형식만 검증하고, 차변/대변 합계 일치와 계정 사용 가능 여부는 journal-ledger 도메인이 검증합니다.</p>
 */
public record JournalLineCommand(
        String drcrType,
        String accountCode,
        BigDecimal amount,
        BigDecimal baseAmount,
        String departmentCode,
        String businessPartnerCode,
        String detailDescription) {

    public JournalLineCommand {
        drcrType = requireJournalSide(drcrType);
        accountCode = requireText(accountCode, "accountCode");
        amount = Objects.requireNonNull(amount, "amount must not be null");
    }

    private static String requireJournalSide(String value) {
        String normalized = requireText(value, "drcrType").toUpperCase(Locale.ROOT);
        try {
            return JournalSide.valueOf(normalized).name();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("drcrType must be DEBIT or CREDIT: " + normalized);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}