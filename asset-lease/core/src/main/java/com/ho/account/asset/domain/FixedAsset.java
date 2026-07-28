package com.ho.account.asset.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_FULLY_DEPRECIATED = "FULLY_DEPRECIATED";

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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 감가상각 결과를 미리 계산합니다. 이 메서드는 자산 상태를 바꾸지 않습니다.
     *
     * <p>초보자 설명: 단건 API는 `depreciate`로 객체를 바로 바꾸지만, Batch는 계산값만 모아
     * JDBC bulk update로 DB에 한 번에 반영합니다. 그래서 대량 처리에서는 이 preview 메서드를 사용해
     * JPA 변경 감지와 JDBC bulk update가 동시에 실행되는 이중 반영 위험을 피합니다.</p>
     */
    public FixedAssetDepreciationResult calculateDepreciation(LocalDate processDate) {
        BigDecimal accumulated = defaultZero(accumulatedDepreciation);
        BigDecimal bookValue = defaultZero(currentBookValue);

        if (!STATUS_ACTIVE.equals(status)) {
            return FixedAssetDepreciationResult.noChange(id, accumulated, bookValue, status);
        }

        BigDecimal residual = defaultZero(residualValue);
        BigDecimal remainingValue = bookValue.subtract(residual);
        if (remainingValue.compareTo(BigDecimal.ZERO) <= 0) {
            return new FixedAssetDepreciationResult(
                    id,
                    BigDecimal.ZERO,
                    accumulated,
                    bookValue,
                    STATUS_FULLY_DEPRECIATED);
        }

        BigDecimal configuredAmount = defaultZero(depreciationAmountPerPeriod);
        if (configuredAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return FixedAssetDepreciationResult.noChange(id, accumulated, bookValue, status);
        }

        BigDecimal amount = configuredAmount.compareTo(remainingValue) >= 0
                ? remainingValue
                : configuredAmount;
        BigDecimal bookValueAfter = bookValue.subtract(amount);
        String statusAfter = amount.compareTo(remainingValue) >= 0 ? STATUS_FULLY_DEPRECIATED : STATUS_ACTIVE;

        return new FixedAssetDepreciationResult(
                id,
                amount,
                accumulated.add(amount),
                bookValueAfter,
                statusAfter);
    }

    public BigDecimal depreciate(LocalDate processDate) {
        FixedAssetDepreciationResult result = calculateDepreciation(processDate);
        if (!result.shouldPersist()) {
            return result.depreciationAmount();
        }

        this.accumulatedDepreciation = result.accumulatedDepreciation();
        this.currentBookValue = result.currentBookValue();
        this.status = result.status();
        this.lastDepreciationDate = processDate;

        return result.depreciationAmount();
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}