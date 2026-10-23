package com.ho.account.mart.batch.tasklet;

import com.ho.account.mart.core.domain.ods.audit.service.OdsReconciliationService;
import com.ho.account.mart.batch.support.BatchStepParameterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * [마트 태스크릿] 기준일의 계정·통화별 GL/SL 잔액 대사를 실행한다.
 * core 서비스가 대사 이력을 커밋한 뒤 불일치 건수를 돌려주면 Step을 실패시켜
 * 뒤따르는 CDM 적재, snapshot 생성, 준비 완료 이벤트를 막는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OdsReconcileTasklet implements Tasklet {

    private final OdsReconciliationService reconciliationService;

    @Override
    @org.springframework.lang.Nullable
    public RepeatStatus execute(@org.springframework.lang.NonNull StepContribution contribution, 
                              @org.springframework.lang.NonNull ChunkContext chunkContext) throws Exception {
        LocalDate baseDate = BatchStepParameterUtils.resolveBaseDate(contribution.getStepExecution());

        log.info("🛡️ [태스크릿 실행] 원천 GL/SL 대사를 시작합니다. (기준일: {})", baseDate);
        int mismatchCount = reconciliationService.reconcileGlVsSl(baseDate);
        if (mismatchCount > 0) {
            // 서비스의 독립 트랜잭션이 이력을 커밋한 다음 Step을 실패시킨다.
            throw new IllegalStateException("GL/SL 대사 불일치: 기준일=" + baseDate + ", 건수=" + mismatchCount);
        }

        log.info("🛡️ [태스크릿 완료] 원천 GL/SL 대사가 성공적으로 끝났습니다.");
        return RepeatStatus.FINISHED;
    }
}
