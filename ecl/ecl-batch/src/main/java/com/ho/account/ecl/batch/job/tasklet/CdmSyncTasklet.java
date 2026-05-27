package com.ho.account.ecl.batch.job.tasklet;

import com.ho.account.ecl.batch.support.BatchParameterUtils;
import com.ho.account.ecl.core.application.service.integration.CdmSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * [CDM Sync Tasklet] 통합 마트 데이터 동기화 태스크릿
 * 
 * 💡 [초보자를 위한 가이드]
 * 본격적인 리스크 계산 요리를 시작하기 전에, 마트(CDM)라는 공용 창고에서 
 * 오늘 쓸 재료들을 우리 주방(신용결산 대손 엔진 DB)으로 옮겨오는 작업을 수행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CdmSyncTasklet implements Tasklet {

    private final CdmSyncService cdmSyncService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        LocalDate baseDate = BatchParameterUtils.resolveBaseDate(contribution.getStepExecution());
        
        log.info("🚀 [Tasklet] CDM 데이터 동기화 태스크릿 기동 (기준일: {})", baseDate);
        
        cdmSyncService.syncFromCdm(baseDate);
        
        log.info("✅ [Tasklet] CDM 데이터 동기화 완료");
        return RepeatStatus.FINISHED;
    }
}
