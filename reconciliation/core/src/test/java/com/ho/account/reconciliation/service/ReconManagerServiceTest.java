package com.ho.account.reconciliation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.ledger.LedgerBalanceSummary;
import com.ho.account.contracts.ledger.LedgerQueryPort;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshot;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotPort;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotRequest;
import com.ho.account.reconciliation.domain.ReconciliationDifference;
import com.ho.account.reconciliation.domain.ReconciliationRun;
import com.ho.account.reconciliation.domain.ReconciliationStageResult;
import com.ho.account.reconciliation.domain.ReconciliationUnit;
import com.ho.account.reconciliation.repository.ReconciliationDifferenceRepository;
import com.ho.account.reconciliation.repository.ReconciliationRunRepository;
import com.ho.account.reconciliation.repository.ReconciliationStageResultRepository;
import com.ho.account.reconciliation.repository.ReconciliationUnitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconManagerServiceTest {

    @Mock private ReconciliationUnitRepository unitRepository;
    @Mock private ReconciliationRunRepository runRepository;
    @Mock private ReconciliationDifferenceRepository differenceRepository;
    @Mock private ReconciliationStageResultRepository stageResultRepository;
    @Mock private JournalQueryPort journalQueryPort;
    @Mock private LedgerQueryPort ledgerQueryPort;
    @Mock private ExternalReconSnapshotPort externalReconSnapshotPort;

    private ReconManagerService reconManagerService;

    @BeforeEach
    void setUp() {
        reconManagerService = new ReconManagerService(
                unitRepository,
                runRepository,
                differenceRepository,
                stageResultRepository,
                externalReconSnapshotPort,
                journalQueryPort,
                ledgerQueryPort,
                new ObjectMapper());
    }

    @Test
    void deepReconciliationUsesTheStandardRunAndDifferenceLifecycle() {
        LocalDate reconDate = LocalDate.of(2026, 5, 11);
        ReconciliationUnit unit = reconciliationUnit();
        when(unitRepository.findById(10L)).thenReturn(Optional.of(unit));
        when(runRepository.save(any(ReconciliationRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(externalReconSnapshotPort.loadSnapshot(any())).thenAnswer(invocation -> {
            ExternalReconSnapshotRequest request = invocation.getArgument(0);
            return new ExternalReconSnapshot(10L, new BigDecimal("1000.00"));
        });
        when(journalQueryPort.getJournalDetailAggregateByAccount(reconDate, reconDate, JournalSide.DEBIT, "11000"))
                .thenReturn(new com.ho.account.contracts.journal.JournalDetailAggregateSummary(
                        1L, new BigDecimal("980.00")));
        when(ledgerQueryPort.calculateLedgerSummary(reconDate, reconDate, "11000", "KRW", "DEBIT"))
                .thenReturn(new com.ho.account.contracts.ledger.LedgerAggregateSummary(1L, new BigDecimal("950.00")));

        ReconciliationRun run = reconManagerService.performDeepReconciliation(10L, reconDate, "tester");

        assertThat(run.getReconciliationUnit()).isSameAs(unit);
        assertThat(run.getTotalAmountSource()).isEqualByComparingTo("1000.00");
        assertThat(run.getTotalAmountTarget()).isEqualByComparingTo("950.00");
        assertThat(run.getUnmatchedAmount()).isEqualByComparingTo("50.00");
        assertThat(run.getUnmatchedItemsCount()).isEqualTo(1L);
        assertThat(run.getStatus()).isEqualTo(ReconciliationRun.ReconciliationRunStatus.PARTIAL);

        List<ReconciliationStageResult> stages = capturedStages();
        assertThat(stages).hasSize(4);
        assertStage(stages, "SOURCE", 10L, "1000.00");
        assertStage(stages, "INTERFACE", 10L, "1000.00");
        assertStage(stages, "JOURNAL", 1L, "980.00");
        assertStage(stages, "LEDGER", 1L, "950.00");

        ArgumentCaptor<ReconciliationDifference> differenceCaptor =
                ArgumentCaptor.forClass(ReconciliationDifference.class);
        verify(differenceRepository).save(differenceCaptor.capture());
        assertThat(differenceCaptor.getValue().getReconciliationRun()).isSameAs(run);
        assertThat(differenceCaptor.getValue().getDifferenceAmount()).isEqualByComparingTo("50.00");
        assertThat(differenceCaptor.getValue().getSlaDueDate()).isEqualTo(reconDate.plusDays(3).atStartOfDay());
        verify(externalReconSnapshotPort, times(2)).loadSnapshot(any());
    }

    private ReconciliationUnit reconciliationUnit() {
        ReconciliationUnit unit = new ReconciliationUnit();
        unit.setId(10L);
        unit.setName("Deep reconciliation");
        unit.setActive(true);
        unit.setCriteriaJson("""
                {
                  "productCode": "LOAN",
                  "currencyCode": "KRW",
                  "legalEntityCode": "HO",
                  "journalAccountCode": "11000",
                  "ledgerAccountCode": "11000",
                  "ledgerCurrencyCode": "KRW",
                  "ledgerAmountBasis": "DEBIT",
                  "toleranceAmount": 0,
                  "slaDays": 3
                }
                """);
        return unit;
    }

    private LedgerBalanceSummary ledgerBalance(String amount) {
        LedgerBalanceSummary balance = new LedgerBalanceSummary();
        balance.setAccountCode("11000");
        balance.setCurrencyCode("KRW");
        balance.setDebitAmount(new BigDecimal(amount));
        balance.setCreditAmount(BigDecimal.ZERO);
        balance.setEndingBalance(new BigDecimal(amount));
        return balance;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<ReconciliationStageResult> capturedStages() {
        ArgumentCaptor<Iterable> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(stageResultRepository).saveAll(captor.capture());
        List<ReconciliationStageResult> stages = new ArrayList<>();
        captor.getValue().forEach(stage -> stages.add((ReconciliationStageResult) stage));
        return stages;
    }

    private void assertStage(List<ReconciliationStageResult> stages, String code, long count, String amount) {
        assertThat(stages)
                .filteredOn(stage -> code.equals(stage.getStageCode()))
                .singleElement()
                .satisfies(stage -> {
                    assertThat(stage.getTotalCount()).isEqualTo(count);
                    assertThat(stage.getTotalAmount()).isEqualByComparingTo(amount);
                });
    }
}
