package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.in.FinalCloseEvidenceUseCase;
import com.ho.account.closing.application.port.out.ClosingAggregatePersistencePort;
import com.ho.account.closing.application.port.out.FinalCloseEvidencePersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.FinalCloseEvidenceControl;
import com.ho.account.closing.domain.FinalCloseEvidencePolicy;
import com.ho.account.closing.domain.FinalCloseEvidenceSet;
import com.ho.account.closing.domain.FinalCloseEvidenceTotal;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records immutable snapshots and applies the mandatory evidence gate for final close. */
@Service
public class FinalCloseEvidenceService implements FinalCloseEvidenceUseCase {

    private final ClosingAggregatePersistencePort aggregates;
    private final FinalCloseEvidencePersistencePort evidence;
    private final FinalCloseEvidenceProperties properties;
    private final Clock clock;

    public FinalCloseEvidenceService(
            ClosingAggregatePersistencePort aggregates,
            FinalCloseEvidencePersistencePort evidence,
            FinalCloseEvidenceProperties properties,
            @Qualifier("finalCloseEvidenceClock") Clock clock) {
        this.aggregates = Objects.requireNonNull(aggregates, "aggregates must not be null");
        this.evidence = Objects.requireNonNull(evidence, "evidence must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null").withZone(ZoneOffset.UTC);
    }

    @Override
    @Transactional
    public FinalCloseEvidenceSet record(Long calendarId, Submission submission, String trustedSubmittedBy) {
        Objects.requireNonNull(submission, "submission must not be null");
        String actor = requireText(trustedSubmittedBy, "trustedSubmittedBy");
        ClosingCalendar calendar = aggregates.lockCalendar(calendarId)
                .orElseThrow(() -> new EntityNotFoundException("ClosingCalendar not found with id: " + calendarId));
        calendar.requireNoTransition();
        if (calendar.getStatus() != ClosingCalendarStatus.IN_PROGRESS) {
            throw new FinalCloseEvidenceValidationException("calendar must be IN_PROGRESS when evidence is submitted");
        }
        if (!Objects.equals(calendarId, submission.calendarId())
                || !Objects.equals(calendar.getFiscalYear(), submission.fiscalYear())
                || !Objects.equals(calendar.getFiscalPeriod(), submission.fiscalPeriod())) {
            throw new FinalCloseEvidenceValidationException("submitted calendar identity does not match the locked calendar");
        }

        FinalCloseEvidenceSet candidate = toEvidenceSet(submission, actor);
        requireDigest(candidate);
        // Latest selection orders by observedAt. Reject a future provider timestamp at admission
        // so it cannot pin the calendar ahead of later, valid snapshots.
        if (submission.observedAt().isAfter(clock.instant())) {
            throw rejected("snapshot observation time is in the future");
        }
        // The database owns the race-safe same-set/same-content idempotency decision.
        return evidence.append(candidate);
    }

    /**
     * Validates and returns the exact evidence set that must be bound to the CLOSED transition.
     * This method is called while the same calendar row is locked by the transition transaction.
     */
    public FinalCloseEvidenceSet requireForFinalClose(ClosingCalendar calendar, FiscalPeriodRef period) {
        Objects.requireNonNull(calendar, "calendar must not be null");
        Objects.requireNonNull(period, "period must not be null");
        FinalCloseEvidenceSet selected = evidence.findLatestByCalendarId(calendar.getId())
                .orElseThrow(() -> rejected("no evidence set exists for the calendar"));

        requireEligible(calendar, period, selected);
        return selected;
    }

    /** Rechecks the original bound set under the calendar lock before the first remote dispatch. */
    public FinalCloseEvidenceSet requireBoundForFinalClose(
            ClosingCalendar calendar, FiscalPeriodRef period, String evidenceSetId) {
        Objects.requireNonNull(calendar, "calendar must not be null");
        Objects.requireNonNull(period, "period must not be null");
        if (evidenceSetId == null || evidenceSetId.isBlank()) {
            throw rejected("prepared transition has no bound evidence set ID");
        }
        FinalCloseEvidenceSet selected = evidence.findByEvidenceSetId(evidenceSetId)
                .orElseThrow(() -> rejected("bound evidence set does not exist: " + evidenceSetId));
        if (!evidenceSetId.equals(selected.evidenceSetId())) {
            throw rejected("loaded evidence set does not match the bound ID");
        }
        requireEligible(calendar, period, selected);
        return selected;
    }

    private void requireEligible(ClosingCalendar calendar, FiscalPeriodRef period, FinalCloseEvidenceSet selected) {
        requireDigest(selected);
        requireIdentity(calendar, period, selected);
        requireFreshness(calendar, selected);
        try {
            FinalCloseEvidencePolicy.requireReconciled(period.fiscalPeriod(), selected.controls());
        } catch (FinalCloseEvidencePolicy.Violation violation) {
            throw rejected(violation.getMessage());
        }
    }

    /** Computes the digest providers must send, including the trusted service identity. */
    public static String computeContentDigest(Submission submission, String trustedSubmittedBy) {
        Objects.requireNonNull(submission, "submission must not be null");
        String actor = requireText(trustedSubmittedBy, "trustedSubmittedBy");
        return computeDigest(new FinalCloseEvidenceSet(
                submission.evidenceSetId(),
                submission.calendarId(),
                submission.fiscalPeriodId(),
                submission.fiscalYear(),
                submission.fiscalPeriod(),
                submission.ledgerCutoff(),
                submission.observedAt(),
                actor,
                "0".repeat(64),
                submission.controls()));
    }

    private static FinalCloseEvidenceSet toEvidenceSet(Submission submission, String actor) {
        return new FinalCloseEvidenceSet(
                submission.evidenceSetId(),
                submission.calendarId(),
                submission.fiscalPeriodId(),
                submission.fiscalYear(),
                submission.fiscalPeriod(),
                submission.ledgerCutoff(),
                submission.observedAt(),
                actor,
                submission.contentDigest(),
                submission.controls());
    }

    private void requireDigest(FinalCloseEvidenceSet selected) {
        String computed = computeDigest(selected);
        if (!MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.US_ASCII),
                selected.contentDigest().getBytes(StandardCharsets.US_ASCII))) {
            throw rejected("content digest does not match the canonical snapshot");
        }
    }

    private void requireIdentity(
            ClosingCalendar calendar,
            FiscalPeriodRef period,
            FinalCloseEvidenceSet selected) {
        if (!Objects.equals(selected.calendarId(), calendar.getId())
                || !Objects.equals(selected.fiscalPeriodId(), period.id())
                || !Objects.equals(selected.fiscalYear(), period.fiscalYear())
                || !Objects.equals(selected.fiscalPeriod(), period.fiscalPeriod())) {
            throw rejected("calendar or fiscal-period identity does not match Master");
        }
        if (!Objects.equals(selected.ledgerCutoff(), period.endDate())) {
            throw rejected("ledger cutoff must equal the authoritative fiscal-period end date");
        }
    }

    private void requireFreshness(ClosingCalendar calendar, FinalCloseEvidenceSet selected) {
        if (calendar.getCloseInitiatedAt() == null) {
            throw rejected("calendar has no close initiation time");
        }
        Instant now = clock.instant();
        Instant initiatedAt = calendar.getCloseInitiatedAt().toInstant(ZoneOffset.UTC);
        if (!selected.observedAt().isAfter(initiatedAt)) {
            throw rejected("snapshot must be observed after the current close initiation");
        }
        if (selected.observedAt().isAfter(now)) {
            throw rejected("snapshot observation time is in the future");
        }
        if (selected.observedAt().isBefore(now.minus(properties.getMaxAge()))) {
            throw rejected("snapshot is older than the configured maximum age");
        }
    }

    private static String computeDigest(FinalCloseEvidenceSet selected) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, "FINAL_CLOSE_EVIDENCE_V1");
            update(digest, selected.evidenceSetId());
            update(digest, selected.calendarId().toString());
            update(digest, selected.fiscalPeriodId().toString());
            update(digest, selected.fiscalYear());
            update(digest, selected.fiscalPeriod());
            update(digest, selected.ledgerCutoff().toString());
            update(digest, selected.observedAt().toString());
            update(digest, selected.submittedBy());
            update(digest, Integer.toString(selected.controls().size()));
            for (FinalCloseEvidenceControl control : selected.controls()) {
                update(digest, control.type().name());
                update(digest, control.sourceSystem());
                update(digest, control.sourceRunId());
                update(digest, control.outcome().name());
                update(digest, Integer.toString(control.blockingItemCount()));
                update(digest, Integer.toString(control.totals().size()));
                for (FinalCloseEvidenceTotal total : control.totals()) {
                    update(digest, total.accountCode());
                    update(digest, total.currencyCode());
                    update(digest, canonical(total.sourceTotal()));
                    update(digest, canonical(total.postedTotal()));
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static String canonical(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.signum() == 0 ? "0" : stripped.toPlainString();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static FinalCloseEvidenceValidationException rejected(String message) {
        return new FinalCloseEvidenceValidationException(message);
    }
}
