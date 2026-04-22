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
 * 고정자산 (Fixed Asset) 엔티티
 * 정액법 및 정률법 감가상각 로직을 내포합니다.
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
    private String depreciationMethod; // STRAIGHT_LINE, DECLINING

    @Column(precision = 19, scale = 2)
    private BigDecimal residualValue = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

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
    private String status; // ACTIVE, DISPOSED, FULLY_DEPRECIATED

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
        if (currentBookValue == null) currentBookValue = acquisitionCost;
        if (accumulatedDepreciation == null) accumulatedDepreciation = BigDecimal.ZERO;
        if (residualValue == null) residualValue = BigDecimal.ZERO;
        calculateInitialDepreciationAmount();
    }

    private void calculateInitialDepreciationAmount() {
        if ("STRAIGHT_LINE".equals(depreciationMethod)) {
            if (usefulLife != null && usefulLife > 0) {
                BigDecimal depreciableAmount = acquisitionCost.subtract(residualValue);
                this.depreciationAmountPerPeriod = depreciableAmount.divide(new BigDecimal(usefulLife * 12), 2, RoundingMode.HALF_UP);
            }
        }
    }

    /**
     * 감가상각비를 계산하고 상태를 업데이트합니다.
     */
    public BigDecimal depreciate(LocalDate targetDate) {
        if (!"ACTIVE".equals(status)) return BigDecimal.ZERO;

        BigDecimal amount = calculateCurrentDepreciationAmount();
        BigDecimal maxDepreciable = currentBookValue.subtract(residualValue);
        amount = amount.min(maxDepreciable);

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            if (this.accumulatedDepreciation == null) this.accumulatedDepreciation = BigDecimal.ZERO;
            
            this.accumulatedDepreciation = this.accumulatedDepreciation.add(amount);
            this.currentBookValue = this.currentBookValue.subtract(amount);
            this.lastDepreciationDate = targetDate;

            // 상각 완료 체크
            if (this.currentBookValue.subtract(this.residualValue).abs().compareTo(new BigDecimal("0.01")) < 0) {
                this.status = "FULLY_DEPRECIATED";
                this.currentBookValue = this.residualValue;
            }
        }
        return amount;
    }

    private BigDecimal calculateCurrentDepreciationAmount() {
        if ("STRAIGHT_LINE".equals(depreciationMethod)) {
            return depreciationAmountPerPeriod != null ? depreciationAmountPerPeriod : BigDecimal.ZERO;
        } else if ("DECLINING".equals(depreciationMethod)) {
            BigDecimal annualRate = new BigDecimal("0.2"); 
            return currentBookValue.multiply(annualRate).divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
