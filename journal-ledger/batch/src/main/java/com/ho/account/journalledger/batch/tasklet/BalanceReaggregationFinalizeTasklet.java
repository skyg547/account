package com.ho.account.journalledger.batch.tasklet;

import com.ho.account.journalledger.application.service.ledger.BalanceReaggregationService;
import com.ho.account.journalledger.batch.support.BatchDateRangeParameterUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/** Publishes only after exact source/balance reconciliation succeeds in the release transaction. */
@Component
@RequiredArgsConstructor
public class BalanceReaggregationFinalizeTasklet implements Tasklet {
    private final BalanceReaggregationService reaggregationService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var stepExecution = contribution.getStepExecution();
        var range = BatchDateRangeParameterUtils.resolveFrozenDateRange(stepExecution);
        reaggregationService.reconcileAndRelease(BatchDateRangeParameterUtils.ownerJobInstanceId(stepExecution),
                range.startDate(), range.endDate());
        return RepeatStatus.FINISHED;
    }
}
