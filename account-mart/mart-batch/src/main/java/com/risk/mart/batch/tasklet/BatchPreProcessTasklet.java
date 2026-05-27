package com.risk.mart.batch.tasklet;

import com.risk.mart.core.application.port.out.IntegratedRiskPositionRepository;
import com.risk.mart.core.support.BatchParameterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * [배치 태스크릿] 배치 전처리 (멱등성 보장)
 *
 * 💡 [초보자를 위한 개념 설명]
 * - 태스크릿(Tasklet): 배치 Step 내에서 단순한 로직을 '한 번에' 실행할 때 사용하는 방식입니다.
 * - 멱등성(Idempotency): 같은 작업을 여러 번 실행해도 결과가 항상 같아야 한다는 원칙입니다.
 * 배치가 중간에 실패해서 다시 돌렸을 때, 이미 생성된 데이터를 먼저 지워줌으로써 중복 적재를 방지합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchPreProcessTasklet implements Tasklet {

    private final IntegratedRiskPositionRepository martRepository;

    @Override
    @org.springframework.lang.Nullable
    public RepeatStatus execute(@org.springframework.lang.NonNull StepContribution contribution, 
                              @org.springframework.lang.NonNull ChunkContext chunkContext) throws Exception {
        java.time.LocalDate baseDate = BatchParameterUtils.resolveBaseDate(contribution.getStepExecution());

        log.info("🛡️ [배치 전처리] 기준일자({})의 기존 마트 데이터를 정리합니다.", baseDate);

        martRepository.deleteByBaseDt(baseDate);

        log.info("🛡️ [배치 전처리] 데이터 정리가 완료되었습니다. (기준일: {})", baseDate);
        return RepeatStatus.FINISHED;
    }
}
