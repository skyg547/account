package com.ho.account.journalledger.batch.tasklet;

import com.ho.account.journalledger.application.service.ledger.BalanceReaggregationService;
import com.ho.account.journalledger.batch.support.BatchDateRangeParameterUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/** Freezes input and acquires the persistent owner barrier in the start-step transaction. */
@Component
@RequiredArgsConstructor
public class BalanceReaggregationStartTasklet implements Tasklet {
    private final BalanceReaggregationService reaggregationService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var stepExecution = contribution.getStepExecution();
        var range = BatchDateRangeParameterUtils.freezeDateRange(stepExecution);
        reaggregationService.start(BatchDateRangeParameterUtils.ownerJobInstanceId(stepExecution),
                range.startDate(), range.endDate());
        return RepeatStatus.FINISHED;
    }
}
