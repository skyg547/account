package com.ho.account.expenditure.domain;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.Department;
import jakarta.persistence.*;
import java.math.BigDecimal;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_code", referencedColumnName = "deptCode")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_code")
    private AccountSubject accountSubject;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal assignedAmount = BigDecimal.ZERO; // 배정 예산

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal usedAmount = BigDecimal.ZERO; // 사용 예산

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public AccountSubject getAccountSubject() { return accountSubject; }
    public void setAccountSubject(AccountSubject accountSubject) { this.accountSubject = accountSubject; }

    public BigDecimal getAssignedAmount() { return assignedAmount; }
    public void setAssignedAmount(BigDecimal assignedAmount) { this.assignedAmount = assignedAmount; }

    public BigDecimal getUsedAmount() { return usedAmount; }
    public void setUsedAmount(BigDecimal usedAmount) { this.usedAmount = usedAmount; }

    // 비즈니스 로직
    public BigDecimal getRemainingAmount() {
        return assignedAmount.subtract(usedAmount);
    }

    public void useBudget(BigDecimal amount) {
        if (getRemainingAmount().compareTo(amount) < 0) {
            throw new IllegalStateException("예산이 부족합니다. 잔액: " + getRemainingAmount());
        }
        this.usedAmount = this.usedAmount.add(amount);
    }
}
