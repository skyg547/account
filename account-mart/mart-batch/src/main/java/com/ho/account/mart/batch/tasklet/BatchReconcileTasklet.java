package com.ho.account.mart.batch.tasklet;

import com.ho.account.mart.core.domain.ods.audit.service.OdsReconciliationService;
import com.ho.account.mart.core.support.BatchParameterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * [마트 태스크릿] 회계 대사(Reconciliation) 실행 태스크릿
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 대사(Reconciliation)란 마치 가계부를 맞추는 것과 같습니다.
 * "은행 전체 장부"(총계정원장, GL)와 우리가 리스크 측정을 위해 만든 "상세 내역"(리스크 데이터 마트, SL)을
 * 비교하여 모든 수치가 정확히 일치하는지 확인하는 과정입니다.
 * 만약 원장에는 100억인데 마트에는 99억뿐이라면, 데이터 누락이나 가공 오류가 있음을 의미하므로 즉시 조치가 필요합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchReconcileTasklet implements Tasklet {

    private final OdsReconciliationService reconciliationService;

    @Override
    @org.springframework.lang.Nullable
    public RepeatStatus execute(@org.springframework.lang.NonNull StepContribution contribution, 
                              @org.springframework.lang.NonNull ChunkContext chunkContext) throws Exception {
        LocalDate baseDate = BatchParameterUtils.resolveBaseDate(contribution.getStepExecution());
        log.info("🛡️ [데이터 대사] 기준일자({})에 대한 회계 정합성 검증을 시작합니다.", baseDate);

        reconciliationService.reconcileMartVsGl(baseDate);

        log.info("🏁 [데이터 대사] 정합성 검증 단계가 종료되었습니다.");
        return RepeatStatus.FINISHED;
    }
}
