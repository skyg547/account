package com.ho.account.expenditure.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/**
 * 부서/계정별 예산 엔티티입니다.
 *
 * 초보자용 설명:
 * 예산은 특정 월(`yearMonth`)에 특정 부서(`deptCode`)가 특정 계정(`accountCode`)으로 쓸 수 있는 한도입니다.
 * master-data 엔티티를 직접 물고 있지 않고 코드만 저장하므로, 기준정보의 실제 이름/상태는 Port를 통해 따로 확인합니다.
 */
@Entity
@Table(name = "budgets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"yearMonth", "dept_code", "account_code"})
})
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 6)
    private String yearMonth; // YYYYMM

    @Column(name = "dept_code", nullable = false, length = 50)
    private String deptCode;

    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal assignedAmount = BigDecimal.ZERO; // 배정 예산

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal usedAmount = BigDecimal.ZERO; // 사용 예산

    @PrePersist
    void prePersist() {
        if (assignedAmount == null) {
            assignedAmount = BigDecimal.ZERO;
        }
        if (usedAmount == null) {
            usedAmount = BigDecimal.ZERO;
        }
    }

    public BigDecimal getRemainingAmount() {
        return assignedAmount.subtract(usedAmount);
    }

    public void useBudget(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("사용 예산 금액은 0보다 커야 합니다.");
        }
        if (getRemainingAmount().compareTo(amount) < 0) {
            throw new IllegalStateException("예산이 부족합니다. 잔여금액: " + getRemainingAmount());
        }
        this.usedAmount = this.usedAmount.add(amount);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }
    public String getDeptCode() { return deptCode; }
    public void setDeptCode(String deptCode) { this.deptCode = deptCode; }
    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
    public BigDecimal getAssignedAmount() { return assignedAmount; }
    public void setAssignedAmount(BigDecimal assignedAmount) { this.assignedAmount = assignedAmount; }
    public BigDecimal getUsedAmount() { return usedAmount; }
    public void setUsedAmount(BigDecimal usedAmount) { this.usedAmount = usedAmount; }
}