package com.ho.account.closing.application.port.in;

import java.time.LocalDate;

/**
 * Read-only admission decision for ordinary journal creation and posting.
 * {@code isClosed=true} means admission is blocked, including temporary Closing controls;
 * it does not mean that the fiscal period has reached its final CLOSED state.
 */
public interface ClosingAdmissionQuery {

    /**
     * Only an explicitly OPEN master period and matching OPEN calendar without a lock allow entry.
     * Missing or invalid master data and lookup failures propagate so callers reject the operation.
     * A date alone cannot authorize a controlled adjustment exception.
     */
    boolean isClosed(LocalDate accountingDate);
}
