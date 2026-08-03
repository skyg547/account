package com.ho.account.ecl.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.ecl.api.controller.AllowanceBatchController;
import com.ho.account.ecl.api.infrastructure.messaging.CdmDataReadyConsumer;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = AllowanceEclApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
class AllowanceEclApiLocalSchemaContextTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void localH2BaselineMigratesAndJpaValidates() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(tableExists(jdbc, "ALLOWANCE_ECL_RESULTS")).isTrue();
        assertThat(tableExists(jdbc, "TRANSITION_MATRIX")).isTrue();
        assertThat(tableExists(jdbc, "ALLOWANCE_SUMMARY")).isTrue();
        assertThat(applicationContext.getBeansOfType(AllowanceBatchController.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(CdmDataReadyConsumer.class)).hasSize(1);
    }

    private boolean tableExists(JdbcTemplate jdbc, String tableName) {
        Integer count = jdbc.queryForObject(
                "select count(*) from information_schema.tables where upper(table_name) = ?",
                Integer.class,
                tableName);
        return count != null && count == 1;
    }
}
