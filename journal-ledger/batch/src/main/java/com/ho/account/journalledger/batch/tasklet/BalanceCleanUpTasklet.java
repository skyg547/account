package com.ho.account.journalledger.batch.tasklet;

import com.ho.account.journalledger.application.service.ledger.BalanceReaggregationService;
import com.ho.account.journalledger.batch.support.BatchDateRangeParameterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * GL/SL 잔액 재집계 전 사전 Clean-up Tasklet.
 *
 * <p>Start Step에서 고정한 기간과 JobInstance owner를 사용합니다. 이 Step이 성공한 뒤의
 * restart에서는 Spring Batch가 Step을 건너뛰므로 저장된 reader checkpoint와 이미 커밋된
 * 출력이 같은 generation에 남습니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceCleanUpTasklet implements Tasklet {

    private final BalanceReaggregationService reaggregationService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        BatchDateRangeParameterUtils.DateRange range =
                BatchDateRangeParameterUtils.resolveFrozenDateRange(contribution.getStepExecution());
        long owner = BatchDateRangeParameterUtils.ownerJobInstanceId(contribution.getStepExecution());

        log.info("[Journal Ledger Batch] Clean-up existing GL/SL balances for re-aggregation. startDate={}, endDate={}",
                range.startDate(), range.endDate());
        reaggregationService.clean(owner, range.startDate(), range.endDate());
        log.info("[Journal Ledger Batch] Clean-up existing GL/SL balances completed. startDate={}, endDate={}",
                range.startDate(), range.endDate());

        return RepeatStatus.FINISHED;
    }
}
