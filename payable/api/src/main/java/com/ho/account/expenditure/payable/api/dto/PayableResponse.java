package com.ho.account.expenditure.payable.api.dto;

import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.domain.PayableStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 매입채무 API 응답 DTO입니다. */
public record PayableResponse(
        Long id,
        String purchaseInvoiceNo,
        String purchaseInvoiceVendorCode,
        String vendorCode,
        BigDecimal originalAmount,
        BigDecimal outstandingAmount,
        LocalDate dueDate,
        PayableStatus status,
        Long journalEntryId
) {
    public static PayableResponse from(Payable payable) {
        return new PayableResponse(
                payable.getId(),
                payable.getPurchaseInvoiceNo(),
                payable.getPurchaseInvoiceVendorCode(),
                payable.getVendorCode(),
                payable.getOriginalAmount(),
                payable.getOutstandingAmount(),
                payable.getDueDate(),
                payable.getStatus(),
                payable.getJournalEntryId());
    }
}