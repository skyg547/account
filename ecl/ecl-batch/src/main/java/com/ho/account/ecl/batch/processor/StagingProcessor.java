package com.ho.account.ecl.batch.processor;

import com.ho.account.ecl.batch.support.BatchParameterUtils;
import com.ho.account.ecl.core.application.pipeline.StagingCalculationPipeline;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * [Step 1] 스테이징 및 기초 PD/LGD 매핑 배치 어댑터.
 *
 * <p>💡 [초보자 가이드]
 * 이 클래스는 Spring Batch가 이해하는 `ItemProcessor` 껍데기입니다.
 * 실제 Stage 판정, PD 산출, 결과 객체 생성은 `ecl-core`의 `StagingCalculationPipeline`이 담당합니다.
 * 이렇게 분리하면 배치 프레임워크가 바뀌어도 IFRS 9 업무 규칙은 core 테스트로 검증할 수 있습니다.
 */
@Component
@StepScope
@RequiredArgsConstructor
public class StagingProcessor implements ItemProcessor<CrAccount, AllowanceEclResult> {

    private final StagingCalculationPipeline stagingCalculationPipeline;

    /** 💡 [초보자 가이드] 배치 실행 시 입력받는 기준 일자입니다. */
    @Value("#{jobParameters['baseDate'] ?: jobParameters['baseDt']}")
    private String baseDateStr;

    @Override
    public AllowanceEclResult process(CrAccount account) {
        LocalDate baseDate = BatchParameterUtils.resolveBaseDate(baseDateStr, null);
        return stagingCalculationPipeline.prepare(account, baseDate);
    }
}