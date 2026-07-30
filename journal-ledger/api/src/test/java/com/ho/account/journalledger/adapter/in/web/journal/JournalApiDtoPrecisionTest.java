package com.ho.account.journalledger.adapter.in.web.journal;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JournalApiDtoPrecisionTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("HTTP 경계도 원장 DECIMAL(19,2)보다 정밀한 금액을 400 검증 대상으로 분류한다.")
    void rejectsFractionalCentBeforeDomainMapping() {
        JournalApiDto.LineRequest request = new JournalApiDto.LineRequest(
                JournalSide.DEBIT,
                "10100",
                new BigDecimal("10.001"),
                new BigDecimal("10.00"),
                null,
                null,
                "fractional cent");

        assertThat(validator.validate(request))
                .anySatisfy(violation ->
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("amount"));
    }
}
