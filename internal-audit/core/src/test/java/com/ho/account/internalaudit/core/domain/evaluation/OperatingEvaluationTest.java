package com.ho.account.internalaudit.core.domain.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OperatingEvaluationTest {

    @ParameterizedTest
    @CsvSource(value = {
            "-1,-2", "-1,0", "0,-1", "-1,NULL", "NULL,-1", "10,11", "0,1",
            "-2147483648,NULL", "NULL,-2147483648", "2147483646,2147483647"
    }, nullValues = "NULL")
    void constructorRejectsImpossibleCounts(Integer sampleSize, Integer exceptionCount) {
        assertThatThrownBy(() -> evaluation(sampleSize, exceptionCount, "EFFECTIVE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "-1,-2", "-1,0", "0,-1", "-1,NULL", "NULL,-1", "10,11", "0,1",
            "-2147483648,NULL", "NULL,-2147483648", "2147483646,2147483647"
    }, nullValues = "NULL")
    void builderCannotBypassCountValidation(Integer sampleSize, Integer exceptionCount) {
        assertThatThrownBy(() -> OperatingEvaluation.builder()
                .sampleSize(sampleSize).exceptionCount(exceptionCount).result("EFFECTIVE").build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "25,2", "10,10", "10,0", "0,0", "NULL,NULL", "0,NULL", "NULL,0",
            "10,NULL", "NULL,10", "2147483647,2147483647", "2147483647,0",
            "2147483647,NULL", "NULL,2147483647"
    }, nullValues = "NULL")
    void preservesAllowedCountsWithoutInferringUnspecifiedValues(Integer sampleSize, Integer exceptionCount) {
        OperatingEvaluation direct = evaluation(sampleSize, exceptionCount, " ineffective ");
        OperatingEvaluation built = direct.toBuilder().result(" effective ").build();

        // Null counts carry unspecified information; accepting them must not silently turn them into zero.
        assertThat(direct.sampleSize()).isEqualTo(sampleSize);
        assertThat(direct.exceptionCount()).isEqualTo(exceptionCount);
        assertThat(direct.result()).isEqualTo("INEFFECTIVE");
        assertThat(built.sampleSize()).isEqualTo(sampleSize);
        assertThat(built.exceptionCount()).isEqualTo(exceptionCount);
        assertThat(built.result()).isEqualTo("EFFECTIVE");
    }

    private OperatingEvaluation evaluation(Integer sampleSize, Integer exceptionCount, String result) {
        return new OperatingEvaluation("op-1", "ctrl-1", "auditor", "2026-09-11",
                sampleSize, exceptionCount, List.of(), result, null);
    }
}
