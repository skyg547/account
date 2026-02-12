package com.ho.account.loan.dto;

import com.ho.account.loan.domain.LoanAmortizationScheduleEntry;
import java.math.BigDecimal;
import java.time.LocalDate;

public class LoanAmortizationScheduleEntryDto {

    private Long id;
    private Long loanContractId;
    private LocalDate paymentDate;
    private Integer periodNumber;
    private BigDecimal startingBalance;
    private BigDecimal scheduledPaymentAmount;
    private BigDecimal interestAmount;
    private BigDecimal principalAmount;
    private BigDecimal endingBalance;
    private BigDecimal deferredFeeAmortization;
    private String entryType;

    public LoanAmortizationScheduleEntryDto() {
    }

    public LoanAmortizationScheduleEntryDto(Long id, Long loanContractId, LocalDate paymentDate, Integer periodNumber, BigDecimal startingBalance, BigDecimal scheduledPaymentAmount, BigDecimal interestAmount, BigDecimal principalAmount, BigDecimal endingBalance, BigDecimal deferredFeeAmortization, String entryType) {
        this.id = id;
        this.loanContractId = loanContractId;
        this.paymentDate = paymentDate;
        this.periodNumber = periodNumber;
        this.startingBalance = startingBalance;
        this.scheduledPaymentAmount = scheduledPaymentAmount;
        this.interestAmount = interestAmount;
        this.principalAmount = principalAmount;
        this.endingBalance = endingBalance;
        this.deferredFeeAmortization = deferredFeeAmortization;
        this.entryType = entryType;
    }

    public static LoanAmortizationScheduleEntryDto fromEntity(LoanAmortizationScheduleEntry entry) {
        Long loanContractId = null;
        if (entry.getLoanContract() != null) {
            loanContractId = entry.getLoanContract().getId();
        }
        return new LoanAmortizationScheduleEntryDto(
                entry.getId(),
                loanContractId,
                entry.getPaymentDate(),
                entry.getPeriodNumber(),
                entry.getStartingBalance(),
                entry.getScheduledPaymentAmount(),
                entry.getInterestAmount(),
                entry.getPrincipalAmount(),
                entry.getEndingBalance(),
                entry.getDeferredFeeAmortization(),
                entry.getEntryType()
        );
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getLoanContractId() {
        return loanContractId;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public Integer getPeriodNumber() {
        return periodNumber;
    }

    public BigDecimal getStartingBalance() {
        return startingBalance;
    }

    public BigDecimal getScheduledPaymentAmount() {
        return scheduledPaymentAmount;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public BigDecimal getEndingBalance() {
        return endingBalance;
    }

    public BigDecimal getDeferredFeeAmortization() {
        return deferredFeeAmortization;
    }

    public String getEntryType() {
        return entryType;
    }
}
