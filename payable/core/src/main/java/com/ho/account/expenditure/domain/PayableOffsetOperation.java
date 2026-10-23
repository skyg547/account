package com.ho.account.expenditure.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** A committed offset event, separate from the payable that can receive many offsets. */
public record PayableOffsetOperation(String operationId, Long payableId, Long advancePaymentId,
                                     BigDecimal amount, Long journalEntryId) {

    public PayableOffsetOperation {
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(payableId, "payableId");
        Objects.requireNonNull(advancePaymentId, "advancePaymentId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(journalEntryId, "journalEntryId");
    }

    public boolean matches(Long candidatePayableId, Long candidateAdvancePaymentId, BigDecimal candidateAmount) {
        return payableId.equals(candidatePayableId)
                && advancePaymentId.equals(candidateAdvancePaymentId)
                && amount.compareTo(candidateAmount) == 0;
    }
}
