package com.ho.account.ecl.core.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * [Master] 거시경제 시나리오 및 PD 조정 계수 (Macro Economic Scenario)
 * 
 * 💡 [초보자를 위한 개념 설명]
 * IFRS 9에서는 단순히 현재 상황만 보는 것이 아니라, 미래에 경기가 좋아질지 나빠질지를 
 * 예측하여 손실(ECL)을 미리 계산해야 합니다.
 * 이 테이블은 '호황', '평균', '침체'와 같은 시나리오별로 발생 확률과 
 * 그에 따라 부도율(PD)을 얼마나 더 높게 혹은 낮게 잡아야 하는지(조정 계수)를 관리합니다.
 */
@Entity
@Table(name = "cr_macro_scenario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrMacroScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 적용 연도 (예: 2026) */
    @Column(name = "apply_year", nullable = false)
    private Integer applyYear;

    /** 시나리오 유형 (BOOM: 호황, BASE: 평균, RECESSION: 침체) */
    @Column(name = "scenario_type", length = 20, nullable = false)
    private String scenarioType;

    /** 시나리오 발생 확률 (가중치, 예: 0.30) */
    @Column(name = "probability_weight", precision = 5, scale = 4)
    private BigDecimal probabilityWeight;

    /** 
     * PD 스칼라 조정 계수 (Z-factor)
     * 💡 평균(BASE)을 1.0으로 볼 때, 침체기에는 부도율이 1.5배 높아진다면 1.5000을 입력합니다.
     */
    @Column(name = "pd_adjustment_factor", precision = 7, scale = 4)
    private BigDecimal pdAdjustmentFactor;

    /** 설명 (예: 2026년 하반기 경기 침체 시나리오 반영) */
    @Column(name = "description")
    private String description;
}
