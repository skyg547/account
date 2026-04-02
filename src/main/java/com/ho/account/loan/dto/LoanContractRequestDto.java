package com.ho.account.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class LoanContractRequestDto {

    private Long id; // 수정 시나리오용

    @NotBlank(message = "대출 계약 번호는 필수입니다.")
    private String loanContractNo;

    @NotBlank(message = "거래처 코드는 필수입니다.")
    private String businessPartnerCode;

    @NotBlank(message = "대출 상품명은 필수입니다.")
    private String loanProduct;

    @NotNull(message = "원금은 필수입니다.")
    @DecimalMin(value = "0.0", inclusive = false, message = "원금은 0보다 커야 합니다.")
    private BigDecimal principalAmount;

    @NotNull(message = "실행일은 필수입니다.")
    private LocalDate disbursementDate;

    @NotNull(message = "만기일은 필수입니다.")
    private LocalDate maturityDate;

    @NotNull(message = "이자율은 필수입니다.")
    @DecimalMin(value = "0.0", message = "이자율은 0 이상이어야 합니다.")
    private BigDecimal interestRate;

    @NotBlank(message = "상환 방식은 필수입니다.")
    private String repaymentMethod;

    private String status; // ACTIVE, PAID_OFF, DEFAULT

    @NotNull(message = "현재 원금 잔액은 필수입니다.")
    @DecimalMin(value = "0.0", message = "현재 원금 잔액은 0 이상이어야 합니다.")
    private BigDecimal currentPrincipalBalance;

    @NotNull(message = "이연 대출 수수료는 필수입니다.")
    @DecimalMin(value = "0.0", message = "이연 대출 수수료는 0 이상이어야 합니다.")
    private BigDecimal deferredLoanFee;

    // effectiveInterestRate는 서비스에서 계산되므로 요청에서는 받지 않음.

    // Getter 및 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLoanContractNo() {
        return loanContractNo;
    }

    public void setLoanContractNo(String loanContractNo) {
        this.loanContractNo = loanContractNo;
    }

    public String getBusinessPartnerCode() {
        return businessPartnerCode;
    }

    public void setBusinessPartnerCode(String businessPartnerCode) {
        this.businessPartnerCode = businessPartnerCode;
    }

    public String getLoanProduct() {
        return loanProduct;
    }

    public void setLoanProduct(String loanProduct) {
        this.loanProduct = loanProduct;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public LocalDate getDisbursementDate() {
        return disbursementDate;
    }

    public void setDisbursementDate(LocalDate disbursementDate) {
        this.disbursementDate = disbursementDate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public String getRepaymentMethod() {
        return repaymentMethod;
    }

    public void setRepaymentMethod(String repaymentMethod) {
        this.repaymentMethod = repaymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getCurrentPrincipalBalance() {
        return currentPrincipalBalance;
    }

    public void setCurrentPrincipalBalance(BigDecimal currentPrincipalBalance) {
        this.currentPrincipalBalance = currentPrincipalBalance;
    }

    public BigDecimal getDeferredLoanFee() {
        return deferredLoanFee;
    }

    public void setDeferredLoanFee(BigDecimal deferredLoanFee) {
        this.deferredLoanFee = deferredLoanFee;
    }
}
