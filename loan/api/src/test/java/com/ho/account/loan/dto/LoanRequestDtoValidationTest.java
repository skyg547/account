package com.ho.account.loan.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.loan.domain.Loan;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LoanRequestDtoValidationTest {

    @Test
    void apiRejectsNonPositivePartnerAndPercentStyleRate() {
        LoanRequestDto request = new LoanRequestDto();
        request.setLoanNumber("LN-INVALID");
        request.setBusinessPartnerId(0L);
        request.setCurrencyCode("KRW");
        request.setLoanType(Loan.LoanType.TERM_LOAN);
        request.setPrincipalAmount(new BigDecimal("1000.00"));
        request.setInterestRate(new BigDecimal("4.50"));
        request.setDisbursalDate(LocalDate.of(2026, 1, 1));
        request.setMaturityDate(LocalDate.of(2027, 1, 1));
        request.setPaymentFrequency(Loan.PaymentFrequency.MONTHLY);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            assertThat(validator.validate(request))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("businessPartnerId", "interestRate");
        }
    }
}
