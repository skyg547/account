package com.ho.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=local",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "management.tracing.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:master_flyway;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.flyway.enabled=true",
                "spring.flyway.baseline-on-migrate=true",
                "spring.flyway.baseline-version=0"
        })
class MasterDataFlywayApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesForwardChangeRequestMigrationsWithoutHibernateSchemaRepair() {
        assertThatCode(() -> jdbcTemplate.queryForList(
                "SELECT payload_json, lock_version, source_reference, applied_at "
                        + "FROM master_data_change_requests WHERE 1 = 0"))
                .doesNotThrowAnyException();
    }
}
