package com.ho.account.reconciliation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.journal.JournalDetailAggregateSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.reconciliation.domain.DifferenceReasonCode;
import com.ho.account.reconciliation.domain.ReconciliationAdjustmentPolicy;
import com.ho.account.reconciliation.domain.ReconciliationDifference;
import com.ho.account.reconciliation.domain.ReconciliationRule;
import com.ho.account.reconciliation.domain.ReconciliationRun;
import com.ho.account.reconciliation.domain.ReconciliationUnit;
import com.ho.account.reconciliation.repository.DifferenceReasonCodeRepository;
import com.ho.account.reconciliation.repository.ReconciliationDifferenceRepository;
import com.ho.account.reconciliation.repository.ReconciliationRuleRepository;
import com.ho.account.reconciliation.repository.ReconciliationRunRepository;
import com.ho.account.reconciliation.repository.ReconciliationUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotPort;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshot;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private ReconciliationUnitRepository reconciliationUnitRepository;

    @Mock
    private ReconciliationRuleRepository reconciliationRuleRepository;

    @Mock
    private DifferenceReasonCodeRepository differenceReasonCodeRepository;

    @Mock
    private ReconciliationRunRepository reconciliationRunRepository;

    @Mock
    private ReconciliationDifferenceRepository reconciliationDifferenceRepository;

    @Mock
    private JournalPostingPort journalPostingPort;

    @Mock
    private JournalQueryPort journalQueryPort;

    @Mock
    private ExternalReconSnapshotPort externalReconSnapshotPort;

    private ReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        reconciliationService = new ReconciliationService(
                reconciliationUnitRepository,
                reconciliationRuleRepository,
                differenceReasonCodeRepository,
                reconciliationRunRepository,
                reconciliationDifferenceRepository,
                journalQueryPort,
                journalPostingPort,
                objectMapper,
                new ReconciliationAdjustmentPolicy(objectMapper),
                externalReconSnapshotPort
        );
    }

    @Test
    void performReconciliationUsesCriteriaSourceAndJournalQueryTarget() {
        LocalDate reconciliationDate = LocalDate.of(2026, 5, 11);
        ReconciliationUnit unit = reconciliationUnit("{\"sourceAmount\":\"1000.00\",\"sourceCount\":2}");
        DifferenceReasonCode reasonCode = reasonCode(false);

        when(reconciliationUnitRepository.findById(10L)).thenReturn(Optional.of(unit));
        when(reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(unit)).thenReturn(List.of());
        stubRunAndDifferenceSaves();
        when(differenceReasonCodeRepository.findByCode("GENERIC_MISMATCH")).thenReturn(Optional.of(reasonCode));
        stubJournalTarget(reconciliationDate, "950.00");
        stubExternalSnapshot(2, "1000.00");

        ReconciliationRun run = reconciliationService.performReconciliation(10L, reconciliationDate);

        assertThat(run.getTotalAmountSource()).isEqualByComparingTo("1000.00");
        assertThat(run.getTotalItemsSource()).isEqualTo(2L);
        assertThat(run.getTotalAmountTarget()).isEqualByComparingTo("950.00");
        assertThat(run.getTotalItemsTarget()).isEqualTo(1L);
        assertThat(run.getUnmatchedAmount()).isEqualByComparingTo("50.00");
        assertThat(run.getUnmatchedItemsCount()).isEqualTo(1L);
        assertThat(run.getMatchedAmount()).isEqualByComparingTo("950.00");
        assertThat(run.getMatchedItemsCount()).isEqualTo(1L);
        assertThat(run.getStatus()).isEqualTo(ReconciliationRun.ReconciliationRunStatus.SUCCESS);

        ArgumentCaptor<ReconciliationDifference> differenceCaptor = ArgumentCaptor.forClass(ReconciliationDifference.class);
        verify(reconciliationDifferenceRepository).save(differenceCaptor.capture());
        ReconciliationDifference difference = differenceCaptor.getValue();
        assertThat(difference.getAmountExpected()).isEqualByComparingTo("1000.00");
        assertThat(difference.getAmountActual()).isEqualByComparingTo("950.00");
        assertThat(difference.getDifferenceAmount()).isEqualByComparingTo("50.00");
        assertThat(difference.getReasonCode()).isSameAs(reasonCode);

        verify(journalPostingPort, never()).createDraftEntry(any());
    }

    @Test
    void performReconciliationDoesNotCreateDifferenceWithinRuleTolerance() {
        LocalDate reconciliationDate = LocalDate.of(2026, 5, 11);
        ReconciliationUnit unit = reconciliationUnit("{\"sourceAmount\":\"1000.00\",\"sourceCount\":2}");
        ReconciliationRule rule = reconciliationRule(ReconciliationRule.ToleranceType.ABSOLUTE, "5.00", true);

        when(reconciliationUnitRepository.findById(10L)).thenReturn(Optional.of(unit));
        when(reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(unit)).thenReturn(List.of(rule));
        stubRunSave();
        stubJournalTarget(reconciliationDate, "995.00");
        stubExternalSnapshot(2, "1000.00");

        ReconciliationRun run = reconciliationService.performReconciliation(10L, reconciliationDate);

        assertThat(run.getTotalAmountSource()).isEqualByComparingTo("1000.00");
        assertThat(run.getTotalAmountTarget()).isEqualByComparingTo("995.00");
        assertThat(run.getUnmatchedAmount()).isEqualByComparingTo("0.00");
        assertThat(run.getUnmatchedItemsCount()).isEqualTo(0L);
        assertThat(run.getMatchedAmount()).isEqualByComparingTo("995.00");
        assertThat(run.getMatchedItemsCount()).isEqualTo(1L);
        assertThat(run.getStatus()).isEqualTo(ReconciliationRun.ReconciliationRunStatus.SUCCESS);

        verify(reconciliationDifferenceRepository, never()).save(any());
        verify(differenceReasonCodeRepository, never()).findByCode("GENERIC_MISMATCH");
        verify(journalPostingPort, never()).createDraftEntry(any());
    }

    @Test
    void defaultReasonCodeDoesNotCreateAdjustmentJournalByDefault() {
        LocalDate reconciliationDate = LocalDate.of(2026, 5, 11);
        ReconciliationUnit unit = reconciliationUnit("{\"sourceAmount\":\"1000.00\",\"sourceCount\":2}");

        when(reconciliationUnitRepository.findById(10L)).thenReturn(Optional.of(unit));
        when(reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(unit)).thenReturn(List.of());
        stubRunAndDifferenceSaves();
        when(differenceReasonCodeRepository.findByCode("GENERIC_MISMATCH")).thenReturn(Optional.empty());
        when(differenceReasonCodeRepository.save(any(DifferenceReasonCode.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubJournalTarget(reconciliationDate, "950.00");
        stubExternalSnapshot(2, "1000.00");

        reconciliationService.performReconciliation(10L, reconciliationDate);

        ArgumentCaptor<DifferenceReasonCode> reasonCaptor = ArgumentCaptor.forClass(DifferenceReasonCode.class);
        verify(differenceReasonCodeRepository).save(reasonCaptor.capture());
        assertThat(reasonCaptor.getValue().isAdjustable()).isFalse();
        verify(journalPostingPort, never()).createDraftEntry(any());
    }

    @Test
    void adjustableReasonUsesConfiguredAdjustmentAccounts() {
        LocalDate reconciliationDate = LocalDate.of(2026, 5, 11);
        ReconciliationUnit unit = reconciliationUnit("""
                {
                  "sourceAmount": "1000.00",
                  "sourceCount": 2,
                  "adjustmentDebitAccountCode": "131000",
                  "adjustmentCreditAccountCode": "211000"
                }
                """);
        DifferenceReasonCode reasonCode = reasonCode(true);

        when(reconciliationUnitRepository.findById(10L)).thenReturn(Optional.of(unit));
        when(reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(unit)).thenReturn(List.of());
        stubRunAndDifferenceSaves();
        when(differenceReasonCodeRepository.findByCode("GENERIC_MISMATCH")).thenReturn(Optional.of(reasonCode));
        stubJournalTarget(reconciliationDate, "950.00");
        stubExternalSnapshot(2, "1000.00");
        when(journalPostingPort.createDraftEntry(any(JournalEntryCommand.class)))
                .thenReturn(new JournalPostingResult(77L, "JE-20260511-0001", "DRAFT"));

        reconciliationService.performReconciliation(10L, reconciliationDate);

        ArgumentCaptor<JournalEntryCommand> commandCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(commandCaptor.capture());
        JournalEntryCommand command = commandCaptor.getValue();

        assertThat(command.entryType()).isEqualTo("ADJUSTMENT");
        assertThat(command.currencyCode()).isEqualTo("KRW");
        assertThat(command.lineageSourceType()).isEqualTo("RECONCILIATION");
        assertThat(command.lines()).hasSize(2);
        assertThat(command.lines()).anySatisfy(line -> {
            assertThat(line.drcrType()).isEqualTo("DEBIT");
            assertThat(line.accountCode()).isEqualTo("131000");
            assertThat(line.amount()).isEqualByComparingTo("50.00");
            assertThat(line.baseAmount()).isEqualByComparingTo("50.00");
        });
        assertThat(command.lines()).anySatisfy(line -> {
            assertThat(line.drcrType()).isEqualTo("CREDIT");
            assertThat(line.accountCode()).isEqualTo("211000");
            assertThat(line.amount()).isEqualByComparingTo("50.00");
            assertThat(line.baseAmount()).isEqualByComparingTo("50.00");
        });
        ArgumentCaptor<ReconciliationDifference> differenceCaptor = ArgumentCaptor.forClass(ReconciliationDifference.class);
        verify(reconciliationDifferenceRepository).save(differenceCaptor.capture());
        assertThat(differenceCaptor.getValue().getAdjustmentJournalEntryId()).isEqualTo(77L);
    }

    @Test
    void adjustableReasonRejectsMissingAdjustmentAccounts() {
        LocalDate reconciliationDate = LocalDate.of(2026, 5, 11);
        ReconciliationUnit unit = reconciliationUnit("{\"sourceAmount\":\"1000.00\",\"sourceCount\":2}");
        DifferenceReasonCode reasonCode = reasonCode(true);

        when(reconciliationUnitRepository.findById(10L)).thenReturn(Optional.of(unit));
        when(reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(unit)).thenReturn(List.of());
        stubRunSave();
        when(differenceReasonCodeRepository.findByCode("GENERIC_MISMATCH")).thenReturn(Optional.of(reasonCode));
        stubJournalTarget(reconciliationDate, "950.00");
        stubExternalSnapshot(2, "1000.00");

        assertThatThrownBy(() -> reconciliationService.performReconciliation(10L, reconciliationDate))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Reconciliation failed")
                .cause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("adjustmentDebitAccountCode")
                .hasMessageContaining("adjustmentCreditAccountCode");

        verify(journalPostingPort, never()).createDraftEntry(any());
        verify(reconciliationDifferenceRepository, never()).save(any());
    }

    @Test
    void resolveDifferenceStoresAdjustmentJournalEntryId() {
        ReconciliationDifference difference = new ReconciliationDifference();
        DifferenceReasonCode reasonCode = reasonCode(true);
        JournalSummary journalSummary = new JournalSummary();
        journalSummary.setId(77L);

        when(reconciliationDifferenceRepository.findById(30L)).thenReturn(Optional.of(difference));
        when(differenceReasonCodeRepository.findById(40L)).thenReturn(Optional.of(reasonCode));
        when(journalQueryPort.getJournalSummary(77L)).thenReturn(journalSummary);
        when(reconciliationDifferenceRepository.save(any(ReconciliationDifference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationDifference resolved = reconciliationService.resolveDifference(
                30L,
                40L,
                77L,
                ReconciliationDifference.ReconciliationDifferenceStatus.RESOLVED,
                "resolver"
        );

        assertThat(resolved.getAdjustmentJournalEntryId()).isEqualTo(77L);
        assertThat(resolved.getReasonCode()).isSameAs(reasonCode);
        assertThat(resolved.getStatus()).isEqualTo(ReconciliationDifference.ReconciliationDifferenceStatus.RESOLVED);
        assertThat(resolved.getResolvedBy()).isEqualTo("resolver");
    }

    private void stubRunAndDifferenceSaves() {
        stubRunSave();
        when(reconciliationDifferenceRepository.save(any(ReconciliationDifference.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubRunSave() {
        when(reconciliationRunRepository.save(any(ReconciliationRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubJournalTarget(LocalDate reconciliationDate, String debitAmount) {
        when(journalQueryPort.getJournalDetailAggregateByAccount(reconciliationDate, reconciliationDate, JournalSide.DEBIT, null))
                .thenReturn(new JournalDetailAggregateSummary(1L, new BigDecimal(debitAmount)));
    }

    private void stubExternalSnapshot(long count, String amount) {
        when(externalReconSnapshotPort.loadSnapshot(any(ExternalReconSnapshotRequest.class))).thenReturn(
                new ExternalReconSnapshot(count, new BigDecimal(amount)));
    }

    private ReconciliationUnit reconciliationUnit(String criteriaJson) {
        ReconciliationUnit unit = new ReconciliationUnit();
        unit.setId(10L);
        unit.setName("GL daily reconciliation");
        unit.setCriteriaJson(criteriaJson);
        return unit;
    }

    private DifferenceReasonCode reasonCode(boolean adjustable) {
        DifferenceReasonCode reasonCode = new DifferenceReasonCode();
        reasonCode.setCode("GENERIC_MISMATCH");
        reasonCode.setName("Generic Mismatch");
        reasonCode.setAdjustable(adjustable);
        reasonCode.setActive(true);
        return reasonCode;
    }

    private ReconciliationRule reconciliationRule(ReconciliationRule.ToleranceType toleranceType, String toleranceValue,
            boolean active) {
        ReconciliationRule rule = new ReconciliationRule();
        rule.setToleranceType(toleranceType);
        rule.setToleranceValue(new BigDecimal(toleranceValue));
        rule.setActive(active);
        return rule;
    }
}
