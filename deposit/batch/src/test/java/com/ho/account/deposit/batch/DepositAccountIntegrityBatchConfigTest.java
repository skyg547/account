package com.ho.account.deposit.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

class DepositAccountIntegrityBatchConfigTest {

    @Test
    @DisplayName("asOfDate JobParameter는 재실행 기준일이므로 반드시 명시해야 한다")
    void requiredLocalDateRejectsMissingAsOfDate() {
        JobParameters parameters = new JobParameters();

        assertThatThrownBy(() -> DepositAccountIntegrityBatchConfig.requiredLocalDate(parameters, "asOfDate"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asOfDate job parameter is required");
    }

    @Test
    @DisplayName("asOfDate JobParameter는 yyyy-MM-dd 형식으로 해석한다")
    void requiredLocalDateParsesIsoDate() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("asOfDate", "2026-06-19")
                .toJobParameters();

        assertThat(DepositAccountIntegrityBatchConfig.requiredLocalDate(parameters, "asOfDate"))
                .isEqualTo(LocalDate.of(2026, 6, 19));
    }

    @Test
    @DisplayName("asOfDate 형식이 틀리면 배치를 시작하지 않는다")
    void requiredLocalDateRejectsInvalidFormat() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("asOfDate", "20260619")
                .toJobParameters();

        assertThatThrownBy(() -> DepositAccountIntegrityBatchConfig.requiredLocalDate(parameters, "asOfDate"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yyyy-MM-dd");
    }
}