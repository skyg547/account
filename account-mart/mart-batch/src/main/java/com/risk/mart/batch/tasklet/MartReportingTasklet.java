package com.risk.mart.batch.tasklet;

import com.risk.mart.core.domain.mart.service.MartReportingService;
import com.risk.mart.core.support.BatchParameterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * [Mart] 리스크 산출용 마트 집계 및 리포팅 데이터 생성
 * 💡 [초보자를 위한 가이드]
 * 포지션 데이터가 모두 적재된 후, 전사 리스크 현황을 한눈에 볼 수 있도록
 * 통계 데이터를 미리 계산하여 요약 테이블(RDM)에 저장하는 작업입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MartReportingTasklet implements Tasklet {

    private final MartReportingService martReportingService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info(">> [Step 4] 전사 리스크 리포팅(RDM) 집계 및 생성 시작...");

        LocalDate baseDate = BatchParameterUtils.resolveBaseDate(contribution.getStepExecution());
        martReportingService.generateRiskReporting(baseDate);

        return RepeatStatus.FINISHED;
    }
}
