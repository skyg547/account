package com.ho.account.closing.infrastructure.source;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ClosingReadOnlySourcesTest {
    @Test
    void sourceConnectionsRemainSeparateReadOnlyAndBoundedWithoutReplacingClosingDataSource() {
        try (var sources = new ClosingReadOnlySources(
                "jdbc:postgresql://journal.invalid/journal", "reader", "test-only",
                "jdbc:postgresql://ecl.invalid/ecl", "reader", "test-only",
                "jdbc:postgresql://master.invalid/master", "reader", "test-only")) {
            HikariDataSource journal = (HikariDataSource) sources.journalDataSource();
            assertThat(journal.isRunning()).isFalse();
            assertThat(journal.isReadOnly()).isTrue();
            assertThat(journal.getConnectionInitSql()).isEqualTo("SET default_transaction_read_only = on");
            assertThat(journal.getMaximumPoolSize()).isEqualTo(1);
            assertThat(sources.journalJdbcTemplate().getDataSource()).isSameAs(journal);
            assertThat(sources.eclJdbcTemplate().getDataSource()).isNotSameAs(journal);
            assertThat(sources.masterDataJdbcTemplate().getDataSource())
                    .isNotSameAs(sources.eclJdbcTemplate().getDataSource());
        }
    }

    @Test
    void invalidConnectionFailsWithoutIncludingConnectionSecrets() {
        assertThatThrownBy(() -> new ClosingReadOnlySources(
                "secret-fixture-url", "reader", "secret-fixture-password",
                "unused", "unused", "unused", "unused", "unused", "unused"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("secret-fixture").hasNoCause();
    }
}
