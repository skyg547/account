package com.ho.account.ecl.batch.support;

import com.ho.account.ecl.core.application.service.allowance.AllowanceCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * [Support] 배치 실행 전 도메인 기저 데이터 캐시를 워밍업하는 공통 Tasklet.
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 대규모 배치 처리(병렬 워커)가 시작되기 전에, 모든 계산에 공통적으로 쓰이는 데이터
 * (부도율, 상품 설정, IFRS 9 모델 파라미터 등)를 DB에서 읽어 메모리(Cache)에 미리 담아두는 역할을 합니다.
 * 이렇게 하면 수백만 번의 DB 조회를 한 번으로 줄일 수 있어 속도가 비약적으로 빨라집니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmingTasklet implements Tasklet {

    private final AllowanceCalculationService allowanceCalculationService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // 1. 배치 파라미터에서 기준일자(baseDate) 추출
        LocalDate baseDate = BatchParameterUtils.resolveBaseDate(contribution.getStepExecution());
        
        log.info("🔥 [캐시 워밍업] 배치 산출을 위한 기저 데이터 로딩을 시작합니다. (기준일: {})", baseDate);
        
        // 2. 핵심 서비스의 캐시 갱신 메서드 호출 
        // (내부적으로 PD, CCF, 모델 파라미터, 전이행렬, 거시경제 시나리오 등을 로드함)
        allowanceCalculationService.refreshAllCaches(baseDate);

        log.info("✅ [캐시 워밍업] 모든 도메인 캐시가 성공적으로 로드되었습니다.");
        
        return RepeatStatus.FINISHED;
    }
}
