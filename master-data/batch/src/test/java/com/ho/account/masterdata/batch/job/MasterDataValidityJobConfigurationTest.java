package com.ho.account.masterdata.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * The business date is an identifying restart parameter, so its fail-fast
 * behavior is tested without starting Spring or touching a database.
 */
class MasterDataValidityJobConfigurationTest {

    @Test
    void parsesRequiredBusinessDate() {
        assertThat(MasterDataValidityJobConfiguration.requireAsOfDate("2026-07-29"))
                .isEqualTo(LocalDate.of(2026, 7, 29));
    }

    @Test
    void rejectsMissingOrMalformedBusinessDate() {
        assertThatThrownBy(() -> MasterDataValidityJobConfiguration.requireAsOfDate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
        assertThatThrownBy(() -> MasterDataValidityJobConfiguration.requireAsOfDate("29-07-2026"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yyyy-MM-dd");
    }
}
