package com.ho.account.ecl.batch.processor;

import com.ho.account.ecl.batch.support.BatchParameterUtils;
import com.ho.account.ecl.core.application.pipeline.ForwardLookingEclCalculationPipeline;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * [Step 3] 미래전망(Forward-Looking) 기대손실(ECL) 산출 배치 어댑터.
 *
 * <p>💡 [초보자 가이드]
 * 배치는 기준일 파라미터를 해석하고 chunk 흐름을 연결한다.
 * 실제 잔존 만기, Lifetime PD, 거시경제 시나리오 가중 ECL 산출은 core pipeline이 맡는다.
 */
@Component
@StepScope
@RequiredArgsConstructor
public class EclProcessor implements ItemProcessor<AllowanceEclResult, AllowanceEclResult> {

    private final ForwardLookingEclCalculationPipeline forwardLookingEclCalculationPipeline;

    /** 💡 [초보자 가이드] 배치 실행 시 입력받는 기준 일자입니다. */
    @Value("#{jobParameters['baseDate'] ?: jobParameters['baseDt']}")
    private String baseDateStr;

    @Override
    public AllowanceEclResult process(AllowanceEclResult result) {
        LocalDate baseDate = BatchParameterUtils.resolveBaseDate(baseDateStr, null);
        return forwardLookingEclCalculationPipeline.apply(result, baseDate);
    }
}