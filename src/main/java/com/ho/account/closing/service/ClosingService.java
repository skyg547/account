package com.ho.account.closing.service;

import com.ho.account.basic.domain.FiscalPeriod;
import com.ho.account.basic.repository.FiscalPeriodRepository;
import com.ho.account.closing.domain.*;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.ClosingGate.ClosingGateStatus;
import com.ho.account.closing.domain.ClosingTask.ClosingTaskStatus;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import com.ho.account.closing.repository.*;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.repository.JournalEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 결산 (Closing) 관련 비즈니스 로직을 처리하는 서비스 클래스.
 * 결산 캘린더, 태스크, 게이트 관리 및 기간 잠금/재오픈, 평가/충당 배치 실행, 결산 조정 등을 담당합니다.
 * 또한, DoD(Definition of Done)에 따라 결산 완료/실패를 시스템이 판정하는 로직을 포함합니다.
 */
@Service
@Transactional
public class ClosingService {

    private final ClosingCalendarRepository closingCalendarRepository;
    private final ClosingTaskRepository closingTaskRepository;
    private final ClosingGateRepository closingGateRepository;
    private final PeriodLockRepository periodLockRepository;
    private final ReopenApprovalRepository reopenApprovalRepository;
    private final ValuationBatchRepository valuationBatchRepository;
    private final ProvisionBatchRepository provisionBatchRepository;
    private final ClosingAdjustmentRepository closingAdjustmentRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository; // FiscalPeriod와 연동
    private final JournalEntryRepository journalEntryRepository; // JournalEntry 생성/조회용

    @Autowired
    public ClosingService(ClosingCalendarRepository closingCalendarRepository,
                          ClosingTaskRepository closingTaskRepository,
                          ClosingGateRepository closingGateRepository,
                          PeriodLockRepository periodLockRepository,
                          ReopenApprovalRepository reopenApprovalRepository,
                          ValuationBatchRepository valuationBatchRepository,
                          ProvisionBatchRepository provisionBatchRepository,
                          ClosingAdjustmentRepository closingAdjustmentRepository,
                          FiscalPeriodRepository fiscalPeriodRepository,
                          JournalEntryRepository journalEntryRepository) {
        this.closingCalendarRepository = closingCalendarRepository;
        this.closingTaskRepository = closingTaskRepository;
        this.closingGateRepository = closingGateRepository;
        this.periodLockRepository = periodLockRepository;
        this.reopenApprovalRepository = reopenApprovalRepository;
        this.valuationBatchRepository = valuationBatchRepository;
        this.provisionBatchRepository = provisionBatchRepository;
        this.closingAdjustmentRepository = closingAdjustmentRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    /**
     * 특정 날짜에 해당하는 회계 기간이 마감되었는지 확인합니다.
     *
     * @param date 확인할 날짜
     * @return 해당 회계 기간이 마감되었으면 true, 아니면 false
     */
    @Transactional(readOnly = true)
    public boolean isClosed(LocalDate date) {
        // 날짜로부터 회계 기간 (YYYYMM 형식) 결정
        String fiscalYear = String.valueOf(date.getYear());
        // 월을 2자리 형식으로 변환
        String fiscalPeriodStr = String.format("%02d", date.getMonthValue());

        return fiscalPeriodRepository.findByFiscalYearAndFiscalPeriod(fiscalYear, fiscalPeriodStr)
                .map(fp -> fp.getClosingStatus() == FiscalPeriod.ClosingStatus.CLOSED ||
                           fp.getClosingStatus() == FiscalPeriod.ClosingStatus.PERMANENTLY_CLOSED)
                .orElse(false); // 회계 기간을 찾을 수 없으면 마감되지 않은 것으로 간주
    }

    // --- ClosingCalendar (결산 캘린더) 관련 메서드 ---

    /**
     * 새로운 결산 캘린더를 생성합니다.
     * @param closingCalendar 생성할 결산 캘린더 엔티티
     * @return 생성된 결산 캘린더
     */
    public ClosingCalendar createClosingCalendar(ClosingCalendar closingCalendar) {
        // 회계 기간 존재 여부 확인 및 연결 (FiscalPeriod 엔티티 활용)
        FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findByFiscalYearAndFiscalPeriod(
                closingCalendar.getFiscalYear(), closingCalendar.getFiscalPeriod())
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found for year " + closingCalendar.getFiscalYear() + " and period " + closingCalendar.getFiscalPeriod()));

        // TODO: 이미 존재하는 closingCalendar 인지 확인

        closingCalendar.setStatus(ClosingCalendarStatus.OPEN); // 초기 상태 OPEN
        return closingCalendarRepository.save(closingCalendar);
    }

    /**
     * ID로 결산 캘린더를 조회합니다.
     * @param id 결산 캘린더 ID
     * @return 조회된 결산 캘린더
     * @throws EntityNotFoundException 해당 ID의 결산 캘린더가 없을 경우
     */
    @Transactional(readOnly = true)
    public ClosingCalendar findClosingCalendarById(Long id) {
        return closingCalendarRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ClosingCalendar not found with id: " + id));
    }

    /**
     * 특정 회계연도와 기간으로 결산 캘린더를 조회합니다.
     * @param fiscalYear 회계연도
     * @param fiscalPeriod 회계기간
     * @return 조회된 결산 캘린더
     * @throws EntityNotFoundException 해당 회계연도 및 기간의 결산 캘린더가 없을 경우
     */
    @Transactional(readOnly = true)
    public ClosingCalendar findClosingCalendarByFiscalPeriod(String fiscalYear, String fiscalPeriod) {
        return closingCalendarRepository.findByFiscalYearAndFiscalPeriod(fiscalYear, fiscalPeriod)
                .orElseThrow(() -> new EntityNotFoundException("ClosingCalendar not found for fiscal year " + fiscalYear + " and period " + fiscalPeriod));
    }

    /**
     * 결산 캘린더의 상태를 업데이트합니다.
     * @param id 결산 캘린더 ID
     * @param newStatus 새로운 상태
     * @param user 업데이트 요청 사용자
     * @return 업데이트된 결산 캘린더
     * @throws EntityNotFoundException 해당 ID의 결산 캘린더가 없을 경우
     */
    public ClosingCalendar updateClosingCalendarStatus(Long id, ClosingCalendarStatus newStatus, String user) {
        ClosingCalendar calendar = findClosingCalendarById(id);
        calendar.setStatus(newStatus);
        // 상태 변경에 따른 추가 audit 정보 업데이트
        if (newStatus == ClosingCalendarStatus.CLOSED) {
            calendar.setClosedBy(user);
            calendar.setClosedAt(LocalDateTime.now());
        } else if (newStatus == ClosingCalendarStatus.OPEN) {
            calendar.setReopenedBy(user);
            calendar.setReopenedAt(LocalDateTime.now());
        } else if (newStatus == ClosingCalendarStatus.IN_PROGRESS && calendar.getCloseInitiatedBy() == null) {
            calendar.setCloseInitiatedBy(user);
            calendar.setCloseInitiatedAt(LocalDateTime.now());
        }
        calendar.setAuditUser(user);
        return closingCalendarRepository.save(calendar);
    }

    // --- ClosingTask (결산 태스크) 관련 메서드 ---

    /**
     * 새로운 결산 태스크를 생성합니다.
     * @param closingTask 생성할 결산 태스크 엔티티
     * @return 생성된 결산 태스크
     */
    public ClosingTask createClosingTask(ClosingTask closingTask) {
        ClosingCalendar calendar = findClosingCalendarById(closingTask.getClosingCalendar().getId());
        closingTask.setClosingCalendar(calendar);
        closingTask.setStatus(ClosingTaskStatus.PENDING); // 초기 상태 PENDING
        return closingTaskRepository.save(closingTask);
    }

    /**
     * 결산 태스크의 상태를 업데이트합니다.
     * @param taskId 결산 태스크 ID
     * @param newStatus 새로운 상태
     * @param user 업데이트 요청 사용자
     * @return 업데이트된 결산 태스크
     * @throws EntityNotFoundException 해당 ID의 결산 태스크가 없을 경우
     */
    public ClosingTask updateClosingTaskStatus(Long taskId, ClosingTaskStatus newStatus, String user) {
        ClosingTask task = closingTaskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("ClosingTask not found with id: " + taskId));
        task.setStatus(newStatus);
        task.setAuditUser(user);
        // TODO: 태스크 완료 시, 조건(completionConditionJson) 검증 로직 추가
        return closingTaskRepository.save(task);
    }

    // --- ClosingGate (결산 게이트) 관련 메서드 ---

    /**
     * 새로운 결산 게이트를 생성합니다.
     * @param closingGate 생성할 결산 게이트 엔티티
     * @return 생성된 결산 게이트
     */
    public ClosingGate createClosingGate(ClosingGate closingGate) {
        ClosingCalendar calendar = findClosingCalendarById(closingGate.getClosingCalendar().getId());
        closingGate.setClosingCalendar(calendar);
        closingGate.setStatus(ClosingGateStatus.PENDING); // 초기 상태 PENDING
        return closingGateRepository.save(closingGate);
    }

    /**
     * 결산 게이트의 통과 조건을 확인하고 상태를 업데이트합니다.
     * @param gateId 결산 게이트 ID
     * @param user 요청 사용자
     * @return 업데이트된 결산 게이트
     * @throws EntityNotFoundException 해당 ID의 결산 게이트가 없을 경우
     */
    public ClosingGate checkAndPassClosingGate(Long gateId, String user) {
        ClosingGate gate = closingGateRepository.findById(gateId)
                .orElseThrow(() -> new EntityNotFoundException("ClosingGate not found with id: " + gateId));

        // TODO: gate.getCheckConditionJson()을 파싱하여 실제 조건 검증 로직 구현
        // 이 로직은 복잡하며, ClosingTask, ReconciliationRun 등의 상태를 확인해야 함.
        boolean conditionsMet = true; // 임시로 true 설정

        if (conditionsMet) {
            gate.setStatus(ClosingGateStatus.PASSED);
            gate.setPassedBy(user);
            gate.setPassedAt(LocalDateTime.now());
        } else {
            gate.setStatus(ClosingGateStatus.FAILED);
            // TODO: 실패 사유 기록
        }
        gate.setAuditUser(user);
        return closingGateRepository.save(gate);
    }

    // --- PeriodLock (기간 잠금) 관련 메서드 ---

    /**
     * 특정 회계 기간을 잠급니다.
     * @param fiscalPeriodId 잠글 회계 기간 ID
     * @param lockType 잠금 유형
     * @param user 잠금 요청 사용자
     * @param reason 잠금 사유
     * @return 생성된 기간 잠금 정보
     */
    public PeriodLock lockPeriod(Long fiscalPeriodId, PeriodLock.PeriodLockType lockType, String user, String reason) {
        FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found with id: " + fiscalPeriodId));

        if (periodLockRepository.findByFiscalPeriod(fiscalPeriod).isPresent()) {
            throw new IllegalStateException("FiscalPeriod " + fiscalPeriod.getFiscalYear() + "-" + fiscalPeriod.getFiscalPeriod() + " is already locked.");
        }

        PeriodLock periodLock = new PeriodLock();
        periodLock.setFiscalPeriod(fiscalPeriod);
        periodLock.setLockType(lockType);
        periodLock.setLockedBy(user);
        periodLock.setLockedAt(LocalDateTime.now());
        periodLock.setReason(reason);
        return periodLockRepository.save(periodLock);
    }

    /**
     * 특정 회계 기간의 잠금을 해제합니다.
     * @param fiscalPeriodId 잠금을 해제할 회계 기간 ID
     * @param user 해제 요청 사용자
     * @return 해제된 기간 잠금 정보
     */
    public void unlockPeriod(Long fiscalPeriodId, String user) {
        FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found with id: " + fiscalPeriodId));

        PeriodLock periodLock = periodLockRepository.findByFiscalPeriod(fiscalPeriod)
                .orElseThrow(() -> new EntityNotFoundException("PeriodLock not found for fiscal period " + fiscalPeriod.getFiscalYear() + "-" + fiscalPeriod.getFiscalPeriod()));

        periodLockRepository.delete(periodLock);
        // TODO: Audit Trail for unlock action
    }

    // --- ReopenApproval (기간 재오픈 승인) 관련 메서드 ---

    /**
     * 마감된 기간 재오픈을 요청합니다.
     * @param fiscalPeriodId 재오픈 요청할 회계 기간 ID
     * @param requestedBy 요청자
     * @param reason 재오픈 사유
     * @return 생성된 재오픈 승인 요청
     */
    public ReopenApproval requestPeriodReopen(Long fiscalPeriodId, String requestedBy, String reason) {
        FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found with id: " + fiscalPeriodId));

        if (fiscalPeriod.getClosingStatus() != FiscalPeriod.ClosingStatus.CLOSED && fiscalPeriod.getClosingStatus() != FiscalPeriod.ClosingStatus.PERMANENTLY_CLOSED) {
            throw new IllegalStateException("FiscalPeriod " + fiscalPeriod.getFiscalYear() + "-" + fiscalPeriod.getFiscalPeriod() + " is not closed and cannot be reopened.");
        }

        ReopenApproval approval = new ReopenApproval();
        approval.setFiscalPeriod(fiscalPeriod);
        approval.setRequestedBy(requestedBy);
        approval.setRequestedAt(LocalDateTime.now());
        approval.setReason(reason);
        approval.setStatus(ReopenApprovalStatus.PENDING);
        // TODO: 재오픈 영향도 자동 산출 규칙(변경된 전표/원장/보고 라인) 로직 구현
        // approval.setImpactAnalysisReport(generateReopenImpactReport(fiscalPeriod));
        return reopenApprovalRepository.save(approval);
    }

    /**
     * 기간 재오픈 요청을 승인 또는 거절합니다.
     * @param approvalId 재오픈 승인 ID
     * @param newStatus 새로운 상태 (APPROVED/REJECTED)
     * @param approvedBy 승인/거절자
     * @return 업데이트된 재오픈 승인 요청
     */
    public ReopenApproval updateReopenApprovalStatus(Long approvalId, ReopenApprovalStatus newStatus, String approvedBy) {
        if (newStatus != ReopenApprovalStatus.APPROVED && newStatus != ReopenApprovalStatus.REJECTED) {
            throw new IllegalArgumentException("Only APPROVED or REJECTED status is allowed for update.");
        }

        ReopenApproval approval = reopenApprovalRepository.findById(approvalId)
                .orElseThrow(() -> new EntityNotFoundException("ReopenApproval not found with id: " + approvalId));

        if (approval.getStatus() != ReopenApprovalStatus.PENDING) {
            throw new IllegalStateException("ReopenApproval is not in PENDING status.");
        }

        approval.setStatus(newStatus);
        approval.setApprovedBy(approvedBy);
        approval.setApprovedAt(LocalDateTime.now());
        approval.setAuditUser(approvedBy);

        if (newStatus == ReopenApprovalStatus.APPROVED) {
            // FiscalPeriod 상태를 OPEN으로 변경
            FiscalPeriod fiscalPeriod = approval.getFiscalPeriod();
            fiscalPeriod.setClosingStatus(FiscalPeriod.ClosingStatus.OPEN);
            fiscalPeriodRepository.save(fiscalPeriod);
            // 해당 기간에 대한 모든 PeriodLock 해제 (PeriodLock 엔티티를 찾아서 삭제)
            periodLockRepository.findByFiscalPeriod(fiscalPeriod).ifPresent(periodLockRepository::delete);
            // ClosingCalendar 상태도 OPEN으로 변경 (연동)
            closingCalendarRepository.findByFiscalYearAndFiscalPeriod(fiscalPeriod.getFiscalYear(), fiscalPeriod.getFiscalPeriod())
                    .ifPresent(calendar -> updateClosingCalendarStatus(calendar.getId(), ClosingCalendarStatus.OPEN, approvedBy));
        }
        return reopenApprovalRepository.save(approval);
    }

    // --- ValuationBatch (평가 배치) 관련 메서드 ---

    /**
     * 외화/금융상품 평가 배치를 실행합니다.
     * @param fiscalPeriodId 회계 기간 ID
     * @param valuationType 평가 유형
     * @param runBy 실행자
     * @return 생성된 평가 배치 기록
     */
    public ValuationBatch runValuationBatch(Long fiscalPeriodId, ValuationBatch.ValuationType valuationType, String runBy) {
        FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found with id: " + fiscalPeriodId));

        ValuationBatch batch = new ValuationBatch();
        batch.setFiscalPeriod(fiscalPeriod);
        batch.setValuationType(valuationType);
        batch.setRunDateTime(LocalDateTime.now());
        batch.setStatus(ValuationBatch.ValuationBatchStatus.RUNNING);
        batch.setRunBy(runBy);
        batch.setAuditUser(runBy);
        batch = valuationBatchRepository.save(batch);

        try {
            // TODO: 실제 평가 로직 구현 (외화 환산, 금융상품 평가 등)
            // 평가 결과를 바탕으로 JournalEntry 생성 및 연결
            JournalEntry valuationJE = createAutomatedJournalEntry(
                    fiscalPeriod.getEndDate(),
                    "자동 " + valuationType.name() + " 평가 분개",
                    "SYSTEM",
                    "VALUATION_BATCH",
                    batch.getId().toString()
            );
            batch.setGeneratedJournalEntry(valuationJE);
            batch.setStatus(ValuationBatch.ValuationBatchStatus.COMPLETED);
            // TODO: 평가 리포트 생성 및 링크 reportLink
        } catch (Exception e) {
            batch.setStatus(ValuationBatch.ValuationBatchStatus.FAILED);
            // TODO: 에러 로깅
            throw new RuntimeException("Valuation batch failed for " + valuationType.name() + " in period " + fiscalPeriodId, e);
        }
        return valuationBatchRepository.save(batch);
    }

    // --- ProvisionBatch (충당/손상 배치) 관련 메서드 ---

    /**
     * 충당/손상 배치 (ECL 연계)를 실행합니다.
     * @param fiscalPeriodId 회계 기간 ID
     * @param provisionType 충당/손상 유형
     * @param runBy 실행자
     * @return 생성된 충당/손상 배치 기록
     */
    public ProvisionBatch runProvisionBatch(Long fiscalPeriodId, ProvisionBatch.ProvisionType provisionType, String runBy) {
        FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found with id: " + fiscalPeriodId));

        ProvisionBatch batch = new ProvisionBatch();
        batch.setFiscalPeriod(fiscalPeriod);
        batch.setProvisionType(provisionType);
        batch.setRunDateTime(LocalDateTime.now());
        batch.setStatus(ProvisionBatch.ProvisionBatchStatus.RUNNING);
        batch.setRunBy(runBy);
        batch.setAuditUser(runBy);
        batch = provisionBatchRepository.save(batch);

        try {
            // TODO: 실제 충당/손상 로직 구현 (ECL 계산 등)
            // 결과 바탕으로 JournalEntry 생성 및 연결
            JournalEntry provisionJE = createAutomatedJournalEntry(
                    fiscalPeriod.getEndDate(),
                    "자동 " + provisionType.name() + " 충당/손상 분개",
                    "SYSTEM",
                    "PROVISION_BATCH",
                    batch.getId().toString()
            );
            batch.setGeneratedJournalEntry(provisionJE);
            batch.setStatus(ProvisionBatch.ProvisionBatchStatus.COMPLETED);
            // TODO: 리포트 생성 및 링크 reportLink
        } catch (Exception e) {
            batch.setStatus(ProvisionBatch.ProvisionBatchStatus.FAILED);
            // TODO: 에러 로깅
            throw new RuntimeException("Provision batch failed for " + provisionType.name() + " in period " + fiscalPeriodId, e);
        }
        return provisionBatchRepository.save(batch);
    }

    // --- ClosingAdjustment (결산 조정) 관련 메서드 ---

    /**
     * 새로운 결산 조정 분개 전표를 기록합니다.
     * @param fiscalPeriodId 회계 기간 ID
     * @param journalEntryId 생성된 조정 분개 전표 ID
     * @param adjustmentType 조정 유형
     * @param description 설명
     * @param approvedBy 승인자
     * @return 생성된 결산 조정 기록
     */
    public ClosingAdjustment createClosingAdjustment(Long fiscalPeriodId, Long journalEntryId, ClosingAdjustment.AdjustmentType adjustmentType, String description, String approvedBy) {
        FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found with id: " + fiscalPeriodId));
        JournalEntry journalEntry = journalEntryRepository.findById(journalEntryId)
                .orElseThrow(() -> new EntityNotFoundException("JournalEntry not found with id: " + journalEntryId));

        // TODO: journalEntry가 ADJUSTMENT 타입인지, 해당 회계 기간에 속하는지 등의 유효성 검사

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

    // --- 마감 완료 조건 (DoD: 마감 완료/실패를 시스템이 판정 가능) ---

    /**
     * 특정 회계 기간의 결산 완료/실패 여부를 시스템이 판정하고 상태를 업데이트합니다.
     * 마감 완료 조건:
     * 1. 모든 필수 ClosingTask가 COMPLETED 상태.
     * 2. 모든 ClosingGate가 PASSED 상태.
     * 3. (선택적) 해당 기간의 모든 ReconciliationRun이 SUCCESS 상태 (또는 특정 조건 충족).
     * @param calendarId 결산 캘린더 ID
     * @param user 판정 요청 사용자
     * @return 업데이트된 결산 캘린더 (CLOSED 또는 IN_PROGRESS/FAILED)
     */
    public ClosingCalendar determineClosingStatus(Long calendarId, String user) {
        ClosingCalendar calendar = findClosingCalendarById(calendarId);

        if (calendar.getStatus() == ClosingCalendarStatus.CLOSED || calendar.getStatus() == ClosingCalendarStatus.PERMANENTLY_CLOSED) {
            throw new IllegalStateException("Closing for fiscal period " + calendar.getFiscalYear() + "-" + calendar.getFiscalPeriod() + " is already closed.");
        }

        List<ClosingTask> tasks = closingTaskRepository.findByClosingCalendarOrderByTaskOrderAsc(calendar);
        List<ClosingGate> gates = closingGateRepository.findByClosingCalendar(calendar);

        // 1. 모든 필수 ClosingTask가 COMPLETED 상태인지 확인
        boolean allMandatoryTasksCompleted = tasks.stream()
                .filter(ClosingTask::isMandatory)
                .allMatch(task -> task.getStatus() == ClosingTaskStatus.COMPLETED);

        if (!allMandatoryTasksCompleted) {
            // TODO: 실패 사유 상세 로깅 및 알림
            updateClosingCalendarStatus(calendarId, ClosingCalendarStatus.IN_PROGRESS, user); // 또는 FAILED 상태 도입
            throw new IllegalStateException("Not all mandatory closing tasks are completed for " + calendar.getFiscalYear() + "-" + calendar.getFiscalPeriod());
        }

        // 2. 모든 ClosingGate가 PASSED 상태인지 확인
        boolean allGatesPassed = gates.stream()
                .allMatch(gate -> gate.getStatus() == ClosingGateStatus.PASSED);

        if (!allGatesPassed) {
            // TODO: 실패 사유 상세 로깅 및 알림
            updateClosingCalendarStatus(calendarId, ClosingCalendarStatus.IN_PROGRESS, user); // 또는 FAILED 상태 도입
            throw new IllegalStateException("Not all closing gates are passed for " + calendar.getFiscalYear() + "-" + calendar.getFiscalPeriod());
        }

        // 3. (선택적) 해당 기간의 모든 ReconciliationRun이 SUCCESS 상태 (또는 특정 조건 충족)
        // TODO: ReconciliationRun 상태 확인 로직 구현 (ReconciliationService 또는 리포지토리 연동)
        // 예: reconciliationRunRepository.findByFiscalPeriodAndStatus(fiscalPeriod, ReconciliationRunStatus.SUCCESS).size() == totalRunsExpected;

        // 모든 조건 충족 시 최종 마감 처리
        return updateClosingCalendarStatus(calendarId, ClosingCalendarStatus.CLOSED, user);
    }

    // --- Helper Methods ---

    /**
     * 자동 생성되는 분개 전표를 생성합니다. (평가/충당 배치 등에서 사용)
     * @param accountingDate 회계일자
     * @param description 적요
     * @param createdBy 생성자
     * @param lineageSourceType 원천 시스템 유형 (예: VALUATION_BATCH)
     * @param lineageSourceId 원천 시스템 ID (예: 배치 ID)
     * @return 생성된 JournalEntry (저장된 상태)
     */
    private JournalEntry createAutomatedJournalEntry(LocalDate accountingDate, String description, String createdBy, String lineageSourceType, String lineageSourceId) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.now());
        entry.setAccountingDate(accountingDate);
        entry.setDescription(description);
        entry.setStatus(JournalEntryStatus.DRAFT); // 자동 생성된 전표도 승인 프로세스를 거칠 수 있음
        entry.setEntryType("ADJUSTMENT"); // 결산 관련 자동 생성 분개는 ADJUSTMENT로 간주
        entry.setCreatedBy(createdBy);
        entry.setAuditUser(createdBy);
        entry.setLineageSourceType(lineageSourceType);
        entry.setLineageSourceId(lineageSourceId);
        // TODO: 계정과목, 금액 등 JournalDetail 생성 로직 추가 필요
        // 현재는 빈 상세 정보로 저장됨. 실제 구현 시 분개 상세 정보 포함되어야 함.

        // 전표번호 생성 (예시)
        entry.setSlipNo(accountingDate.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-AUTO-" + journalEntryRepository.count());

        return journalEntryRepository.save(entry);
    }
}
