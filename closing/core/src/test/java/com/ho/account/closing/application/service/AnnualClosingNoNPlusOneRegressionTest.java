package com.ho.account.closing.application.service;

import com.ho.account.contracts.journal.JournalQueryPort;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Compiles on the audited service and fails while it retains per-journal reads. */
class AnnualClosingNoNPlusOneRegressionTest {
    @Test
    void annualUseCaseCannotRetainPerJournalDetailQueryDependency() {
        assertThat(Arrays.stream(AnnualClosingService.class.getDeclaredFields())
                .map(Field::getType))
                .doesNotContain(JournalQueryPort.class);
    }
}
