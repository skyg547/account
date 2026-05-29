package com.ho.account.ecl.core.application.service.calculation;

import com.ho.account.ecl.core.application.port.out.CrAccountCollateralRepository;
import com.ho.account.ecl.core.domain.calculator.EadCalculator;
import com.ho.account.ecl.core.domain.calculator.AllowanceModelParams;
import com.ho.account.ecl.core.domain.collateral.CrAccountCollateral;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * [백엔드] EAD(부도시익스포저) 및 CRM(신용위험완화) 산출 통합 서비스
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 1. EAD (Exposure At Default)란?
 *    - 고객이 부도난 시점에 은행이 노출된 '총 위험 금액'입니다.
 *    - 현재 대출 잔액에 더해, 미래에 더 빌려갈 금액(난외 자산 * CCF)을 합쳐서 구합니다.
 * 2. CRM (Collateral Mitigation, 신용위험완화)이란?
 *    - 담보를 잡음으로써 실제 위험 금액을 줄이는 기법입니다.
 * 3. EAD* (EAD Star)란?
 *    - CRM 기법(담보 배분 등)을 적용하여 '위험이 경감된 최종 익스포저'를 말합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EadCrmCalculationService {

    private final CrAccountCollateralRepository accountCollateralRepository;
    private final EadCalculator eadCalculator;

    /**
     * 계좌 잔액, CCF, 배분된 담보를 종합하여 최종 EAD*를 산출합니다.
     *
     * ⚙️ [산출 프로세스 요약]
     * 1. 계좌에 연결된 모든 담보를 조회하여 총 가액과 '가중평균 헤어컷'을 구합니다.
     * 2. ECL 엔진(EadCalculator)을 호출하여 아래 공식에 따라 EAD*를 도출합니다.
     *    - 公式: EAD* = max(0, [EAD Raw - 담보가액 * (1 - Haircut)])
     *
     * @param account   산출 대상 계좌
     * @param ccfRate   적용 CCF 비율
     * @param modelParams 모델 파라미터 (LGD Floor 등)
     * @return EAD 및 CRM 산출 결과 통합 객체
     */
    public EadCrmResult calculateEadCrm(CrAccount account, BigDecimal ccfRate, AllowanceModelParams modelParams) {
        
        // 1. 배분된 담보(CRM) 집계 및 헤어컷 계산
        // 💡 [비즈니스 설명] 담보가 여러 개(예: 주식+부동산)일 경우, 각 담보의 변동성(Haircut)을 반영한 
        //    통합적인 담보 가치를 구해야 합니다.
        List<CrAccountCollateral> collaterals = accountCollateralRepository.findByAccount(account);
        BigDecimal totalCollateralAmt = BigDecimal.ZERO;
        BigDecimal weightedHaircutSum = BigDecimal.ZERO;
        String majorCollateralType = "UNSECURED";

        for (CrAccountCollateral ac : collaterals) {
            BigDecimal amt = ac.getAllocationAmount() != null ? ac.getAllocationAmount() : BigDecimal.ZERO;
            // 💡 Haircut(헤어컷): 담보 자산의 가격 하락 가능성을 미리 차감하는 비율입니다. (예: 주식 15% 감가)
            BigDecimal hc = (ac.getCollateral() != null && ac.getCollateral().getBaseHaircut() != null) 
                    ? ac.getCollateral().getBaseHaircut() : BigDecimal.ZERO;
            
            totalCollateralAmt = totalCollateralAmt.add(amt);
            weightedHaircutSum = weightedHaircutSum.add(amt.multiply(hc));
            
            // LGD 산출에 쓰일 주담보 유형 식별
            if (ac.getCollateral() != null && ac.getCollateral().getCollateralType() != null) {
                majorCollateralType = ac.getCollateral().getCollateralType();
            }
        }

        // 💡 가중평균 헤어컷: (담보A*헤어컷A + 담보B*헤어컷B) / 총담보가액
        BigDecimal effectiveHaircut = totalCollateralAmt.compareTo(BigDecimal.ZERO) > 0
                ? weightedHaircutSum.divide(totalCollateralAmt, 8, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 2. 전문 ECL 엔진(EadCalculator) 호출
        // 💡 EadCalculator는 IFRS 9 모델표준방법/내부등급법의 복잡한 EAD 산식을 코드화한 핵심 모듈입니다.
        Object[] rawResult = eadCalculator.calculateAdvancedEAD(
                account.getOutstandingAmount(), 
                account.getNotionalAmount(),
                ccfRate, 
                totalCollateralAmt, 
                effectiveHaircut,
                modelParams.getSecuredLgdFloor(),
                modelParams.getUnsecuredLgdFloor()
        );

        // 3. 결과 래핑 및 반환
        return EadCrmResult.builder()
                .eadStar((BigDecimal) rawResult[0])      // 담보 반영 후 최종 위험액
                .appliedCcf((BigDecimal) rawResult[1])   // 적용된 CCF
                .eadRaw((BigDecimal) rawResult[2])       // 담보 반영 전 순수 노출액
                .crmDeduction((BigDecimal) rawResult[3]) // 담보로 인해 줄어든 금액
                .weightedLgd((BigDecimal) rawResult[4])  // 담보 비중에 따른 가중평균 LGD
                .totalCollateralAmt(totalCollateralAmt)
                .majorCollateralType(majorCollateralType)
                .build();
    }

    @lombok.Value
    @lombok.Builder
    public static class EadCrmResult {
        BigDecimal eadStar;
        BigDecimal appliedCcf;
        BigDecimal eadRaw;
        BigDecimal crmDeduction;
        BigDecimal weightedLgd;
        BigDecimal totalCollateralAmt;
        String majorCollateralType;
    }
}


