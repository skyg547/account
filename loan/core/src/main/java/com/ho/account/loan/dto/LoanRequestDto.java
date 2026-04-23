package com.ho.account.loan.dto;

import com.ho.account.loan.domain.Loan;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 대출 (Loan) 요청 DTO
 */
@Data
public class LoanRequestDto {
    @NotBlank
    @Size(max = 50)
    private String loanNumber;

    @NotNull
    private Long businessPartnerId;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currencyCode;

    @NotNull
    private Loan.LoanType loanType;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal principalAmount;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal interestRate;

    @NotNull
    private LocalDate disbursalDate;

    @NotNull
    private LocalDate maturityDate;

    @NotNull
    private Loan.PaymentFrequency paymentFrequency;

    private BigDecimal initialEIR; // 선택값이며 서비스에서 계산 가능

    public Loan toEntity() {
        Loan loan = new Loan();
        loan.setLoanNumber(this.loanNumber);
        // BusinessPartner와 Currency는 서비스에서 설정
        loan.setLoanType(this.loanType);
        loan.setPrincipalAmount(this.principalAmount);
        loan.setInterestRate(this.interestRate);
        loan.setDisbursalDate(this.disbursalDate);
        loan.setMaturityDate(this.maturityDate);
        loan.setPaymentFrequency(this.paymentFrequency);
        loan.setInitialEIR(this.initialEIR);
        return loan;
    }
}
