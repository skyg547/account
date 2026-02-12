package com.ho.account.loan.dto;

import com.ho.account.loan.domain.LoanContract;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class LoanContractDto {

    private Long id;
    private String loanContractNo;
    private String businessPartnerCode;
    private String businessPartnerName;
    private String loanProduct;
    private BigDecimal principalAmount;
    private LocalDate disbursementDate;
    private LocalDate maturityDate;
    private BigDecimal interestRate;
    private String repaymentMethod;
    private String status;
    private BigDecimal currentPrincipalBalance;
    private BigDecimal deferredLoanFee;
    private BigDecimal effectiveInterestRate;
    private BigDecimal totalInterestPaid;
    private BigDecimal totalPrincipalPaid;
    private List<LoanAmortizationScheduleEntryDto> amortizationSchedule;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private String auditUser;

    public LoanContractDto() {
    }

    public LoanContractDto(Long id, String loanContractNo, String businessPartnerCode, String businessPartnerName, String loanProduct, BigDecimal principalAmount, LocalDate disbursementDate, LocalDate maturityDate, BigDecimal interestRate, String repaymentMethod, String status, BigDecimal currentPrincipalBalance, BigDecimal deferredLoanFee, BigDecimal effectiveInterestRate, BigDecimal totalInterestPaid, BigDecimal totalPrincipalPaid, List<LoanAmortizationScheduleEntryDto> amortizationSchedule, LocalDateTime createDate, LocalDateTime updateDate, String auditUser) {
        this.id = id;
        this.loanContractNo = loanContractNo;
        this.businessPartnerCode = businessPartnerCode;
        this.businessPartnerName = businessPartnerName;
        this.loanProduct = loanProduct;
        this.principalAmount = principalAmount;
        this.disbursementDate = disbursementDate;
        this.maturityDate = maturityDate;
        this.interestRate = interestRate;
        this.repaymentMethod = repaymentMethod;
        this.status = status;
        this.currentPrincipalBalance = currentPrincipalBalance;
        this.deferredLoanFee = deferredLoanFee;
        this.effectiveInterestRate = effectiveInterestRate;
        this.totalInterestPaid = totalInterestPaid;
        this.totalPrincipalPaid = totalPrincipalPaid;
        this.amortizationSchedule = amortizationSchedule;
        this.createDate = createDate;
        this.updateDate = updateDate;
        this.auditUser = auditUser;
    }

    public static LoanContractDto fromEntity(LoanContract loanContract) {
        String bpCode = null;
        String bpName = null;
        if (loanContract.getBusinessPartner() != null) {
            bpCode = loanContract.getBusinessPartner().getBusinessPartnerCode();
            bpName = loanContract.getBusinessPartner().getBusinessPartnerName();
        }

        List<LoanAmortizationScheduleEntryDto> scheduleDtos = loanContract.getAmortizationSchedule().stream()
                .map(LoanAmortizationScheduleEntryDto::fromEntity)
                .collect(Collectors.toList());

        return new LoanContractDto(
                loanContract.getId(),
                loanContract.getLoanContractNo(),
                bpCode,
                bpName,
                loanContract.getLoanProduct(),
                loanContract.getPrincipalAmount(),
                loanContract.getDisbursementDate(),
                loanContract.getMaturityDate(),
                loanContract.getInterestRate(),
                loanContract.getRepaymentMethod(),
                loanContract.getStatus(),
                loanContract.getCurrentPrincipalBalance(),
                loanContract.getDeferredLoanFee(),
                loanContract.getEffectiveInterestRate(),
                loanContract.getTotalInterestPaid(),
                loanContract.getTotalPrincipalPaid(),
                scheduleDtos,
                loanContract.getCreateDate(),
                loanContract.getUpdateDate(),
                loanContract.getAuditUser()
        );
    }

    // Getters (Setters are generally not needed for DTOs unless for specific use cases like deserialization)
    public Long getId() {
        return id;
    }

    public String getLoanContractNo() {
        return loanContractNo;
    }

    public String getBusinessPartnerCode() {
        return businessPartnerCode;
    }

    public String getBusinessPartnerName() {
        return businessPartnerName;
    }

    public String getLoanProduct() {
        return loanProduct;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public LocalDate getDisbursementDate() {
        return disbursementDate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public String getRepaymentMethod() {
        return repaymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getCurrentPrincipalBalance() {
        return currentPrincipalBalance;
    }

    public BigDecimal getDeferredLoanFee() {
        return deferredLoanFee;
    }

    public BigDecimal getEffectiveInterestRate() {
        return effectiveInterestRate;
    }

    public BigDecimal getTotalInterestPaid() {
        return totalInterestPaid;
    }

    public BigDecimal getTotalPrincipalPaid() {
        return totalPrincipalPaid;
    }

    public List<LoanAmortizationScheduleEntryDto> getAmortizationSchedule() {
        return amortizationSchedule;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public String getAuditUser() {
        return auditUser;
    }
}
