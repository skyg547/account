package com.risk.credit.core.domain.result;

import com.risk.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [Entity] 월간 리스크 산출 요약 정보
 */
@Entity
@Table(name = "cr_monthly_summaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CrMonthlySummary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate baseDate;

    @Column(length = 50)
    private String productGroup;

    @Column(length = 30)
    private String customerType;

    @Column(length = 20)
    private String staging;

    private Integer totalCount;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalEad;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalEadStar;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalRwaSa;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalRwaIrb;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalExpectedLoss;

    @Column(precision = 10, scale = 8)
    private BigDecimal avgPd;

    @Column(precision = 10, scale = 8)
    private BigDecimal avgLgd;
}
