package com.ho.account.ecl.core.domain.result;

import com.ho.account.shared.finance.entity.BaseEntity;
import com.ho.account.shared.finance.enums.CalculationStatus;
import com.ho.account.shared.finance.enums.CrStaging;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [Entity] 대손충당금(IFRS9) 산출 결과 엔티티 (Risk Mart Core)
 *
 * 💡 [초보자를 위한 개념 설명]
 * 이 엔티티는 재무 결산 시스템의 '최종 성적표'입니다.
 * 원장 데이터(원금, 한도 등)와 마스터 데이터(등급별 PD, LGD 등)를 결합하여
 * 산출된 모든 규제 및 회계 지표(EAD, RWA, EL)를 기준일자별로 보관합니다.
 * 이 테이블의 데이터를 기반으로 금융당국 리포팅 및 경영진 대시보드가 구성됩니다.
 *
 * 📊 [핵심 컬럼군]
 * 1. 식별 정보: baseDate(기준일), account(계좌)
 * 2. 리스크 변수: pd(부도율), lgd(손실률)
 * 3. 산출 지표: eadStar(보정 노출액), rwa(위험가중자산), expectedLoss(기대손실)
 */
@Entity
@Table(name = "cr_risk_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(CrRiskResultId.class)
public class CrRiskResult extends BaseEntity {

    @Id
    @Column(name = "id")
    private Long id;

    /** 기준 일자 */
    @Id
    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    /** 산출 대상 계좌 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private CrAccount account;

    /** 산출 시점 스테이징 */
    @Enumerated(EnumType.STRING)
    @Column(name = "staging", nullable = false, length = 20)
    private CrStaging staging;

    /** 부도시 익스포저 (EAD) */
    @Column(name = "ead", precision = 19, scale = 4)
    private BigDecimal ead;

    /** 적용된 CCF */
    @Column(name = "applied_ccf", precision = 5, scale = 4)
    private BigDecimal appliedCcf;

    /** 
     * [v2.4 고도화] 적용된 부도율 (PD, Probability of Default)
     * 차주 등급 또는 계좌별 등급에 따라 산출된 최종 부도 확률 (0.0 ~ 1.0)
     */
    @Column(name = "pd", precision = 10, scale = 8)
    private BigDecimal pd;

    /** 
     * 부도시 손실률 (LGD, Loss Given Default)
     * 부도 발생 시 실제 떼이게 되는 금액의 비율 (담보에 따라 차등)
     */
    @Column(name = "lgd", precision = 10, scale = 8)
    private BigDecimal lgd;

    /** 기대 손실 (Expected Loss) */
    @Column(name = "expected_loss", precision = 19, scale = 4)
    private BigDecimal expectedLoss;

    /** 비기대 손실 (Unexpected Loss) */
    @Column(name = "unexpected_loss", precision = 19, scale = 4)
    private BigDecimal unexpectedLoss;

    // --- Forward-Looking (미래전망) 시나리오별 ECL ---
    
    /** 호황(Boom) 시나리오 기대손실 */
    @Column(name = "ecl_boom", precision = 19, scale = 4)
    private BigDecimal eclBoom;

    /** 평균(Base) 시나리오 기대손실 */
    @Column(name = "ecl_base", precision = 19, scale = 4)
    private BigDecimal eclBase;

    /** 침체(Recession) 시나리오 기대손실 */
    @Column(name = "ecl_recession", precision = 19, scale = 4)
    private BigDecimal eclRecession;

    /** 3개 시나리오 가중평균 최종 기대손실 (Weighted ECL) */
    @Column(name = "weighted_ecl", precision = 19, scale = 4)
    private BigDecimal weightedEcl;

    // --- Audit Trail 및 중간 산출물 기록 필드 ---
    @Column(name = "k_value", precision = 10, scale = 8)
    private BigDecimal kValue;

    /** 
     * 자산상관계수 (R, Asset Correlation) 
     * 국제 금융 규제 IRB 수식에서 차주 유형별로 적용되는 상관관계 값
     */
    @Column(name = "r_value", precision = 10, scale = 8)
    private BigDecimal rValue;

    /** 
     * 만기 조정 계수 (b, Maturity Adjustment) 
     * 잔여 만기에 따라 위험가중치를 조정해주는 값
     */
    @Column(name = "maturity_adj", precision = 10, scale = 8)
    private BigDecimal maturityAdj;

    @Column(name = "ead_star", precision = 19, scale = 4)
    private BigDecimal eadStar;

    @Column(name = "crm_deduction", precision = 19, scale = 4)
    private BigDecimal crmDeduction;

    @Column(name = "applied_rw", precision = 5, scale = 4)
    private BigDecimal appliedRw;

    /** 위험가중자산 - 표준방법 (RWA SA) */
    @Column(name = "rwa_sa", precision = 19, scale = 4)
    private BigDecimal rwaSa;

    /** 위험가중자산 - 내부등급법 (RWA IRB) */
    @Column(name = "rwa_irb", precision = 19, scale = 4)
    private BigDecimal rwaIrb;

    /** 산출 상태 (COMPLETED, FAILED 등) */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CalculationStatus status;

    /** 오류 메시지 */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /** 산출 완료 일시 */
    @Column(name = "calculation_completed_at")
    private LocalDateTime calculationCompletedAt;
}
