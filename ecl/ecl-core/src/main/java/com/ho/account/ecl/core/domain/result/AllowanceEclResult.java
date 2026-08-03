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
 * [Entity] 대손충당금(IFRS9) 산출 결과 엔티티.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 이 엔티티는 재무 결산 시스템의 '최종 성적표'입니다.
 * 원장 데이터(원금, 한도 등)와 마스터 데이터(등급별 PD, LGD 등)를 결합하여
 * 산출된 IFRS 9 대손충당금 지표(EAD, PD, LGD, ECL)를 기준일자별로 보관합니다.
 * 이 테이블의 완료 데이터를 기반으로 회계 summary를 생성합니다.
 *
 * 📊 [핵심 컬럼군]
 * 1. 식별 정보: baseDate(기준일), account(계좌)
 * 2. 산출 변수: pd(부도율), lgd(손실률)
 * 3. 산출 지표: eadStar(보정 노출액), expectedLoss(기대손실), weightedEcl(미래전망 가중 ECL)
 */
@Entity
@Table(name = "allowance_ecl_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(AllowanceEclResultId.class)
public class AllowanceEclResult extends BaseEntity {

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
    @Column(name = "applied_ccf", precision = 10, scale = 6)
    private BigDecimal appliedCcf;

    /** 
     * [v2.4 고도화] 적용된 부도율 (PD, Probability of Default)
     * 차주 등급 또는 계좌별 등급에 따라 산출된 최종 부도 확률 (0.0 ~ 1.0)
     */
    @Column(name = "pd", precision = 15, scale = 10)
    private BigDecimal pd;

    /** 
     * 부도시 손실률 (LGD, Loss Given Default)
     * 부도 발생 시 실제 떼이게 되는 금액의 비율 (담보에 따라 차등)
     */
    @Column(name = "lgd", precision = 15, scale = 10)
    private BigDecimal lgd;

    /** 기대 손실 (Expected Loss) */
    @Column(name = "expected_loss", precision = 19, scale = 4)
    private BigDecimal expectedLoss;

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

    @Column(name = "ead_star", precision = 19, scale = 4)
    private BigDecimal eadStar;

    @Column(name = "crm_deduction", precision = 19, scale = 4)
    private BigDecimal crmDeduction;

    /** 산출 상태 (COMPLETED, FAILED 등) */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CalculationStatus status;

    /** 오류 메시지 */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 산출 완료 일시 */
    @Column(name = "calculation_completed_at")
    private LocalDateTime calculationCompletedAt;
}

