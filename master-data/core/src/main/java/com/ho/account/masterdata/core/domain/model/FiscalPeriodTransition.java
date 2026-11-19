package com.ho.account.masterdata.core.domain.model;

import com.ho.account.masterdata.core.domain.exception.FiscalPeriodTransitionConflictException;
import java.time.Instant;
import java.util.Objects;

/** Immutable operation history; lifecycle policy is independent of JPA and transport. */
public record FiscalPeriodTransition(String operationId, Long fiscalPeriodId,
        FiscalPeriod.ClosingStatus expectedStatus, FiscalPeriod.ClosingStatus targetStatus,
        String auditUser, Phase phase, Instant createdAt, Instant appliedAt,
        Instant cancelledAt, Instant confirmedAt) {
    public enum Phase { PREPARED, APPLIED, CANCELLED, CONFIRMED }

    public static FiscalPeriodTransition prepare(String operationId, FiscalPeriod period,
            FiscalPeriod.ClosingStatus expected, FiscalPeriod.ClosingStatus target, String actor, Instant now) {
        if (period.getClosingStatus() != expected) {
            throw conflict("Fiscal transition expected state does not match current state");
        }
        // Validate on a detached domain copy: preparation reserves identity without changing status.
        FiscalPeriod candidate = FiscalPeriod.reconstitute(period.getId(), period.getFiscalYear(),
                period.getFiscalPeriod(), period.getStartDate(), period.getEndDate(), period.getClosingStatus(),
                period.getCreatedAt(), period.getUpdatedAt(), period.getAuditUser());
        try {
            candidate.changeClosingStatus(target, "closing:" + actor);
        } catch (IllegalStateException invalidTransition) {
            throw conflict("Fiscal transition violates period state rules");
        }
        return new FiscalPeriodTransition(operationId, period.getId(), expected, target, actor,
                Phase.PREPARED, now, null, null, null);
    }

    public void requireCommand(String id, Long periodId, FiscalPeriod.ClosingStatus expected,
            FiscalPeriod.ClosingStatus target, String actor) {
        if (!Objects.equals(operationId, id) || !Objects.equals(fiscalPeriodId, periodId)
                || expectedStatus != expected || targetStatus != target || !Objects.equals(auditUser, actor)) {
            throw conflict("Fiscal transition operation ID belongs to a different command");
        }
    }

    public FiscalPeriodTransition apply(FiscalPeriod period, Instant now) {
        requirePeriod(period);
        if (phase == Phase.APPLIED || phase == Phase.CONFIRMED) return this;
        if (phase == Phase.CANCELLED) throw conflict("Cancelled fiscal transition cannot be applied");
        if (period.getClosingStatus() != expectedStatus) {
            throw conflict("Fiscal transition expected state does not match current state");
        }
        period.changeClosingStatus(targetStatus, "closing:" + auditUser);
        return new FiscalPeriodTransition(operationId, fiscalPeriodId, expectedStatus, targetStatus,
                auditUser, Phase.APPLIED, createdAt, now, null, null);
    }

    public FiscalPeriodTransition cancel(String actor, Instant now) {
        if (actor == null || !actor.matches("[A-Za-z0-9._@-]{1,42}")) {
            throw new IllegalArgumentException("Verified cancellation actor is required");
        }
        if (phase == Phase.CANCELLED) return this;
        if (phase == Phase.APPLIED || phase == Phase.CONFIRMED) {
            throw conflict("Applied fiscal transition cannot be cancelled");
        }
        return new FiscalPeriodTransition(operationId, fiscalPeriodId, expectedStatus, targetStatus,
                auditUser, Phase.CANCELLED, createdAt, null, now, null);
    }

    public FiscalPeriodTransition confirm(FiscalPeriod period, Instant now) {
        requirePeriod(period);
        if (phase == Phase.CONFIRMED) return this;
        if (phase != Phase.APPLIED) throw conflict("Only an applied fiscal transition can be confirmed");
        if (expectedStatus != FiscalPeriod.ClosingStatus.CLOSED || targetStatus != FiscalPeriod.ClosingStatus.OPEN) {
            throw conflict("Only an applied reopen can be confirmed");
        }
        if (period.getClosingStatus() != targetStatus) {
            throw conflict("Fiscal transition target no longer matches current state");
        }
        // A distinct confirmed history entry releases admission only after Closing's durable completion.
        return new FiscalPeriodTransition(operationId, fiscalPeriodId, expectedStatus, targetStatus,
                auditUser, Phase.CONFIRMED, createdAt, appliedAt, null, now);
    }

    private void requirePeriod(FiscalPeriod period) {
        if (!Objects.equals(fiscalPeriodId, period.getId())) throw conflict("Fiscal transition period mismatch");
    }

    private static FiscalPeriodTransitionConflictException conflict(String message) {
        return new FiscalPeriodTransitionConflictException(message);
    }
}
