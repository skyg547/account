package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.in.ClosingUseCase;
import com.ho.account.closing.application.port.out.ClosingAdjustmentPersistencePort;
import com.ho.account.closing.application.port.out.ClosingAuditLogPersistencePort;
import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.application.port.out.ClosingGatePersistencePort;
import com.ho.account.closing.application.port.out.ClosingTaskPersistencePort;
import com.ho.account.closing.application.port.out.PeriodLockPersistencePort;
import com.ho.account.closing.application.port.out.ProvisionBatchPersistencePort;
import com.ho.account.closing.application.port.out.ReopenApprovalPersistencePort;
import com.ho.account.closing.application.port.out.ValuationBatchPersistencePort;
import com.ho.account.closing.domain.ClosingAdjustment;
import com.ho.account.closing.domain.ClosingAuditLog;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingGate;
import com.ho.account.closing.domain.ClosingTask;
import com.ho.account.closing.domain.PeriodLock;
import com.ho.account.closing.domain.ProvisionBatch;
import com.ho.account.closing.domain.ReopenApproval;
import com.ho.account.closing.domain.ValuationBatch;
import com.ho.account.closing.domain.ClosingAuditLog.ActionType;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.ClosingGate.ClosingGateStatus;
import com.ho.account.closing.domain.ClosingTask.ClosingTaskStatus;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import com.ho.account.masterdata.core.application.port.out.FiscalPeriodPersistencePort;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결산 (Closing) 관련 비즈니스 로직을 처리하는 서비스 클래스.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ClosingService implements ClosingUseCase {

    private final ClosingCalendarPersistencePort closingCalendarPersistencePort;
    private final ClosingTaskPersistencePort closingTaskPersistencePort;
    private final ClosingGatePersistencePort closingGatePersistencePort;
    private final PeriodLockPersistencePort periodLockPersistencePort;
    private final ReopenApprovalPersistencePort reopenApprovalPersistencePort;
    private final ValuationBatchPersistencePort valuationBatchPersistencePort;
    private final ProvisionBatchPersistencePort provisionBatchPersistencePort;
    private final ClosingAdjustmentPersistencePort closingAdjustmentPersistencePort;
    private final ClosingAuditLogPersistencePort closingAuditLogPersistencePort;
    
    private final FiscalPeriodPersistencePort fiscalPeriodPersistencePort;
    private final JournalPostingPort journalPostingPort;
    private final JournalQueryPort journalQueryPort;

    @Transactional(readOnly = true)
    @Override
    public boolean isClosed(LocalDate date) {
        String fiscalYear = String.valueOf(date.getYear());
        String fiscalPeriodStr = String.format("%02d", date.getMonthValue());

        return fiscalPeriodPersistencePort.findByFiscalYearAndFiscalPeriod(fiscalYear, fiscalPeriodStr)
                .map(fp -> fp.getClosingStatus() == FiscalPeriod.ClosingStatus.CLOSED ||
                           fp.getClosingStatus() == FiscalPeriod.ClosingStatus.PERMANENTLY_CLOSED)
                .orElse(false);
    }

    @Override
    public ClosingCalendar createClosingCalendar(ClosingCalendar closingCalendar) {
        fiscalPeriodPersistencePort.findByFiscalYearAndFiscalPeriod(
                closingCalendar.getFiscalYear(), closingCalendar.getFiscalPeriod())
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));

        closingCalendar.setStatus(ClosingCalendarStatus.OPEN);
        return closingCalendarPersistencePort.save(closingCalendar);
    }

    @Transactional(readOnly = true)
    @Override
    public ClosingCalendar findClosingCalendarById(Long id) {
        return closingCalendarPersistencePort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ClosingCalendar not found with id: " + id));
    }

    @Transactional(readOnly = true)
    @Override
    public ClosingCalendar findClosingCalendarByFiscalPeriod(String fiscalYear, String fiscalPeriod) {
        return closingCalendarPersistencePort.findByFiscalYearAndFiscalPeriod(fiscalYear, fiscalPeriod)
                .orElseThrow(() -> new EntityNotFoundException("ClosingCalendar not found"));
    }

    @Override
    public ClosingCalendar updateClosingCalendarStatus(Long id, ClosingCalendarStatus newStatus, String user) {
        ClosingCalendar calendar = findClosingCalendarById(id);
        String prevStatus = calendar.getStatus().name();
        
        calendar.setStatus(newStatus);
        if (newStatus == ClosingCalendarStatus.CLOSED) {
            calendar.setClosedBy(user);
            calendar.setClosedAt(LocalDateTime.now());
        }
        calendar.setAuditUser(user);
        ClosingCalendar saved = closingCalendarPersistencePort.save(calendar);

        // 감사 로그 기록
        closingAuditLogPersistencePort.save(ClosingAuditLog.create(
                saved, ActionType.CALENDAR_IN_PROGRESS, prevStatus, newStatus.name(), user, "Status updated by user"));
        
        return saved;
    }

    @Override
    public ClosingTask createClosingTask(ClosingTask closingTask) {
        ClosingCalendar calendar = findClosingCalendarById(closingTask.getClosingCalendar().getId());
        closingTask.setClosingCalendar(calendar);
        closingTask.setStatus(ClosingTaskStatus.PENDING);
        return closingTaskPersistencePort.save(closingTask);
    }

    @Override
    public ClosingTask updateClosingTaskStatus(Long taskId, ClosingTaskStatus newStatus, String user) {
        ClosingTask task = closingTaskPersistencePort.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("ClosingTask not found"));
        String prevStatus = task.getStatus().name();
        
        // 도메인 메서드 활용
        if (newStatus == ClosingTaskStatus.COMPLETED) {
            task.complete(user);
        } else if (newStatus == ClosingTaskStatus.IN_PROGRESS) {
            task.start(user);
        } else {
            task.setStatus(newStatus);
            task.setAuditUser(user);
        }

        ClosingTask saved = closingTaskPersistencePort.save(task);

        // 감사 로그 기록
        closingAuditLogPersistencePort.save(ClosingAuditLog.create(
                saved.getClosingCalendar(), ActionType.TASK_STATUS_CHANGED, 
                "TASK:" + task.getName() + ":" + prevStatus, newStatus.name(), user, "Task status updated"));

        return saved;
    }

    @Override
    public ClosingGate createClosingGate(ClosingGate closingGate) {
        ClosingCalendar calendar = findClosingCalendarById(closingGate.getClosingCalendar().getId());
        closingGate.setClosingCalendar(calendar);
        closingGate.setStatus(ClosingGateStatus.PENDING);
        return closingGatePersistencePort.save(closingGate);
    }

    @Override
    public ClosingGate checkAndPassClosingGate(Long gateId, String user) {
        ClosingGate gate = closingGatePersistencePort.findById(gateId)
                .orElseThrow(() -> new EntityNotFoundException("ClosingGate not found"));
        gate.setStatus(ClosingGateStatus.PASSED);
        gate.setPassedBy(user);
        gate.setPassedAt(LocalDateTime.now());
        gate.setAuditUser(user);
        ClosingGate saved = closingGatePersistencePort.save(gate);

        // 감사 로그 기록
        closingAuditLogPersistencePort.save(ClosingAuditLog.create(
                saved.getClosingCalendar(), ActionType.GATE_PASSED, 
                "GATE:" + gate.getName() + ":PENDING", "PASSED", user, "Gate passed"));

        return saved;
    }

    @Override
    public PeriodLock lockPeriod(Long fiscalPeriodId, PeriodLock.PeriodLockType lockType, String user, String reason) {
        FiscalPeriod fiscalPeriod = fiscalPeriodPersistencePort.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));

        PeriodLock periodLock = new PeriodLock();
        periodLock.setFiscalPeriod(fiscalPeriod);
        periodLock.setLockType(lockType);
        periodLock.setLockedBy(user);
        periodLock.setLockedAt(LocalDateTime.now());
        periodLock.setReason(reason);
        PeriodLock saved = periodLockPersistencePort.save(periodLock);

        // 로그는 Calendar 기반이므로 Calendar를 찾아야 함 (현재는 생략하거나 FP 기반 로그 구현 필요)
        return saved;
    }

    @Override
    public void unlockPeriod(Long fiscalPeriodId, String user) {
        FiscalPeriod fiscalPeriod = fiscalPeriodPersistencePort.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));

        periodLockPersistencePort.findByFiscalPeriod(fiscalPeriod).ifPresent(periodLockPersistencePort::delete);
    }

    @Override
    public ReopenApproval requestPeriodReopen(Long fiscalPeriodId, String requestedBy, String reason) {
        FiscalPeriod fiscalPeriod = fiscalPeriodPersistencePort.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));

        ReopenApproval approval = new ReopenApproval();
        approval.setFiscalPeriod(fiscalPeriod);
        approval.setRequestedBy(requestedBy);
        approval.setRequestedAt(LocalDateTime.now());
        approval.setReason(reason);
        approval.setStatus(ReopenApprovalStatus.PENDING);
        ReopenApproval saved = reopenApprovalPersistencePort.save(approval);

        // 감사 로그 (재오픈 요청)
        try {
            ClosingCalendar calendar = findClosingCalendarByFiscalPeriod(fiscalPeriod.getFiscalYear(), fiscalPeriod.getFiscalPeriod());
            closingAuditLogPersistencePort.save(ClosingAuditLog.create(
                    calendar, ActionType.REOPEN_REQUEST, "CLOSED", "REOPEN_PENDING", requestedBy, reason));
        } catch (Exception e) { /* ignore if calendar not found */ }

        return saved;
    }

    @Override
    public ReopenApproval updateReopenApprovalStatus(Long approvalId, ReopenApprovalStatus newStatus, String approvedBy) {
        ReopenApproval approval = reopenApprovalPersistencePort.findById(approvalId)
                .orElseThrow(() -> new EntityNotFoundException("ReopenApproval not found"));
        approval.setStatus(newStatus);
        approval.setApprovedBy(approvedBy);
        approval.setApprovedAt(LocalDateTime.now());
        
        if (newStatus == ReopenApprovalStatus.APPROVED) {
            FiscalPeriod fp = approval.getFiscalPeriod();
            fp.setClosingStatus(FiscalPeriod.ClosingStatus.OPEN);
            fiscalPeriodPersistencePort.save(fp);

            // 감사 로그 (재오픈 승인)
            try {
                ClosingCalendar calendar = findClosingCalendarByFiscalPeriod(fp.getFiscalYear(), fp.getFiscalPeriod());
                calendar.setStatus(ClosingCalendarStatus.OPEN);
                closingCalendarPersistencePort.save(calendar);
                closingAuditLogPersistencePort.save(ClosingAuditLog.create(
                        calendar, ActionType.REOPEN_APPROVED, "CLOSED", "OPEN", approvedBy, "Reopen approved"));
            } catch (Exception e) { /* ignore */ }
        }
        return reopenApprovalPersistencePort.save(approval);
    }

    @Override
    public ValuationBatch runValuationBatch(Long fiscalPeriodId, ValuationBatch.ValuationType valuationType, String runBy) {
        FiscalPeriod fiscalPeriod = fiscalPeriodPersistencePort.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));

        ValuationBatch batch = new ValuationBatch();
        batch.setFiscalPeriod(fiscalPeriod);
        batch.setValuationType(valuationType);
        batch.setRunDateTime(LocalDateTime.now());
        batch.setStatus(ValuationBatch.ValuationBatchStatus.RUNNING);
        batch.setRunBy(runBy);
        batch.setAuditUser(runBy);
        batch = valuationBatchPersistencePort.save(batch);

        try {
            JournalPostingResult result = createAutomatedJournalEntry(
                    fiscalPeriod.getEndDate(),
                    "자동 " + valuationType.name() + " 평가 분개",
                    "SYSTEM",
                    "VALUATION_BATCH",
                    batch.getId().toString(),
                    BigDecimal.valueOf(1000)
            );
            batch.setGeneratedJournalEntryId(result.journalEntryId());
            batch.setStatus(ValuationBatch.ValuationBatchStatus.COMPLETED);
            batch.setReportLink("/reports/valuation/" + batch.getId());
        } catch (Exception e) {
            batch.setStatus(ValuationBatch.ValuationBatchStatus.FAILED);
            throw new RuntimeException("Valuation batch failed", e);
        }
        return valuationBatchPersistencePort.save(batch);
    }

    @Override
    public ProvisionBatch runProvisionBatch(Long fiscalPeriodId, ProvisionBatch.ProvisionType provisionType, String runBy) {
        FiscalPeriod fiscalPeriod = fiscalPeriodPersistencePort.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));

        ProvisionBatch batch = new ProvisionBatch();
        batch.setFiscalPeriod(fiscalPeriod);
        batch.setProvisionType(provisionType);
        batch.setRunDateTime(LocalDateTime.now());
        batch.setStatus(ProvisionBatch.ProvisionBatchStatus.RUNNING);
        batch.setRunBy(runBy);
        batch = provisionBatchPersistencePort.save(batch);

        try {
            JournalPostingResult result = createAutomatedJournalEntry(
                    fiscalPeriod.getEndDate(),
                    "자동 " + provisionType.name() + " 충당 분개",
                    "SYSTEM",
                    "PROVISION_BATCH",
                    batch.getId().toString(),
                    BigDecimal.valueOf(500)
            );
            batch.setGeneratedJournalEntryId(result.journalEntryId());
            batch.setStatus(ProvisionBatch.ProvisionBatchStatus.COMPLETED);
        } catch (Exception e) {
            batch.setStatus(ProvisionBatch.ProvisionBatchStatus.FAILED);
            throw new RuntimeException("Provision batch failed", e);
        }
        return provisionBatchPersistencePort.save(batch);
    }

    @Override
    public ClosingAdjustment createClosingAdjustment(Long fiscalPeriodId, Long journalEntryId, ClosingAdjustment.AdjustmentType adjustmentType, String description, String approvedBy) {
        FiscalPeriod fiscalPeriod = fiscalPeriodPersistencePort.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));

        // 1. 회기 기간 상태 확인
        if (fiscalPeriod.getClosingStatus() != FiscalPeriod.ClosingStatus.OPEN) {
            throw new IllegalStateException("Fiscal period is not OPEN. Current status: " + fiscalPeriod.getClosingStatus());
        }

        // 2. 전표 존재 여부 확인
        JournalSummary journalSummary = journalQueryPort.getJournalSummary(journalEntryId);
        
        // 3. 회계 일자 정합성 확인
        if (journalSummary.getAccountingDate().isBefore(fiscalPeriod.getStartDate()) ||
            journalSummary.getAccountingDate().isAfter(fiscalPeriod.getEndDate())) {
            throw new IllegalArgumentException("Journal accounting date is outside the fiscal period range.");
        }

        // 4. 대차 평균 확인 (Balance Check)
        List<JournalDetailSummary> details = journalQueryPort.getJournalDetails(journalEntryId);
        BigDecimal totalDebit = details.stream()
                .filter(d -> d.getSide() == JournalSide.DEBIT)
                .map(JournalDetailSummary::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = details.stream()
                .filter(d -> d.getSide() == JournalSide.CREDIT)
                .map(JournalDetailSummary::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalStateException("Journal is not balanced. Debit: " + totalDebit + ", Credit: " + totalCredit);
        }
        
        if (totalDebit.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("Journal amount cannot be zero.");
        }

        ClosingAdjustment adjustment = new ClosingAdjustment();
        adjustment.setFiscalPeriod(fiscalPeriod);
        adjustment.setJournalEntryId(journalEntryId);
        adjustment.setAdjustmentType(adjustmentType);
        adjustment.setDescription(description);
        adjustment.setApprovedBy(approvedBy);
        adjustment.setApprovedAt(LocalDateTime.now());
        adjustment.setAuditUser(approvedBy);
        ClosingAdjustment saved = closingAdjustmentPersistencePort.save(adjustment);

        // 감사 로그 기록
        try {
            ClosingCalendar calendar = findClosingCalendarByFiscalPeriod(fiscalPeriod.getFiscalYear(), fiscalPeriod.getFiscalPeriod());
            closingAuditLogPersistencePort.save(ClosingAuditLog.create(
                    calendar, ActionType.ADJUSTMENT_CREATED, null, "CREATED", approvedBy, "Adjustment Entry ID: " + journalEntryId));
        } catch (Exception e) { /* ignore */ }

        return saved;
    }

    @Override
    public ClosingCalendar determineClosingStatus(Long calendarId, String user) {
        ClosingCalendar calendar = findClosingCalendarById(calendarId);
        String prevStatus = calendar.getStatus() != null ? calendar.getStatus().name() : ClosingCalendarStatus.OPEN.name();

        // 1. 도메인 메서드에 완료 가능 여부 위임
        List<ClosingTask> tasks = closingTaskPersistencePort.findByClosingCalendar(calendar);
        List<ClosingGate> gates = closingGatePersistencePort.findByClosingCalendar(calendar);
        calendar.validateReadyToClose(tasks, gates);

        // 2. 도메인 메서드에 상태 변경 위임
        calendar.close(user);
        
        fiscalPeriodPersistencePort.findByFiscalYearAndFiscalPeriod(calendar.getFiscalYear(), calendar.getFiscalPeriod())
                .ifPresent(fp -> {
                    fp.setClosingStatus(FiscalPeriod.ClosingStatus.CLOSED);
                    fiscalPeriodPersistencePort.save(fp);
                });

        ClosingCalendar saved = closingCalendarPersistencePort.save(calendar);

        // 감사 로그 기록
        closingAuditLogPersistencePort.save(ClosingAuditLog.create(
                saved, ActionType.CALENDAR_CLOSED, prevStatus, "CLOSED", user, "Closing completed successfully"));

        return saved;
    }

    private JournalPostingResult createAutomatedJournalEntry(
            LocalDate accountingDate,
            String description,
            String createdBy,
            String lineageSourceType,
            String lineageSourceId,
            BigDecimal amount) {
        List<JournalLineCommand> lines = List.of(
                new JournalLineCommand("DEBIT", "999998", amount, amount, null, null, description + " (차변)"),
                new JournalLineCommand("CREDIT", "999999", amount, amount, null, null, description + " (대변)")
        );

        JournalEntryCommand command = new JournalEntryCommand(
                accountingDate,
                accountingDate,
                description,
                "ADJUSTMENT",
                "KRW",
                BigDecimal.ONE,
                createdBy,
                createdBy,
                lineageSourceType,
                lineageSourceId,
                lines
        );

        return journalPostingPort.createDraftEntry(command);
    }
}
