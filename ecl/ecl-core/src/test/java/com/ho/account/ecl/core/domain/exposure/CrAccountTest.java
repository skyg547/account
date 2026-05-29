package com.ho.account.ecl.core.domain.exposure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrAccountTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 5, 29);

    @Test
    @DisplayName("만기일이 없으면 기본 잔존만기 2.5년을 사용한다")
    void resolveMaturityYearsUsesDefaultWhenMaturityDateIsMissing() {
        CrAccount account = CrAccount.builder().maturityDate(null).build();

        assertEquals(2.5d, account.resolveMaturityYears(BASE_DATE));
    }

    @Test
    @DisplayName("만기까지 1년 미만이면 최소 잔존만기 1년을 사용한다")
    void resolveMaturityYearsUsesMinimumOneYear() {
        CrAccount account = CrAccount.builder()
                .maturityDate(BASE_DATE.plusDays(30))
                .build();

        assertEquals(1.0d, account.resolveMaturityYears(BASE_DATE));
    }

    @Test
    @DisplayName("만기일까지 남은 일수를 연 단위로 환산한다")
    void resolveMaturityYearsByRemainingDays() {
        CrAccount account = CrAccount.builder()
                .maturityDate(BASE_DATE.plusDays(730))
                .build();

        assertEquals(2.0d, account.resolveMaturityYears(BASE_DATE));
    }
}
