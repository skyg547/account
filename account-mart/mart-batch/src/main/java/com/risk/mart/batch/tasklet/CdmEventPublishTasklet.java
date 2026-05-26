package com.risk.mart.batch.tasklet;

import com.risk.mart.core.infrastructure.messaging.CdmEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * [Tasklet] CDM 데이터 준비 완료 알림 발행 태스크렛
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CdmEventPublishTasklet implements Tasklet {

    private final CdmEventPublisher eventPublisher;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        String baseDateStr = (String) chunkContext.getStepContext().getJobParameters().get("baseDate");
        LocalDate baseDate = LocalDate.parse(baseDateStr);
        
        log.info("📢 [Batch] CDM 데이터 적재 완료 감지. 이벤트를 발행합니다. (기준일: {})", baseDate);
        
        // 이벤트 발행 (Trace ID는 현재 배치 맥락에서 추출하거나 신규 생성)
        eventPublisher.publishDataReady(baseDate, "BATCH-" + System.currentTimeMillis());

        return RepeatStatus.FINISHED;
    }
}
