package com.risk.credit.core.domain.collateral;

import com.risk.common.entity.BaseEntity;
import com.risk.credit.core.domain.exposure.CrCustomer;
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

    /** 규제 기본 헤어컷 (Hc, 0.0 ~ 1.0) */
    @Column(name = "base_haircut", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal baseHaircut = BigDecimal.ZERO;

    // --- 아파트/부동산 상세 정보 필드 ---

    /** 선순위 채권액 (나보다 앞선 채권자가 먼저 가져갈 수 있는 금액) */
    @Column(name = "prior_lien_amt", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal priorLienAmount = BigDecimal.ZERO;

    /** LTV 한도 (예: 0.70) */
    @Column(name = "ltv_limit", precision = 5, scale = 4)
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
}
