package com.ho.account.expenditure.payable.api.dto;

import com.ho.account.expenditure.domain.Payment;
import com.ho.account.expenditure.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 지급 API 응답 DTO입니다. */
public record PaymentResponse(
        Long id,
        LocalDate paymentDate,
        String vendorCode,
        Long payableId,
        BigDecimal amount,
        String bankAccount,
        String referenceNo,
        int executionAttempts,
        String failureReason,
        PaymentStatus status,
        Long journalEntryId,
        Long paymentRunId
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentDate(),
                payment.getVendorCode(),
                payment.getPayableId(),
                payment.getAmount(),
                payment.getBankAccount(),
                payment.getReferenceNo(),
                payment.getExecutionAttempts(),
                payment.getFailureReason(),
                payment.getStatus(),
                payment.getJournalEntryId(),
                payment.getPaymentRun() == null ? null : payment.getPaymentRun().getId());
    }
}