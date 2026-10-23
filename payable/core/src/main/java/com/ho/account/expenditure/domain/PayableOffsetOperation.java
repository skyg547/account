package com.ho.account.expenditure.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** A committed offset event, separate from the payable that can receive many offsets. */
public record PayableOffsetOperation(String operationId, Long payableId, Long advancePaymentId,
                                     BigDecimal amount, LocalDate accountingDate, Long journalEntryId) {

    public PayableOffsetOperation {
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(payableId, "payableId");
        Objects.requireNonNull(advancePaymentId, "advancePaymentId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(accountingDate, "accountingDate");
        Objects.requireNonNull(journalEntryId, "journalEntryId");
    }

    public boolean matches(Long candidatePayableId, Long candidateAdvancePaymentId,
                           BigDecimal candidateAmount, LocalDate candidateAccountingDate) {
        return payableId.equals(candidatePayableId)
                && advancePaymentId.equals(candidateAdvancePaymentId)
                && amount.compareTo(candidateAmount) == 0
                && accountingDate.equals(candidateAccountingDate);
    }
}
