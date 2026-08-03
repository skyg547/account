package com.ho.account.mart.batch;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = AllowanceMartBatchApplication.class)
@ActiveProfiles("local")
class AllowanceMartBatchLocalSchemaContextTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    void localH2BaselineAndBatchMetadataCoexist() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(tableExists(jdbc, "ALLOWANCE_INPUT_POSITIONS")).isTrue();
        assertThat(tableExists(jdbc, "BATCH_JOB_INSTANCE")).isTrue();
    }

    private boolean tableExists(JdbcTemplate jdbc, String tableName) {
        Integer count = jdbc.queryForObject(
                "select count(*) from information_schema.tables where upper(table_name) = ?",
                Integer.class,
                tableName);
        return count != null && count == 1;
    }
}
