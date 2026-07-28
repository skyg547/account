package com.ho.account.loan.web;

import com.ho.account.loan.application.port.in.LoanUseCase;
import com.ho.account.loan.domain.DeferredItemType;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanDisbursal;
import com.ho.account.loan.domain.LoanEvent;
import com.ho.account.loan.domain.RecalculationRun;
import com.ho.account.loan.dto.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * [헥사고날 아키텍처 - 인바운드 어댑터 (Inbound Web Adapter)]
 * 대출 회계 (Loan Accounting) 관련 REST API를 제공하는 컨트롤러.
 * 대출 계약 관리, 실행, 이연 부대손익, EIR 스케줄 및 재계산 등을 처리합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 대출 모듈이 외부(예: 웹 브라우저, 모바일 앱, 혹은 사내 타 시스템)와 소통하기 위해 
 * 열어둔 '접수처(Web Adapter)'입니다. 외부에서 들어온 HTTP 요청(JSON 등)을
 * 애플리케이션 서비스(LoanService)가 이해할 수 있는 형태의 객체(Dto)로 변환하여 전달하고,
 * 서비스의 처리 결과를 다시 외부 포맷(JSON)으로 응답합니다.
 * 이를 통해 핵심 비즈니스 로직은 HTTP, REST 같은 웹 기술에 종속되지 않고 순수하게 유지될 수 있습니다.
 *
 * <p>@todo 현재 요청 DTO의 user 값을 감사 주체로 신뢰합니다. 완료 조건은 Gateway가 검증한 principal을
 * 보안 컨텍스트에서 추출하고, 요청 본문의 user 필드를 제거하며, 위조 actor가 저장·전표 lineage에
 * 들어가지 않는 API 통합 테스트를 추가하는 것입니다.</p>
 */
@RestController
@RequestMapping("/api/loan")
public class LoanController {

    private final LoanUseCase loanUseCase;

    @Autowired
    public LoanController(LoanUseCase loanUseCase) {
        this.loanUseCase = loanUseCase;
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
        Loan createdLoan = loanUseCase.createLoan(loan);
        return new ResponseEntity<>(LoanDto.fromEntity(createdLoan), HttpStatus.CREATED);
    }

    /**
     * ID로 대출을 조회합니다.
     * @param id 대출 ID
     * @return 조회된 대출 정보
     */
    @GetMapping("/loans/{id}")
    public ResponseEntity<LoanDto> getLoanById(@PathVariable("id") Long id) {
        Loan loan = loanUseCase.findLoanById(id);
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
        LoanDisbursal disbursal = loanUseCase.disburseLoan(
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
        DeferredItemType createdItemType = loanUseCase.createDeferredItemType(itemType);
        return new ResponseEntity<>(DeferredItemTypeDto.fromEntity(createdItemType), HttpStatus.CREATED);
    }

    /**
     * 코드로 이연 항목 유형을 조회합니다.
     * @param code 이연 항목 유형 코드
     * @return 조회된 이연 항목 유형 정보
     */
    @GetMapping("/deferred-item-types/by-code/{code}")
    public ResponseEntity<DeferredItemTypeDto> getDeferredItemTypeByCode(@PathVariable("code") String code) {
        DeferredItemType itemType = loanUseCase.findDeferredItemTypeByCode(code);
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
        com.ho.account.loan.domain.DeferredItem deferredItem = loanUseCase.createDeferredItem(
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
        List<com.ho.account.loan.domain.EIRAmortizationSchedule> schedule = loanUseCase.generateAmortizationSchedule(
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
    public ResponseEntity<LoanEventResultDto> processLoanEvent(@Valid @RequestBody LoanEventRequestDto requestDto) {
        LoanUseCase.LoanEventResult result = loanUseCase.processLoanEvent(
                requestDto.getLoanId(),
                requestDto.getEventType(),
                requestDto.getEventDate(),
                requestDto.getDescription(),
                requestDto.getUser(),
                Optional.ofNullable(requestDto.getNewPrincipal()),
                Optional.ofNullable(requestDto.getNewMaturityDate()));
        return new ResponseEntity<>(LoanEventResultDto.from(result), HttpStatus.CREATED);
    }

    // --- DoD 시나리오 재현 API ---

    /**
     * 대출회계 DoD 시나리오 (실행 -> 이연 -> 3개월 상각 -> 중도상환 재계산)를 재현합니다.
     * @param requestDto 대출 ID 및 사용자 정보
     * @return 최종 재계산 실행 기록 정보
     */
    @PostMapping("/dod-scenario")
    public ResponseEntity<RecalculationRunDto> reproduceDoDScenario(@Valid @RequestBody DoDScenarioRequestDto requestDto) {
        RecalculationRun run = loanUseCase.reproduceDoDScenario(requestDto.getLoanId(), requestDto.getUser());
        return ResponseEntity.ok(RecalculationRunDto.fromEntity(run));
    }
}
