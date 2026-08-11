package com.ho.account.ecl.core.domain.collateral;

import com.ho.account.shared.finance.entity.BaseEntity;
import com.ho.account.ecl.core.domain.exposure.CrCustomer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * [CRM] 담보(Collateral) 마스터 엔티티
 *
 * [초보자를 위한 개념 설명]
 * 담보는 차주가 돈을 빌릴 때 만약의 부도 상황에 대비해 담보로 잡는 안전장치입니다. 
 * 감정가와 선순위 채권, LTV 한도 등을 통해 실제 인정 가능한 담보가치를 계산합니다.
 */
@Entity
@Table(name = "cr_collaterals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrCollateral extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 담보 소유 차주 (Many-to-One) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CrCustomer customer;

    /** 담보 코드 */
    @Column(name = "collateral_code", nullable = false, unique = true, length = 50)
    private String collateralCode;

    /** 담보 유형 (REAL_ESTATE, CASH, GUARANTEE, VEHICLE 등) */
    @Column(name = "collateral_type", nullable = false, length = 30)
    private String collateralType;

    /** 감정 가액 (Appraisal Value) */
    @Column(name = "appraisal_amt", nullable = false, precision = 19, scale = 4)
    private BigDecimal appraisalAmount;

    /** 모델 기본 헤어컷 (Hc, 0.0 ~ 1.0) */
    @Column(name = "base_haircut", nullable = false, precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal baseHaircut = BigDecimal.ZERO;

    // --- 아파트/부동산 상세 정보 필드 ---

    /** 선순위 채권액 (나보다 앞선 채권자가 먼저 가져갈 수 있는 금액) */
    @Column(name = "prior_lien_amt", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal priorLienAmount = BigDecimal.ZERO;

    /** LTV 한도 (예: 0.70) */
    @Column(name = "ltv_limit", precision = 10, scale = 6)
    private BigDecimal ltvLimit;

    /** KB 시세 (부동산인 경우) */
    @Column(name = "kb_market_price", precision = 19, scale = 4)
    private BigDecimal kbMarketPrice;

    /** 법정동 코드 */
    @Column(name = "district_code", length = 10)
    private String districtCode;

    /** 활성 상태 여부 */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // ==========================================
    // 도메인 모델 비즈니스 메서드 (DDD Domain Model)
    // ==========================================

    /**
     * [도메인 모델] 부동산/아파트 담보의 유효 가치(Available Collateral Value)를 산출합니다.
     *
     * 💡 [초보자를 위한 개념 설명 & 금융 산식]
     * 1. 시장가(Market Value): KB 시세가 존재하면 우선 사용하고, 없으면 감정가액(Appraisal Amount)을 사용합니다.
     * 2. 담보 한도(LTV Limit): 시장가 * LTV 비율(기본 70%)로 은행이 법적으로 빌려줄 수 있는 한도를 계산합니다.
     * 3. 선순위 차감(Prior Lien): 해당 담보에 설정된 선순위 채권액을 차감합니다.
     * 
     * 산식: max(0, (시장가 * LTV) - 선순위 채권액)
     *
     * @return 부동산/아파트 담보의 유효 가액 (0원 이상)
     */
    public BigDecimal calculateRealEstateEffectiveValue() {
        BigDecimal marketValue = (kbMarketPrice != null) ? kbMarketPrice : appraisalAmount;
        if (marketValue == null) {
            marketValue = BigDecimal.ZERO;
        }
        BigDecimal effectiveLtv = (ltvLimit != null) ? ltvLimit : new BigDecimal("0.70");
        BigDecimal priorLien = (priorLienAmount != null) ? priorLienAmount : BigDecimal.ZERO;

        BigDecimal effectiveValue = marketValue.multiply(effectiveLtv).subtract(priorLien);
        return effectiveValue.max(BigDecimal.ZERO);
    }

    /**
     * [도메인 모델] 담보 유형별 최종 유효 가치(Effective Collateral Value)를 산출합니다.
     *
     * 💡 [DDD 핵심 원칙]
     * 담보 엔티티 스스로 자신의 유효 가치를 계산함으로써, Anemic Domain Model(빈약한 도메인 모델)을 방지하고
     * 객체 지향적인 캡슐화(Encapsulation)를 달성합니다.
     *
     * - 부동산/아파트 담보: KB시세, LTV, 선순위 채권을 반영하여 가치 산출
     * - 일반 담보(예금, 유가증권 등): 감정가액에 기본 헤어컷(Haircut)을 반영하여 산출
     *
     * @return 담보의 최종 유효 가치
     */
    public BigDecimal calculateEffectiveValue() {
        if ("REAL_ESTATE".equalsIgnoreCase(collateralType) || "APARTMENT".equalsIgnoreCase(collateralType)) {
            return calculateRealEstateEffectiveValue();
        }
        if (appraisalAmount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal haircut = (baseHaircut != null) ? baseHaircut : BigDecimal.ZERO;
        return appraisalAmount.multiply(BigDecimal.ONE.subtract(haircut));
    }
}


