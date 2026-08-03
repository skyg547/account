package com.ho.account.ecl.batch;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = AllowanceEclBatchApplication.class)
@ActiveProfiles("local")
class AllowanceEclBatchLocalSchemaContextTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    void localH2BaselineAndBatchMetadataCoexist() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(tableExists(jdbc, "ALLOWANCE_ACCOUNT_MAPPINGS")).isTrue();
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
