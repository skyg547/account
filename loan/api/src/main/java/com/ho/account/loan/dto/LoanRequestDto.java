package com.ho.account.loan.dto;

// HTTP 요청 계약은 loan:api 인바운드 어댑터가 소유합니다.

import com.ho.account.loan.domain.Loan;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    @Positive
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
    @DecimalMax(value = "1.0", inclusive = true)
    private BigDecimal interestRate;

    @NotNull
    private LocalDate disbursalDate;

    @NotNull
    private LocalDate maturityDate;

    @NotNull
    private Loan.PaymentFrequency paymentFrequency;

    public Loan toEntity() {
        return Loan.create(
                loanNumber,
                businessPartnerId,
                currencyCode,
                loanType,
                principalAmount,
                interestRate,
                disbursalDate,
                maturityDate,
                paymentFrequency,
                "SYSTEM");
    }
}
