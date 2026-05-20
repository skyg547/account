package com.ho.account.deposit.adapter.in.web;

import com.ho.account.deposit.application.port.in.OpenAccountUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

/**
 * [헥사고날 아키텍처 - 인바운드 어댑터 (Inbound Web Adapter)]
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 외부(인터넷, 웹 브라우저, 다른 시스템 등)에서 들어오는 HTTP 요청(REST API)을 받아주는 '접수처'입니다.
 * 사용자가 "계좌를 만들어주세요!"라고 요청을 보내면, 이 어댑터가 요청 데이터(JSON)를 받아서 
 * 애플리케이션 내부에 정의된 '유즈케이스(UseCase)' 규격에 맞게 변환한 뒤 서비스를 호출합니다.
 * 도메인(핵심 로직)은 웹이라는 기술에 대해 전혀 모른 채 보호받을 수 있습니다.
 */
@RestController
@RequestMapping("/api/deposits")
@RequiredArgsConstructor
public class DepositController {

    private final OpenAccountUseCase openAccountUseCase;

    @PostMapping("/accounts")
    public String openAccount(@RequestBody OpenAccountRequest request) {
        // HTTP 요청 DTO를 애플리케이션 계층이 이해할 수 있는 Command 객체로 변환하여 전달
        return openAccountUseCase.openAccount(new OpenAccountUseCase.OpenAccountCommand(
            request.customerCode(),
            request.productCode(),
            request.currencyCode(),
            request.initialDeposit(),
            request.interestRate()
        ));
    }

    public record OpenAccountRequest(
        String customerCode,
        String productCode,
        String currencyCode,
        BigDecimal initialDeposit,
        BigDecimal interestRate
    ) {}
}

