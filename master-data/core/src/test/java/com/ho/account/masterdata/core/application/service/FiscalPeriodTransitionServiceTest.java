package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.ho.account.contracts.masterdata.FiscalPeriodClosingStatus;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionCommand;
import com.ho.account.masterdata.core.application.port.out.FiscalPeriodPersistencePort;
import com.ho.account.masterdata.core.application.port.out.FiscalPeriodTransitionPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataAuditPort;
import com.ho.account.masterdata.core.domain.audit.MasterDataAuditEvent;
import com.ho.account.masterdata.core.domain.exception.FiscalPeriodTransitionConflictException;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import com.ho.account.masterdata.core.domain.model.FiscalPeriodTransition;
import com.ho.account.masterdata.core.domain.model.FiscalPeriodTransition.Phase;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Port collaboration tests; actual commit/rollback evidence is in the JPA and two-database suites. */
@ExtendWith(MockitoExtension.class)
class FiscalPeriodTransitionServiceTest {
    private static final String ID = "reopen@773";
    private static final Instant CREATED = Instant.parse("2026-11-01T00:00:00Z");
    @Mock private FiscalPeriodPersistencePort periods;
    @Mock private FiscalPeriodTransitionPersistencePort receipts;
    @Mock private MasterDataAuditPort audits;
    private FiscalPeriodTransitionService service;

    @BeforeEach
    void createService() { service = new FiscalPeriodTransitionService(periods, receipts, audits); }

    @Test
    void prepareLocksBeforeReadingReceiptAndFencesThenInsertsWithoutChangingPeriod() {
        var period = period(FiscalPeriod.ClosingStatus.CLOSED);
        when(periods.findByIdForUpdate(1L)).thenReturn(Optional.of(period));
        when(receipts.insert(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.prepare(command("approver"));

        var ordered = inOrder(periods, receipts);
        ordered.verify(periods).findByIdForUpdate(1L);
        ordered.verify(receipts).findByOperationId(ID);
        ordered.verify(receipts).hasPrepared(1L);
        ordered.verify(receipts).hasUnconfirmedReopen(1L);
        ordered.verify(receipts).insert(result);
        ordered.verifyNoMoreInteractions();
        result.requireCommand(ID, 1L, FiscalPeriod.ClosingStatus.CLOSED, FiscalPeriod.ClosingStatus.OPEN, "approver");
        assertThat(result.phase()).isEqualTo(Phase.PREPARED);
        assertThat(period.getClosingStatus()).isEqualTo(FiscalPeriod.ClosingStatus.CLOSED);
        verifyNoInteractions(audits);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void existingPeriodFenceRejectsPrepareWithoutAnyWrite(boolean preparedFence) {
        when(periods.findByIdForUpdate(1L)).thenReturn(Optional.of(period(FiscalPeriod.ClosingStatus.CLOSED)));
        when(receipts.hasPrepared(1L)).thenReturn(preparedFence);
        if (!preparedFence) when(receipts.hasUnconfirmedReopen(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.prepare(command("approver")))
                .isInstanceOf(FiscalPeriodTransitionConflictException.class);

        verify(periods, never()).save(any());
        verify(receipts, never()).insert(any());
        verify(receipts, never()).saveLifecycle(any());
        verifyNoInteractions(audits);
        if (preparedFence) verify(receipts, never()).hasUnconfirmedReopen(1L);
    }

    @Test
    void commandRebindingIsRejectedBeforeFenceChecksOrWrites() {
        when(periods.findByIdForUpdate(1L)).thenReturn(Optional.of(period(FiscalPeriod.ClosingStatus.CLOSED)));
        when(receipts.findByOperationId(ID)).thenReturn(Optional.of(receipt(Phase.PREPARED)));

        assertThatThrownBy(() -> service.prepare(command("another-approver")))
                .isInstanceOf(FiscalPeriodTransitionConflictException.class);

        verify(periods).findByIdForUpdate(1L);
        verify(receipts).findByOperationId(ID);
        verifyNoMoreInteractions(periods, receipts);
        verifyNoInteractions(audits);
    }

    @ParameterizedTest
    @EnumSource(Phase.class)
    void identicalPrepareReturnsOriginalHistoryWithoutWritesInEveryPhase(Phase phase) {
        var original = receipt(phase);
        when(periods.findByIdForUpdate(1L)).thenReturn(Optional.of(period(FiscalPeriod.ClosingStatus.OPEN)));
        when(receipts.findByOperationId(ID)).thenReturn(Optional.of(original));

        assertThat(service.prepare(command("approver"))).isSameAs(original);

        verify(periods).findByIdForUpdate(1L);
        verify(receipts).findByOperationId(ID);
        verifyNoMoreInteractions(periods, receipts);
        verifyNoInteractions(audits);
    }

    @ParameterizedTest
    @CsvSource({"apply,APPLIED", "apply,CONFIRMED", "cancel,CANCELLED", "confirm,CONFIRMED"})
    void terminalRetriesLockAndReadButNeverRepeatEffects(String operation, Phase phase) {
        var original = receipt(phase);
        stubOperation(original, FiscalPeriod.ClosingStatus.OPEN);

        assertThat(invoke(operation)).isSameAs(original);

        var ordered = inOrder(periods, receipts);
        ordered.verify(receipts).findPeriodId(ID);
        ordered.verify(periods).findByIdForUpdate(1L);
        ordered.verify(receipts).findByOperationId(ID);
        ordered.verifyNoMoreInteractions();
        verifyNoInteractions(audits);
    }

    @Test
    void applySavesPeriodThenAuditsOriginalApproverBeforeReceiptPhase() {
        var period = stubOperation(receipt(Phase.PREPARED), FiscalPeriod.ClosingStatus.CLOSED);
        FiscalPeriodTransition result;
        try (var ignored = MasterDataActorContext.enter("closing", "retry-operator")) {
            result = service.apply(ID);
        }

        var event = ArgumentCaptor.forClass(MasterDataAuditEvent.class);
        var ordered = inOrder(periods, audits, receipts);
        ordered.verify(receipts).findPeriodId(ID);
        ordered.verify(periods).findByIdForUpdate(1L);
        ordered.verify(receipts).findByOperationId(ID);
        ordered.verify(periods).save(period);
        ordered.verify(audits).append(event.capture());
        ordered.verify(receipts).saveLifecycle(result);
        ordered.verifyNoMoreInteractions();
        assertThat(period.getClosingStatus()).isEqualTo(FiscalPeriod.ClosingStatus.OPEN);
        assertThat(period.getAuditUser()).isEqualTo("closing:approver");
        assertThat(event.getValue().businessActor()).isEqualTo("approver");
        assertThat(event.getValue().serviceActor()).isEqualTo("closing");
        assertThat(event.getValue().eventType()).isEqualTo("FISCAL_PERIOD_STATUS");
        assertThat(result.phase()).isEqualTo(Phase.APPLIED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"cancel", "confirm"})
    void lifecycleAuditUsesCancellationActorOrOriginalApprover(String operation) {
        stubOperation(receipt(operation.equals("cancel") ? Phase.PREPARED : Phase.APPLIED),
                FiscalPeriod.ClosingStatus.OPEN);
        FiscalPeriodTransition result;
        try (var ignored = MasterDataActorContext.enter("closing", "unrelated-retry-actor")) {
            result = invoke(operation);
        }
        var event = ArgumentCaptor.forClass(MasterDataAuditEvent.class);
        verify(audits).append(event.capture());
        assertThat(event.getValue().businessActor()).isEqualTo(operation.equals("cancel") ? "recovery" : "approver");
        assertThat(result.auditUser()).isEqualTo("approver");
        verify(receipts).saveLifecycle(result);
        verify(periods, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"apply", "cancel", "confirm"})
    void auditFailureEscapesWithoutPersistingLaterReceiptPhase(String operation) {
        stubOperation(receipt(operation.equals("confirm") ? Phase.APPLIED : Phase.PREPARED),
                operation.equals("apply") ? FiscalPeriod.ClosingStatus.CLOSED : FiscalPeriod.ClosingStatus.OPEN);
        var failure = new IllegalStateException("synthetic audit failure");
        doThrow(failure).when(audits).append(any());

        assertThatThrownBy(() -> invoke(operation)).isSameAs(failure);

        verify(receipts, never()).saveLifecycle(any());
        verify(receipts, never()).insert(any());
    }

    @Test
    void periodSaveFailureEscapesBeforeAuditOrReceiptWrite() {
        stubOperation(receipt(Phase.PREPARED), FiscalPeriod.ClosingStatus.CLOSED);
        var failure = new IllegalStateException("synthetic period save failure");
        when(periods.save(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.apply(ID)).isSameAs(failure);

        verifyNoInteractions(audits);
        verify(receipts, never()).saveLifecycle(any());
    }

    private FiscalPeriodTransition invoke(String operation) {
        return switch (operation) {
            case "apply" -> service.apply(ID);
            case "cancel" -> service.cancel(ID, "recovery");
            case "confirm" -> service.confirm(ID);
            default -> throw new AssertionError(operation);
        };
    }

    private FiscalPeriod stubOperation(FiscalPeriodTransition receipt, FiscalPeriod.ClosingStatus status) {
        var period = period(status);
        when(receipts.findPeriodId(ID)).thenReturn(Optional.of(1L));
        when(periods.findByIdForUpdate(1L)).thenReturn(Optional.of(period));
        when(receipts.findByOperationId(ID)).thenReturn(Optional.of(receipt));
        return period;
    }

    private FiscalPeriodTransitionCommand command(String actor) {
        return new FiscalPeriodTransitionCommand(ID, 1L, FiscalPeriodClosingStatus.CLOSED,
                FiscalPeriodClosingStatus.OPEN, actor);
    }

    private FiscalPeriodTransition receipt(Phase phase) {
        return new FiscalPeriodTransition(ID, 1L, FiscalPeriod.ClosingStatus.CLOSED, FiscalPeriod.ClosingStatus.OPEN,
                "approver", phase, CREATED, phase == Phase.APPLIED || phase == Phase.CONFIRMED ? CREATED : null,
                phase == Phase.CANCELLED ? CREATED : null, phase == Phase.CONFIRMED ? CREATED : null);
    }

    private FiscalPeriod period(FiscalPeriod.ClosingStatus status) {
        return FiscalPeriod.reconstitute(1L, "2026", "11", LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), status, null, null, "prior");
    }
}
