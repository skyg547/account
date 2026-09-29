package com.ho.account.closing.web;

import com.ho.account.closing.application.port.in.AnnualClosingUseCase;
import com.ho.account.closing.application.port.in.ClosingUseCase;
import com.ho.account.closing.domain.*;
import com.ho.account.closing.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * [헥사고날 아키텍처 - 인바운드 어댑터 (Inbound Web Adapter)]
 * 결산 (Closing) 관련 REST API를 제공하는 컨트롤러.
 * 결산 캘린더, 태스크, 게이트, 기간 잠금 및 재오픈 승인, 평가/충당 배치, 결산 조정 등을 관리합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 결산 기간 오픈, 재오픈, 결산 평가 자동 분개 등 외부 시스템이나 사용자가 
 * 애플리케이션으로 명령을 보내는 유일한 관문(Web API)입니다.
 * HTTP 요청(Request Dto)을 받아, 애플리케이션이 이해할 수 있는 명령으로 변환 후 `ClosingUseCase`에 전달합니다.
 * Gateway가 검증해 다시 만든 identity 헤더만 명령의 actor로 신뢰하며, API 포트의 직접 외부 공개는 지원하지 않습니다.
 */
@RestController
@RequestMapping("/api/closing")
@RequiredArgsConstructor
public class ClosingController {

    private final ClosingUseCase closingUseCase;
    private final AnnualClosingUseCase annualClosingUseCase;

    // --- ClosingCalendar (결산 캘린더) API ---

    /**
     * 새로운 결산 캘린더를 생성합니다.
     * @param requestDto 생성할 결산 캘린더 정보
     * @return 생성된 결산 캘린더 정보
     */
    @PostMapping("/calendars")
    public ResponseEntity<ClosingCalendarDto> createClosingCalendar(
            @Valid @RequestBody ClosingCalendarRequestDto requestDto,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        ClosingCalendar calendar = requestDto.toEntity();
        calendar.setAuditUser(trustedActor);
        ClosingCalendar createdCalendar = closingUseCase.createClosingCalendar(calendar);
        return new ResponseEntity<>(ClosingCalendarDto.fromEntity(createdCalendar), HttpStatus.CREATED);
    }

    /**
     * ID로 결산 캘린더를 조회합니다.
     * @param id 결산 캘린더 ID
     * @return 조회된 결산 캘린더 정보
     */
    @GetMapping("/calendars/{id}")
    public ResponseEntity<ClosingCalendarDto> getClosingCalendarById(@PathVariable("id") Long id) {
        ClosingCalendar calendar = closingUseCase.findClosingCalendarById(id);
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
        ClosingCalendar calendar = closingUseCase.findClosingCalendarByFiscalPeriod(fiscalYear, fiscalPeriod);
        return ResponseEntity.ok(ClosingCalendarDto.fromEntity(calendar));
    }

    /**
     * 결산 캘린더의 상태를 업데이트합니다.
     * @param id 결산 캘린더 ID
     * @param requestDto 업데이트할 상태 정보
     * @return 업데이트된 결산 캘린더 정보
     */
    @PutMapping("/calendars/{id}/status")
    public ResponseEntity<ClosingCalendarDto> updateClosingCalendarStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody ClosingCalendarStatusUpdateDto requestDto,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        ClosingCalendar updatedCalendar = closingUseCase.updateClosingCalendarStatus(
                id, requestDto.getStatus(), trustedActor);
        return ResponseEntity.ok(ClosingCalendarDto.fromEntity(updatedCalendar));
    }

    // --- ClosingTask (결산 태스크) API ---

    /**
     * 특정 결산 캘린더의 태스크 목록을 조회합니다.
     * @param calendarId 결산 캘린더 ID
     * @return 해당 캘린더에 속한 결산 태스크 목록
     */
    @GetMapping("/calendars/{calendarId}/tasks")
    public ResponseEntity<List<ClosingTaskDto>> getClosingTasksByCalendarId(@PathVariable("calendarId") Long calendarId) {
        List<ClosingTaskDto> tasks = closingUseCase.findClosingTasksByCalendarId(calendarId).stream()
                .map(ClosingTaskDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tasks);
    }

    /**
     * 새로운 결산 태스크를 생성합니다.
     * @param requestDto 생성할 결산 태스크 정보
     * @return 생성된 결산 태스크 정보
     */
    @PostMapping("/tasks")
    public ResponseEntity<ClosingTaskDto> createClosingTask(
            @Valid @RequestBody ClosingTaskRequestDto requestDto,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        ClosingCalendar calendar = closingUseCase.findClosingCalendarById(requestDto.getCalendarId());
        ClosingTask task = requestDto.toEntity();
        task.setClosingCalendar(calendar);
        task.setAuditUser(trustedActor);
        ClosingTask createdTask = closingUseCase.createClosingTask(task);
        return new ResponseEntity<>(ClosingTaskDto.fromEntity(createdTask), HttpStatus.CREATED);
    }

    /**
     * 결산 태스크의 상태를 업데이트합니다.
     * @param id 결산 태스크 ID
     * @param requestDto 업데이트할 상태 정보
     * @return 업데이트된 결산 태스크 정보
     */
    @PutMapping("/tasks/{id}/status")
    public ResponseEntity<ClosingTaskDto> updateClosingTaskStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody ClosingTaskStatusUpdateDto requestDto,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        ClosingTask updatedTask = closingUseCase.updateClosingTaskStatus(id, requestDto.getStatus(), trustedActor);
        return ResponseEntity.ok(ClosingTaskDto.fromEntity(updatedTask));
    }

    // --- ClosingGate (결산 게이트) API ---

    /**
     * 새로운 결산 게이트를 생성합니다.
     * @param requestDto 생성할 결산 게이트 정보
     * @return 생성된 결산 게이트 정보
     */
    @PostMapping("/gates")
    public ResponseEntity<ClosingGateDto> createClosingGate(
            @Valid @RequestBody ClosingGateRequestDto requestDto,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        ClosingCalendar calendar = closingUseCase.findClosingCalendarById(requestDto.getCalendarId());
        ClosingGate gate = requestDto.toEntity();
        gate.setClosingCalendar(calendar);
        gate.setAuditUser(trustedActor);
        ClosingGate createdGate = closingUseCase.createClosingGate(gate);
        return new ResponseEntity<>(ClosingGateDto.fromEntity(createdGate), HttpStatus.CREATED);
    }

    /**
     * 결산 게이트의 통과 조건을 확인하고 상태를 업데이트합니다.
     * @param id 결산 게이트 ID
     * @return 업데이트된 결산 게이트 정보
     */
    @PutMapping("/gates/{id}/check")
    public ResponseEntity<ClosingGateDto> checkAndPassClosingGate(
            @PathVariable("id") Long id,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        ClosingGate updatedGate = closingUseCase.checkAndPassClosingGate(id, trustedActor);
        return ResponseEntity.ok(ClosingGateDto.fromEntity(updatedGate));
    }

    // --- PeriodLock (기간 잠금) API ---

    /**
     * 특정 회계 기간을 잠급니다.
     * @param requestDto 기간 잠금 요청 정보
     * @return 생성된 기간 잠금 정보
     */
    @PostMapping("/period-locks")
    public ResponseEntity<PeriodLockDto> lockPeriod(
            @Valid @RequestBody PeriodLockRequestDto requestDto,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        PeriodLock periodLock = closingUseCase.lockPeriod(
                requestDto.getFiscalPeriodId(), requestDto.getLockType(), trustedActor, requestDto.getReason());
        return new ResponseEntity<>(PeriodLockDto.fromEntity(periodLock), HttpStatus.CREATED);
    }

    /**
     * 특정 회계 기간의 잠금을 해제합니다.
     * @param fiscalPeriodId 잠금을 해제할 회계 기간 ID
     * @return 응답 없음
     */
    @DeleteMapping("/period-locks/{fiscalPeriodId}")
    public ResponseEntity<Void> unlockPeriod(
            @PathVariable("fiscalPeriodId") Long fiscalPeriodId,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        closingUseCase.unlockPeriod(fiscalPeriodId, trustedActor);
        return ResponseEntity.noContent().build();
    }

    // --- ReopenApproval (기간 재오픈 승인) API ---

    /**
     * 마감된 기간 재오픈을 요청합니다.
     * @param requestDto 재오픈 요청 정보
     * @return 생성된 재오픈 승인 요청 정보
     */
    @PostMapping("/reopen-approvals")
    public ResponseEntity<ReopenApprovalDto> requestPeriodReopen(
            @Valid @RequestBody ReopenApprovalRequestDto requestDto,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        ReopenApproval approval = closingUseCase.requestPeriodReopen(
                requestDto.getFiscalPeriodId(), trustedActor, requestDto.getReason());
        return new ResponseEntity<>(ReopenApprovalDto.fromEntity(approval), HttpStatus.CREATED);
    }

    /**
     * 기간 재오픈 요청을 승인 또는 거절합니다.
     * @param id 재오픈 승인 ID
     * @param requestDto 업데이트할 상태 정보
     * @return 업데이트된 재오픈 승인 요청 정보
     */
    @PutMapping("/reopen-approvals/{id}/status")
    public ResponseEntity<ReopenApprovalDto> updateReopenApprovalStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReopenApprovalStatusUpdateDto requestDto,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        ReopenApproval updatedApproval = closingUseCase.updateReopenApprovalStatus(
                id, requestDto.getStatus(), trustedActor);
        return ResponseEntity.ok(ReopenApprovalDto.fromEntity(updatedApproval));
    }

    // --- ValuationBatch (평가 배치) API ---

    /**
     * 외화/금융상품 평가 배치를 실행합니다.
     * @param requestDto 평가 배치 요청 정보
     * @return 생성된 평가 배치 기록 정보
     */
    @PostMapping("/valuation-batches/run")
    public ResponseEntity<ValuationBatchDto> runValuationBatch(
            @Valid @RequestBody ValuationBatchRequestDto requestDto,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        ValuationBatch batch = closingUseCase.runValuationBatch(
                requestDto.getFiscalPeriodId(), requestDto.getValuationType(), trustedActor);
        return new ResponseEntity<>(ValuationBatchDto.fromEntity(batch), HttpStatus.CREATED);
    }

    // --- ProvisionBatch (충당/손상 배치) API ---

    /**
     * 충당/손상 배치 (ECL 연계)를 실행합니다.
     * @param requestDto 충당/손상 배치 요청 정보
     * @return 생성된 충당/손상 배치 기록 정보
     */
    @PostMapping("/provision-batches/run")
    public ResponseEntity<ProvisionBatchDto> runProvisionBatch(
            @Valid @RequestBody ProvisionBatchRequestDto requestDto,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        ProvisionBatch batch = closingUseCase.runProvisionBatch(
                requestDto.getFiscalPeriodId(), requestDto.getProvisionType(), trustedActor);
        return new ResponseEntity<>(ProvisionBatchDto.fromEntity(batch), HttpStatus.CREATED);
    }

    // --- ClosingAdjustment (결산 조정) API ---

    /**
     * 새로운 결산 조정 분개 전표를 기록합니다.
     * @param requestDto 결산 조정 요청 정보
     * @return 생성된 결산 조정 기록 정보
     */
    @PostMapping("/adjustments")
    public ResponseEntity<ClosingAdjustmentDto> createClosingAdjustment(
            @Valid @RequestBody ClosingAdjustmentRequestDto requestDto,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        ClosingAdjustment adjustment = closingUseCase.createClosingAdjustment(
                requestDto.getFiscalPeriodId(),
                requestDto.getJournalEntryId(),
                requestDto.getAdjustmentType(),
                requestDto.getDescription(),
                trustedActor
        );
        return new ResponseEntity<>(ClosingAdjustmentDto.fromEntity(adjustment), HttpStatus.CREATED);
    }

    // --- 마감 완료 판정 (DoD) API ---

    /**
     * 특정 결산 캘린더의 마감 완료/실패 여부를 시스템이 판정합니다.
     * @param requestDto 결산 캘린더 ID
     * @return 업데이트된 결산 캘린더 정보 (CLOSED 또는 IN_PROGRESS/FAILED)
     */
    @PostMapping("/calendars/determine-status")
    public ResponseEntity<ClosingCalendarDto> determineClosingStatus(
            @Valid @RequestBody ClosingStatusDetermineRequestDto requestDto,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        ClosingCalendar updatedCalendar = closingUseCase.determineClosingStatus(
                requestDto.getCalendarId(), trustedActor);
        return ResponseEntity.ok(ClosingCalendarDto.fromEntity(updatedCalendar));
    }

    /**
     * 연차 결산(손익 대체 분개 생성)을 수행합니다.
     */
    @PostMapping("/annual/perform-income-statement-closing")
    public ResponseEntity<Void> performIncomeStatementClosing(
            @Valid @RequestBody AnnualClosingRequestDto requestDto,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        annualClosingUseCase.performIncomeStatementClosing(requestDto.getYear());
        return ResponseEntity.ok().build();
    }
}
