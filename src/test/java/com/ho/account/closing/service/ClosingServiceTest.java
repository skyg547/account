package com.ho.account.closing.service;

import com.ho.account.basic.domain.FiscalPeriod;
import com.ho.account.basic.repository.FiscalPeriodRepository;
import com.ho.account.closing.domain.*;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.ClosingGate.ClosingGateStatus;
import com.ho.account.closing.domain.ClosingTask.ClosingTaskStatus;
import com.ho.account.closing.domain.PeriodLock.PeriodLockType;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import com.ho.account.closing.repository.*;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.repository.JournalEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClosingServiceTest {

    @Mock private ClosingCalendarRepository closingCalendarRepository;
    @Mock private ClosingTaskRepository closingTaskRepository;
    @Mock private ClosingGateRepository closingGateRepository;
    @Mock private PeriodLockRepository periodLockRepository;
    @Mock private ReopenApprovalRepository reopenApprovalRepository;
    @Mock private ValuationBatchRepository valuationBatchRepository;
    @Mock private ProvisionBatchRepository provisionBatchRepository;
    @Mock private ClosingAdjustmentRepository closingAdjustmentRepository;
    @Mock private FiscalPeriodRepository fiscalPeriodRepository;
    @Mock private JournalEntryRepository journalEntryRepository;

    @InjectMocks
    private ClosingService closingService;

    private FiscalPeriod testFiscalPeriod;
    private ClosingCalendar testClosingCalendar;
    private ClosingTask testClosingTask;
    private ClosingGate testClosingGate;

    @BeforeEach
    void setUp() {
        testFiscalPeriod = new FiscalPeriod();
        testFiscalPeriod.setId(1L);
        testFiscalPeriod.setFiscalYear("2023");
        testFiscalPeriod.setFiscalPeriod("12");
        testFiscalPeriod.setStartDate(LocalDate.of(2023, 12, 1));
        testFiscalPeriod.setEndDate(LocalDate.of(2023, 12, 31));
        testFiscalPeriod.setClosingStatus(FiscalPeriod.ClosingStatus.OPEN);

        testClosingCalendar = new ClosingCalendar();
        testClosingCalendar.setId(10L);
        testClosingCalendar.setFiscalYear("2023");
        testClosingCalendar.setFiscalPeriod("12");
        testClosingCalendar.setStatus(ClosingCalendarStatus.OPEN);

        testClosingTask = new ClosingTask();
        testClosingTask.setId(20L);
        testClosingTask.setClosingCalendar(testClosingCalendar);
        testClosingTask.setName("Mandatory Task");
        testClosingTask.setMandatory(true);
        testClosingTask.setStatus(ClosingTaskStatus.PENDING);
        testClosingTask.setTaskOrder(1);

        testClosingGate = new ClosingGate();
        testClosingGate.setId(30L);
        testClosingGate.setClosingCalendar(testClosingCalendar);
        testClosingGate.setName("All Tasks Completed Gate");
        testClosingGate.setStatus(ClosingGateStatus.PENDING);
    }

    @Test
    void testCreateClosingCalendar() {
        when(fiscalPeriodRepository.findByFiscalYearAndFiscalPeriod(anyString(), anyString()))
                .thenReturn(Optional.of(testFiscalPeriod));
        when(closingCalendarRepository.save(any(ClosingCalendar.class))).thenReturn(testClosingCalendar);

        ClosingCalendar created = closingService.createClosingCalendar(testClosingCalendar);

        assertNotNull(created);
        assertEquals(ClosingCalendarStatus.OPEN, created.getStatus());
        verify(closingCalendarRepository, times(1)).save(testClosingCalendar);
    }

    @Test
    void testUpdateClosingCalendarStatus_close() {
        when(closingCalendarRepository.findById(anyLong())).thenReturn(Optional.of(testClosingCalendar));
        when(closingCalendarRepository.save(any(ClosingCalendar.class))).thenReturn(testClosingCalendar);

        ClosingCalendar updated = closingService.updateClosingCalendarStatus(10L, ClosingCalendarStatus.CLOSED, "tester");

        assertNotNull(updated);
        assertEquals(ClosingCalendarStatus.CLOSED, updated.getStatus());
        assertEquals("tester", updated.getClosedBy());
        assertNotNull(updated.getClosedAt());
    }

    @Test
    void testUpdateClosingTaskStatus() {
        when(closingTaskRepository.findById(anyLong())).thenReturn(Optional.of(testClosingTask));
        when(closingTaskRepository.save(any(ClosingTask.class))).thenReturn(testClosingTask);

        ClosingTask updated = closingService.updateClosingTaskStatus(20L, ClosingTaskStatus.COMPLETED, "tester");

        assertNotNull(updated);
        assertEquals(ClosingTaskStatus.COMPLETED, updated.getStatus());
        assertEquals("tester", updated.getAuditUser());
    }

    @Test
    void testCheckAndPassClosingGate() {
        when(closingGateRepository.findById(anyLong())).thenReturn(Optional.of(testClosingGate));
        when(closingGateRepository.save(any(ClosingGate.class))).thenReturn(testClosingGate);
        // conditionsMet = true 상황을 시뮬레이션

        ClosingGate passedGate = closingService.checkAndPassClosingGate(30L, "approver");

        assertNotNull(passedGate);
        assertEquals(ClosingGateStatus.PASSED, passedGate.getStatus());
        assertEquals("approver", passedGate.getPassedBy());
        assertNotNull(passedGate.getPassedAt());
    }

    @Test
    void testLockPeriod_success() {
        when(fiscalPeriodRepository.findById(anyLong())).thenReturn(Optional.of(testFiscalPeriod));
        when(periodLockRepository.findByFiscalPeriod(any(FiscalPeriod.class))).thenReturn(Optional.empty());
        when(periodLockRepository.save(any(PeriodLock.class))).thenReturn(new PeriodLock());

        PeriodLock lock = closingService.lockPeriod(1L, PeriodLockType.ALL_TRANSACTIONS, "admin", "closing");

        assertNotNull(lock);
        verify(periodLockRepository, times(1)).save(any(PeriodLock.class));
    }

    @Test
    void testRequestPeriodReopen_success() {
        testFiscalPeriod.setClosingStatus(FiscalPeriod.ClosingStatus.CLOSED);
        when(fiscalPeriodRepository.findById(anyLong())).thenReturn(Optional.of(testFiscalPeriod));
        when(reopenApprovalRepository.save(any(ReopenApproval.class))).thenReturn(new ReopenApproval());

        ReopenApproval approval = closingService.requestPeriodReopen(1L, "user", "urgent adjustment");

        assertNotNull(approval);
        assertEquals(ReopenApprovalStatus.PENDING, approval.getStatus());
        verify(reopenApprovalRepository, times(1)).save(any(ReopenApproval.class));
    }

    @Test
    void testUpdateReopenApprovalStatus_approved() {
        ReopenApproval pendingApproval = new ReopenApproval();
        pendingApproval.setId(1L);
        pendingApproval.setFiscalPeriod(testFiscalPeriod);
        pendingApproval.setStatus(ReopenApprovalStatus.PENDING);
        testFiscalPeriod.setClosingStatus(FiscalPeriod.ClosingStatus.CLOSED); // 재오픈 전에 기간이 마감 상태인지 확인

        when(reopenApprovalRepository.findById(anyLong())).thenReturn(Optional.of(pendingApproval));
        when(reopenApprovalRepository.save(any(ReopenApproval.class))).thenReturn(pendingApproval);
        when(fiscalPeriodRepository.save(any(FiscalPeriod.class))).thenReturn(testFiscalPeriod);
        when(closingCalendarRepository.findByFiscalYearAndFiscalPeriod(anyString(), anyString()))
                .thenReturn(Optional.of(testClosingCalendar));
        when(closingCalendarRepository.save(any(ClosingCalendar.class))).thenReturn(testClosingCalendar);
        when(periodLockRepository.findByFiscalPeriod(any(FiscalPeriod.class))).thenReturn(Optional.empty()); // 기존 잠금 없음

        ReopenApproval approved = closingService.updateReopenApprovalStatus(1L, ReopenApprovalStatus.APPROVED, "manager");

        assertNotNull(approved);
        assertEquals(ReopenApprovalStatus.APPROVED, approved.getStatus());
        assertEquals(FiscalPeriod.ClosingStatus.OPEN, testFiscalPeriod.getClosingStatus());
        assertEquals(ClosingCalendarStatus.OPEN, testClosingCalendar.getStatus());
        verify(fiscalPeriodRepository, times(1)).save(testFiscalPeriod);
    }

    @Test
    void testDetermineClosingStatus_success() {
        // 성공적인 마감을 위한 캘린더, 태스크, 게이트 준비
        testClosingCalendar.setStatus(ClosingCalendarStatus.IN_PROGRESS);
        testClosingTask.setStatus(ClosingTaskStatus.COMPLETED);
        testClosingGate.setStatus(ClosingGateStatus.PASSED);

        when(closingCalendarRepository.findById(anyLong())).thenReturn(Optional.of(testClosingCalendar));
        when(closingTaskRepository.findByClosingCalendarOrderByTaskOrderAsc(any(ClosingCalendar.class)))
                .thenReturn(Arrays.asList(testClosingTask));
        when(closingGateRepository.findByClosingCalendar(any(ClosingCalendar.class)))
                .thenReturn(Arrays.asList(testClosingGate));
        when(closingCalendarRepository.save(any(ClosingCalendar.class))).thenReturn(testClosingCalendar);

        ClosingCalendar finalCalendar = closingService.determineClosingStatus(10L, "final_approver");

        assertNotNull(finalCalendar);
        assertEquals(ClosingCalendarStatus.CLOSED, finalCalendar.getStatus());
    }

    @Test
    void testDetermineClosingStatus_tasksNotCompleted_throwsException() {
        // 태스크는 PENDING 상태 유지
        when(closingCalendarRepository.findById(anyLong())).thenReturn(Optional.of(testClosingCalendar));
        when(closingTaskRepository.findByClosingCalendarOrderByTaskOrderAsc(any(ClosingCalendar.class)))
                .thenReturn(Arrays.asList(testClosingTask));
        when(closingCalendarRepository.save(any(ClosingCalendar.class))).thenReturn(testClosingCalendar);


        assertThrows(IllegalStateException.class, () -> closingService.determineClosingStatus(10L, "final_approver"));
        verify(closingCalendarRepository, times(1)).save(any(ClosingCalendar.class)); // 캘린더 상태가 IN_PROGRESS로 변경됨
    }
}
