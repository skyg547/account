package com.ho.account.loan.web;

import com.ho.account.loan.domain.DeferredItemType;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanDisbursal;
import com.ho.account.loan.domain.LoanEvent;
import com.ho.account.loan.domain.RecalculationRun;
import com.ho.account.loan.dto.*;
import com.ho.account.loan.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 대출 회계 (Loan Accounting) 관련 REST API를 제공하는 컨트롤러.
 * 대출 계약 관리, 실행, 이연 부대손익, EIR 스케줄 및 재계산 등을 처리합니다.
 */
@RestController
@RequestMapping("/api/loan")
public class LoanController {

    private final LoanService loanService;

    @Autowired
    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // --- Loan (대출) API ---

    /**
     * 새로운 대출을 생성합니다.
     * @param requestDto 생성할 대출 정보
     * @return 생성된 대출 정보
     */
    @PostMapping("/loans")
    public ResponseEntity<LoanDto> createLoan(@Valid @RequestBody LoanRequestDto requestDto) {
        Loan loan = requestDto.toEntity();
        Loan createdLoan = loanService.createLoan(loan);
        return new ResponseEntity<>(LoanDto.fromEntity(createdLoan), HttpStatus.CREATED);
    }

    /**
     * ID로 대출을 조회합니다.
     * @param id 대출 ID
     * @return 조회된 대출 정보
     */
    @GetMapping("/loans/{id}")
    public ResponseEntity<LoanDto> getLoanById(@PathVariable("id") Long id) {
        Loan loan = loanService.findLoanById(id);
        return ResponseEntity.ok(LoanDto.fromEntity(loan));
    }

    // --- LoanDisbursal (대출 실행) API ---

    /**
     * 대출을 실행하고 관련 분개 전표를 생성합니다.
     * @param requestDto 대출 실행 요청 정보
     * @return 생성된 대출 실행 기록 정보
     */
    @PostMapping("/disbursals")
    public ResponseEntity<LoanDisbursalDto> disburseLoan(@Valid @RequestBody LoanDisbursalRequestDto requestDto) {
        LoanDisbursal disbursal = loanService.disburseLoan(
                requestDto.getLoanId(),
                requestDto.getDisbursalDate(),
                requestDto.getDisbursedAmount(),
                requestDto.getUser()
        );
        return new ResponseEntity<>(LoanDisbursalDto.fromEntity(disbursal), HttpStatus.CREATED);
    }

    // --- DeferredItemType (이연 항목 유형) API ---

    /**
     * 새로운 이연 항목 유형을 생성합니다.
     * @param requestDto 생성할 이연 항목 유형 정보
     * @return 생성된 이연 항목 유형 정보
     */
    @PostMapping("/deferred-item-types")
    public ResponseEntity<DeferredItemTypeDto> createDeferredItemType(@Valid @RequestBody DeferredItemTypeRequestDto requestDto) {
        DeferredItemType itemType = requestDto.toEntity();
        DeferredItemType createdItemType = loanService.createDeferredItemType(itemType);
        return new ResponseEntity<>(DeferredItemTypeDto.fromEntity(createdItemType), HttpStatus.CREATED);
    }

    /**
     * 코드로 이연 항목 유형을 조회합니다.
     * @param code 이연 항목 유형 코드
     * @return 조회된 이연 항목 유형 정보
     */
    @GetMapping("/deferred-item-types/by-code/{code}")
    public ResponseEntity<DeferredItemTypeDto> getDeferredItemTypeByCode(@PathVariable("code") String code) {
        DeferredItemType itemType = loanService.findDeferredItemTypeByCode(code);
        return ResponseEntity.ok(DeferredItemTypeDto.fromEntity(itemType));
    }

    // --- DeferredItem (이연 항목) API ---

    /**
     * 대출에 대한 이연 항목을 생성하고 초기 분개 전표를 발행합니다.
     * @param requestDto 이연 항목 생성 요청 정보
     * @return 생성된 이연 항목 정보
     */
    @PostMapping("/deferred-items")
    public ResponseEntity<DeferredItemDto> createDeferredItem(@Valid @RequestBody DeferredItemRequestDto requestDto) {
        com.ho.account.loan.domain.DeferredItem deferredItem = loanService.createDeferredItem(
                requestDto.getLoanId(),
                requestDto.getDeferredItemTypeId(),
                requestDto.getAmount(),
                requestDto.getDeferralDate(),
                requestDto.getAmortizationEndDate(),
                requestDto.getUser()
        );
        return new ResponseEntity<>(DeferredItemDto.fromEntity(deferredItem), HttpStatus.CREATED);
    }

    // --- EIR Amortization Schedule (EIR 상각 스케줄) API ---

    /**
     * 대출의 EIR 상각 스케줄을 생성합니다. (최초 실행 시 또는 재계산 시)
     * @param requestDto 스케줄 생성 요청 정보
     * @return 생성된 상각 스케줄 목록
     */
    @PostMapping("/amortization-schedules/generate")
    public ResponseEntity<List<EIRAmortizationScheduleDto>> generateAmortizationSchedule(@Valid @RequestBody AmortizationScheduleGenerateRequestDto requestDto) {
        List<com.ho.account.loan.domain.EIRAmortizationSchedule> schedule = loanService.generateAmortizationSchedule(
                requestDto.getLoanId(),
                requestDto.getRecalculationDate(),
                requestDto.getNewEIR(),
                requestDto.getUser()
        );
        List<EIRAmortizationScheduleDto> dtoList = schedule.stream()
                .map(EIRAmortizationScheduleDto::fromEntity)
                .collect(Collectors.toList());
        return new ResponseEntity<>(dtoList, HttpStatus.CREATED);
    }

    // --- LoanEvent (대출 이벤트) 및 Recalculation (재계산) API ---

    /**
     * 대출 이벤트(중도상환, 조건 변경 등)를 기록하고, 필요한 경우 재계산을 수행합니다.
     * @param requestDto 대출 이벤트 요청 정보
     * @return 재계산 실행 기록 (재계산이 발생한 경우)
     */
    @PostMapping("/events")
    public ResponseEntity<RecalculationRunDto> processLoanEvent(@Valid @RequestBody LoanEventRequestDto requestDto) {
        // LoanEvent를 먼저 기록 (여기서는 서비스에서 처리한다고 가정)
        // RecalculationRun은 재계산이 필요한 이벤트에 대해서만 반환
        RecalculationRun run = loanService.recalculateLoan(
                requestDto.getLoanId(),
                requestDto.getEventDate(),
                RecalculationRun.RecalculationReason.valueOf(requestDto.getEventType().name()), // 이벤트 유형을 재계산 사유로 매핑
                requestDto.getUser(),
                requestDto.getNewPrincipal(),
                requestDto.getNewMaturityDate()
        );
        return new ResponseEntity<>(RecalculationRunDto.fromEntity(run), HttpStatus.CREATED);
    }

    // --- DoD 시나리오 재현 API ---

    /**
     * 대출회계 DoD 시나리오 (실행 -> 이연 -> 3개월 상각 -> 중도상환 재계산)를 재현합니다.
     * @param requestDto 대출 ID 및 사용자 정보
     * @return 최종 재계산 실행 기록 정보
     */
    @PostMapping("/dod-scenario")
    public ResponseEntity<RecalculationRunDto> reproduceDoDScenario(@Valid @RequestBody DoDScenarioRequestDto requestDto) {
        RecalculationRun run = loanService.reproduceDoDScenario(requestDto.getLoanId(), requestDto.getUser());
        return ResponseEntity.ok(RecalculationRunDto.fromEntity(run));
    }
}
