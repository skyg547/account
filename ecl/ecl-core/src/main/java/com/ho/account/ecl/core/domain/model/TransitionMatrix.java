package com.ho.account.ecl.core.domain.model;

import com.ho.account.shared.finance.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [Entity] 신용 등급 전이 행렬 (Credit Rating Transition Matrix) 엔티티
 * 일정 기간 동안 차주의 신용 등급이 변화할 확률을 관리하며 
 * IFRS 9 생애주기 부도율(Lifetime PD) 추정에 필수적인 데이터입니다.
 *
 * [초보자를 위한 개념 설명]
 * 전이 행렬(Transition Matrix)이란 "오늘 AAA 등급인 고객이 1년 후에 AAA일 확률은 얼마이며, 
 * 혹시 AA로 떨어질 확률은 얼마인가"를 표로 나타낸 것입니다. 
 * 재무 결산 시스템은 이 데이터를 통해 만기까지 고객의 등급이 어떻게 변하고, 
 * 최종적으로 부도날 확률이 얼마가 될지를 통계적으로 예측합니다.
 */
@Entity
@Table(name = "transition_matrix", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"base_date", "from_rating", "to_rating"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransitionMatrix extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 기준 일자 */
    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    /** 시작 등급 (시나리오 시작 시점의 등급) */
    @Column(name = "from_rating", nullable = false, length = 10)
    private String fromRating;

    /** 도착 등급 (일정 기간 후 변화된 등급) */
    @Column(name = "to_rating", nullable = false, length = 10)
    private String toRating;

    /** 전이 확률 (해당 경로로 등급이 변화할 확률, 0.0 ~ 1.0) */
    @Column(name = "probability", nullable = false, precision = 10, scale = 6)
    private BigDecimal probability;

    /** 측정 기간 (단위: 년, 보통 1년) */
    @Column(name = "time_horizon")
    @Builder.Default
    private Integer timeHorizon = 1;
}
