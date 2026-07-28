package com.ho.account.reporting.core.domain.service;

import java.math.BigDecimal;

/**
 * 바젤 III 규제 기준에 따라 위험가중자산(RWA, Risk-Weighted Assets)을 산출하는 도메인 서비스입니다.
 * 
 * <p><b>초보자 가이드(Beginner's Guide):</b></p>
 * <p>
 * RWA는 은행이 대출을 해줄 때, 그 대출이 얼마나 떼일 위험(리스크)이 있는지를 금액으로 환산한 지표입니다.
 * 예를 들어 정부에 100억을 빌려주면 위험이 낮아 RWA는 0원이지만,
 * 일반 기업에 100억을 빌려주면 위험이 높아 RWA가 100억(또는 그 이상)으로 잡힙니다.
 * 은행은 금융감독원(금감원, FSS)에 주기적으로 BIS 비율(자기자본 / RWA)을 보고해야 하며,
 * 이 수치가 기준치 아래로 떨어지면 은행업 인가가 취소될 수 있는 매우 중요한 프로세스입니다.
 * </p>
 */
public class RwaCalculator {

    /**
     * 표준 방법(Standardised Approach)을 사용하여 특정 자산의 RWA를 계산합니다.
     * 
     * @param exposureAmount 익스포저 금액 (부도 시 은행이 잃을 수 있는 최대 금액, EAD)
     * @param riskWeight 위험가중치 (예: 0.0=0%, 0.5=50%, 1.0=100%)
     * @return 계산된 위험가중자산 금액 (RWA)
     */
    public BigDecimal calculateStandardisedRwa(BigDecimal exposureAmount, BigDecimal riskWeight) {
        if (exposureAmount == null || exposureAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (riskWeight == null || riskWeight.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("위험가중치는 음수가 될 수 없습니다.");
        }

        // RWA = EAD * 위험가중치
        return exposureAmount.multiply(riskWeight);
    }

    /**
     * 내부등급법(IRB, Internal Ratings-Based Approach) 등 고급 모델에 의한
     * RWA 계산을 위한 기초 뼈대 메서드.
     * 
     * @param exposureAmount EAD (부도 시 노출 금액)
     * @param pd 부도확률 (Probability of Default)
     * @param lgd 부도시 손실률 (Loss Given Default)
     * @return IRB 방식에 의한 예상 RWA
     */
    public BigDecimal calculateIrbRwa(BigDecimal exposureAmount, BigDecimal pd, BigDecimal lgd) {
        // 실제 바젤 규제에 따른 IRB RWA 산출 공식은 복잡한 비선형 함수(상관관계 R 계수 등)를 포함합니다.
        // 현재는 스켈레톤(Skeleton) 코드로, 단순 Expected Loss 수준으로 모사합니다.
        
        if (exposureAmount == null || pd == null || lgd == null) {
            return BigDecimal.ZERO;
        }
        
        // Expected Loss = EAD * PD * LGD
        BigDecimal expectedLoss = exposureAmount.multiply(pd).multiply(lgd);
        
        // 실제 규제자본(Unexpected Loss) 계산 로직이 이 위치에 추가되어야 합니다.
        // (Epic-27): 바젤 III 규제 기반의 상관계수(R) 및 만기조정(M) 수식 적용 필요
        
        return expectedLoss.multiply(new BigDecimal("1.5")); // 임의의 가중치 적용
    }
}
