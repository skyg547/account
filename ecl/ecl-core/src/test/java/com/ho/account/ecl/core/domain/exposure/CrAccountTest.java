package com.ho.account.ecl.core.domain.exposure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CrAccountTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 5, 29);

    @Test
    @DisplayName("만기일이 없으면 기본 잔존만기 2.5년을 사용한다")
    void resolveMaturityYearsUsesDefaultWhenMaturityDateIsMissing() {
        CrAccount account = CrAccount.builder().maturityDate(null).build();

        assertThat(account.resolveMaturityYears(BASE_DATE)).isEqualByComparingTo(new BigDecimal("2.5"));
    }

    @Test
    @DisplayName("만기까지 1년 미만이면 최소 잔존만기 1년을 사용한다")
    void resolveMaturityYearsUsesMinimumOneYear() {
        CrAccount account = CrAccount.builder()
                .maturityDate(BASE_DATE.plusDays(30))
                .build();

        assertThat(account.resolveMaturityYears(BASE_DATE)).isEqualByComparingTo(new BigDecimal("1.0"));
    }

    @Test
    @DisplayName("만기일까지 남은 일수를 연 단위로 환산한다")
    void resolveMaturityYearsByRemainingDays() {
        CrAccount account = CrAccount.builder()
                .maturityDate(BASE_DATE.plusDays(730))
                .build();

        assertThat(account.resolveMaturityYears(BASE_DATE)).isEqualByComparingTo(new BigDecimal("2.0"));
    }
}
