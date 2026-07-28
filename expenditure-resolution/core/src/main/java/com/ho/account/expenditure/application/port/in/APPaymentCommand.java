package com.ho.account.expenditure.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AP 지급 유즈케이스 입력 명령입니다.
 * API 요청 DTO와 분리해 core가 HTTP 입력 모양에 의존하지 않도록 합니다.
 */
public record APPaymentCommand(
        Long expenditureResolutionId,
        Long taxInvoiceId,
        LocalDateTime paymentDate,
        BigDecimal amount,
        String paymentMethod) {
}