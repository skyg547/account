package com.ho.account.asset.domain;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lease_contracts")
public class LeaseContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String contractNo;

    @Column(nullable = false, length = 100)
    private String contractName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_code", referencedColumnName = "businessPartnerCode")
    private BusinessPartner lessor; // 리스 제공자 (거래처)

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyPayment; // 월 리스료

    @Column(nullable = false)
    private Integer paymentDay; // 매월 지급일 (예: 25일)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_code", referencedColumnName = "code")
    private Department department; // 관리 부서

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_code")
    private AccountSubject expenseAccount; // 리스료 비용 계정 (예: 지급임차료)

    @Column(length = 20)
    private String status; // ACTIVE, TERMINATED, EXPIRED

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

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public BusinessPartner getLessor() {
        return lessor;
    }

    public void setLessor(BusinessPartner lessor) {
        this.lessor = lessor;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getMonthlyPayment() {
        return monthlyPayment;
    }

    public void setMonthlyPayment(BigDecimal monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }

    public Integer getPaymentDay() {
        return paymentDay;
    }

    public void setPaymentDay(Integer paymentDay) {
        this.paymentDay = paymentDay;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public AccountSubject getExpenseAccount() {
        return expenseAccount;
    }

    public void setExpenseAccount(AccountSubject expenseAccount) {
        this.expenseAccount = expenseAccount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
