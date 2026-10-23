package com.ho.account.expenditure.application.port.in;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 매입채무와 선급금 상계 유즈케이스 입력값입니다.
 *
 * <p>초보자용 설명: 상계는 채무와 선급금 두 잔액을 동시에 줄이는 처리입니다.
 * 그래서 어떤 채무(payableId), 어떤 선급금(advancePaymentId), 얼마(offsetAmount)와
 * 재시도 시 유지할 작업 ID(operationId)를 함께 넘겨야 합니다.</p>
 */
public record OffsetPayableCommand(Long payableId, Long advancePaymentId, BigDecimal offsetAmount,
                                   String operationId) {
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
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException("operationId is required for a retryable offset");
        }
        operationId = operationId.trim();
        if (operationId.length() > 64) {
            throw new IllegalArgumentException("operationId must not exceed 64 characters");
        }
    }
}
