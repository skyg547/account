package com.ho.account.expenditure.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 선급금 등록 유즈케이스 입력값입니다.
 *
 * <p>초보자용 설명: 선급금은 물건이나 서비스를 받기 전에 먼저 지급한 돈입니다.
 * 이 command는 선급금 원장에 필요한 거래처, 지급일, 금액을 core 경계에서 명확히 표현합니다.</p>
 */
public record AdvancePaymentCommand(
        String vendorCode,
        LocalDate paymentDate,
        BigDecimal amount,
        String description,
        String operationId
) {
    /** Legacy Java callers remain source-compatible but must supply a stable business key to execute. */
    @Deprecated
    public AdvancePaymentCommand(String vendorCode, LocalDate paymentDate, BigDecimal amount, String description) {
        this(vendorCode, paymentDate, amount, description, null);
    }

    public AdvancePaymentCommand {
        if (operationId == null || operationId.isBlank() || operationId.trim().length() > 100) {
            throw new IllegalArgumentException("operationId is required and must not exceed 100 characters");
        }
        operationId = operationId.trim();
        if (vendorCode == null || vendorCode.isBlank()) {
            throw new IllegalArgumentException("vendorCode is required");
        }
        vendorCode = vendorCode.trim();
        paymentDate = Objects.requireNonNull(paymentDate, "paymentDate is required");
        amount = Objects.requireNonNull(amount, "amount is required");
        amount = exactAmount(amount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (description != null) {
            description = description.trim();
        }
    }
    private static BigDecimal exactAmount(BigDecimal value) {
        try {
            BigDecimal normalized = value.setScale(2, java.math.RoundingMode.UNNECESSARY);
            if (normalized.precision() > 19) throw new IllegalArgumentException("Amount exceeds NUMERIC(19,2)");
            return normalized;
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Amount must be exactly representable at scale 2", exception);
        }
    }
}
