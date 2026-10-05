package com.ho.account.asset.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * IFRS 16 에 따라 인식되는 사용권자산 엔티티.
 * 리스 계약과 1:1 관계를 가집니다.
 */
@Entity
@Table(name = "right_of_use_assets")
public class RightOfUseAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_contract_id", nullable = false, unique = true)
    private LeaseContract leaseContract; // 관련 리스 계약

    @Column(nullable = false, length = 100)
    private String assetName; // 사용권자산 명칭

    @Column(nullable = false)
    private LocalDate recognitionDate; // 사용권자산 인식일

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal initialValue; // 최초 인식 가액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBookValue; // 현재 장부가액 (상각 후)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal accumulatedDepreciation; // 누적 상각액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal depreciationAmountPerPeriod; // 기간별 상각액

    @Column(length = 20)
    private String status = STATUS_ACTIVE; // ACTIVE, DISPOSED, TERMINATED, FULLY_DEPRECIATED

    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_FULLY_DEPRECIATED = "FULLY_DEPRECIATED";
    public static final String STATUS_DISPOSED = "DISPOSED";
    public static final String STATUS_TERMINATED = "TERMINATED";

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null)
            status = STATUS_ACTIVE;
    }

    /**
     * 🎓 [교육적 주석 - IFRS 16 사용권자산 장부가액 음수 불가 원칙 & 안전 상각액 산출]
     * IFRS 16(리스) 회계기준에 따라 사용권자산(Right-of-Use Asset)은 차감상각(Depreciation)을 진행하지만,
     * 재측정은 최초 인식가액을 보존하면서 현재 장부가액을 조정합니다. 상각은 조정된 장부가액을
     * 기준으로 하며 장부가액(Net Book Value)이 음수(Negative)가 되는 것은 엄격히 금지됩니다.
     *
     * 이 메서드는 계획된 상각액(targetAmount)이 현재 잔여 장부가액을 초과하더라도,
     * 자산의 장부가액이 0원 미만으로 떨어지지 않도록 남은 장부가액 범위 내에서만 안전하게 상각액을 한도 조정(Limit)합니다.
     *
     * @param targetAmount 상각을 시도할 대상 금액 (null일 경우 기간별 설정 상각액 사용)
     * @return 남은 장부가액을 초과하지 않도록 보정된 안전 상각액
     */
    public BigDecimal calculateSafeDepreciationAmount(BigDecimal targetAmount) {
        BigDecimal bookValue = currentBookValue != null ? currentBookValue : BigDecimal.ZERO;
        if (bookValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal plannedAmount = targetAmount != null ? targetAmount : depreciationAmountPerPeriod;
        if (plannedAmount == null || plannedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 계획된 상각액이 남아있는 장부가액보다 크거나 같으면 남은 장부가액만큼만 상각하여 장부가액 0원 보장
        return plannedAmount.compareTo(bookValue) >= 0 ? bookValue : plannedAmount;
    }

    /**
     * 🎓 [교육적 주석 - Rich Domain Model & 도메인 불변성(Domain Invariant) 보장]
     * 사용권자산의 월 감가상각 상태 전이를 도메인 내부에서 안전하게 실행합니다.
     * 외부 서비스나 배치 작업이 직접 엔티티의 장부가액 필드를 조작(Setter 호출)하게 되면,
     * 장부가액 음수 전락이나 상태 미전이 같은 회계 오류가 발생할 수 있습니다.
     * 따라서 헥사고날/DDD 원칙에 따라 감가상각 도메인 로직을 엔티티 내부로 캡슐화합니다.
     *
     * @return 실제 반영된 감가상각액 (0원 이상)
     */
    public BigDecimal depreciate() {
        return depreciate(depreciationAmountPerPeriod);
    }

    /** Clear rounding residue when the final scheduled installment is processed. */
    public BigDecimal depreciateRemaining() {
        return depreciate(currentBookValue);
    }

    private BigDecimal depreciate(BigDecimal targetAmount) {
        if (!STATUS_ACTIVE.equals(status)) {
            return BigDecimal.ZERO;
        }

        BigDecimal safeAmount = calculateSafeDepreciationAmount(targetAmount);
        if (safeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = STATUS_FULLY_DEPRECIATED;
            return BigDecimal.ZERO;
        }

        BigDecimal accumulated = accumulatedDepreciation != null ? accumulatedDepreciation : BigDecimal.ZERO;
        BigDecimal newAccumulated = accumulated.add(safeAmount);
        // Remeasurement changes current book value without rewriting original recognition or past depreciation.
        BigDecimal newBookValue = currentBookValue.subtract(safeAmount);

        // 하한선 보정: 음수 방지 0원 Floor
        if (newBookValue.compareTo(BigDecimal.ZERO) <= 0) {
            newBookValue = BigDecimal.ZERO;
            this.status = STATUS_FULLY_DEPRECIATED;
        }

        // 도메인 불변성(Domain Invariant) 최종 가드: 장부가액은 절대 음수일 수 없음
        if (newBookValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("ROU Asset book value cannot be negative. Current: " + newBookValue);
        }

        this.accumulatedDepreciation = newAccumulated;
        this.currentBookValue = newBookValue;

        return safeAmount;
    }

    public void applyRemeasurement(BigDecimal adjustmentAmount, int remainingPeriods) {
        if (!STATUS_ACTIVE.equals(status) && !STATUS_FULLY_DEPRECIATED.equals(status)) {
            throw new IllegalStateException("ROU asset is not available for remeasurement");
        }
        if (adjustmentAmount == null || currentBookValue == null || remainingPeriods <= 0) {
            throw new IllegalArgumentException("ROU remeasurement requires a book value and remaining periods");
        }
        BigDecimal adjustedBookValue = currentBookValue.add(adjustmentAmount);
        if (adjustedBookValue.signum() < 0) {
            throw new IllegalStateException("ROU adjustment exceeds remaining book value");
        }
        this.currentBookValue = adjustedBookValue;
        this.depreciationAmountPerPeriod = adjustedBookValue.divide(
                BigDecimal.valueOf(remainingPeriods), 2, RoundingMode.HALF_UP);
        this.status = adjustedBookValue.signum() == 0 ? STATUS_FULLY_DEPRECIATED : STATUS_ACTIVE;
    }

    // Getter 및 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LeaseContract getLeaseContract() {
        return leaseContract;
    }

    public void setLeaseContract(LeaseContract leaseContract) {
        this.leaseContract = leaseContract;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public LocalDate getRecognitionDate() {
        return recognitionDate;
    }

    public void setRecognitionDate(LocalDate recognitionDate) {
        this.recognitionDate = recognitionDate;
    }

    public BigDecimal getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(BigDecimal initialValue) {
        this.initialValue = initialValue;
    }

    public BigDecimal getCurrentBookValue() {
        return currentBookValue;
    }

    public void setCurrentBookValue(BigDecimal currentBookValue) {
        this.currentBookValue = currentBookValue;
    }

    public BigDecimal getAccumulatedDepreciation() {
        return accumulatedDepreciation;
    }

    public void setAccumulatedDepreciation(BigDecimal accumulatedDepreciation) {
        this.accumulatedDepreciation = accumulatedDepreciation;
    }

    public BigDecimal getDepreciationAmountPerPeriod() {
        return depreciationAmountPerPeriod;
    }

    public void setDepreciationAmountPerPeriod(BigDecimal depreciationAmountPerPeriod) {
        this.depreciationAmountPerPeriod = depreciationAmountPerPeriod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
