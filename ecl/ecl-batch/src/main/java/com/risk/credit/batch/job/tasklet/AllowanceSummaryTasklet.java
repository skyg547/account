package com.risk.credit.batch.job.tasklet;

import com.risk.credit.batch.support.BatchParameterUtils;
import com.risk.credit.core.application.service.allowance.AllowanceSummaryService;
import com.risk.credit.core.domain.allowance.AllowanceSummaryBuildResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class AllowanceSummaryTasklet implements Tasklet {

    private static final String DEFAULT_MODEL_VERSION = "v1";

    private final AllowanceSummaryService allowanceSummaryService;

    @Value("#{jobParameters['runId']}")
    private String runId;

    @Value("#{jobParameters['modelVersion']}")
    private String modelVersion;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate baseDate = BatchParameterUtils.resolveBaseDate(contribution.getStepExecution());
        String resolvedRunId = hasText(runId)
                ? runId.trim()
                : "ECL-" + baseDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        String resolvedModelVersion = hasText(modelVersion) ? modelVersion.trim() : DEFAULT_MODEL_VERSION;

        AllowanceSummaryBuildResult result = allowanceSummaryService.rebuildAllowanceSummary(
                baseDate,
                resolvedRunId,
                resolvedModelVersion);

        log.info("[Allowance Summary] completed. baseDate={}, runId={}, modelVersion={}, sourceResults={}, summaryRows={}",
                result.baseDate(),
                result.runId(),
                result.modelVersion(),
                result.sourceResultCount(),
                result.summaryRowCount());
        return RepeatStatus.FINISHED;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
