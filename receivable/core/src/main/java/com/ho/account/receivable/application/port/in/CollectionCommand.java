package com.ho.account.receivable.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 고객 수납 등록 유즈케이스 입력값입니다.
 *
 * <p>초보자용 설명: 수납은 은행 입금 자료가 시스템에 들어오는 사건입니다. 이 command는
 * 수납일, 고객, 금액, 계좌/참조번호를 core 경계에서 명확하게 표현합니다.</p>
 */
public record CollectionCommand(
        LocalDate collectionDate,
        String customerCode,
        BigDecimal amount,
        String bankAccount,
        String virtualAccount,
        String referenceNo
) {
    public CollectionCommand {
        collectionDate = Objects.requireNonNull(collectionDate, "collectionDate is required");
        customerCode = requireText(customerCode, "customerCode");
        amount = Objects.requireNonNull(amount, "amount is required");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        bankAccount = trimToNull(bankAccount);
        virtualAccount = trimToNull(virtualAccount);
        referenceNo = trimToNull(referenceNo);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}