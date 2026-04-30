package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.in.ClosingUseCase;
import com.ho.account.closing.application.port.out.ClosingAdjustmentPersistencePort;
import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.application.port.out.ClosingGatePersistencePort;
import com.ho.account.closing.application.port.out.ClosingTaskPersistencePort;
import com.ho.account.closing.application.port.out.PeriodLockPersistencePort;
import com.ho.account.closing.application.port.out.ProvisionBatchPersistencePort;
import com.ho.account.closing.application.port.out.ReopenApprovalPersistencePort;
import com.ho.account.closing.application.port.out.ValuationBatchPersistencePort;
import com.ho.account.closing.domain.ClosingAdjustment;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingGate;
import com.ho.account.closing.domain.ClosingTask;
import com.ho.account.closing.domain.PeriodLock;
import com.ho.account.closing.domain.ProvisionBatch;
import com.ho.account.closing.domain.ReopenApproval;
import com.ho.account.closing.domain.ValuationBatch;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.ClosingGate.ClosingGateStatus;
import com.ho.account.closing.domain.ClosingTask.ClosingTaskStatus;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
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
    
    private final FiscalPeriodPersistencePort fiscalPeriodPersistencePort;
    private final JournalPostingPort journalPostingPort;

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
        calendar.setStatus(newStatus);
        if (newStatus == ClosingCalendarStatus.CLOSED) {
            calendar.setClosedBy(user);
            calendar.setClosedAt(LocalDateTime.now());
        }
        calendar.setAuditUser(user);
        return closingCalendarPersistencePort.save(calendar);
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
        task.setStatus(newStatus);
        task.setAuditUser(user);
        return closingTaskPersistencePort.save(task);
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
        return closingGatePersistencePort.save(gate);
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
        return periodLockPersistencePort.save(periodLock);
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
        return reopenApprovalPersistencePort.save(approval);
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

        ClosingAdjustment adjustment = new ClosingAdjustment();
        adjustment.setFiscalPeriod(fiscalPeriod);
        adjustment.setJournalEntryId(journalEntryId);
        adjustment.setAdjustmentType(adjustmentType);
        adjustment.setDescription(description);
        adjustment.setApprovedBy(approvedBy);
        adjustment.setApprovedAt(LocalDateTime.now());
        adjustment.setAuditUser(approvedBy);
        return closingAdjustmentPersistencePort.save(adjustment);
    }

    @Override
    public ClosingCalendar determineClosingStatus(Long calendarId, String user) {
        ClosingCalendar calendar = findClosingCalendarById(calendarId);
        calendar.setStatus(ClosingCalendarStatus.CLOSED);
        
        fiscalPeriodPersistencePort.findByFiscalYearAndFiscalPeriod(calendar.getFiscalYear(), calendar.getFiscalPeriod())
                .ifPresent(fp -> {
                    fp.setClosingStatus(FiscalPeriod.ClosingStatus.CLOSED);
                    fiscalPeriodPersistencePort.save(fp);
                });

        return closingCalendarPersistencePort.save(calendar);
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
