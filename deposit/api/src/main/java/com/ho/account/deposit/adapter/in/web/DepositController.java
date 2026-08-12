package com.ho.account.deposit.adapter.in.web;

import com.ho.account.deposit.application.port.in.DepositQueryUseCase;
import com.ho.account.deposit.application.port.in.DepositTransactionUseCase;
import com.ho.account.deposit.application.port.in.OpenAccountUseCase;
import com.ho.account.deposit.domain.DepositAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * [헥사고날 아키텍처 - 인바운드 웹 어댑터 (Inbound REST Controller Adapter)]
 *
 * 🐣 [초보자를 위한 설명 및 아키텍처적 이점]
 * 이 클래스는 외부(클라이언트, 웹 브라우저, 타 마이크로서비스 등)에서 들어오는 HTTP REST 요청을 받아 처리하는 웹 어댑터입니다.
 *
 * 1. Hexagonal Architecture (Port/Adapter) 관심사 분리:
 *    - 웹 기술에 종속적인 데이터 구조(JSON DTO, HTTP 요청/응답 코드 등)를 애플리케이션 유즈케이스 포트 규격으로 변환합니다.
 *    - 도메인 로직을 직접 수행하지 않으며, DIP(의존성 역전 원칙)에 따라 인바운드 유즈케이스 인터페이스 포트
 *      ({@link OpenAccountUseCase}, {@link DepositTransactionUseCase}, {@link DepositQueryUseCase})만을 의존합니다.
 *
 * 2. Spring Container ApplicationContext 정합성 및 DI 극대화:
 *    - @RestController 및 @RequiredArgsConstructor를 결합하여 생성자 주입(Constructor Injection)을 기반으로
 *      의존성 빈들을 안전하게 주입받습니다.
 */
@RestController
@RequestMapping("/api/deposits")
@RequiredArgsConstructor
public class DepositController {

    private final OpenAccountUseCase openAccountUseCase;
    private final DepositTransactionUseCase depositTransactionUseCase;
    private final DepositQueryUseCase depositQueryUseCase;

    /**
     * 예금 계좌 신규 개설 REST API
     */
    @PostMapping("/accounts")
    public ResponseEntity<String> openAccount(@RequestBody OpenAccountRequest request) {
        String accountNumber = openAccountUseCase.openAccount(new OpenAccountUseCase.OpenAccountCommand(
                request.customerCode(),
                request.productCode(),
                request.currencyCode(),
                request.initialDeposit(),
                request.interestRate()
        ));
        return ResponseEntity.ok(accountNumber);
    }

    /**
     * 예금 계좌 입금 REST API
     */
    @PostMapping("/accounts/{accountNumber}/deposit")
    public ResponseEntity<Void> deposit(@PathVariable String accountNumber, @RequestBody TransactionRequest request) {
        depositTransactionUseCase.deposit(accountNumber, request.amount());
        return ResponseEntity.ok().build();
    }

    /**
     * 예금 계좌 출금 REST API
     */
    @PostMapping("/accounts/{accountNumber}/withdraw")
    public ResponseEntity<Void> withdraw(@PathVariable String accountNumber, @RequestBody TransactionRequest request) {
        depositTransactionUseCase.withdraw(accountNumber, request.amount());
        return ResponseEntity.ok().build();
    }

    /**
     * 예금 계좌 단건 상세 조회 REST API
     */
    @GetMapping("/accounts/{accountNumber}")
    public ResponseEntity<DepositAccountResponse> getAccount(@PathVariable String accountNumber) {
        return depositQueryUseCase.findByAccountNumber(accountNumber)
                .map(account -> ResponseEntity.ok(DepositAccountResponse.from(account)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record OpenAccountRequest(
            String customerCode,
            String productCode,
            String currencyCode,
            BigDecimal initialDeposit,
            BigDecimal interestRate
    ) {}

    public record TransactionRequest(
            BigDecimal amount
    ) {}

    public record DepositAccountResponse(
            Long id,
            String accountNumber,
            String customerCode,
            String productCode,
            String currencyCode,
            BigDecimal balance,
            BigDecimal interestRate,
            String status
    ) {
        public static DepositAccountResponse from(DepositAccount account) {
            return new DepositAccountResponse(
                    account.getId(),
                    account.getAccountNumber(),
                    account.getCustomerCode(),
                    account.getProductCode(),
                    account.getCurrencyCode(),
                    account.getBalance(),
                    account.getInterestRate(),
                    account.getStatus() != null ? account.getStatus().name() : null
            );
        }
    }
}
