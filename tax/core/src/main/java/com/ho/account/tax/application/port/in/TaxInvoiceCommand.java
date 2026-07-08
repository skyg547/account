package com.ho.account.tax.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 세금계산서 유즈케이스로 들어오는 업무 명령입니다.
 *
 * 초보자용 설명:
 * Controller의 JSON DTO는 HTTP 입력 모양이고, 이 Command는 core 업무 흐름이 이해하는 입력 모양입니다.
 * 이렇게 분리하면 core는 Web/Jakarta Validation 같은 기술 세부사항을 몰라도 세금계산서 규칙을 처리할 수 있습니다.
 */
public record TaxInvoiceCommand(
        String issueId,
        String type,
        LocalDate issueDate,
        String businessPartnerCode,
        BigDecimal supplyAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount) {
}
