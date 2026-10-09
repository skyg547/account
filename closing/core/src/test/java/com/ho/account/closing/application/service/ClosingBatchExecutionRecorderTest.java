package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.ProvisionBatchPersistencePort;
import com.ho.account.closing.application.port.out.ValuationBatchPersistencePort;
import com.ho.account.closing.domain.ProvisionBatch;
import com.ho.account.closing.domain.ValuationBatch;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClosingBatchExecutionRecorderTest {

    @Mock
    private ValuationBatchPersistencePort valuationBatchPersistencePort;
    @Mock
    private ProvisionBatchPersistencePort provisionBatchPersistencePort;
    @Mock
    private com.ho.account.closing.application.port.out.ClosingFinancialRunLockPort runLocks;

    private ClosingBatchExecutionRecorder recorder;
    private FiscalPeriodRef period;

    @BeforeEach
    void setUp() {
        recorder = new ClosingBatchExecutionRecorder(
                valuationBatchPersistencePort,
                provisionBatchPersistencePort,
                runLocks);
        period = new FiscalPeriodRef(
                10L,
                "2026",
                "05",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                "OPEN");
    }

    @Test
    void sameKeyReturnsOriginalCompletedValuationAndRejectsChangedContent() {
        ValuationBatch original = new ValuationBatch();
        original.setId(77L);
        original.setFiscalPeriodId(period.id());
        original.setValuationType(ValuationBatch.ValuationType.FX_RATE);
        original.setRunBy("ADMIN");
        original.setStatus(ValuationBatch.ValuationBatchStatus.PENDING_APPROVAL);
        original.setGeneratedJournalEntryId(900L);
        when(valuationBatchPersistencePort.findByExecutionKey("same-key"))
                .thenReturn(Optional.of(original));

        ValuationBatch retry = recorder.claimValuation(
                period, ValuationBatch.ValuationType.FX_RATE, "ADMIN", "same-key");

        assertThat(retry).isSameAs(original);
        assertThat(retry.getGeneratedJournalEntryId()).isEqualTo(900L);
        assertThatThrownBy(() -> recorder.claimValuation(
                period, ValuationBatch.ValuationType.FX_RATE, "OTHER", "same-key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different business content");
    }

    @Test
    void unknownProvisionOutcomeReclaimsSameRunAndRecordsMultiJournalEffect() {
        ProvisionBatch original = new ProvisionBatch();
        original.setId(88L);
        original.setFiscalPeriodId(period.id());
        original.setProvisionType(ProvisionBatch.ProvisionType.ECL);
        original.setRunBy("ADMIN");
        original.setStatus(ProvisionBatch.ProvisionBatchStatus.RUNNING);
        when(provisionBatchPersistencePort.findById(88L)).thenReturn(Optional.of(original));
        when(provisionBatchPersistencePort.findByExecutionKey("same-key"))
                .thenReturn(Optional.of(original));
        when(provisionBatchPersistencePort.save(original)).thenReturn(original);

        recorder.provisionOutcomeUnknown(88L, 2, "ADMIN");
        assertThat(original.getStatus()).isEqualTo(ProvisionBatch.ProvisionBatchStatus.RECONCILIATION_REQUIRED);
        assertThat(original.getJournalCount()).isEqualTo(2);

        ProvisionBatch retry = recorder.claimProvision(
                period, ProvisionBatch.ProvisionType.ECL, "ADMIN", "same-key");
        assertThat(retry.getId()).isEqualTo(88L);
        verify(provisionBatchPersistencePort, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.argThat(
                batch -> batch != original));
    }

    @Test
    void completedValuationStoresZeroCountWithoutInventingJournalId() {
        ValuationBatch batch = new ValuationBatch();
        batch.setId(77L);
        when(valuationBatchPersistencePort.findById(77L)).thenReturn(Optional.of(batch));
        when(valuationBatchPersistencePort.save(batch)).thenReturn(batch);

        ValuationBatch finished = recorder.finishValuation(77L, 0, null, false, "ADMIN");

        assertThat(finished.getStatus()).isEqualTo(ValuationBatch.ValuationBatchStatus.COMPLETED);
        assertThat(finished.getJournalCount()).isZero();
        assertThat(finished.getGeneratedJournalEntryId()).isNull();
    }

    @Test
    void preparationFailureWithoutManifestHasNoPendingFinancialEffects() {
        ValuationBatch batch = new ValuationBatch();
        batch.setId(77L);
        batch.setStatus(ValuationBatch.ValuationBatchStatus.RUNNING);
        when(valuationBatchPersistencePort.findById(77L)).thenReturn(Optional.of(batch));
        when(valuationBatchPersistencePort.save(batch)).thenReturn(batch);

        recorder.valuationNoEffectFailure(77L, "ADMIN");

        assertThat(batch.getStatus()).isEqualTo(ValuationBatch.ValuationBatchStatus.FAILED);
        assertThat(batch.getJournalCount()).isZero();
    }

    @Test
    void pendingProvisionStoresMultipleDraftCountWithoutInventingSingleId() {
        ProvisionBatch batch = new ProvisionBatch();
        batch.setId(88L);
        when(provisionBatchPersistencePort.findById(88L)).thenReturn(Optional.of(batch));
        when(provisionBatchPersistencePort.save(batch)).thenReturn(batch);

        ProvisionBatch finished = recorder.finishProvision(88L, 2, null, false, "ADMIN");

        assertThat(finished.getStatus()).isEqualTo(ProvisionBatch.ProvisionBatchStatus.PENDING_APPROVAL);
        assertThat(finished.getJournalCount()).isEqualTo(2);
        assertThat(finished.getGeneratedJournalEntryId()).isNull();
    }
}
