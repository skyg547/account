package com.ho.account.ecl.batch.processor;

import com.ho.account.ecl.batch.support.BatchParameterUtils;
import com.ho.account.ecl.core.application.service.calculation.ForwardLookingEclService;
import com.ho.account.ecl.core.application.service.calculation.IrbParameterService;
import com.ho.account.ecl.core.application.service.calculation.LifetimePdService;
import com.ho.account.ecl.core.domain.calculator.IrbRegulatoryParams;
import com.ho.account.ecl.core.domain.result.CrRiskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * [Step 3] 미래전망(Forward-Looking) 기대손실(ECL) 산출 프로세서
 * 
 * 💡 [초보자 가이드]
 * 고객이 부도가 났을 때 '얼마나 떼일 것인가'를 미리 예상하여 장부에 쌓아두는 '충당금(ECL)'을 계산합니다.
 * 특히 현재 상황뿐만 아니라 미래의 경기 상황(호황, 불황 등)을 미리 예측하여 결과에 반영(Forward-Looking)하는 고도화된 단계입니다.
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class EclProcessor implements ItemProcessor<CrRiskResult, CrRiskResult> {

    /** 💡 [초보자 가이드] 평생 동안 부도날 확률의 곡선(Lifetime PD Curve)을 그려주는 서비스입니다. */
    private final LifetimePdService lifetimePdService;
    
    /** 💡 [초보자 가이드] 거시경제 시나리오별로 가중평균된 최종 충당금을 계산해줍니다. */
    private final ForwardLookingEclService forwardLookingEclService;
    
    /** 💡 [초보자 가이드] 규제 파라미터(할인율 등)를 제공하는 서비스입니다. */
    private final IrbParameterService irbParameterService;

    /** 💡 [초보자 가이드] 배치 실행 시 입력받는 '기준 일자'입니다. */
    @Value("#{jobParameters['baseDate'] ?: jobParameters['baseDt']}")
    private String baseDateStr;

    /**
     * 이전 단계 결과를 받아 미래 예측 기반의 기대손실(ECL)을 산출합니다.
     * 
     * @param result 2단계(EAD)가 완료된 대손충당금(IFRS9) 산출 결과 객체
     * @return ECL 산출 결과가 반영된 객체
     */
    @Override
    public CrRiskResult process(CrRiskResult result) {
        // 💡 [로그] 현재 처리 중인 계좌 번호와 상태를 기록하여 추적 가능하게 합니다.
        log.info("🚀 [Step 3] ECL 산출 시작 - 계좌: {}, 기존상태: {}", result.getAccount().getAccountNo(), result.getStatus());
        
        // 💡 [날짜 처리] 배치 파라미터로 전달된 문자열 날짜를 실제 LocalDate 객체로 변환합니다.
        LocalDate baseDate = BatchParameterUtils.resolveBaseDate(baseDateStr, null);
        
        // 💡 [파라미터] 규제 당국이 정한 할인율 등 계산에 필요한 기본 설정값을 가져옵니다.
        IrbRegulatoryParams irbParams = irbParameterService.getParameters();

        // 1. 잔존 만기 산출 (최소 1년)
        // 💡 [만기] 대출이 끝날 때까지 남은 기간을 계산합니다. 최소 1년은 유지되도록 설정합니다.
        double maturityYears = 2.5d;
        if (result.getAccount().getMaturityDate() != null) {
            long remainingDays = ChronoUnit.DAYS.between(baseDate, result.getAccount().getMaturityDate());
            maturityYears = Math.max(remainingDays / 365.0d, 1.0d);
        }

        // 2. Marginal PD 곡선 생성 (전이행렬 활용)
        String resolvedRating = result.getAccount().getInternalRating() != null 
                ? result.getAccount().getInternalRating() 
                : result.getAccount().getCustomer().getInternalRating();
        
        List<BigDecimal> marginalPds = lifetimePdService.generateMarginalPdCurve(
                result.getPd(),
                maturityYears,
                resolvedRating,
                baseDate
        );

        // 3. 가중평균 ECL 산출
        ForwardLookingEclService.FlEclResult flEcl = forwardLookingEclService.calculateWeightedEcl(
                result.getStaging(),
                marginalPds,
                result.getLgd(),
                result.getEadStar(),
                irbParams.getDefaultDiscountRate(),
                baseDate.getYear()
        );

        // 결과 업데이트
        result.setEclBoom(flEcl.getEclBoom());
        result.setEclBase(flEcl.getEclBase());
        result.setEclRecession(flEcl.getEclRecession());
        result.setWeightedEcl(flEcl.getWeightedEcl());
        result.setExpectedLoss(flEcl.getWeightedEcl());

        return result;
    }
}
