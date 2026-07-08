package com.ho.account.receivable.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 매출 인보이스 생성 유즈케이스 입력값입니다.
 *
 * <p>초보자용 설명: API DTO는 HTTP JSON 검증을 담당하고, 이 command는 core 업무 흐름이
 * 실제로 필요한 값만 담습니다. 금액 합계, 날짜, actor 같은 핵심 정합성은 API를 거치지 않는
 * Batch/내부 호출에서도 동일하게 지켜집니다.</p>
 */
public record SalesInvoiceCommand(
        String invoiceNo,
        String customerCode,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal totalAmount,
        BigDecimal taxAmount,
        BigDecimal netAmount,
        String createdBy,
        String description
) {
    public SalesInvoiceCommand {
        invoiceNo = requireText(invoiceNo, "invoiceNo");
        customerCode = requireText(customerCode, "customerCode");
        issueDate = Objects.requireNonNull(issueDate, "issueDate is required");
        dueDate = Objects.requireNonNull(dueDate, "dueDate is required");
        totalAmount = requirePositive(totalAmount, "totalAmount");
        taxAmount = requireZeroOrPositive(taxAmount, "taxAmount");
        netAmount = requirePositive(netAmount, "netAmount");
        createdBy = requireText(createdBy, "createdBy");
        if (dueDate.isBefore(issueDate)) {
            throw new IllegalArgumentException("dueDate cannot be before issueDate");
        }
        if (totalAmount.compareTo(netAmount.add(taxAmount)) != 0) {
            throw new IllegalArgumentException("totalAmount must equal netAmount + taxAmount");
        }
        if (description != null) {
            description = description.trim();
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        return value;
    }

    private static BigDecimal requireZeroOrPositive(BigDecimal value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or positive");
        }
        return value;
    }
}