package com.ho.account.reporting.adapter.in.batch;

import com.ho.account.reporting.application.port.in.GenerateStatementUseCase;
import com.ho.account.reporting.domain.model.FinancialStatement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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
     * 초보자 가이드: 실제로는 Spring Batch의 Job/Step 구조를 사용하여 구현됩니다.
     */
    public void runMonthlyClosingBatch(int year, int month) {
        // @todo 운영 대량 실행은 Spring Batch Job/Step과 JobParameter(baseDate/requester)로 전환해 재실행 가능성을 보장한다.
        log.info("Starting Monthly Closing Batch for {}-{}", year, month);

        // 보고서 생성 기준일 설정 (해당 월의 말일)
        LocalDateTime baseDate = LocalDateTime.of(year, month, 1, 0, 0).plusMonths(1).minusDays(1);

        // 1. 재무상태표(BS) 생성 요청
        GenerateStatementUseCase.GenerateCommand bsCommand = new GenerateStatementUseCase.GenerateCommand(
            FinancialStatement.StatementType.BALANCE_SHEET,
            baseDate,
            "SYSTEM_BATCH"
        );
        generateStatementUseCase.generate(bsCommand);
        log.info("Balance Sheet generated for batch.");

        // 2. 손익계산서(PL) 생성 요청
        GenerateStatementUseCase.GenerateCommand plCommand = new GenerateStatementUseCase.GenerateCommand(
            FinancialStatement.StatementType.INCOME_STATEMENT,
            baseDate,
            "SYSTEM_BATCH"
        );
        generateStatementUseCase.generate(plCommand);
        log.info("Income Statement generated for batch.");
        
        log.info("Monthly Closing Batch completed.");
    }
}
