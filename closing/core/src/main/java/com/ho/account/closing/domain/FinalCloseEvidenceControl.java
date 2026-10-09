package com.ho.account.closing.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Immutable result supplied by one authoritative close-control provider.
 * The source run ID is lineage, not a display label, and cannot be changed after submission.
 */
public record FinalCloseEvidenceControl(
        Type type,
        String sourceSystem,
        String sourceRunId,
        Outcome outcome,
        int blockingItemCount,
        List<FinalCloseEvidenceTotal> totals) {

    public enum Outcome { PASS, FAIL }

    public enum Type {
        AP_SUBLEDGER("payable"),
        AR_SUBLEDGER("receivable"),
        LEASE_SUBLEDGER("asset-lease"),
        LOAN_SUBLEDGER("loan"),
        JOURNAL_ADJUSTMENTS("journal-ledger"),
        ECL_RECONCILIATION("ecl"),
        ANNUAL_TRANSFER("closing");

        private final String expectedSourceSystem;

        Type(String expectedSourceSystem) {
            this.expectedSourceSystem = expectedSourceSystem;
        }

        public String expectedSourceSystem() {
            return expectedSourceSystem;
        }
    }

    public FinalCloseEvidenceControl {
        type = Objects.requireNonNull(type, "type must not be null");
        sourceSystem = requireText(sourceSystem, "sourceSystem");
        sourceRunId = requireText(sourceRunId, "sourceRunId");
        outcome = Objects.requireNonNull(outcome, "outcome must not be null");
        if (blockingItemCount < 0) {
            throw new IllegalArgumentException("blockingItemCount must not be negative");
        }
        totals = totals == null ? List.of() : totals.stream()
                .map(total -> Objects.requireNonNull(total, "totals must not contain null"))
                .sorted(Comparator.comparing(FinalCloseEvidenceTotal::accountCode)
                        .thenComparing(FinalCloseEvidenceTotal::currencyCode))
                .toList();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
