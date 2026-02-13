package com.ho.account.closing.web;

import com.ho.account.closing.domain.*;
import com.ho.account.closing.dto.*;
import com.ho.account.closing.service.ClosingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 결산 (Closing) 관련 REST API를 제공하는 컨트롤러.
 * 결산 캘린더, 태스크, 게이트, 기간 잠금 및 재오픈 승인, 평가/충당 배치, 결산 조정 등을 관리합니다.
 */
@RestController
@RequestMapping("/api/closing")
public class ClosingController {

    private final ClosingService closingService;

    @Autowired
    public ClosingController(ClosingService closingService) {
        this.closingService = closingService;
    }

    // --- ClosingCalendar (결산 캘린더) API ---

    /**
     * 새로운 결산 캘린더를 생성합니다.
     * @param requestDto 생성할 결산 캘린더 정보
     * @return 생성된 결산 캘린더 정보
     */
    @PostMapping("/calendars")
    public ResponseEntity<ClosingCalendarDto> createClosingCalendar(@Valid @RequestBody ClosingCalendarRequestDto requestDto) {
        ClosingCalendar calendar = requestDto.toEntity();
        ClosingCalendar createdCalendar = closingService.createClosingCalendar(calendar);
        return new ResponseEntity<>(ClosingCalendarDto.fromEntity(createdCalendar), HttpStatus.CREATED);
    }

    /**
     * ID로 결산 캘린더를 조회합니다.
     * @param id 결산 캘린더 ID
     * @return 조회된 결산 캘린더 정보
     */
    @GetMapping("/calendars/{id}")
    public ResponseEntity<ClosingCalendarDto> getClosingCalendarById(@PathVariable Long id) {
        ClosingCalendar calendar = closingService.findClosingCalendarById(id);
        return ResponseEntity.ok(ClosingCalendarDto.fromEntity(calendar));
    }

    /**
     * 회계연도와 기간으로 결산 캘린더를 조회합니다.
     * @param fiscalYear 회계연도
     * @param fiscalPeriod 회계기간
     * @return 조회된 결산 캘린더 정보
     */
    @GetMapping("/calendars/by-period")
    public ResponseEntity<ClosingCalendarDto> getClosingCalendarByFiscalPeriod(@RequestParam String fiscalYear, @RequestParam String fiscalPeriod) {
        ClosingCalendar calendar = closingService.findClosingCalendarByFiscalPeriod(fiscalYear, fiscalPeriod);
        return ResponseEntity.ok(ClosingCalendarDto.fromEntity(calendar));
    }

    /**
     * 결산 캘린더의 상태를 업데이트합니다.
     * @param id 결산 캘린더 ID
     * @param requestDto 업데이트할 상태 및 사용자 정보
     * @return 업데이트된 결산 캘린더 정보
     */
    @PutMapping("/calendars/{id}/status")
    public ResponseEntity<ClosingCalendarDto> updateClosingCalendarStatus(@PathVariable Long id, @Valid @RequestBody ClosingCalendarStatusUpdateDto requestDto) {
        ClosingCalendar updatedCalendar = closingService.updateClosingCalendarStatus(id, requestDto.getStatus(), requestDto.getUser());
        return ResponseEntity.ok(ClosingCalendarDto.fromEntity(updatedCalendar));
    }

    // --- ClosingTask (결산 태스크) API ---

    /**
     * 새로운 결산 태스크를 생성합니다.
     * @param requestDto 생성할 결산 태스크 정보
     * @return 생성된 결산 태스크 정보
     */
    @PostMapping("/tasks")
    public ResponseEntity<ClosingTaskDto> createClosingTask(@Valid @RequestBody ClosingTaskRequestDto requestDto) {
        ClosingCalendar calendar = closingService.findClosingCalendarById(requestDto.getCalendarId());
        ClosingTask task = requestDto.toEntity();
        task.setClosingCalendar(calendar);
        ClosingTask createdTask = closingService.createClosingTask(task);
        return new ResponseEntity<>(ClosingTaskDto.fromEntity(createdTask), HttpStatus.CREATED);
    }

    /**
     * 결산 태스크의 상태를 업데이트합니다.
     * @param id 결산 태스크 ID
     * @param requestDto 업데이트할 상태 및 사용자 정보
     * @return 업데이트된 결산 태스크 정보
     */
    @PutMapping("/tasks/{id}/status")
    public ResponseEntity<ClosingTaskDto> updateClosingTaskStatus(@PathVariable Long id, @Valid @RequestBody ClosingTaskStatusUpdateDto requestDto) {
        ClosingTask updatedTask = closingService.updateClosingTaskStatus(id, requestDto.getStatus(), requestDto.getUser());
        return ResponseEntity.ok(ClosingTaskDto.fromEntity(updatedTask));
    }

    // --- ClosingGate (결산 게이트) API ---

    /**
     * 새로운 결산 게이트를 생성합니다.
     * @param requestDto 생성할 결산 게이트 정보
     * @return 생성된 결산 게이트 정보
     */
    @PostMapping("/gates")
    public ResponseEntity<ClosingGateDto> createClosingGate(@Valid @RequestBody ClosingGateRequestDto requestDto) {
        ClosingCalendar calendar = closingService.findClosingCalendarById(requestDto.getCalendarId());
        ClosingGate gate = requestDto.toEntity();
        gate.setClosingCalendar(calendar);
        ClosingGate createdGate = closingService.createClosingGate(gate);
        return new ResponseEntity<>(ClosingGateDto.fromEntity(createdGate), HttpStatus.CREATED);
    }

    /**
     * 결산 게이트의 통과 조건을 확인하고 상태를 업데이트합니다.
     * @param id 결산 게이트 ID
     * @param user 요청 사용자
     * @return 업데이트된 결산 게이트 정보
     */
    @PutMapping("/gates/{id}/check")
    public ResponseEntity<ClosingGateDto> checkAndPassClosingGate(@PathVariable Long id, @RequestParam String user) {
        ClosingGate updatedGate = closingService.checkAndPassClosingGate(id, user);
        return ResponseEntity.ok(ClosingGateDto.fromEntity(updatedGate));
    }

    // --- PeriodLock (기간 잠금) API ---

    /**
     * 특정 회계 기간을 잠급니다.
     * @param requestDto 기간 잠금 요청 정보
     * @return 생성된 기간 잠금 정보
     */
    @PostMapping("/period-locks")
    public ResponseEntity<PeriodLockDto> lockPeriod(@Valid @RequestBody PeriodLockRequestDto requestDto) {
        PeriodLock periodLock = closingService.lockPeriod(requestDto.getFiscalPeriodId(), requestDto.getLockType(), requestDto.getUser(), requestDto.getReason());
        return new ResponseEntity<>(PeriodLockDto.fromEntity(periodLock), HttpStatus.CREATED);
    }

    /**
     * 특정 회계 기간의 잠금을 해제합니다.
     * @param fiscalPeriodId 잠금을 해제할 회계 기간 ID
     * @param user 해제 요청 사용자
     * @return 응답 없음
     */
    @DeleteMapping("/period-locks/{fiscalPeriodId}")
    public ResponseEntity<Void> unlockPeriod(@PathVariable Long fiscalPeriodId, @RequestParam String user) {
        closingService.unlockPeriod(fiscalPeriodId, user);
        return ResponseEntity.noContent().build();
    }

    // --- ReopenApproval (기간 재오픈 승인) API ---

    /**
     * 마감된 기간 재오픈을 요청합니다.
     * @param requestDto 재오픈 요청 정보
     * @return 생성된 재오픈 승인 요청 정보
     */
    @PostMapping("/reopen-approvals")
    public ResponseEntity<ReopenApprovalDto> requestPeriodReopen(@Valid @RequestBody ReopenApprovalRequestDto requestDto) {
        ReopenApproval approval = closingService.requestPeriodReopen(requestDto.getFiscalPeriodId(), requestDto.getRequestedBy(), requestDto.getReason());
        return new ResponseEntity<>(ReopenApprovalDto.fromEntity(approval), HttpStatus.CREATED);
    }

    /**
     * 기간 재오픈 요청을 승인 또는 거절합니다.
     * @param id 재오픈 승인 ID
     * @param requestDto 업데이트할 상태 및 승인자 정보
     * @return 업데이트된 재오픈 승인 요청 정보
     */
    @PutMapping("/reopen-approvals/{id}/status")
    public ResponseEntity<ReopenApprovalDto> updateReopenApprovalStatus(@PathVariable Long id, @Valid @RequestBody ReopenApprovalStatusUpdateDto requestDto) {
        ReopenApproval updatedApproval = closingService.updateReopenApprovalStatus(id, requestDto.getStatus(), requestDto.getApprovedBy());
        return ResponseEntity.ok(ReopenApprovalDto.fromEntity(updatedApproval));
    }

    // --- ValuationBatch (평가 배치) API ---

    /**
     * 외화/금융상품 평가 배치를 실행합니다.
     * @param requestDto 평가 배치 요청 정보
     * @return 생성된 평가 배치 기록 정보
     */
    @PostMapping("/valuation-batches/run")
    public ResponseEntity<ValuationBatchDto> runValuationBatch(@Valid @RequestBody ValuationBatchRequestDto requestDto) {
        ValuationBatch batch = closingService.runValuationBatch(requestDto.getFiscalPeriodId(), requestDto.getValuationType(), requestDto.getRunBy());
        return new ResponseEntity<>(ValuationBatchDto.fromEntity(batch), HttpStatus.CREATED);
    }

    // --- ProvisionBatch (충당/손상 배치) API ---

    /**
     * 충당/손상 배치 (ECL 연계)를 실행합니다.
     * @param requestDto 충당/손상 배치 요청 정보
     * @return 생성된 충당/손상 배치 기록 정보
     */
    @PostMapping("/provision-batches/run")
    public ResponseEntity<ProvisionBatchDto> runProvisionBatch(@Valid @RequestBody ProvisionBatchRequestDto requestDto) {
        ProvisionBatch batch = closingService.runProvisionBatch(requestDto.getFiscalPeriodId(), requestDto.getProvisionType(), requestDto.getRunBy());
        return new ResponseEntity<>(ProvisionBatchDto.fromEntity(batch), HttpStatus.CREATED);
    }

    // --- ClosingAdjustment (결산 조정) API ---

    /**
     * 새로운 결산 조정 분개 전표를 기록합니다.
     * @param requestDto 결산 조정 요청 정보
     * @return 생성된 결산 조정 기록 정보
     */
    @PostMapping("/adjustments")
    public ResponseEntity<ClosingAdjustmentDto> createClosingAdjustment(@Valid @RequestBody ClosingAdjustmentRequestDto requestDto) {
        ClosingAdjustment adjustment = closingService.createClosingAdjustment(
                requestDto.getFiscalPeriodId(),
                requestDto.getJournalEntryId(),
                requestDto.getAdjustmentType(),
                requestDto.getDescription(),
                requestDto.getApprovedBy()
        );
        return new ResponseEntity<>(ClosingAdjustmentDto.fromEntity(adjustment), HttpStatus.CREATED);
    }

    // --- 마감 완료 판정 (DoD) API ---

    /**
     * 특정 결산 캘린더의 마감 완료/실패 여부를 시스템이 판정합니다.
     * @param requestDto 결산 캘린더 ID 및 판정 요청 사용자 정보
     * @return 업데이트된 결산 캘린더 정보 (CLOSED 또는 IN_PROGRESS/FAILED)
     */
    @PostMapping("/calendars/determine-status")
    public ResponseEntity<ClosingCalendarDto> determineClosingStatus(@Valid @RequestBody ClosingStatusDetermineRequestDto requestDto) {
        ClosingCalendar updatedCalendar = closingService.determineClosingStatus(requestDto.getCalendarId(), requestDto.getUser());
        return ResponseEntity.ok(ClosingCalendarDto.fromEntity(updatedCalendar));
    }
}
