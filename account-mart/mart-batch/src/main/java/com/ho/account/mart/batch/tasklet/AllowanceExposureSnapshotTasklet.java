package com.ho.account.mart.batch.tasklet;

import com.ho.account.mart.core.application.service.allowance.AllowanceExposureSnapshotService;
import com.ho.account.mart.core.domain.allowance.AllowanceExposureSnapshotBuildResult;
import com.ho.account.mart.batch.support.BatchStepParameterUtils;
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
public class AllowanceExposureSnapshotTasklet implements Tasklet {

    private final AllowanceExposureSnapshotService snapshotService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate baseDate = BatchStepParameterUtils.resolveBaseDate(contribution.getStepExecution());

        log.info("[Allowance Exposure Snapshot] rebuild started. baseDate={}", baseDate);
        AllowanceExposureSnapshotBuildResult result = snapshotService.rebuildSnapshot(baseDate);
        log.info("[Allowance Exposure Snapshot] rebuild finished. baseDate={}, sourcePositions={}, snapshotRows={}",
                result.baseDate(), result.sourcePositionCount(), result.snapshotRowCount());

        return RepeatStatus.FINISHED;
    }
}
