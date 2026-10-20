package com.ho.account.journalledger.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.SlipNumberAllocationException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = SlipNumberPersistenceAdapterTest.Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.name=slip-number-persistence-test",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/journal-migration",
        "spring.sql.init.mode=never",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.vault.enabled=false",
        "eureka.client.enabled=false"
})
class SlipNumberPersistenceAdapterTest {
    private static final String DATABASE_NAME = "slip_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:" + DATABASE_NAME
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.schemas", () -> "public");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "public");
    }

    @Autowired private JournalPersistencePort persistence;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void adapterAllocatesFromMigratedSequenceAndFailsClosedOnDatabaseError() {
        long first = persistence.nextSlipNumber();
        assertThat(persistence.nextSlipNumber()).isEqualTo(first + 1);

        jdbc.execute("DROP SEQUENCE journal_slip_no_seq");
        assertThatThrownBy(persistence::nextSlipNumber)
                .isInstanceOf(SlipNumberAllocationException.class)
                .hasMessage("전표번호 채번에 실패했습니다.")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.ho.account.journalledger.infrastructure.persistence")
    @EnableJpaRepositories("com.ho.account.journalledger.infrastructure.persistence.repository")
    @Import(JournalPersistenceAdapter.class)
    static class Application { }
}
