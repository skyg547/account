package com.ho.account.reconciliation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.repository.JournalDetailRepository;
import com.ho.account.journal.repository.JournalEntryRepository;
import com.ho.account.reconciliation.domain.DifferenceReasonCode;
import com.ho.account.reconciliation.domain.ReconciliationDifference;
import com.ho.account.reconciliation.domain.ReconciliationRule;
import com.ho.account.reconciliation.domain.ReconciliationRun;
import com.ho.account.reconciliation.domain.ReconciliationUnit;
import com.ho.account.reconciliation.repository.DifferenceReasonCodeRepository;
import com.ho.account.reconciliation.repository.ReconciliationDifferenceRepository;
import com.ho.account.reconciliation.repository.ReconciliationRuleRepository;
import com.ho.account.reconciliation.repository.ReconciliationRunRepository;
import com.ho.account.reconciliation.repository.ReconciliationUnitRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock private ReconciliationUnitRepository reconciliationUnitRepository;
    @Mock private ReconciliationRuleRepository reconciliationRuleRepository;
    @Mock private DifferenceReasonCodeRepository differenceReasonCodeRepository;
    @Mock private ReconciliationRunRepository reconciliationRunRepository;
    @Mock private ReconciliationDifferenceRepository reconciliationDifferenceRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private JournalDetailRepository journalDetailRepository;
    @Mock private AccountSubjectRepository accountSubjectRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private ReconciliationService reconciliationService;

    private ReconciliationUnit testUnit;
    private DifferenceReasonCode defaultReasonCode;
    private AccountSubject debitAccount;
    private AccountSubject creditAccount;

    @BeforeEach
    void setUp() {
        testUnit = new ReconciliationUnit();
        testUnit.setId(1L);
        testUnit.setName("Bank vs Book");
        testUnit.setDescription("Bank statement vs General Ledger reconciliation");
        testUnit.setFrequency(ReconciliationUnit.ReconciliationFrequency.DAILY);
        testUnit.setReconciliationType(ReconciliationUnit.ReconciliationType.BANK_BOOK);
        testUnit.setCriteriaJson("{\"bankAccount\":\"123-456\", \"currency\":\"KRW\"}");
        testUnit.setActive(true);

        defaultReasonCode = new DifferenceReasonCode();
        defaultReasonCode.setId(1L);
        defaultReasonCode.setCode("GENERIC_MISMATCH");
        defaultReasonCode.setName("Generic mismatch");
        defaultReasonCode.setDescription("Default mismatch");
        defaultReasonCode.setAdjustable(true);
        defaultReasonCode.setActive(true);

        debitAccount = new AccountSubject();
        debitAccount.setCode("121000");
        debitAccount.setName("Suspense");

        creditAccount = new AccountSubject();
        creditAccount.setCode("999999");
        creditAccount.setName("Recon Adjustment");
    }

    @Test
    void testCreateReconciliationUnit() {
        when(reconciliationUnitRepository.save(any(ReconciliationUnit.class))).thenReturn(testUnit);

        ReconciliationUnit createdUnit = reconciliationService.createReconciliationUnit(testUnit);

        assertNotNull(createdUnit);
        assertEquals("Bank vs Book", createdUnit.getName());
        verify(reconciliationUnitRepository, times(1)).save(testUnit);
    }

    @Test
    void testFindReconciliationUnitById_found() {
        when(reconciliationUnitRepository.findById(anyLong())).thenReturn(Optional.of(testUnit));

        ReconciliationUnit foundUnit = reconciliationService.findReconciliationUnitById(1L);

        assertNotNull(foundUnit);
        assertEquals("Bank vs Book", foundUnit.getName());
    }

    @Test
    void testFindReconciliationUnitById_notFound() {
        when(reconciliationUnitRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> reconciliationService.findReconciliationUnitById(1L));
    }

    @Test
    void testPerformReconciliation_successWithDifferenceAndAdjustment() {
        LocalDate reconciliationDate = LocalDate.now();

        when(reconciliationUnitRepository.findById(anyLong())).thenReturn(Optional.of(testUnit));
        when(reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(any(ReconciliationUnit.class)))
                .thenReturn(Collections.<ReconciliationRule>emptyList());
        when(reconciliationRunRepository.save(any(ReconciliationRun.class))).thenAnswer(invocation -> {
            ReconciliationRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                run.setId(1L);
            }
            return run;
        });
        when(differenceReasonCodeRepository.findByCode(anyString())).thenReturn(Optional.of(defaultReasonCode));
        when(accountSubjectRepository.findById("121000")).thenReturn(Optional.of(debitAccount));
        when(accountSubjectRepository.findById("999999")).thenReturn(Optional.of(creditAccount));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry entry = invocation.getArgument(0);
            if (entry.getId() == null) {
                entry.setId(100L);
            }
            return entry;
        });
        when(journalEntryRepository.count()).thenReturn(1L);
        when(reconciliationDifferenceRepository.save(any(ReconciliationDifference.class))).thenAnswer(invocation -> {
            ReconciliationDifference difference = invocation.getArgument(0);
            if (difference.getId() == null) {
                difference.setId(200L);
            }
            return difference;
        });

        ReconciliationRun resultRun = reconciliationService.performReconciliation(1L, reconciliationDate);

        assertNotNull(resultRun);
        assertEquals(ReconciliationRun.ReconciliationRunStatus.SUCCESS, resultRun.getStatus());
        assertEquals(BigDecimal.valueOf(50.00).setScale(2), resultRun.getUnmatchedAmount().setScale(2));
        verify(reconciliationRunRepository, times(2)).save(any(ReconciliationRun.class));
        verify(reconciliationDifferenceRepository, times(1)).save(any(ReconciliationDifference.class));
        verify(journalEntryRepository, times(1)).save(any(JournalEntry.class));
    }

    @Test
    void testAssignDifference() {
        ReconciliationDifference difference = new ReconciliationDifference();
        difference.setId(1L);
        difference.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.PENDING);

        when(reconciliationDifferenceRepository.findById(anyLong())).thenReturn(Optional.of(difference));
        when(reconciliationDifferenceRepository.save(any(ReconciliationDifference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime slaDueDate = LocalDateTime.now().plusDays(7);
        ReconciliationDifference assignedDiff = reconciliationService.assignDifference(1L, "user123", slaDueDate);

        assertNotNull(assignedDiff);
        assertEquals("user123", assignedDiff.getAssignedToUser());
        assertEquals(slaDueDate, assignedDiff.getSlaDueDate());
        assertEquals(ReconciliationDifference.ReconciliationDifferenceStatus.ASSIGNED, assignedDiff.getStatus());
    }

    @Test
    void testResolveDifference_withAdjustableReasonAndAdjustmentEntry() {
        ReconciliationDifference difference = new ReconciliationDifference();
        difference.setId(1L);
        difference.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.ASSIGNED);

        JournalEntry adjustmentEntry = new JournalEntry();
        adjustmentEntry.setId(100L);
        adjustmentEntry.setEntryType("ADJUSTMENT");

        when(reconciliationDifferenceRepository.findById(1L)).thenReturn(Optional.of(difference));
        when(differenceReasonCodeRepository.findById(1L)).thenReturn(Optional.of(defaultReasonCode));
        when(journalEntryRepository.findById(100L)).thenReturn(Optional.of(adjustmentEntry));
        when(reconciliationDifferenceRepository.save(any(ReconciliationDifference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationDifference resolved = reconciliationService.resolveDifference(
                1L,
                1L,
                100L,
                ReconciliationDifference.ReconciliationDifferenceStatus.RESOLVED,
                "resolver1");

        assertEquals(ReconciliationDifference.ReconciliationDifferenceStatus.RESOLVED, resolved.getStatus());
        assertEquals("resolver1", resolved.getResolvedBy());
        assertNotNull(resolved.getResolvedAt());
        assertEquals(defaultReasonCode, resolved.getReasonCode());
        assertEquals(adjustmentEntry, resolved.getAdjustmentJournalEntry());
    }

    @Test
    void testResolveDifference_adjustableReasonWithoutAdjustmentEntry_throwsException() {
        ReconciliationDifference difference = new ReconciliationDifference();
        difference.setId(1L);
        difference.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.ASSIGNED);

        when(reconciliationDifferenceRepository.findById(1L)).thenReturn(Optional.of(difference));
        when(differenceReasonCodeRepository.findById(1L)).thenReturn(Optional.of(defaultReasonCode));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reconciliationService.resolveDifference(
                        1L,
                        1L,
                        null,
                        ReconciliationDifference.ReconciliationDifferenceStatus.RESOLVED,
                        "resolver1"));

        assertTrue(exception.getMessage().contains("Adjustable reason code requires"));
    }
}
