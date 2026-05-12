package com.ho.account.reconciliation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import com.ho.account.contracts.ledger.LedgerBalanceSummary;
import com.ho.account.contracts.ledger.LedgerQueryPort;
import com.ho.account.reconciliation.domain.ReconStageResult;
import com.ho.account.reconciliation.domain.ReconUnitDefinition;
import com.ho.account.reconciliation.domain.ReconciliationResult;
import com.ho.account.reconciliation.domain.ReconciliationStatus;
import com.ho.account.reconciliation.domain.ReconciliationType;
import com.ho.account.reconciliation.domain.ReconciliationVariance;
import com.ho.account.reconciliation.repository.ReconStageResultRepository;
import com.ho.account.reconciliation.repository.ReconUnitDefinitionRepository;
import com.ho.account.reconciliation.repository.ReconciliationResultRepository;
import com.ho.account.reconciliation.repository.ReconciliationVarianceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconManagerServiceTest {

    @Mock
    private ReconUnitDefinitionRepository unitRepository;

    @Mock
    private ReconStageResultRepository stageResultRepository;

    @Mock
    private ReconciliationResultRepository resultRepository;

    @Mock
    private ReconciliationVarianceRepository varianceRepository;

    @Mock
    private JournalQueryPort journalQueryPort;

    @Mock
    private LedgerQueryPort ledgerQueryPort;

    private ReconManagerService reconManagerService;

    @BeforeEach
    void setUp() {
        reconManagerService = new ReconManagerService(
                unitRepository,
                stageResultRepository,
                resultRepository,
                varianceRepository,
                journalQueryPort,
                ledgerQueryPort,
                new ObjectMapper()
        );
    }

    @Test
    void performDeepReconciliationUsesConfiguredSourceAndLedgerQuery() {
        LocalDate reconDate = LocalDate.of(2026, 5, 11);
        ReconUnitDefinition unit = reconUnitDefinition();
        JournalSummary journalSummary = new JournalSummary();
        journalSummary.setId(99L);

        when(unitRepository.findById("UNIT-001")).thenReturn(Optional.of(unit));
        when(resultRepository.save(any(ReconciliationResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(journalQueryPort.getJournalSummaries(reconDate, reconDate)).thenReturn(List.of(journalSummary));
        when(journalQueryPort.getJournalDetails(99L)).thenReturn(List.of(
                journalDetail(JournalSide.DEBIT, "11000", "980.00"),
                journalDetail(JournalSide.CREDIT, "21000", "980.00")
        ));
        when(ledgerQueryPort.getGlBalanceSummaries(reconDate, reconDate, "11000", "KRW"))
                .thenReturn(List.of(ledgerBalance("11000", "KRW", "950.00")));

        ReconciliationResult result = reconManagerService.performDeepReconciliation("UNIT-001", reconDate, "tester");

        assertThat(result.getTotalAmountSource()).isEqualByComparingTo("1000.00");
        assertThat(result.getTotalCountSource()).isEqualTo(10L);
        assertThat(result.getTotalAmountTarget()).isEqualByComparingTo("950.00");
        assertThat(result.getTotalCountTarget()).isEqualTo(1L);
        assertThat(result.getVarianceAmount()).isEqualByComparingTo("50.00");
        assertThat(result.getVarianceCount()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(ReconciliationStatus.VARIANCE_FOUND);

        List<ReconStageResult> stages = capturedStages();
        assertThat(stages).hasSize(4);
        assertStage(stages, "SOURCE", 10L, "1000.00");
        assertStage(stages, "INTERFACE", 10L, "1000.00");
        assertStage(stages, "JOURNAL", 1L, "980.00");
        assertStage(stages, "LEDGER", 1L, "950.00");

        ArgumentCaptor<ReconciliationVariance> varianceCaptor = ArgumentCaptor.forClass(ReconciliationVariance.class);
        verify(varianceRepository).save(varianceCaptor.capture());
        assertThat(varianceCaptor.getValue().getVarianceCode()).isEqualTo("DEEP_MISMATCH");
        assertThat(varianceCaptor.getValue().getAmount()).isEqualByComparingTo("50.00");
    }

    private ReconUnitDefinition reconUnitDefinition() {
        ReconUnitDefinition unit = new ReconUnitDefinition();
        unit.setUnitId("UNIT-001");
        unit.setUnitName("Deep reconciliation");
        unit.setReconType(ReconciliationType.ACCOUNT_TOTALS);
        unit.setToleranceAmount(BigDecimal.ZERO);
        unit.setSlaDays(3);
        unit.setMatchingRulesJson("""
                {
                  "sourceAmount": "1000.00",
                  "sourceCount": 10,
                  "interfaceAmount": "1000.00",
                  "interfaceCount": 10,
                  "journalAccountCode": "11000",
                  "ledgerAccountCode": "11000",
                  "ledgerCurrencyCode": "KRW",
                  "ledgerAmountBasis": "DEBIT"
                }
                """);
        return unit;
    }

    private JournalDetailSummary journalDetail(JournalSide side, String accountCode, String amount) {
        JournalDetailSummary detail = new JournalDetailSummary();
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setBaseAmount(new BigDecimal(amount));
        return detail;
    }

    private LedgerBalanceSummary ledgerBalance(String accountCode, String currencyCode, String debitAmount) {
        LedgerBalanceSummary balance = new LedgerBalanceSummary();
        balance.setAccountCode(accountCode);
        balance.setCurrencyCode(currencyCode);
        balance.setDebitAmount(new BigDecimal(debitAmount));
        balance.setCreditAmount(BigDecimal.ZERO);
        balance.setEndingBalance(new BigDecimal(debitAmount));
        return balance;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<ReconStageResult> capturedStages() {
        ArgumentCaptor<Iterable> stageCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(stageResultRepository).saveAll(stageCaptor.capture());

        List<ReconStageResult> stages = new ArrayList<>();
        for (Object stage : stageCaptor.getValue()) {
            stages.add((ReconStageResult) stage);
        }
        return stages;
    }

    private void assertStage(List<ReconStageResult> stages, String stageCode, long count, String amount) {
        assertThat(stages)
                .filteredOn(stage -> stageCode.equals(stage.getStageCode()))
                .singleElement()
                .satisfies(stage -> {
                    assertThat(stage.getTotalCount()).isEqualTo(count);
                    assertThat(stage.getTotalAmount()).isEqualByComparingTo(amount);
                });
    }
}
