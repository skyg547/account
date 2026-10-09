package com.ho.account.closing.application.port.in;

import java.time.LocalDate;

/**
 * Calculates evidence-backed financial closing adjustments.
 *
 * <p>The caller supplies the persisted Closing batch row ID. It becomes part of every journal's
 * immutable lineage, so a configured amount or an unrelated job execution cannot substitute for
 * the audited financial source rows.</p>
 */
public interface FinancialClosingCalculation {

    FinancialClosingCalculationResult runFxValuation(LocalDate valuationDate, Long valuationBatchId);

    FinancialClosingCalculationResult runEclProvision(LocalDate closingDate, Long provisionBatchId);

    /** Performs the complete FX evidence preflight without creating, approving, or posting journals. */
    void validateFxValuation(LocalDate valuationDate, Long valuationBatchId);
}
