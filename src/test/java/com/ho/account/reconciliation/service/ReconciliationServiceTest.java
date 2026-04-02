package com.ho.account.reconciliation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.repository.JournalDetailRepository;
import com.ho.account.journal.repository.JournalEntryRepository;
import com.ho.account.reconciliation.domain.*;
import com.ho.account.reconciliation.domain.ReconciliationDifference.DifferenceType;
import com.ho.account.reconciliation.domain.ReconciliationDifference.ReconciliationDifferenceStatus;
import com.ho.account.reconciliation.domain.ReconciliationRun.ReconciliationRunStatus;
import com.ho.account.reconciliation.domain.ReconciliationUnit.ReconciliationFrequency;
import com.ho.account.reconciliation.domain.ReconciliationUnit.ReconciliationType;
import com.ho.account.reconciliation.repository.*;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private JournalDetailRepository journalDetailRepository;
    @Mock
    private AccountSubjectRepository accountSubjectRepository;
    @Mock
    private ObjectMapper objectMapper; // ObjectMapper도 mock 처리

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
        testUnit.setFrequency(ReconciliationFrequency.DAILY);
        testUnit.setReconciliationType(ReconciliationType.BANK_BOOK);
        testUnit.setCriteriaJson("{\"bankAccount\":\"123-456\", \"currency\":\"KRW\"}");
        testUnit.setActive(true);

        defaultReasonCode = new DifferenceReasonCode();
        defaultReasonCode.setId(1L);
        defaultReasonCode.setCode("GENERIC_MISMATCH");
        defaultReasonCode.setName("일반 불일치");
        defaultReasonCode.setDescription("자동 매칭되지 않은 일반적인 불일치");
        defaultReasonCode.setAdjustable(true);
        defaultReasonCode.setActive(true);

        debitAccount = new AccountSubject();
        debitAccount.setCode("121000");
        debitAccount.setName("미결제 계정");

        creditAccount = new AccountSubject();
        creditAccount.setCode("999999");
        creditAccount.setName("대사차이 조정 계정");
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
        verify(reconciliationUnitRepository, times(1)).findById(1L);
    }

    @Test
    void testFindReconciliationUnitById_notFound() {
        when(reconciliationUnitRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> reconciliationService.findReconciliationUnitById(1L));
        verify(reconciliationUnitRepository, times(1)).findById(1L);
    }

    @Test
    void testPerformReconciliation_successWithDifferenceAndAdjustment() {
        LocalDate reconciliationDate = LocalDate.now();

        // performReconciliation용 목 설정
        when(reconciliationUnitRepository.findById(anyLong())).thenReturn(Optional.of(testUnit));
        when(reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(any(ReconciliationUnit.class))).thenReturn(Collections.emptyList()); // 이 기본 테스트에서는 규칙을 적용하지 않음
        when(reconciliationRunRepository.save(any(ReconciliationRun.class))).thenAnswer(invocation -> {
            ReconciliationRun run = invocation.getArgument(0);
            if (run.getId() == null) run.setId(1L); // ID 생성 시뮬레이션
            return run;
        });
        when(differenceReasonCodeRepository.findByCode(anyString())).thenReturn(Optional.of(defaultReasonCode));
        when(differenceReasonCodeRepository.save(any(DifferenceReasonCode.class))).thenReturn(defaultReasonCode); // createDefaultReasonCode용
        when(accountSubjectRepository.findById("121000")).thenReturn(Optional.of(debitAccount));
        when(accountSubjectRepository.findById("999999")).thenReturn(Optional.of(creditAccount));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry je = invocation.getArgument(0);
            if (je.getId() == null) je.setId(100L); // ID 생성 시뮬레이션
            return je;
        });
        when(journalEntryRepository.count()).thenReturn(1L); // slipNo 생성용
        when(reconciliationDifferenceRepository.save(any(ReconciliationDifference.class))).thenAnswer(invocation -> {
            ReconciliationDifference diff = invocation.getArgument(0);
            if (diff.getId() == null) diff.setId(200L); // ID 생성 시뮬레이션
            return diff;
        });

        ReconciliationRun resultRun = reconciliationService.performReconciliation(1L, reconciliationDate);

        assertNotNull(resultRun);
        assertEquals(ReconciliationRunStatus.SUCCESS, resultRun.getStatus());
        assertEquals(BigDecimal.valueOf(100.00).setScale(2), resultRun.getUnmatchedAmount().setScale(2)); // 목 처리된 미매칭 금액 확인

        verify(reconciliationUnitRepository, times(1)).findById(1L);
        verify(reconciliationRunRepository, times(2)).save(any(ReconciliationRun.class)); // 초기 저장 및 최종 갱신
        verify(reconciliationDifferenceRepository, times(1)).save(any(ReconciliationDifference.class));
        verify(journalEntryRepository, times(1)).save(any(JournalEntry.class)); // 조정 전표 생성 여부 검증
    }

    @Test
    void testAssignDifference() {
        ReconciliationDifference difference = new ReconciliationDifference();
        difference.setId(1L);
        difference.setStatus(ReconciliationDifferenceStatus.PENDING);

        when(reconciliationDifferenceRepository.findById(anyLong())).thenReturn(Optional.of(difference));
        when(reconciliationDifferenceRepository.save(any(ReconciliationDifference.class))).thenReturn(difference);

        LocalDateTime slaDueDate = LocalDateTime.now().plusDays(7);
        ReconciliationDifference assignedDiff = reconciliationService.assignDifference(1L, "user123", slaDueDate);

        assertNotNull(assignedDiff);
        assertEquals("user123", assignedDiff.getAssignedToUser());
        assertEquals(slaDueDate, assignedDiff.getSlaDueDate());
        assertEquals(ReconciliationDifferenceStatus.ASSIGNED, assignedDiff.getStatus());
        verify(reconciliationDifferenceRepository, times(1)).findById(1L);
        verify(reconciliationDifferenceRepository, times(1)).save(difference);
    }

    @Test
    void testResolveDifference_withAdjustableReasonAndAdjustmentEntry() {
        ReconciliationDifference difference = new ReconciliationDifference();
        difference.setId(1L);
        difference.setStatus(ReconciliationDifferenceStatus.ASSIGNED);

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
                ReconciliationDifferenceStatus.RESOLVED,
                "resolver1"
        );

        assertEquals(ReconciliationDifferenceStatus.RESOLVED, resolved.getStatus());
        assertEquals("resolver1", resolved.getResolvedBy());
        assertNotNull(resolved.getResolvedAt());
        assertEquals(defaultReasonCode, resolved.getReasonCode());
        assertEquals(adjustmentEntry, resolved.getAdjustmentJournalEntry());
    }

    @Test
    void testResolveDifference_adjustableReasonWithoutAdjustmentEntry_throwsException() {
        ReconciliationDifference difference = new ReconciliationDifference();
        difference.setId(1L);
        difference.setStatus(ReconciliationDifferenceStatus.ASSIGNED);

        when(reconciliationDifferenceRepository.findById(1L)).thenReturn(Optional.of(difference));
        when(differenceReasonCodeRepository.findById(1L)).thenReturn(Optional.of(defaultReasonCode));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> reconciliationService.resolveDifference(
                        1L,
                        1L,
                        null,
                        ReconciliationDifferenceStatus.RESOLVED,
                        "resolver1"
                ));

        assertTrue(exception.getMessage().contains("Adjustable reason code requires"));
    }
}
