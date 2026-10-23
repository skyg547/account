package com.ho.account.expenditure.application.port.in;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 매입채무와 선급금 상계 유즈케이스 입력값입니다.
 *
 * <p>초보자용 설명: 상계는 채무와 선급금 두 잔액을 동시에 줄이는 처리입니다.
 * 그래서 어떤 채무(payableId), 어떤 선급금(advancePaymentId), 얼마(offsetAmount)와
 * 재시도 시 유지할 작업 ID(operationId)와 회계일(accountingDate)을 함께 넘겨야 합니다.</p>
 */
public record OffsetPayableCommand(Long payableId, Long advancePaymentId, BigDecimal offsetAmount,
                                   String operationId, LocalDate accountingDate) {
    public OffsetPayableCommand {
        if (payableId == null || payableId <= 0) {
            throw new IllegalArgumentException("payableId must be greater than zero");
        }
        if (advancePaymentId == null || advancePaymentId <= 0) {
            throw new IllegalArgumentException("advancePaymentId must be greater than zero");
        }
        offsetAmount = Objects.requireNonNull(offsetAmount, "offsetAmount is required");
        if (offsetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("offsetAmount must be greater than zero");
        }
        try {
            // NUMERIC(19,2) 저장 전에 정확한 금액만 허용해 저널과 잔액이 다른 값으로 반올림되지 않게 합니다.
            offsetAmount = offsetAmount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("offsetAmount must have at most two decimal places", exception);
        }
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException("operationId is required for a retryable offset");
        }
        operationId = operationId.trim();
        if (operationId.length() > 60) {
            throw new IllegalArgumentException("operationId must not exceed 60 characters");
        }
        Objects.requireNonNull(accountingDate, "accountingDate is required for a retryable offset");
    }
}
