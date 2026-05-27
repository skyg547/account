package com.risk.mart.batch.tasklet;

import com.risk.mart.core.application.service.allowance.AllowanceExposureSnapshotService;
import com.risk.mart.core.domain.allowance.AllowanceExposureSnapshotBuildResult;
import com.risk.mart.core.support.BatchParameterUtils;
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
        LocalDate baseDate = BatchParameterUtils.resolveBaseDate(contribution.getStepExecution());

        log.info("[Allowance Exposure Snapshot] rebuild started. baseDate={}", baseDate);
        AllowanceExposureSnapshotBuildResult result = snapshotService.rebuildSnapshot(baseDate);
        log.info("[Allowance Exposure Snapshot] rebuild finished. baseDate={}, sourcePositions={}, snapshotRows={}",
                result.baseDate(), result.sourcePositionCount(), result.snapshotRowCount());

        return RepeatStatus.FINISHED;
    }
}
