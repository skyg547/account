package com.ho.account.ecl.batch.job.tasklet;

import com.ho.account.ecl.batch.support.BatchParameterUtils;
import com.ho.account.ecl.core.application.service.allowance.AllowanceExposureSyncService;
import com.ho.account.ecl.core.domain.allowance.AllowanceExposureSyncResult;
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
public class AllowanceExposureSyncTasklet implements Tasklet {

    private final AllowanceExposureSyncService syncService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate baseDate = BatchParameterUtils.resolveBaseDate(contribution.getStepExecution());

        AllowanceExposureSyncResult result = syncService.syncFromAllowanceSnapshot(baseDate);
        log.info("[Allowance Exposure Sync] completed. baseDate={}, sourceSnapshots={}, customers={}, accounts={}",
                result.baseDate(),
                result.sourceSnapshotCount(),
                result.customerUpsertCount(),
                result.accountUpsertCount());

        return RepeatStatus.FINISHED;
    }
}
