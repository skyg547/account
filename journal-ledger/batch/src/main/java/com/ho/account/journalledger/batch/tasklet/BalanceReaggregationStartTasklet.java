package com.ho.account.journalledger.batch.tasklet;

import com.ho.account.journalledger.application.service.ledger.BalanceReaggregationService;
import com.ho.account.journalledger.batch.support.BatchDateRangeParameterUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Resolves and freezes input, then acquires the persistent owner barrier in the start-step transaction. */
@Component
@RequiredArgsConstructor
public class BalanceReaggregationStartTasklet implements Tasklet {
    private final BalanceReaggregationService reaggregationService;
    private final Clock clock;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var stepExecution = contribution.getStepExecution();
        // Validate every supplied alias before start() takes writer locks or changes the barrier.
        var requestedRange = BatchDateRangeParameterUtils.resolveDateRange(stepExecution, clock);
        var effectiveRange = reaggregationService.start(
                BatchDateRangeParameterUtils.ownerJobInstanceId(stepExecution),
                requestedRange.startDate(), requestedRange.endDate());
        // Freeze the same expanded range persisted in V15. Cleanup, reader, writer and finalize
        // all resolve this execution context on restart instead of recomputing a moving horizon.
        BatchDateRangeParameterUtils.freezeDateRange(stepExecution,
                new BatchDateRangeParameterUtils.DateRange(
                        effectiveRange.startDate(), effectiveRange.endDate()));
        return RepeatStatus.FINISHED;
    }
}
