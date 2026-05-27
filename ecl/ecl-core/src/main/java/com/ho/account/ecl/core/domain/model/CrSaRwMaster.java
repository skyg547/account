package com.ho.account.ecl.core.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [Entity] SA(표준방법) 위험가중치 마스터 (CrSaRwMaster)
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 이 테이블은 "어떤 차주에게 몇 %의 위험 가중치를 줄 것인가?"를 정의한 규제 매핑 표입니다.
 * 예: CORPORATE(기업) + AAA등급 = 0.20 (20%)
 */
@Entity
@Table(name = "cr_sa_rw_masters", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"customer_type", "rating_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrSaRwMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 차주 유형 (RETAIL, CORPORATE, SME, FINANCIAL_INSTITUTION 등) */
    @Column(name = "customer_type", nullable = false, length = 30)
    private String customerType;

    /** 신용 등급 코드 (AAA, AA+, ..., D 또는 전 등급 공통인 'ALL', 무등급 'UNRATED') */
    @Column(name = "rating_code", nullable = false, length = 10)
    private String ratingCode;

    /** 적용 위험가중치 (Risk Weight, 예: 0.7500) */
    @Column(name = "risk_weight", nullable = false, precision = 5, scale = 4)
    private BigDecimal riskWeight;

    /** 설명 */
    @Column(name = "description", length = 200)
    private String description;

    /** 생성 일시 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
