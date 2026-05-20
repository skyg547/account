package com.ho.account.asset.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * [DDD(도메인 주도 설계) - Aggregate Root]
 * 고정자산(Fixed Asset) 엔티티 — 회사가 소유한 유형자산의 가치와 상각 상태를 관리합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 고정자산은 회사에서 오래 쓸 목적으로 산 물건(컴퓨터, 자동차, 건물 등)을 말합니다.
 * 이 클래스는 이 물건을 "얼마에 샀는지(취득가액)", "지금까지 가치가 얼마나 깎였는지(감가상각누계액)",
 * 그리고 "지금 장부상 가치는 얼마인지(장부가액)"를 관리하는 핵심 모델입니다.
 * Rich Domain Model 원칙에 따라, 매달 가치가 깎이는 계산(depreciate) 로직을 도메인 객체가 직접 수행하여 데이터 정합성을 보장합니다.
 */
@Entity
@Table(name = "fixed_assets", indexes = {
    @Index(name = "idx_fixed_asset_status", columnList = "status")
})
@Getter @Setter
@NoArgsConstructor
public class FixedAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String assetCode;

    @Column(nullable = false)
    private String assetName;

    @Column(name = "account_code", length = 20)
    private String accountCode;

    @Column(name = "accumulated_account_code", length = 20)
    private String accumulatedAccountCode;

    @Column(name = "expense_account_code", length = 20)
    private String expenseAccountCode;

    @Column(nullable = false)
    private LocalDate acquisitionDate;

    @Column(nullable = false)
    private BigDecimal acquisitionCost;

    @Column(nullable = false)
    private int usefulLife; // 월 단위

    private String depreciationMethod;

    private BigDecimal residualValue = BigDecimal.ZERO;

    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

    private BigDecimal currentBookValue;

    private BigDecimal depreciationAmountPerPeriod;

    private LocalDate lastDepreciationDate;

    @Column(nullable = false)
    private String status; // ACTIVE, DISPOSED, FULLY_DEPRECIATED

    @Column(name = "dept_code", length = 20)
    private String departmentCode;

    public BigDecimal depreciate(LocalDate processDate) {
        if (!"ACTIVE".equals(status)) return BigDecimal.ZERO;

        BigDecimal amount = depreciationAmountPerPeriod;
        BigDecimal remainingValue = currentBookValue.subtract(residualValue);

        if (amount.compareTo(remainingValue) >= 0) {
            amount = remainingValue;
            this.status = "FULLY_DEPRECIATED";
        }

        this.accumulatedDepreciation = this.accumulatedDepreciation.add(amount);
        this.currentBookValue = this.currentBookValue.subtract(amount);
        this.lastDepreciationDate = processDate;

        return amount;
    }
}
