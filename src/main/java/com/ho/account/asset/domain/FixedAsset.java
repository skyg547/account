package com.ho.account.asset.domain;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.Department;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fixed_assets")
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
    private AccountSubject accountSubject; // 자산 계정 (예: 차량운반구)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accumulated_account_code")
    private AccountSubject accumulatedAccount; // 감가상각누계액 계정 (차감 계정)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_account_code")
    private AccountSubject expenseAccount; // 감가상각비 계정 (비용 계정)

    @Column(nullable = false)
    private LocalDate acquisitionDate; // 취득일

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal acquisitionCost; // 취득원가

    @Column(nullable = false)
    private Integer usefulLife; // 내용연수 (년)

    @Column(length = 20)
    private String depreciationMethod; // STRAIGHT_LINE(정액법), DECLINING(정률법)

    @Column(precision = 19, scale = 2)
    private BigDecimal residualValue = BigDecimal.ZERO; // 잔존가치

    @Column(precision = 19, scale = 2)
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO; // 현재까지의 감가상각누계액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBookValue; // 현재 장부가액 (취득원가 - 감가상각누계액)

    @Column(precision = 19, scale = 2)
    private BigDecimal depreciationAmountPerPeriod; // 기간별 감가상각액

    @Column
    private LocalDate lastDepreciationDate; // 마지막 감가상각 처리일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_code", referencedColumnName = "code")
    private Department department; // 관리 부서

    @Column(length = 20)
    private String status; // ACTIVE(사용중), DISPOSED(처분), FULLY_DEPRECIATED(상각완료)

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "ACTIVE";
        }
        if (currentBookValue == null) {
            currentBookValue = acquisitionCost;
        }
        if (depreciationAmountPerPeriod == null && acquisitionCost != null && usefulLife != null) {
            // Calculate depreciationAmountPerPeriod based on method
            calculateDepreciationAmountPerPeriod();
        }
        // lastDepreciationDate should be null initially
    }

    // Helper method to calculate depreciationAmountPerPeriod
    private void calculateDepreciationAmountPerPeriod() {
        if ("STRAIGHT_LINE".equals(depreciationMethod)) {
            if (usefulLife != null && usefulLife > 0) {
                BigDecimal depreciableAmount = acquisitionCost.subtract(residualValue);
                // Monthly depreciation for straight-line
                this.depreciationAmountPerPeriod = depreciableAmount.divide(new BigDecimal(usefulLife * 12), 2, BigDecimal.ROUND_HALF_UP);
            } else {
                this.depreciationAmountPerPeriod = BigDecimal.ZERO;
            }
        } else if ("DECLINING".equals(depreciationMethod)) {
            // For declining balance, depreciation is calculated each period on the book value.
            // So, depreciationAmountPerPeriod will be calculated dynamically in the service.
            this.depreciationAmountPerPeriod = BigDecimal.ZERO; // Initialize as zero, service will calculate
        } else {
            this.depreciationAmountPerPeriod = BigDecimal.ZERO;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public AccountSubject getAccountSubject() {
        return accountSubject;
    }

    public void setAccountSubject(AccountSubject accountSubject) {
        this.accountSubject = accountSubject;
    }

    public AccountSubject getAccumulatedAccount() {
        return accumulatedAccount;
    }

    public void setAccumulatedAccount(AccountSubject accumulatedAccount) {
        this.accumulatedAccount = accumulatedAccount;
    }

    public AccountSubject getExpenseAccount() {
        return expenseAccount;
    }

    public void setExpenseAccount(AccountSubject expenseAccount) {
        this.expenseAccount = expenseAccount;
    }

    public LocalDate getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(LocalDate acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public BigDecimal getAcquisitionCost() {
        return acquisitionCost;
    }

    public void setAcquisitionCost(BigDecimal acquisitionCost) {
        this.acquisitionCost = acquisitionCost;
    }

    public Integer getUsefulLife() {
        return usefulLife;
    }

    public void setUsefulLife(Integer usefulLife) {
        this.usefulLife = usefulLife;
    }

    public String getDepreciationMethod() {
        return depreciationMethod;
    }

    public void setDepreciationMethod(String depreciationMethod) {
        this.depreciationMethod = depreciationMethod;
    }

    public BigDecimal getResidualValue() {
        return residualValue;
    }

    public void setResidualValue(BigDecimal residualValue) {
        this.residualValue = residualValue;
    }

    public BigDecimal getAccumulatedDepreciation() {
        return accumulatedDepreciation;
    }

    public void setAccumulatedDepreciation(BigDecimal accumulatedDepreciation) {
        this.accumulatedDepreciation = accumulatedDepreciation;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public BigDecimal getCurrentBookValue() {
        return currentBookValue;
    }

    public void setCurrentBookValue(BigDecimal currentBookValue) {
        this.currentBookValue = currentBookValue;
    }

    public BigDecimal getDepreciationAmountPerPeriod() {
        return depreciationAmountPerPeriod;
    }

    public void setDepreciationAmountPerPeriod(BigDecimal depreciationAmountPerPeriod) {
        this.depreciationAmountPerPeriod = depreciationAmountPerPeriod;
    }

    public LocalDate getLastDepreciationDate() {
        return lastDepreciationDate;
    }

    public void setLastDepreciationDate(LocalDate lastDepreciationDate) {
        this.lastDepreciationDate = lastDepreciationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
