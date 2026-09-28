package com.ho.account.closing.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Immutable, append-only distributed snapshot used to authorize one final-close attempt.
 *
 * <p>The provider's set ID and source lineage are preserved. A set has no mutation or delete
 * operation; a correction is a newer set, and the newest set governs final-close validation.
 */
public record FinalCloseEvidenceSet(
        String evidenceSetId,
        Long calendarId,
        Long fiscalPeriodId,
        String fiscalYear,
        String fiscalPeriod,
        LocalDate ledgerCutoff,
        Instant observedAt,
        String submittedBy,
        String contentDigest,
        List<FinalCloseEvidenceControl> controls) {

    private static final Comparator<FinalCloseEvidenceControl> CONTROL_ORDER =
            Comparator.comparing(FinalCloseEvidenceControl::type)
                    .thenComparing(FinalCloseEvidenceControl::sourceSystem)
                    .thenComparing(FinalCloseEvidenceControl::sourceRunId);

    public FinalCloseEvidenceSet {
        evidenceSetId = requireText(evidenceSetId, "evidenceSetId");
        if (evidenceSetId.length() > 100) {
            throw new IllegalArgumentException("evidenceSetId must not exceed 100 characters");
        }
        calendarId = requirePositive(calendarId, "calendarId");
        fiscalPeriodId = requirePositive(fiscalPeriodId, "fiscalPeriodId");
        fiscalYear = requireText(fiscalYear, "fiscalYear");
        fiscalPeriod = requireText(fiscalPeriod, "fiscalPeriod");
        ledgerCutoff = Objects.requireNonNull(ledgerCutoff, "ledgerCutoff must not be null");
        observedAt = Objects.requireNonNull(observedAt, "observedAt must not be null")
                // PostgreSQL TIMESTAMP WITH TIME ZONE stores microseconds; canonicalize before hashing
                // so the submitted snapshot and its persistence round-trip have identical content.
                .truncatedTo(ChronoUnit.MICROS);
        submittedBy = requireText(submittedBy, "submittedBy");
        contentDigest = requireDigest(contentDigest);
        controls = controls == null ? List.of() : controls.stream()
                .map(control -> Objects.requireNonNull(control, "controls must not contain null"))
                .sorted(CONTROL_ORDER)
                .toList();
    }

    private static Long requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String requireDigest(String value) {
        String normalized = requireText(value, "contentDigest").toLowerCase(java.util.Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("contentDigest must be a lowercase SHA-256 hex value");
        }
        return normalized;
    }
}
