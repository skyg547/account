package com.ho.account.expenditure.payable.api.dto;

import com.ho.account.expenditure.domain.PurchaseInvoice;
import com.ho.account.expenditure.domain.PurchaseInvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 매입 인보이스 API 응답 DTO입니다.
 *
 * <p>초보자용 설명: API 응답은 화면과 외부 클라이언트가 보는 계약입니다. JPA 엔티티를 그대로 노출하지 않고
 * 필요한 필드만 담으면, 도메인 내부 구조가 바뀌어도 API 계약을 더 안정적으로 유지할 수 있습니다.</p>
 */
public record PurchaseInvoiceResponse(
        Long id,
        String invoiceNo,
        String vendorCode,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal netAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        PurchaseInvoiceStatus status,
        Long journalEntryId,
        String description,
        String createdBy
) {
    public static PurchaseInvoiceResponse from(PurchaseInvoice invoice) {
        return new PurchaseInvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNo(),
                invoice.getVendorCode(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                invoice.getNetAmount(),
                invoice.getTaxAmount(),
                invoice.getTotalAmount(),
                invoice.getStatus(),
                invoice.getJournalEntryId(),
                invoice.getDescription(),
                invoice.getCreatedBy());
    }
}