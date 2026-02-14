package com.ho.account.asset.dto;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LeaseContractRequest {

    @NotNull
    @Size(min = 1, max = 20)
    private String contractNo;

    @NotNull
    @Size(min = 1, max = 100)
    private String contractName;

    private String lessorBusinessPartnerCode; // 리스 제공자 (거래처 코드)

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal monthlyPayment; // 월 리스료

    @NotNull
    private Integer paymentDay; // 매월 지급일 (예: 25일)

    private String departmentCode; // 관리 부서 코드

    private String expenseAccountCode; // 리스료 비용 계정 코드

    private String status; // ACTIVE, TERMINATED, EXPIRED

    // IFRS 16 Fields
    private boolean ifrs16Applicable = false;
    private boolean shortTermLease = false;
    private boolean lowValueLease = false;

    @DecimalMin(value = "0.00")
    private BigDecimal discountRate;

    @DecimalMin(value = "0.00")
    private BigDecimal initialRightOfUseAssetValue;

    @DecimalMin(value = "0.00")
    private BigDecimal initialLeaseLiabilityValue;

    // Getters and Setters
    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public String getContractName() {
        return contractName;
    }

    public void setName(String contractName) {
        this.contractName = contractName;
    }

    public String getLessorBusinessPartnerCode() {
        return lessorBusinessPartnerCode;
    }

    public void setLessorBusinessPartnerCode(String lessorBusinessPartnerCode) {
        this.lessorBusinessPartnerCode = lessorBusinessPartnerCode;
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

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getExpenseAccountCode() {
        return expenseAccountCode;
    }

    public void setExpenseAccountCode(String expenseAccountCode) {
        this.expenseAccountCode = expenseAccountCode;
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
