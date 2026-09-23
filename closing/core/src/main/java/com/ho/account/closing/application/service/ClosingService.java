package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.in.ClosingUseCase;
import com.ho.account.closing.application.port.out.ClosingAdjustmentPersistencePort;
import com.ho.account.closing.application.port.out.ClosingAuditLogPersistencePort;
import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.application.port.out.ClosingGatePersistencePort;
import com.ho.account.closing.application.port.out.ClosingTaskPersistencePort;
import com.ho.account.closing.application.port.out.PeriodLockPersistencePort;
import com.ho.account.closing.application.port.out.ReopenApprovalPersistencePort;
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
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/**
 * [헥사고날 아키텍처 - 애플리케이션 서비스 (Application Service)]
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 결산(Closing) 모듈의 '오케스트레이터(지휘자)'입니다.
 * 월말 결산 시기가 오면 평가(Valuation), 충당(Provision), 마감(Close) 등 수많은 작업이 순차적으로 실행되어야 합니다.
 * 이 서비스는 "결산 달력(ClosingCalendar)을 만들고, 태스크(Task)를 진행시키고, 최종적으로 장부를 닫아라!"라는 명령(UseCase)을 받아,
 * 도메인 모델에게 상태 변경을 위임하고, 영속성 포트(Repository)를 통해 DB에 저장하며,
 * 타 모듈(MasterData, JournalLedger) 포트를 호출하여 의존성을 깔끔하게 처리합니다.
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
    private final ClosingBatchExecutionRecorder batchExecutionRecorder;
    private final ClosingAdjustmentPersistencePort closingAdjustmentPersistencePort;
    private final ClosingAuditLogPersistencePort closingAuditLogPersistencePort;
    
    private final FiscalPeriodControlPort fiscalPeriodControlPort;
    private final JournalPostingPort journalPostingPort;
    private final JournalQueryPort journalQueryPort;
    private final ClosingAccountingProperties closingAccountingProperties;

    @Transactional(readOnly = true)
    @Override
    public boolean isClosed(LocalDate date) {
        String fiscalYear = String.valueOf(date.getYear());
        String fiscalPeriodStr = String.format("%02d", date.getMonthValue());

        FiscalPeriodRef fiscalPeriod = fiscalPeriodControlPort.findFiscalPeriod(fiscalYear, fiscalPeriodStr)
                .orElseThrow(() -> new IllegalStateException(
                        "Fiscal period is missing for accounting date " + date));
        return isClosedPeriod(fiscalPeriod);
    }

    @Override
    public ClosingCalendar createClosingCalendar(ClosingCalendar closingCalendar) {
        fiscalPeriodControlPort.findFiscalPeriod(
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

    @Transactional(readOnly = true)
    @Override
    public List<ClosingTask> findClosingTasksByCalendarId(Long calendarId) {
        if (calendarId == null || calendarId <= 0) {
            throw new IllegalArgumentException("calendarId must be positive");
        }
        return closingTaskPersistencePort.findByClosingCalendarId(calendarId);
    }

    @Override
    public ClosingCalendar updateClosingCalendarStatus(Long id, ClosingCalendarStatus newStatus, String user) {
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        if (newStatus == ClosingCalendarStatus.CLOSED) {
            return determineClosingStatus(id, user);
        }
        if (newStatus != ClosingCalendarStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Direct calendar status update only supports IN_PROGRESS; close and reopen use controlled flows.");
        }
        ClosingCalendar calendar = findClosingCalendarById(id);
        String prevStatus = calendar.getStatus().name();
        calendar.start(user);
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
        if (newStatus == ClosingTaskStatus.COMPLETED && hasText(task.getCompletionConditionJson())) {
            // @todo Replace JSON text with a typed evidence port. Completion means an evaluator
            // verifies the referenced run/status and persists immutable evidence with this task.
            throw new IllegalStateException(
                    "Configured task completion conditions require a typed evidence evaluator.");
        }
        task.changeStatus(newStatus, user);

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
        if (hasText(gate.getCheckConditionJson())) {
            // @todo Introduce a typed ClosingGateEvidencePort. The gate may pass only after the
            // external evidence is verified and stored with source ID, result and verification time.
            throw new IllegalStateException(
                    "Configured gate conditions require a typed evidence evaluator.");
        }
        List<ClosingTask> tasks = closingTaskPersistencePort.findByClosingCalendar(gate.getClosingCalendar());
        gate.pass(user, tasks);
        ClosingGate saved = closingGatePersistencePort.save(gate);

        // 감사 로그 기록
        closingAuditLogPersistencePort.save(ClosingAuditLog.create(
                saved.getClosingCalendar(), ActionType.GATE_PASSED, 
                "GATE:" + gate.getName() + ":PENDING", "PASSED", user, "Gate passed"));

        return saved;
    }

    @Override
    public PeriodLock lockPeriod(Long fiscalPeriodId, PeriodLock.PeriodLockType lockType, String user, String reason) {
        FiscalPeriodRef fiscalPeriod = fiscalPeriodControlPort.findFiscalPeriodById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        if (periodLockPersistencePort.findByFiscalPeriodId(fiscalPeriod.id()).isPresent()) {
            throw new IllegalStateException("Fiscal period is already locked.");
        }
        requireActor(user, "user");
        Objects.requireNonNull(lockType, "lockType must not be null");

        PeriodLock periodLock = new PeriodLock();
        periodLock.assignFiscalPeriod(fiscalPeriod.id(), fiscalPeriod.fiscalYear(), fiscalPeriod.fiscalPeriod());
        periodLock.setLockType(lockType);
        periodLock.setLockedBy(user);
        periodLock.setLockedAt(LocalDateTime.now());
        periodLock.setReason(reason);
        periodLock.setAuditUser(user);
        PeriodLock saved = periodLockPersistencePort.save(periodLock);

        ClosingCalendar calendar = findClosingCalendarByFiscalPeriod(
                fiscalPeriod.fiscalYear(), fiscalPeriod.fiscalPeriod());
        closingAuditLogPersistencePort.save(ClosingAuditLog.create(
                calendar,
                ActionType.PERIOD_LOCK,
                fiscalPeriod.closingStatus(),
                "LOCKED:" + lockType,
                user,
                reason));
        return saved;
    }

    @Override
    public void unlockPeriod(Long fiscalPeriodId, String user) {
        FiscalPeriodRef fiscalPeriod = fiscalPeriodControlPort.findFiscalPeriodById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        requireActor(user, "user");
        PeriodLock lock = periodLockPersistencePort.findByFiscalPeriodId(fiscalPeriod.id())
                .orElseThrow(() -> new IllegalStateException("Fiscal period is not locked."));
        ClosingCalendar calendar = findClosingCalendarByFiscalPeriod(
                fiscalPeriod.fiscalYear(), fiscalPeriod.fiscalPeriod());
        closingAuditLogPersistencePort.save(ClosingAuditLog.create(
                calendar,
                ActionType.PERIOD_UNLOCK,
                "LOCKED:" + lock.getLockType(),
                fiscalPeriod.closingStatus(),
                user,
                "Period lock released"));
        // @todo Preserve the row with active=false, unlockedBy/At/reason after a forward migration.
        // The immutable audit log above is the current recovery trail; deleting it is not allowed.
        periodLockPersistencePort.delete(lock);
    }

    @Override
    public ReopenApproval requestPeriodReopen(Long fiscalPeriodId, String requestedBy, String reason) {
        FiscalPeriodRef fiscalPeriod = fiscalPeriodControlPort.findFiscalPeriodById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        if (!"CLOSED".equals(fiscalPeriod.closingStatus())) {
            throw new IllegalStateException(
                    "Only a CLOSED fiscal period can request reopen. Current status: "
                            + fiscalPeriod.closingStatus());
        }
        if (reopenApprovalPersistencePort.existsByFiscalPeriodIdAndStatus(
                fiscalPeriod.id(), ReopenApprovalStatus.PENDING)) {
            throw new IllegalStateException("A pending reopen request already exists for the fiscal period.");
        }

        ReopenApproval approval = new ReopenApproval();
        approval.assignFiscalPeriod(fiscalPeriod.id(), fiscalPeriod.fiscalYear(), fiscalPeriod.fiscalPeriod());
        approval.request(requestedBy, reason);
        ReopenApproval saved = reopenApprovalPersistencePort.save(approval);

        ClosingCalendar calendar = findClosingCalendarByFiscalPeriod(
                fiscalPeriod.fiscalYear(), fiscalPeriod.fiscalPeriod());
        closingAuditLogPersistencePort.save(ClosingAuditLog.create(
                calendar, ActionType.REOPEN_REQUEST, "CLOSED", "REOPEN_PENDING", requestedBy, reason));

        return saved;
    }

    @Override
    public ReopenApproval updateReopenApprovalStatus(Long approvalId, ReopenApprovalStatus newStatus, String approvedBy) {
        ReopenApproval approval = reopenApprovalPersistencePort.findById(approvalId)
                .orElseThrow(() -> new EntityNotFoundException("ReopenApproval not found"));
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        if (newStatus == ReopenApprovalStatus.APPROVED) {
            FiscalPeriodRef fp = fiscalPeriodControlPort.findFiscalPeriodById(approval.getFiscalPeriodId())
                    .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
            if (!"CLOSED".equals(fp.closingStatus())) {
                throw new IllegalStateException("Only a CLOSED fiscal period can be reopened.");
            }
            approval.assignFiscalPeriod(fp.id(), fp.fiscalYear(), fp.fiscalPeriod());
            ClosingCalendar calendar = findClosingCalendarByFiscalPeriod(fp.fiscalYear(), fp.fiscalPeriod());
            approval.approve(approvedBy);
            calendar.reopen(approvedBy);
            FiscalPeriodRef reopenedPeriod = fiscalPeriodControlPort.updateClosingStatus(fp.id(), "OPEN", approvedBy);
            if (!"OPEN".equals(reopenedPeriod.closingStatus())) {
                throw new IllegalStateException("Master fiscal period did not transition to OPEN.");
            }
            closingCalendarPersistencePort.save(calendar);
            closingAuditLogPersistencePort.save(ClosingAuditLog.create(
                    calendar, ActionType.REOPEN_APPROVED, "CLOSED", "OPEN", approvedBy, "Reopen approved"));
        } else if (newStatus == ReopenApprovalStatus.REJECTED) {
            approval.reject(approvedBy);
            FiscalPeriodRef fp = fiscalPeriodControlPort.findFiscalPeriodById(approval.getFiscalPeriodId())
                    .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
            ClosingCalendar calendar = findClosingCalendarByFiscalPeriod(fp.fiscalYear(), fp.fiscalPeriod());
            closingAuditLogPersistencePort.save(ClosingAuditLog.create(
                    calendar,
                    ActionType.REOPEN_REJECTED,
                    "REOPEN_PENDING",
                    "CLOSED",
                    approvedBy,
                    "Reopen rejected"));
        } else {
            throw new IllegalStateException("Reopen decision must be APPROVED or REJECTED.");
        }
        return reopenApprovalPersistencePort.save(approval);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ValuationBatch runValuationBatch(Long fiscalPeriodId, ValuationBatch.ValuationType valuationType, String runBy) {
        FiscalPeriodRef fiscalPeriod = fiscalPeriodControlPort.findFiscalPeriodById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        ClosingAccountingProperties.AutomatedJournalRule accountingRule =
                closingAccountingProperties.requireValuationRule(valuationType);

        // @todo Add an explicit execution key with a unique constraint. Completion requires
        // period/type/business-date/key lookup plus duplicate-request and crash-recovery tests.
        ValuationBatch batch = batchExecutionRecorder.startValuation(
                fiscalPeriod, valuationType, runBy);

        try {
            JournalPostingResult result = createAutomatedJournalEntry(
                    fiscalPeriod.endDate(),
                    "자동 " + valuationType.name() + " 평가 분개",
                    "SYSTEM",
                    "VALUATION_BATCH",
                    batch.getId().toString(),
                    accountingRule
            );
            return batchExecutionRecorder.markValuationPendingApproval(
                    batch.getId(),
                    result.journalEntryId(),
                    "/reports/valuation/" + batch.getId(),
                    runBy);
        } catch (Exception e) {
            markValuationFailedPreservingCause(batch.getId(), runBy, e);
            throw new RuntimeException("Valuation batch failed", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ProvisionBatch runProvisionBatch(Long fiscalPeriodId, ProvisionBatch.ProvisionType provisionType, String runBy) {
        FiscalPeriodRef fiscalPeriod = fiscalPeriodControlPort.findFiscalPeriodById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        ClosingAccountingProperties.AutomatedJournalRule accountingRule =
                closingAccountingProperties.requireProvisionRule(provisionType);

        ProvisionBatch batch = batchExecutionRecorder.startProvision(
                fiscalPeriod, provisionType, runBy);

        try {
            JournalPostingResult result = createAutomatedJournalEntry(
                    fiscalPeriod.endDate(),
                    "자동 " + provisionType.name() + " 충당 분개",
                    "SYSTEM",
                    "PROVISION_BATCH",
                    batch.getId().toString(),
                    accountingRule
            );
            return batchExecutionRecorder.markProvisionPendingApproval(
                    batch.getId(),
                    result.journalEntryId(),
                    runBy);
        } catch (Exception e) {
            markProvisionFailedPreservingCause(batch.getId(), runBy, e);
            throw new RuntimeException("Provision batch failed", e);
        }
    }

    @Override
    public ClosingAdjustment createClosingAdjustment(Long fiscalPeriodId, Long journalEntryId, ClosingAdjustment.AdjustmentType adjustmentType, String description, String approvedBy) {
        FiscalPeriodRef fiscalPeriod = fiscalPeriodControlPort.findFiscalPeriodById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));

        // 1. 회기 기간 상태 확인
        if (!"OPEN".equals(fiscalPeriod.closingStatus())) {
            throw new IllegalStateException("Fiscal period is not OPEN. Current status: " + fiscalPeriod.closingStatus());
        }

        // 2. 전표 존재 여부 확인
        JournalSummary journalSummary = journalQueryPort.getJournalSummary(journalEntryId);
        
        // 3. 회계 일자 정합성 확인
        if (journalSummary.getAccountingDate().isBefore(fiscalPeriod.startDate()) ||
            journalSummary.getAccountingDate().isAfter(fiscalPeriod.endDate())) {
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
        adjustment.assignFiscalPeriod(fiscalPeriod.id(), fiscalPeriod.fiscalYear(), fiscalPeriod.fiscalPeriod());
        adjustment.setJournalEntryId(journalEntryId);
        adjustment.setAdjustmentType(adjustmentType);
        adjustment.setDescription(description);
        adjustment.setApprovedBy(approvedBy);
        adjustment.setApprovedAt(LocalDateTime.now());
        adjustment.setAuditUser(approvedBy);
        ClosingAdjustment saved = closingAdjustmentPersistencePort.save(adjustment);

        ClosingCalendar calendar = findClosingCalendarByFiscalPeriod(
                fiscalPeriod.fiscalYear(), fiscalPeriod.fiscalPeriod());
        closingAuditLogPersistencePort.save(ClosingAuditLog.create(
                calendar,
                ActionType.ADJUSTMENT_CREATED,
                null,
                "CREATED",
                approvedBy,
                "Adjustment Entry ID: " + journalEntryId));

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

        FiscalPeriodRef fiscalPeriod = fiscalPeriodControlPort
                .findFiscalPeriod(calendar.getFiscalYear(), calendar.getFiscalPeriod())
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        if (!"OPEN".equals(fiscalPeriod.closingStatus())) {
            throw new IllegalStateException(
                    "Only an OPEN fiscal period can be closed. Current status: "
                            + fiscalPeriod.closingStatus());
        }

        // 2. 도메인 메서드에 상태 변경 위임
        calendar.close(user);
        FiscalPeriodRef closedPeriod =
                fiscalPeriodControlPort.updateClosingStatus(fiscalPeriod.id(), "CLOSED", user);
        if (!"CLOSED".equals(closedPeriod.closingStatus())) {
            throw new IllegalStateException("Master fiscal period did not transition to CLOSED.");
        }

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
            ClosingAccountingProperties.AutomatedJournalRule accountingRule) {
        BigDecimal amount = accountingRule.getAmount();
        List<JournalLineCommand> lines = List.of(
                new JournalLineCommand(
                        "DEBIT",
                        accountingRule.getDebitAccountCode(),
                        amount,
                        amount,
                        null,
                        null,
                        description + " (차변)"),
                new JournalLineCommand(
                        "CREDIT",
                        accountingRule.getCreditAccountCode(),
                        amount,
                        amount,
                        null,
                        null,
                        description + " (대변)")
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

    private boolean isClosedPeriod(FiscalPeriodRef fiscalPeriod) {
        return "CLOSED".equals(fiscalPeriod.closingStatus())
                || "PERMANENTLY_CLOSED".equals(fiscalPeriod.closingStatus());
    }

    private void requireActor(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void markValuationFailedPreservingCause(Long batchId, String actor, Exception cause) {
        try {
            batchExecutionRecorder.markValuationFailed(batchId, actor);
        } catch (RuntimeException recorderFailure) {
            cause.addSuppressed(recorderFailure);
        }
    }

    private void markProvisionFailedPreservingCause(Long batchId, String actor, Exception cause) {
        try {
            batchExecutionRecorder.markProvisionFailed(batchId, actor);
        } catch (RuntimeException recorderFailure) {
            cause.addSuppressed(recorderFailure);
        }
    }
}
