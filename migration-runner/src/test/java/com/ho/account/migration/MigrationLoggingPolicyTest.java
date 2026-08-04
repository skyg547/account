package com.ho.account.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MigrationLoggingPolicyTest {

    @Test
    void flywayInfoLoggingIsDisabledSoJdbcUrlsAreNotPrinted() {
        Logger logger = LoggerFactory.getLogger("org.flywaydb");

        assertThat(logger.isErrorEnabled()).isFalse();
        assertThat(logger.isWarnEnabled()).isFalse();
        assertThat(logger.isInfoEnabled()).isFalse();
    }
}
