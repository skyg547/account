package com.ho.account.asset.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 고정자산 엔티티
 */
@Entity
@Table(name = "fixed_assets")
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
