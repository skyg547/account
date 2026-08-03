package com.ho.account.ecl.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [Entity] LGD(부도시손실률) 세그먼트 마스터
 * 차주 유형 및 담보 유형별로 신용 부도시 손실률(LGD) 기준 정보를 관리합니다.
 *
 * [초보자를 위한 개념 설명]
 * LGD(Loss Given Default)는 고객이 부도났을 때 "실제로 얼마만큼 돈을 잃게 되는가"에 대한 비율입니다. 
 * 예를 들어 담보가 없는 신용대출은 LGD가 45%로 높지만, 
 * 아파트 담보가 있는 대출은 나중에 집을 팔아 돈을 회수할 수 있으므로 LGD가 10% 정도로 낮아집니다.
 */
@Entity
@Table(name = "cr_lgd_segment_masters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrLgdSegmentMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 세그먼트 명칭 */
    @Column(name = "segment_name", nullable = false, length = 100)
    private String segmentName;

    /** 차주 유형 (CORPORATE, RETAIL 등) */
    @Column(name = "customer_type", nullable = false, length = 30)
    private String customerType;

    /** 담보 유형 (UNSECURED, REAL_ESTATE 등) */
    @Column(name = "collateral_type", nullable = false, length = 30)
    private String collateralType;

    /** 신용 부도시 손실률 (LGD, 0.0 ~ 1.0) */
    @Column(name = "lgd_value", nullable = false, precision = 15, scale = 10)
    private BigDecimal lgdValue;

    /** 생성 일시 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
