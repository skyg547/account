package com.ho.account.ecl.batch.processor;

import com.ho.account.ecl.core.application.service.calculation.CcfCalculationService;
import com.ho.account.ecl.core.application.service.calculation.EadCrmCalculationService;
import com.ho.account.ecl.core.application.service.calculation.IrbParameterService;
import com.ho.account.ecl.core.application.service.calculation.LgdCalculationService;
import com.ho.account.ecl.core.domain.calculator.IrbRegulatoryParams;
import com.ho.account.ecl.core.domain.result.CrRiskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * [Step 2] CRM(담보배분) 및 EAD(부도시노출액) 산출 프로세서
 * 
 * 💡 [초보자 가이드]
 * 부도가 났을 때 실제로 떼일 돈이 얼마인지(EAD)를 계산하는 단계입니다.
 * 특히 고객이 제공한 담보(아파트, 상가 등)를 고려하여, 담보가 있는 만큼은 떼일 염려가 적으므로 그만큼을 '차감(CRM)'해주는 중요한 로직을 수행합니다.
 * 
 * 용어 설명:
 * - EAD (Exposure at Default): 부도 시 노출액. 고객이 부도났을 때 은행이 회수해야 할 총 금액입니다.
 * - CRM (Credit Risk Mitigation): 신용위험 경감. 담보 등을 통해 은행이 떼일 위험을 줄이는 활동입니다.
 * - LGD (Loss Given Default): 부도 시 손실률. 부도 시 담보를 처분하고도 회수하지 못해 발생하는 손실 비율입니다.
 * - RWA (Risk Weighted Assets): 위험가중자산. 자산의 위험도에 따라 산출된 자산 가치로, 은행의 건전성 지표가 됩니다.
 * - ECL (Expected Credit Loss): 예상 손실. 미래에 발생할 것으로 예상되는 신용 손실액입니다.
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class EadCrmProcessor implements ItemProcessor<CrRiskResult, CrRiskResult> {

    /** 💡 [초보자 가이드] CCF(신용환산계수)를 계산하는 서비스입니다. */
    private final CcfCalculationService ccfCalculationService;
    
    /** 💡 [초보자 가이드] 실제 담보를 배분하고 최종 노출액을 계산하는 핵심 서비스입니다. */
    private final EadCrmCalculationService eadCrmCalculationService;
    
    /** 💡 [초보자 가이드] 최종 확정된 LGD 수치를 결정해주는 서비스입니다. */
    private final LgdCalculationService lgdCalculationService;
    
    /** 💡 [초보자 가이드] 규제 파라미터(CCF 등)를 로드하는 서비스입니다. */
    private final IrbParameterService irbParameterService;

    /** 💡 [초보자 가이드] 배치 실행 시 입력받는 '기준 일자'입니다. */
    @Value("#{jobParameters['baseDate'] ?: jobParameters['baseDt']}")
    private String baseDateStr;

    /**
     * 이전 단계에서 생성된 결과 객체를 받아 CRM과 EAD를 계산하여 업데이트합니다.
     * 
     * @param result 1단계(스테이징)가 완료된 대손충당금(IFRS9) 산출 결과 객체
     * @return EAD 및 CRM 산출 결과가 반영된 객체
     */
    @Override
    public CrRiskResult process(CrRiskResult result) {
        IrbRegulatoryParams irbParams = irbParameterService.getParameters();

        log.debug("⚡ [Phase 2] 리스크 요소 산출 시작 - 계좌: {}", result.getAccount().getAccountNo());

        // 🟢 Phase 2-1. CCF (Credit Conversion Factor) 산출
        // [비즈니스 로직] 미사용 약정액이 실제 대출로 전환될 확률을 제품군별로 매핑합니다.
        BigDecimal finalCcf = ccfCalculationService.calculateCcf(result.getAccount().getProductCode());

        // 🟢 Phase 2-2. EAD (Exposure at Default) 및 CRM (Credit Risk Mitigation) 산출
        // [비즈니스 로직] 총 노출액에서 담보의 리스크 경감 효과를 차감하여 최종 EAD를 산출합니다.
        EadCrmCalculationService.EadCrmResult eadCrm = 
                eadCrmCalculationService.calculateEadCrm(result.getAccount(), finalCcf, irbParams);

        // 🟢 Phase 2-3. LGD (Loss Given Default) 확정
        // [비즈니스 로직] 부도 시 회수 불가능한 손실률을 담보 유형 및 회수 기대치에 따라 확정합니다.
        BigDecimal finalLgd = lgdCalculationService.calculateLgd(
                result.getAccount().getCustomer().getCustomerType().name(),
                eadCrm.getMajorCollateralType(),
                eadCrm.getTotalCollateralAmt().compareTo(BigDecimal.ZERO) > 0,
                irbParams
        );

        // 산출 결과 세팅 및 감사(Audit) 데이터 기록
        result.setEad(eadCrm.getEadRaw());
        result.setEadStar(eadCrm.getEadStar());
        result.setAppliedCcf(eadCrm.getAppliedCcf());
        result.setCrmDeduction(eadCrm.getCrmDeduction());
        result.setLgd(finalLgd);

        log.debug("   -> 산출 완료: EAD={}, LGD={}, CCF={}", 
                result.getEadStar(), result.getLgd(), result.getAppliedCcf());

        return result;
    }
}
