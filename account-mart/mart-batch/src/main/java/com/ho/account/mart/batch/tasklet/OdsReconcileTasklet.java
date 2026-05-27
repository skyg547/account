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
 * [마트 태스크릿] ODS 데이터 대사 및 품질 검증 실행
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 이 태스크릿은 두 가지 중요한 일을 합니다:
 * 1. 데이터 품질(DQ) 체크: "이름이 비어있는 고객이 있는가 ", "금액이 말도 안 되게 큰가 " 등을 검사합니다.
 * 2. 원천 시스템 대사: "원천 시스템 파일의 합계"와 "우리 DB에 들어온 합계"가 일치하는지 최종 확인합니다.
 * 이 과정이 통과되어야 리스크 핵심 산출(Credit, Interest Rate)을 시작할 수 있습니다.
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
        LocalDate baseDate = BatchParameterUtils.resolveBaseDate(contribution.getStepExecution());

        log.info("🛡️ [태스크릿 실행] 원천 GL/SL 대사를 시작합니다. (기준일: {})", baseDate);
        reconciliationService.reconcileGlVsSl(baseDate);

        log.info("🛡️ [태스크릿 완료] 원천 GL/SL 대사가 성공적으로 끝났습니다.");
        return RepeatStatus.FINISHED;
    }
}
