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

    // --- IFRS 16 Fields ---

    /**
     * IFRS 16 적용 대상 여부. true일 경우 사용권자산과 리스부채를 인식합니다.
     */
    @Column(nullable = false)
    private boolean ifrs16Applicable = false;

    /**
     * 단기 리스 예외 적용 여부. (IFRS 16.5(a))
     */
    @Column(nullable = false)
    private boolean shortTermLease = false;

    /**
     * 소액 기초자산 리스 예외 적용 여부. (IFRS 16.5(b))
     */
    @Column(nullable = false)
    private boolean lowValueLease = false;

    /**
     * 리스 내재이자율 또는 리스이용자의 증분차입이자율. 리스부채 현재가치 평가에 사용됩니다.
     */
    @Column(precision = 5, scale = 4)
    private BigDecimal discountRate;

    /**
     * 최초 인식 시점의 사용권자산 가치.
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal initialRightOfUseAssetValue;

    /**
     * 최초 인식 시점의 리스부채 가치.
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal initialLeaseLiabilityValue;


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

    // Getter 및 Setter
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

    public boolean isIfrs16Applicable() {
        return ifrs16Applicable;
    }

    public void setIfrs16Applicable(boolean ifrs16Applicable) {
        this.ifrs16Applicable = ifrs16Applicable;
    }

    public boolean isShortTermLease() {
        return shortTermLease;
    }

    public void setShortTermLease(boolean shortTermLease) {
        this.shortTermLease = shortTermLease;
    }

    public boolean isLowValueLease() {
        return lowValueLease;
    }

    public void setLowValueLease(boolean lowValueLease) {
        this.lowValueLease = lowValueLease;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public BigDecimal getInitialRightOfUseAssetValue() {
        return initialRightOfUseAssetValue;
    }

    public void setInitialRightOfUseAssetValue(BigDecimal initialRightOfUseAssetValue) {
        this.initialRightOfUseAssetValue = initialRightOfUseAssetValue;
    }

    public BigDecimal getInitialLeaseLiabilityValue() {
        return initialLeaseLiabilityValue;
    }

    public void setInitialLeaseLiabilityValue(BigDecimal initialLeaseLiabilityValue) {
        this.initialLeaseLiabilityValue = initialLeaseLiabilityValue;
    }
}
