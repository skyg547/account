package com.ho.account.journalledger.batch.tasklet;

import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.batch.support.BatchDateRangeParameterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * GL/SL 잔액 재집계 Tasklet.
 *
 * <p>초보자 설명: 과거 전표가 수정되거나 누락 전표가 뒤늦게 전기되면,
 * 해당 기간의 잔액을 지우고 POSTED 전표 기준으로 다시 계산해야 한다.
 * batch는 기간 파라미터를 읽고 core의 LedgerService를 호출하는 오케스트레이션만 담당한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceReaggregationTasklet implements Tasklet {

    private final LedgerService ledgerService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        BatchDateRangeParameterUtils.DateRange range =
                BatchDateRangeParameterUtils.resolveDateRange(contribution.getStepExecution());

        log.info("[Journal Ledger Batch] GL/SL balance re-aggregation started. startDate={}, endDate={}",
                range.startDate(), range.endDate());
        ledgerService.reaggregateLedgerBalancesForPeriod(range.startDate(), range.endDate());
        log.info("[Journal Ledger Batch] GL/SL balance re-aggregation completed. startDate={}, endDate={}",
                range.startDate(), range.endDate());
        return RepeatStatus.FINISHED;
    }
}