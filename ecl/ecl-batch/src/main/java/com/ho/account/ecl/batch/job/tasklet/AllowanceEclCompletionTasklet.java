package com.ho.account.ecl.batch.job.tasklet;

import com.ho.account.ecl.batch.support.BatchParameterUtils;
import com.ho.account.ecl.core.application.service.allowance.AllowanceEclCompletionService;
import com.ho.account.ecl.core.domain.allowance.AllowanceEclCompletionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllowanceEclCompletionTasklet implements Tasklet {

    private final AllowanceEclCompletionService completionService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate baseDate = BatchParameterUtils.resolveBaseDate(contribution.getStepExecution());

        AllowanceEclCompletionResult result = completionService.completeCalculatedEclResults(baseDate);
        log.info("[Allowance ECL Completion] completed. baseDate={}, calculatedResults={}, completedResults={}",
                result.baseDate(), result.calculatedResultCount(), result.completedResultCount());

        return RepeatStatus.FINISHED;
    }
}
