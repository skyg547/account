package com.ho.account.contracts.masterdata;

import java.util.Optional;

/**
 * Contract for querying and updating fiscal period closing status without exposing master-data entities.
 */
public interface FiscalPeriodControlPort {

    Optional<FiscalPeriodRef> findFiscalPeriodById(Long id);

    Optional<FiscalPeriodRef> findFiscalPeriod(String fiscalYear, String fiscalPeriod);

    FiscalPeriodRef updateClosingStatus(Long fiscalPeriodId, String closingStatus, String auditUser);

    /**
     * Durably reserves one exact transition payload. A retry with the same operation ID must return
     * its receipt only when every command field matches; a different payload is a conflict.
     * Preparation leaves the persisted fiscal status unchanged. Admission-facing reads may
     * conservatively report CLOSED for a prepared close; a prepared reopen must never report OPEN.
     */
    default FiscalPeriodTransitionReceipt prepareClosingTransition(FiscalPeriodTransitionCommand command) {
        throw new UnsupportedOperationException("Fiscal transition preparation is not supported by this adapter");
    }

    /**
     * Applies a prepared expected-state transition and its receipt in one provider transaction.
     * An already applied operation returns the same receipt, including after a lost HTTP response.
     * An APPLIED reopen must still read as CLOSED to admission callers until confirmed.
     */
    default FiscalPeriodTransitionReceipt applyClosingTransition(String operationId) {
        throw new UnsupportedOperationException("Fiscal transition application is not supported by this adapter");
    }

    /**
     * Confirms an APPLIED operation after Closing has durably finalized its local records. A
     * prepared reopen remains admission-facing CLOSED until this confirmation commits; repeating
     * confirmation returns the same CONFIRMED receipt after a lost response.
     */
    default FiscalPeriodTransitionReceipt confirmClosingTransition(String operationId) {
        throw new UnsupportedOperationException("Fiscal transition confirmation is not supported by this adapter");
    }

    /**
     * Terminally cancels a PREPARED operation under the provider's period/receipt lock. The
     * cancellation actor is independently authenticated, validated with the same safe 42-character
     * actor syntax as the command, and audited; APPLIED and CONFIRMED cannot be cancelled.
     * Repeating a cancellation returns the existing CANCELLED receipt without changing state.
     */
    default FiscalPeriodTransitionReceipt cancelClosingTransition(String operationId, String cancellationActor) {
        throw new UnsupportedOperationException("Fiscal transition cancellation is not supported by this adapter");
    }

    /** Returns the durable receipt used to reconcile an unknown remote outcome. */
    default Optional<FiscalPeriodTransitionReceipt> findTransitionReceipt(String operationId) {
        throw new UnsupportedOperationException("Fiscal transition receipt lookup is not supported by this adapter");
    }
}
