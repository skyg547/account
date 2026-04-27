package com.ho.account.closing.service;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.FiscalPeriodPersistencePort;
import com.ho.account.closing.domain.*;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.ClosingGate.ClosingGateStatus;
import com.ho.account.closing.domain.ClosingTask.ClosingTaskStatus;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import com.ho.account.closing.repository.*;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 결산 (Closing) 관련 비즈니스 로직을 처리하는 서비스 클래스.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ClosingService {

    private final ClosingCalendarRepository closingCalendarRepository;
    private final ClosingTaskRepository closingTaskRepository;
    private final ClosingGateRepository closingGateRepository;
    private final PeriodLockRepository periodLockRepository;
    private final ReopenApprovalRepository reopenApprovalRepository;
    private final ValuationBatchRepository valuationBatchRepository;
    private final ProvisionBatchRepository provisionBatchRepository;
    private final ClosingAdjustmentRepository closingAdjustmentRepository;
    
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final FiscalPeriodPersistencePort fiscalPeriodPersistencePort;
    private final JournalPersistencePort journalPersistencePort;
    private final JournalUseCase journalUseCase;

    @Transactional(readOnly = true)
    public boolean isClosed(LocalDate date) {
        String fiscalYear = String.valueOf(date.getYear());
        String fiscalPeriodStr = String.format("%02d", date.getMonthValue());

        return fiscalPeriodPersistencePort.findByFiscalYearAndFiscalPeriod(fiscalYear, fiscalPeriodStr)
                .map(fp -> fp.getClosingStatus() == FiscalPeriod.ClosingStatus.CLOSED ||
                           fp.getClosingStatus() == FiscalPeriod.ClosingStatus.PERMANENTLY_CLOSED)
                .orElse(false);
    }

    public ClosingCalendar createClosingCalendar(ClosingCalendar closingCalendar) {
        FiscalPeriod fiscalPeriod = fiscalPeriodPersistencePort.findByFiscalYearAndFiscalPeriod(
                closingCalendar.getFiscalYear(), closingCalendar.getFiscalPeriod())
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));

        closingCalendar.setStatus(ClosingCalendarStatus.OPEN);
        return closingCalendarRepository.save(closingCalendar);
    }

    @Transactional(readOnly = true)
    public ClosingCalendar findClosingCalendarById(Long id) {
        return closingCalendarRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ClosingCalendar not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public ClosingCalendar findClosingCalendarByFiscalPeriod(String fiscalYear, String fiscalPeriod) {
        return closingCalendarRepository.findByFiscalYearAndFiscalPeriod(fiscalYear, fiscalPeriod)
                .orElseThrow(() -> new EntityNotFoundException("ClosingCalendar not found"));
    }

    public ClosingCalendar updateClosingCalendarStatus(Long id, ClosingCalendarStatus newStatus, String user) {
        ClosingCalendar calendar = findClosingCalendarById(id);
        calendar.setStatus(newStatus);
        if (newStatus == ClosingCalendarStatus.CLOSED) {
            calendar.setClosedBy(user);
            calendar.setClosedAt(LocalDateTime.now());
        }
        calendar.setAuditUser(user);
        return closingCalendarRepository.save(calendar);
    }

    public ClosingTask createClosingTask(ClosingTask closingTask) {
        ClosingCalendar calendar = findClosingCalendarById(closingTask.getClosingCalendar().getId());
        closingTask.setClosingCalendar(calendar);
        closingTask.setStatus(ClosingTaskStatus.PENDING);
        return closingTaskRepository.save(closingTask);
    }

    public ClosingTask updateClosingTaskStatus(Long taskId, ClosingTaskStatus newStatus, String user) {
        ClosingTask task = closingTaskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("ClosingTask not found"));
        task.setStatus(newStatus);
        task.setAuditUser(user);
        return closingTaskRepository.save(task);
    }

    public ClosingGate createClosingGate(ClosingGate closingGate) {
        ClosingCalendar calendar = findClosingCalendarById(closingGate.getClosingCalendar().getId());
        closingGate.setClosingCalendar(calendar);
        closingGate.setStatus(ClosingGateStatus.PENDING);
        return closingGateRepository.save(closingGate);
    }

    public ClosingGate checkAndPassClosingGate(Long gateId, String user) {
        ClosingGate gate = closingGateRepository.findById(gateId)
                .orElseThrow(() -> new EntityNotFoundException("ClosingGate not found"));
        gate.setStatus(ClosingGateStatus.PASSED);
        gate.setPassedBy(user);
        gate.setPassedAt(LocalDateTime.now());
        gate.setAuditUser(user);
        return closingGateRepository.save(gate);
    }

    public PeriodLock lockPeriod(Long fiscalPeriodId, PeriodLock.PeriodLockType lockType, String user, String reason) {
        FiscalPeriod fiscalPeriod = fiscalPeriodPersistencePort.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));

        PeriodLock periodLock = new PeriodLock();
        periodLock.setFiscalPeriod(fiscalPeriod);
        periodLock.setLockType(lockType);
        periodLock.setLockedBy(user);
        periodLock.setLockedAt(LocalDateTime.now());
        periodLock.setReason(reason);
        return periodLockRepository.save(periodLock);
    }

    public void unlockPeriod(Long fiscalPeriodId, String user) {
        FiscalPeriod fiscalPeriod = fiscalPeriodPersistencePort.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));

        periodLockRepository.findByFiscalPeriod(fiscalPeriod).ifPresent(periodLockRepository::delete);
    }

    public ReopenApproval requestPeriodReopen(Long fiscalPeriodId, String requestedBy, String reason) {
        FiscalPeriod fiscalPeriod = fiscalPeriodPersistencePort.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));

        ReopenApproval approval = new ReopenApproval();
        approval.setFiscalPeriod(fiscalPeriod);
        approval.setRequestedBy(requestedBy);
        approval.setRequestedAt(LocalDateTime.now());
        approval.setReason(reason);
        approval.setStatus(ReopenApprovalStatus.PENDING);
        return reopenApprovalRepository.save(approval);
    }

    public ReopenApproval updateReopenApprovalStatus(Long approvalId, ReopenApprovalStatus newStatus, String approvedBy) {
        ReopenApproval approval = reopenApprovalRepository.findById(approvalId)
                .orElseThrow(() -> new EntityNotFoundException("ReopenApproval not found"));
        approval.setStatus(newStatus);
        approval.setApprovedBy(approvedBy);
        approval.setApprovedAt(LocalDateTime.now());
        
        if (newStatus == ReopenApprovalStatus.APPROVED) {
            FiscalPeriod fp = approval.getFiscalPeriod();
            fp.setClosingStatus(FiscalPeriod.ClosingStatus.OPEN);
            fiscalPeriodPersistencePort.save(fp);
        }
        return reopenApprovalRepository.save(approval);
    }

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
        batch = valuationBatchRepository.save(batch);

        try {
            JournalEntry valuationJE = createAutomatedJournalEntry(
                    fiscalPeriod.getEndDate(),
                    "자동 " + valuationType.name() + " 평가 분개",
                    "SYSTEM",
                    "VALUATION_BATCH",
                    batch.getId().toString(),
                    BigDecimal.valueOf(1000)
            );
            batch.setGeneratedJournalEntry(valuationJE);
            batch.setStatus(ValuationBatch.ValuationBatchStatus.COMPLETED);
            batch.setReportLink("/reports/valuation/" + batch.getId());
        } catch (Exception e) {
            batch.setStatus(ValuationBatch.ValuationBatchStatus.FAILED);
            throw new RuntimeException("Valuation batch failed", e);
        }
        return valuationBatchRepository.save(batch);
    }

    public ProvisionBatch runProvisionBatch(Long fiscalPeriodId, ProvisionBatch.ProvisionType provisionType, String runBy) {
        FiscalPeriod fiscalPeriod = fiscalPeriodPersistencePort.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));

        ProvisionBatch batch = new ProvisionBatch();
        batch.setFiscalPeriod(fiscalPeriod);
        batch.setProvisionType(provisionType);
        batch.setRunDateTime(LocalDateTime.now());
        batch.setStatus(ProvisionBatch.ProvisionBatchStatus.RUNNING);
        batch.setRunBy(runBy);
        batch = provisionBatchRepository.save(batch);

        try {
            JournalEntry provisionJE = createAutomatedJournalEntry(
                    fiscalPeriod.getEndDate(),
                    "자동 " + provisionType.name() + " 충당 분개",
                    "SYSTEM",
                    "PROVISION_BATCH",
                    batch.getId().toString(),
                    BigDecimal.valueOf(500)
            );
            batch.setGeneratedJournalEntry(provisionJE);
            batch.setStatus(ProvisionBatch.ProvisionBatchStatus.COMPLETED);
        } catch (Exception e) {
            batch.setStatus(ProvisionBatch.ProvisionBatchStatus.FAILED);
            throw new RuntimeException("Provision batch failed", e);
        }
        return provisionBatchRepository.save(batch);
    }

    public ClosingAdjustment createClosingAdjustment(Long fiscalPeriodId, Long journalEntryId, ClosingAdjustment.AdjustmentType adjustmentType, String description, String approvedBy) {
        FiscalPeriod fiscalPeriod = fiscalPeriodPersistencePort.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        JournalEntry journalEntry = journalPersistencePort.findById(journalEntryId)
                .orElseThrow(() -> new EntityNotFoundException("JournalEntry not found"));

        ClosingAdjustment adjustment = new ClosingAdjustment();
        adjustment.setFiscalPeriod(fiscalPeriod);
        adjustment.setJournalEntry(journalEntry);
        adjustment.setAdjustmentType(adjustmentType);
        adjustment.setDescription(description);
        adjustment.setApprovedBy(approvedBy);
        adjustment.setApprovedAt(LocalDateTime.now());
        adjustment.setAuditUser(approvedBy);
        return closingAdjustmentRepository.save(adjustment);
    }

    public ClosingCalendar determineClosingStatus(Long calendarId, String user) {
        ClosingCalendar calendar = findClosingCalendarById(calendarId);
        calendar.setStatus(ClosingCalendarStatus.CLOSED);
        
        fiscalPeriodPersistencePort.findByFiscalYearAndFiscalPeriod(calendar.getFiscalYear(), calendar.getFiscalPeriod())
                .ifPresent(fp -> {
                    fp.setClosingStatus(FiscalPeriod.ClosingStatus.CLOSED);
                    fiscalPeriodPersistencePort.save(fp);
                });

        return closingCalendarRepository.save(calendar);
    }

    private JournalEntry createAutomatedJournalEntry(
            LocalDate accountingDate,
            String description,
            String createdBy,
            String lineageSourceType,
            String lineageSourceId,
            BigDecimal amount) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.now());
        entry.setAccountingDate(accountingDate);
        entry.setDescription(description);
        entry.setEntryType("ADJUSTMENT");
        entry.setCreatedBy(createdBy);
        entry.setAuditUser(createdBy);
        entry.setLineageSourceType(lineageSourceType);
        entry.setLineageSourceId(lineageSourceId);
        
        AccountSubject dummyDebitAccount = accountSubjectPersistencePort.findByCode("999998")
                .orElseGet(() -> accountSubjectPersistencePort.save(createDummyAccountSubject("999998", "더미 차변 계정")));
        AccountSubject dummyCreditAccount = accountSubjectPersistencePort.findByCode("999999")
                .orElseGet(() -> accountSubjectPersistencePort.save(createDummyAccountSubject("999999", "더미 대변 계정")));

        JournalDetail debitDetail = new JournalDetail();
        debitDetail.setSide(JournalSide.DEBIT);
        debitDetail.setAccountSubject(dummyDebitAccount);
        debitDetail.setAmount(amount);
        debitDetail.setBaseAmount(amount);
        debitDetail.setDetailDescription(description + " (차변)");
        entry.addDetail(debitDetail);

        JournalDetail creditDetail = new JournalDetail();
        creditDetail.setSide(JournalSide.CREDIT);
        creditDetail.setAccountSubject(dummyCreditAccount);
        creditDetail.setAmount(amount);
        creditDetail.setBaseAmount(amount);
        creditDetail.setDetailDescription(description + " (대변)");
        entry.addDetail(creditDetail);

        entry.setSlipNo(accountingDate.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-AUTO-" + System.currentTimeMillis());

        return journalPersistencePort.save(entry);
    }

    private AccountSubject createDummyAccountSubject(String code, String name) {
        AccountSubject account = new AccountSubject();
        account.setCode(code);
        account.setName(name);
        account.setCategory(AccountSubject.AccountCategory.ASSETS);
        account.setAccountType(AccountSubject.AccountType.ASSETS.name());
        account.setBalanceType(AccountSubject.BalanceType.DEBIT);
        account.setUnsettled(false);
        account.setFixedAsset(false);
        account.setValidFrom(LocalDate.now());
        account.setValidTo(LocalDate.of(9999, 12, 31));
        return account;
    }
}
