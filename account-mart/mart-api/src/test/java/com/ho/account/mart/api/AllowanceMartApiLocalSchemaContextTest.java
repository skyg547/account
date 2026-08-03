package com.ho.account.mart.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = AllowanceMartApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
class AllowanceMartApiLocalSchemaContextTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    void localH2BaselineMigratesAndJpaValidates() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(tableExists(jdbc, "ODS_ACC_MST")).isTrue();
        assertThat(tableExists(jdbc, "ALLOWANCE_EXPOSURE_SNAPSHOTS")).isTrue();
        assertThat(tableExists(jdbc, "MARKET_YIELD_CURVE_POINT")).isTrue();
    }

    @Test
    void nullableRepositoryKeysHaveProductionEquivalentUniqueness() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        jdbc.update("insert into market_rate(base_dt, rate_name, rate) values (DATE '2026-04-30', 'BASE', 0.01)");
        assertThatThrownBy(() -> jdbc.update(
                "insert into market_rate(base_dt, rate_name, rate) values (DATE '2026-04-30', 'BASE', 0.02)"))
                .isInstanceOf(RuntimeException.class);

        jdbc.update("insert into market_yield_curve(base_dt) values (DATE '2026-04-30')");
        assertThatThrownBy(() -> jdbc.update(
                "insert into market_yield_curve(base_dt) values (DATE '2026-04-30')"))
                .isInstanceOf(RuntimeException.class);

        jdbc.update("insert into market_yield_curve_point(tenor_label) values ('ON')");
        assertThatThrownBy(() -> jdbc.update(
                "insert into market_yield_curve_point(tenor_label) values ('TN')"))
                .isInstanceOf(RuntimeException.class);
    }

    private boolean tableExists(JdbcTemplate jdbc, String tableName) {
        Integer count = jdbc.queryForObject(
                "select count(*) from information_schema.tables where upper(table_name) = ?",
                Integer.class,
                tableName);
        return count != null && count == 1;
    }
}
