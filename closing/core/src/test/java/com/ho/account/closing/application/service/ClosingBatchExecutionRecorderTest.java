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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClosingBatchExecutionRecorderTest {

    @Mock
    private ValuationBatchPersistencePort valuationBatchPersistencePort;
    @Mock
    private ProvisionBatchPersistencePort provisionBatchPersistencePort;

    private ClosingBatchExecutionRecorder recorder;
    private FiscalPeriodRef period;

    @BeforeEach
    void setUp() {
        recorder = new ClosingBatchExecutionRecorder(
                valuationBatchPersistencePort,
                provisionBatchPersistencePort);
        period = new FiscalPeriodRef(
                10L,
                "2026",
                "05",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                "OPEN");
    }

    @Test
    void valuationHistoryMovesFromRunningToPendingApproval() {
        when(valuationBatchPersistencePort.save(any(ValuationBatch.class)))
                .thenAnswer(invocation -> {
                    ValuationBatch batch = invocation.getArgument(0);
                    batch.setId(77L);
                    return batch;
                });

        ValuationBatch running = recorder.startValuation(
                period,
                ValuationBatch.ValuationType.FX_RATE,
                " ADMIN ");
        when(valuationBatchPersistencePort.findById(77L)).thenReturn(Optional.of(running));

        ValuationBatch pending = recorder.markValuationPendingApproval(
                77L,
                900L,
                "/reports/valuation/77",
                "ADMIN");

        assertThat(pending.getStatus()).isEqualTo(ValuationBatch.ValuationBatchStatus.PENDING_APPROVAL);
        assertThat(pending.getGeneratedJournalEntryId()).isEqualTo(900L);
        assertThat(pending.getRunBy()).isEqualTo("ADMIN");
        assertThat(pending.getFiscalPeriodId()).isEqualTo(10L);
    }

    @Test
    void valuationFailureRemainsExplicit() {
        ValuationBatch batch = new ValuationBatch();
        batch.setId(77L);
        batch.setStatus(ValuationBatch.ValuationBatchStatus.RUNNING);
        when(valuationBatchPersistencePort.findById(77L)).thenReturn(Optional.of(batch));

        recorder.markValuationFailed(77L, "ADMIN");

        assertThat(batch.getStatus()).isEqualTo(ValuationBatch.ValuationBatchStatus.FAILED);
        verify(valuationBatchPersistencePort).save(batch);
    }

    @Test
    void provisionHistoryMovesFromRunningToPendingApproval() {
        when(provisionBatchPersistencePort.save(any(ProvisionBatch.class)))
                .thenAnswer(invocation -> {
                    ProvisionBatch batch = invocation.getArgument(0);
                    batch.setId(88L);
                    return batch;
                });

        ProvisionBatch running = recorder.startProvision(
                period,
                ProvisionBatch.ProvisionType.ECL,
                "ADMIN");
        when(provisionBatchPersistencePort.findById(88L)).thenReturn(Optional.of(running));

        ProvisionBatch pending = recorder.markProvisionPendingApproval(88L, 901L, "ADMIN");

        assertThat(pending.getStatus()).isEqualTo(ProvisionBatch.ProvisionBatchStatus.PENDING_APPROVAL);
        assertThat(pending.getGeneratedJournalEntryId()).isEqualTo(901L);
        assertThat(pending.getFiscalPeriodId()).isEqualTo(10L);
    }

    @Test
    void provisionFailureRemainsExplicit() {
        ProvisionBatch batch = new ProvisionBatch();
        batch.setId(88L);
        batch.setStatus(ProvisionBatch.ProvisionBatchStatus.RUNNING);
        when(provisionBatchPersistencePort.findById(88L)).thenReturn(Optional.of(batch));

        recorder.markProvisionFailed(88L, "ADMIN");

        assertThat(batch.getStatus()).isEqualTo(ProvisionBatch.ProvisionBatchStatus.FAILED);
        verify(provisionBatchPersistencePort).save(batch);
    }
}
