package com.ho.account.reporting.adapter.in.web;

import com.ho.account.reporting.application.port.in.GenerateStatementUseCase;
import com.ho.account.reporting.domain.model.FinancialStatement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * [인바운드 어댑터] ReportingController
 * 초보자 가이드: 이 클래스는 안내데스크와 같습니다.
 * 사용자가 웹 브라우저를 통해 보고서 생성을 요청하면 가장 먼저 만나게 되는 곳입니다.
 */
@RestController
@RequestMapping("/api/v1/reporting")
@RequiredArgsConstructor
public class ReportingController {

    // 핵심 로직(UseCase)을 사용하기 위해 인터페이스를 주입받습니다.
    private final GenerateStatementUseCase generateStatementUseCase;

    /**
     * 재무제표 생성 API
     * 예시 URL: POST /api/v1/reporting/generate?type=BALANCE_SHEET&baseDate=2026-03-31T00:00:00
     */
    @PostMapping("/generate")
    public FinancialStatement generateStatement(
        @RequestParam("type") FinancialStatement.StatementType type,
        @RequestParam("baseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baseDate,
        @RequestHeader("X-User-ID") String userId
    ) {
        // 1. 사용자의 요청을 서비스가 이해할 수 있는 명령(Command) 객체로 바꿉니다.
        GenerateStatementUseCase.GenerateCommand command = new GenerateStatementUseCase.GenerateCommand(
            type, 
            baseDate, 
            userId
        );

        // 2. 서비스에 보고서 생성을 요청하고 결과를 받아 사용자에게 돌려줍니다.
        return generateStatementUseCase.generate(command);
    }
}
