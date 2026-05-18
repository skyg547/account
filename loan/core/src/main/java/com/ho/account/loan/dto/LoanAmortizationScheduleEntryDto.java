package com.ho.account.loan.dto;

import com.ho.account.loan.domain.LoanAmortizationScheduleEntry;
import java.math.BigDecimal;
import java.time.LocalDate;

public class LoanAmortizationScheduleEntryDto {

    private Long id;
    private Long loanId;
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

    public LoanAmortizationScheduleEntryDto(Long id, Long loanId, LocalDate paymentDate, Integer periodNumber, BigDecimal startingBalance, BigDecimal scheduledPaymentAmount, BigDecimal interestAmount, BigDecimal principalAmount, BigDecimal endingBalance, BigDecimal deferredFeeAmortization, String entryType) {
        this.id = id;
        this.loanId = loanId;
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
        Long loanId = null;
        if (entry.getLoan() != null) {
            loanId = entry.getLoan().getId();
        }
        return new LoanAmortizationScheduleEntryDto(
                entry.getId(),
                loanId,
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

    // Getter
    public Long getId() {
        return id;
    }

    public Long getLoanId() {
        return loanId;
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
