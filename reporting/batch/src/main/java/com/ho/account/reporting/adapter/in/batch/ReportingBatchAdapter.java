package com.ho.account.reporting.adapter.in.batch;

import com.ho.account.reporting.application.port.in.GenerateStatementUseCase;
import com.ho.account.reporting.domain.model.FinancialStatement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * [인바운드 어댑터 - 배치] ReportingBatchAdapter
 * 초보자 가이드: 이 클래스는 야간 근무자와 같습니다.
 * 사람이 직접 요청하지 않아도, 정해진 시간(예: 매달 말일 새벽)이 되면 자동으로 대량의 보고서를 미리 만들어둡니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportingBatchAdapter {

    private final GenerateStatementUseCase generateStatementUseCase;

    /**
     * 월말 보고서 자동 생성 배치 작업 시뮬레이션
     * 초보자 가이드: 테스트나 로컬 학습에서는 연/월만 넣어 월말 기준일을 쉽게 만들 수 있습니다.
     * 실제 운영 실행은 {@link ReportingStatementBatchConfig}의 Spring Batch Job/Step이
     * JobParameter(baseDate/requester)를 검증한 뒤 이 어댑터를 호출합니다.
     */
    public void runMonthlyClosingBatch(int year, int month) {
        // 보고서 생성 기준일 설정 (해당 월의 말일)
        LocalDate baseDate = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        runStatementGenerationBatch(baseDate, "SYSTEM_BATCH");
    }

    /**
     * Spring Batch Step이 호출하는 실제 인바운드 어댑터 메서드입니다.
     * 초보자 가이드: 배치 모듈은 언제/어떤 파라미터로 실행할지만 담당하고,
     * 재무제표 생성 규칙은 core의 {@link GenerateStatementUseCase}가 처리합니다.
     */
    public void runStatementGenerationBatch(LocalDate baseDate, String requesterId) {
        Objects.requireNonNull(baseDate, "baseDate must not be null");
        if (requesterId == null || requesterId.isBlank()) {
            throw new IllegalArgumentException("requesterId is required.");
        }

        LocalDateTime baseDateTime = baseDate.atStartOfDay();
        log.info("Starting Monthly Closing Batch for baseDate={}, requester={}", baseDate, requesterId);

        // 1. 재무상태표(BS) 생성 요청
        GenerateStatementUseCase.GenerateCommand bsCommand = new GenerateStatementUseCase.GenerateCommand(
            FinancialStatement.StatementType.BALANCE_SHEET,
            baseDateTime,
            requesterId
        );
        generateStatementUseCase.generate(bsCommand);
        log.info("Balance Sheet generated for batch.");

        // 2. 손익계산서(PL) 생성 요청
        GenerateStatementUseCase.GenerateCommand plCommand = new GenerateStatementUseCase.GenerateCommand(
            FinancialStatement.StatementType.INCOME_STATEMENT,
            baseDateTime,
            requesterId
        );
        generateStatementUseCase.generate(plCommand);
        log.info("Income Statement generated for batch.");
        
        log.info("Monthly Closing Batch completed.");
    }
}
