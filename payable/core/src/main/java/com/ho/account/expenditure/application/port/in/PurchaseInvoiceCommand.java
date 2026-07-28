package com.ho.account.expenditure.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 매입 인보이스 생성 유즈케이스 입력값입니다.
 *
 * <p>초보자용 설명: API DTO는 HTTP JSON과 Bean Validation을 담당하고, 이 command는
 * core 업무 흐름이 실제로 필요한 값만 담습니다. Batch나 다른 모듈이 같은 유즈케이스를
 * 호출해도 금액 합계, 날짜, actor 같은 핵심 정합성은 core 경계에서 한 번 더 확인됩니다.</p>
 */
public record PurchaseInvoiceCommand(
        String invoiceNo,
        String vendorCode,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal totalAmount,
        BigDecimal taxAmount,
        BigDecimal netAmount,
        String createdBy,
        String description
) {
    public PurchaseInvoiceCommand {
        invoiceNo = requireText(invoiceNo, "invoiceNo");
        vendorCode = requireText(vendorCode, "vendorCode");
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