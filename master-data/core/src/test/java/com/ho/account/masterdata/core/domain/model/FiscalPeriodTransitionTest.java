package com.ho.account.masterdata.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.masterdata.core.domain.exception.FiscalPeriodTransitionConflictException;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FiscalPeriodTransitionTest {
    private static final Instant CREATED = Instant.parse("2026-11-01T00:00:00Z");

    @Test
    void prepareDoesNotMutatePeriodAndEachReopenPhasePreservesOriginalCommand() {
        var period = period(FiscalPeriod.ClosingStatus.CLOSED);
        var prepared = prepare(period, FiscalPeriod.ClosingStatus.OPEN);
        assertThat(period.getClosingStatus()).isEqualTo(FiscalPeriod.ClosingStatus.CLOSED);
        var applied = prepared.apply(period, CREATED.plusSeconds(1));
        var confirmed = applied.confirm(period, CREATED.plusSeconds(2));
        assertThat(prepared.phase()).isEqualTo(FiscalPeriodTransition.Phase.PREPARED);
        assertThat(applied.phase()).isEqualTo(FiscalPeriodTransition.Phase.APPLIED);
        assertThat(confirmed.phase()).isEqualTo(FiscalPeriodTransition.Phase.CONFIRMED);
        assertThat(confirmed.createdAt()).isEqualTo(CREATED);
        assertThat(confirmed.appliedAt()).isEqualTo(CREATED.plusSeconds(1));
        assertThat(confirmed.confirmedAt()).isEqualTo(CREATED.plusSeconds(2));
        confirmed.requireCommand("reopen@773", 1L, FiscalPeriod.ClosingStatus.CLOSED,
                FiscalPeriod.ClosingStatus.OPEN, "approver");
        assertThat(confirmed.apply(period, CREATED.plusSeconds(3))).isSameAs(confirmed);
        assertThat(confirmed.confirm(period, CREATED.plusSeconds(3))).isSameAs(confirmed);
    }

    @Test
    void cancellationIsATombstoneAndAppliedOrConfirmedOperationsCannotBeCancelled() {
        var period = period(FiscalPeriod.ClosingStatus.CLOSED);
        var prepared = prepare(period, FiscalPeriod.ClosingStatus.OPEN);
        var cancelled = prepared.cancel("recovery", CREATED.plusSeconds(1));
        assertThat(cancelled.cancel("recovery", CREATED.plusSeconds(2))).isSameAs(cancelled);
        assertThatThrownBy(() -> cancelled.apply(period, CREATED.plusSeconds(2)))
                .isInstanceOf(FiscalPeriodTransitionConflictException.class);
        assertThatThrownBy(() -> cancelled.confirm(period, CREATED.plusSeconds(2)))
                .isInstanceOf(FiscalPeriodTransitionConflictException.class);
        var applied = prepared.apply(period, CREATED.plusSeconds(1));
        assertThatThrownBy(() -> applied.cancel("recovery", CREATED.plusSeconds(2)))
                .isInstanceOf(FiscalPeriodTransitionConflictException.class);
        var confirmed = applied.confirm(period, CREATED.plusSeconds(2));
        assertThatThrownBy(() -> confirmed.cancel("recovery", CREATED.plusSeconds(3)))
                .isInstanceOf(FiscalPeriodTransitionConflictException.class);
    }

    @Test
    void confirmationRequiresAppliedReopenAndMatchingPeriodState() {
        var closed = period(FiscalPeriod.ClosingStatus.CLOSED);
        var reopen = prepare(closed, FiscalPeriod.ClosingStatus.OPEN);
        assertThatThrownBy(() -> reopen.confirm(closed, CREATED.plusSeconds(1)))
                .isInstanceOf(FiscalPeriodTransitionConflictException.class);
        var opened = period(FiscalPeriod.ClosingStatus.OPEN);
        var appliedClose = prepare(opened, FiscalPeriod.ClosingStatus.CLOSED).apply(opened, CREATED.plusSeconds(1));
        assertThatThrownBy(() -> appliedClose.confirm(opened, CREATED.plusSeconds(2)))
                .isInstanceOf(FiscalPeriodTransitionConflictException.class);
        var appliedReopen = reopen.apply(closed, CREATED.plusSeconds(1));
        closed.changeClosingStatus(FiscalPeriod.ClosingStatus.CLOSED, "later");
        assertThatThrownBy(() -> appliedReopen.confirm(closed, CREATED.plusSeconds(2)))
                .isInstanceOf(FiscalPeriodTransitionConflictException.class);
    }

    @Test
    void preparationUsesPeriodRulesAndFullCommandGuardRejectsRebinding() {
        assertThatThrownBy(() -> prepare(period(FiscalPeriod.ClosingStatus.OPEN),
                FiscalPeriod.ClosingStatus.PERMANENTLY_CLOSED))
                .isInstanceOf(FiscalPeriodTransitionConflictException.class);
        var receipt = prepare(period(FiscalPeriod.ClosingStatus.CLOSED), FiscalPeriod.ClosingStatus.OPEN);
        assertThatThrownBy(() -> receipt.requireCommand("reopen@773", 2L,
                FiscalPeriod.ClosingStatus.CLOSED, FiscalPeriod.ClosingStatus.OPEN, "approver"))
                .isInstanceOf(FiscalPeriodTransitionConflictException.class);
    }

    private FiscalPeriodTransition prepare(FiscalPeriod period, FiscalPeriod.ClosingStatus target) {
        return FiscalPeriodTransition.prepare("reopen@773", period, period.getClosingStatus(), target, "approver", CREATED);
    }

    private FiscalPeriod period(FiscalPeriod.ClosingStatus status) {
        return FiscalPeriod.reconstitute(1L, "2026", "11", LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), status, null, null, "prior");
    }
}
