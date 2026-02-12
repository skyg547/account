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
        if (status == null)
            status = "ACTIVE";
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
