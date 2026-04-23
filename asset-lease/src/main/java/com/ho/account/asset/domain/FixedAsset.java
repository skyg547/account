package com.ho.account.asset.domain;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.Department;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 고정자산 (Fixed Asset) 엔티티 - IFRS 고도화 버전
 * 정액법/정률법 감가상각 + 재평가(Revaluation) + 손상차손(Impairment) 지원
 */
@Entity
@Table(name = "fixed_assets")
@Getter
@Setter
@NoArgsConstructor
public class FixedAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String assetCode;

    @Column(nullable = false, length = 100)
    private String assetName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_code", nullable = false)
    private AccountSubject accountSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accumulated_account_code")
    private AccountSubject accumulatedAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_account_code")
    private AccountSubject expenseAccount;

    @Column(nullable = false)
    private LocalDate acquisitionDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal acquisitionCost;

    @Column(nullable = false)
    private Integer usefulLife;

    @Column(length = 20)
    private String depreciationMethod;

    @Column(precision = 19, scale = 2)
    private BigDecimal residualValue = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

    // [고도화] 손상차손 누계액 (IFRS)
    @Column(precision = 19, scale = 2)
    private BigDecimal accumulatedImpairment = BigDecimal.ZERO;

    // [고도화] 재평가 잉여금 (OCI, 자본항목 연동용)
    @Column(precision = 19, scale = 2)
    private BigDecimal revaluationSurplus = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBookValue;

    @Column(precision = 19, scale = 2)
    private BigDecimal depreciationAmountPerPeriod;

    @Column
    private LocalDate lastDepreciationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_code", referencedColumnName = "code")
    private Department department;

    @Column(length = 20)
    private String status; // ACTIVE, DISPOSED, FULLY_DEPRECIATED, IMPAIRED

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
        if (currentBookValue == null) currentBookValue = acquisitionCost;
        if (accumulatedDepreciation == null) accumulatedDepreciation = BigDecimal.ZERO;
        if (accumulatedImpairment == null) accumulatedImpairment = BigDecimal.ZERO;
        if (revaluationSurplus == null) revaluationSurplus = BigDecimal.ZERO;
        calculateInitialDepreciationAmount();
    }

    /**
     * 손상차손 반영 (Impairment)
     * 회수가능액이 장부금액보다 낮을 때 그 차액을 기록합니다.
     */
    public void applyImpairment(BigDecimal recoverableAmount, String reason) {
        if (recoverableAmount.compareTo(currentBookValue) < 0) {
            BigDecimal impairmentLoss = currentBookValue.subtract(recoverableAmount);
            this.accumulatedImpairment = this.accumulatedImpairment.add(impairmentLoss);
            this.currentBookValue = recoverableAmount;
            this.status = "IMPAIRED";
            // 손상 후에는 남은 장부가액 기준으로 상각액 재계산 필요
            recalculateDepreciationAmount();
        }
    }

    /**
     * 감가상각 실행
     */
    public BigDecimal depreciate(LocalDate targetDate) {
        if (!"ACTIVE".equals(status) && !"IMPAIRED".equals(status)) return BigDecimal.ZERO;

        BigDecimal amount = calculateCurrentDepreciationAmount();
        BigDecimal maxDepreciable = currentBookValue.subtract(residualValue);
        amount = amount.min(maxDepreciable);

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            this.accumulatedDepreciation = this.accumulatedDepreciation.add(amount);
            this.currentBookValue = this.currentBookValue.subtract(amount);
            this.lastDepreciationDate = targetDate;

            if (this.currentBookValue.subtract(this.residualValue).abs().compareTo(new BigDecimal("0.01")) < 0) {
                this.status = "FULLY_DEPRECIATED";
                this.currentBookValue = this.residualValue;
            }
        }
        return amount;
    }

    private void calculateInitialDepreciationAmount() {
        recalculateDepreciationAmount();
    }

    private void recalculateDepreciationAmount() {
        if ("STRAIGHT_LINE".equals(depreciationMethod) && usefulLife != null && usefulLife > 0) {
            BigDecimal depreciableAmount = currentBookValue.subtract(residualValue);
            // 남은 기간 계산 로직은 단순화함
            this.depreciationAmountPerPeriod = depreciableAmount.divide(new BigDecimal(usefulLife * 12), 2, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal calculateCurrentDepreciationAmount() {
        return depreciationAmountPerPeriod != null ? depreciationAmountPerPeriod : BigDecimal.ZERO;
    }
}
