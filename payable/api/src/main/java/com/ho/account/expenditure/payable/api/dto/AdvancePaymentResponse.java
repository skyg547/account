package com.ho.account.expenditure.payable.api.dto;

import com.ho.account.expenditure.domain.AdvancePayment;
import com.ho.account.expenditure.domain.AdvancePaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 선급금 API 응답 DTO입니다. */
public record AdvancePaymentResponse(
        Long id,
        String vendorCode,
        LocalDate paymentDate,
        BigDecimal amount,
        BigDecimal outstandingAmount,
        String description,
        AdvancePaymentStatus status,
        Long journalEntryId
) {
    public static AdvancePaymentResponse from(AdvancePayment advancePayment) {
        return new AdvancePaymentResponse(
                advancePayment.getId(),
                advancePayment.getVendorCode(),
                advancePayment.getPaymentDate(),
                advancePayment.getAmount(),
                advancePayment.getOutstandingAmount(),
                advancePayment.getDescription(),
                advancePayment.getStatus(),
                advancePayment.getJournalEntryId());
    }
}