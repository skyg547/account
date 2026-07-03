package com.ho.account.ecl.batch.processor;

import com.ho.account.ecl.core.application.pipeline.EadCrmCalculationPipeline;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * [Step 2] CRM(담보배분) 및 EAD(부도시노출액) 산출 배치 어댑터.
 *
 * <p>💡 [초보자 가이드]
 * EAD, CRM, LGD 업무 판단은 `ecl-core`의 `EadCrmCalculationPipeline`에 있다.
 * 이 adapter는 chunk로 읽힌 결과 객체를 core pipeline에 넘기고, 산출된 객체를 writer로 돌려준다.
 */
@Component
@StepScope
@RequiredArgsConstructor
public class EadCrmProcessor implements ItemProcessor<AllowanceEclResult, AllowanceEclResult> {

    private final EadCrmCalculationPipeline eadCrmCalculationPipeline;

    @Override
    public AllowanceEclResult process(AllowanceEclResult result) {
        return eadCrmCalculationPipeline.apply(result);
    }
}