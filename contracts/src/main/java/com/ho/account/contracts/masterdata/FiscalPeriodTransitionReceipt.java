package com.ho.account.contracts.masterdata;

import java.util.Objects;

/** Durable provider outcome; all command fields are retained for exact-payload reconciliation. */
public record FiscalPeriodTransitionReceipt(
        String operationId,
        Long fiscalPeriodId,
        FiscalPeriodClosingStatus expectedStatus,
        FiscalPeriodClosingStatus targetStatus,
        String auditUser,
        Phase phase) {

    /** CONFIRMED and CANCELLED are terminal; both retain the original command for reconciliation. */
    public enum Phase { PREPARED, APPLIED, CONFIRMED, CANCELLED }

    public FiscalPeriodTransitionReceipt {
        new FiscalPeriodTransitionCommand(operationId, fiscalPeriodId, expectedStatus, targetStatus, auditUser);
        Objects.requireNonNull(phase, "phase");
    }

    /** A receipt is usable for recovery only when it belongs to the complete local command. */
    public boolean matches(FiscalPeriodTransitionCommand command) {
        return command != null
                && operationId.equals(command.operationId())
                && fiscalPeriodId.equals(command.fiscalPeriodId())
                && expectedStatus == command.expectedStatus()
                && targetStatus == command.targetStatus()
                && auditUser.equals(command.auditUser());
    }
}
