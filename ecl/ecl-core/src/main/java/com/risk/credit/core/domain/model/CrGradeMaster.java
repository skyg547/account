package com.risk.credit.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [Entity] 신용등급 마스터 (Internal Rating Master).
 * 은행 내부의 신용 등급별 부도율(PD) 기준 정보를 관리합니다.
 *
 * [초보자를 위한 개념 설명]
 * 은행은 고객을 AAA부터 D까지 여러 등급으로 나눕니다. 
 * 이 마스터 테이블은 "AAA 등급 고객은 통계적으로 1년에 0.01% 확률로 부도난다"와 같은 
 * 등급별 부도 확률(PD) 값을 정의합니다.
 */
@Entity
@Table(name = "cr_grade_masters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrGradeMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 등급 코드 (예: AAA, A1, 1, 2 등) */
    @Column(name = "rating_code", nullable = false, unique = true)
    private String ratingCode;

    /** 등급별 신용 부도율 (PD) */
    @Column(name = "pd_value", nullable = false, precision = 10, scale = 8)
    private BigDecimal pdValue;

    /** 등급 순서 (정렬용, 숫자가 작을수록 우량) */
    @Column(name = "notch_order", nullable = false)
    private Integer notchOrder;

    /** 등급 상세 설명 */
    @Column(name = "description")
    private String description;

    /** 생성 일시 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
