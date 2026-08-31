package com.ho.account.asset.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

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

    @Column(name = "account_code", nullable = false, length = 20)
    private String accountCode;

    @Column(name = "accumulated_account_code", length = 20)
    private String accumulatedAccountCode;

    @Column(name = "expense_account_code", length = 20)
    private String expenseAccountCode;

    @Column(nullable = false)
    private LocalDate acquisitionDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal acquisitionCost;

    @Column(nullable = false)
    private int usefulLife; // 월 단위

    private String depreciationMethod;

    @Column(precision = 19, scale = 2)
    private BigDecimal residualValue = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBookValue;

    @Column(precision = 19, scale = 2)
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
     * 🎓 [교육적 주석 - 유형자산 감가상각 한도 검증 & 안전 상각액 산출]
     * 고정자산 감가상각 시, 남은 상각가능가액(현재 장부가액 - 잔존가액)을 초과하는 상각 시도를 방지합니다.
     * 상각액이 상각가능가액을 초과할 경우 잔존가액(Residual Value)까지만 상각되도록 한도를 자동 보정하여
     * 장부가액이 잔존가액 미만 또는 음수로 떨어지는 회계적 불변성 위반을 방지합니다.
     *
     * @param targetAmount 상각을 시도할 대상 금액 (null일 경우 기간별 설정 상각액 사용)
     * @return 잔존가액 보존 및 장부가액 음수 전락 방지를 보장하는 안전 상각액
     */
    public BigDecimal calculateSafeDepreciationAmount(BigDecimal targetAmount) {
        if (!STATUS_ACTIVE.equals(status)) {
            return BigDecimal.ZERO;
        }

        BigDecimal bookValue = defaultZero(currentBookValue);
        BigDecimal residual = defaultZero(residualValue);
        BigDecimal remainingValue = bookValue.subtract(residual);

        if (remainingValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal planned = targetAmount != null ? targetAmount : defaultZero(depreciationAmountPerPeriod);
        if (planned.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return planned.compareTo(remainingValue) >= 0 ? remainingValue : planned;
    }

    /**
     * 🎓 [교육적 주석 - 회계 기간 단위 감가상각 멱등성(Idempotency) 검증]
     * 동일한 회계 기간(동일 연-월) 내에서 이미 감가상각이 완료되었거나,
     * 마지막 상각일자 이전/당일자로 중복 상각을 시도하는 경우를 방지합니다.
     * 이를 통해 월말 배치 재실행 또는 중복 호출 시 이중 상각 및 장부가액 훼손을 방지합니다.
     *
     * @param processDate 상각 처리 기준일자
     * @return 이미 해당 기간에 상각이 반영되었으면 true, 그렇지 않으면 false
     */
    public boolean isDepreciatedForPeriod(LocalDate processDate) {
        if (processDate == null || lastDepreciationDate == null) {
            return false;
        }
        return !processDate.isAfter(lastDepreciationDate)
                || YearMonth.from(processDate).equals(YearMonth.from(lastDepreciationDate));
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

        if (isDepreciatedForPeriod(processDate)) {
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

        BigDecimal safeAmount = calculateSafeDepreciationAmount(depreciationAmountPerPeriod);
        if (safeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return FixedAssetDepreciationResult.noChange(id, accumulated, bookValue, status);
        }

        BigDecimal bookValueAfter = bookValue.subtract(safeAmount);
        String statusAfter = safeAmount.compareTo(remainingValue) >= 0 ? STATUS_FULLY_DEPRECIATED : STATUS_ACTIVE;

        return new FixedAssetDepreciationResult(
                id,
                safeAmount,
                accumulated.add(safeAmount),
                bookValueAfter,
                statusAfter);
    }

    public void initializeAcquisitionBalances() {
        if (acquisitionCost == null || acquisitionCost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("acquisition cost must be greater than zero");
        }
        BigDecimal residual = defaultZero(residualValue);
        if (residual.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("residual value must not be negative");
        }
        if (residual.compareTo(acquisitionCost) > 0) {
            throw new IllegalArgumentException("residual value must not exceed acquisition cost");
        }
        residualValue = residual;
        accumulatedDepreciation = BigDecimal.ZERO;
        currentBookValue = acquisitionCost;
    }

    /**
     * 🎓 [교육적 주석 - 도메인 불변성(Domain Invariants) & 음수 전락 방지]
     * 고정자산의 감가상각 상태를 직접 변경합니다.
     * 감가상각 완료 후 장부가액이 잔존가액 미만으로 떨어지거나 음수(Negative)가 발생하지 않도록
     * 도메인 불변성 방어막을 형성합니다.
     */
    public BigDecimal depreciate(LocalDate processDate) {
        if (!STATUS_ACTIVE.equals(status) || isDepreciatedForPeriod(processDate)) {
            return BigDecimal.ZERO;
        }

        FixedAssetDepreciationResult result = calculateDepreciation(processDate);
        if (!result.shouldPersist()) {
            return result.depreciationAmount();
        }

        // 도메인 불변성 가드: 장부가액 하한선(잔존가액) 검증
        BigDecimal residual = defaultZero(residualValue);
        if (result.currentBookValue().compareTo(residual) < 0) {
            throw new IllegalStateException("Fixed asset book value cannot fall below residual value. Target: " + result.currentBookValue());
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
