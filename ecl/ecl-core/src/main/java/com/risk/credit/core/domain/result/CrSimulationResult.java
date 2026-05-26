package com.risk.credit.core.domain.result;

import com.risk.common.enums.CalculationStatus;
import com.risk.common.enums.CrStaging;
import com.risk.common.enums.StressScenario;
import com.risk.credit.core.domain.exposure.CrAccount;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [Entity] 신용 리스크 스트레스 테스트 결과 엔티티
 * 위기 시나리오(Stress Scenario) 상황에서의 시뮬레이션 결과를 규제 산출 결과와 분리하여 통합합니다.
 *
 * [초보자를 위한 개념 설명]
 * 이 테이블은 "만약 경제 위기가 오면 어떻게 될까"를 시험해본 결과지입니다. 
 * 금리 급등, 부동산 가격 하락 등 스트레스 시나리오를 적용했을 때 예상되는 
 * 부도율(PD) 증가 효과와 손실(ECL/RWA)의 변동폭(Delta)을 기록하여 
 * 경영진이 위기 대응 계획을 세우는 데 도움을 줍니다.
 */
@Entity
@Table(name = "cr_simulation_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrSimulationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 시뮬레이션 산출 기준 일자 */
    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    /** 적용한 위기 시나리오 (예: LIGHT_RECESSION, SEVERE_SHOCK 등) */
    @Enumerated(EnumType.STRING)
    @Column(name = "scenario", nullable = false, length = 30)
    private StressScenario scenario;

    /** 시뮬레이션 대상 계좌 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private CrAccount account;

    /** 시뮬레이션 시점의 스테이징 */
    @Enumerated(EnumType.STRING)
    @Column(name = "staging", length = 20)
    private CrStaging staging;

    // --- 시뮬레이션 산출 결과 필드 ---

    /** 시뮬레이션 보정 EAD* */
    @Column(name = "ead_star", precision = 19, scale = 4)
    private BigDecimal eadStar;

    /** 스트레스 상황을 가정한 조정 부도율 (Stress PD) */
    @Column(name = "stress_pd", precision = 10, scale = 8)
    private BigDecimal stressPd;

    /** 스트레스 상황을 가정한 조정 손실률 (Stress LGD) */
    @Column(name = "stress_lgd", precision = 10, scale = 8)
    private BigDecimal stressLgd;

    /** 스트레스 상황에서 예상되는 기대 손실 (Stress ECL) */
    @Column(name = "stress_ecl", precision = 19, scale = 4)
    private BigDecimal stressEcl;

    /** 스트레스 상황에서 예상되는 위험가중자산 (Stress RWA IRB) */
    @Column(name = "stress_rwa_irb", precision = 19, scale = 4)
    private BigDecimal stressRwaIrb;

    // --- 기준(Baseline) 대비 증감액 (리포트용 기록) ---

    /** 기준 대비 기대 손실 증가액 */
    @Column(name = "ecl_delta", precision = 19, scale = 4)
    private BigDecimal eclDelta;

    /** 기준 대비 위험가중자산 증가액 */
    @Column(name = "rwa_delta", precision = 19, scale = 4)
    private BigDecimal rwaDelta;

    /** 시뮬레이션 수행 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private CalculationStatus status;

    /** 시뮬레이션 완료 일시 */
    @Column(name = "simulation_completed_at")
    private LocalDateTime simulationCompletedAt;
}
