package com.ho.account.journalledger.batch;

import com.ho.account.journalledger.application.port.out.BalanceReaggregationControlPort;
import com.ho.account.journalledger.application.service.ledger.BalanceReaggregationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("local")
@SpringBootTest(
        classes = JournalLedgerBatchApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.batch.job.enabled=false"
)
class JournalLedgerBatchLocalProfileTest {

    @Autowired Environment environment;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired BalanceReaggregationControlPort control;
    @Autowired BalanceReaggregationService reaggregationService;

    @Test
    void actualLocalBatchContextMigratesV15AndExecutesOwnerLifecycle() {
        assertThat(environment.getProperty("spring.datasource.url"))
                .startsWith("jdbc:h2:mem:journal-ledger-batch");
        assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '15' AND success = TRUE",
                Integer.class)).isOne();
        assertThat(control.snapshot().status()).isEqualTo(BalanceReaggregationControlPort.Status.OPEN);

        LocalDate date = LocalDate.of(2026, 9, 24);
        reaggregationService.start(767L, date, date);
        assertThat(control.snapshot().status()).isEqualTo(BalanceReaggregationControlPort.Status.REBUILDING);
        assertThat(control.snapshot().ownerJobInstanceId()).isEqualTo(767L);

        reaggregationService.reconcileAndRelease(767L, date, date);
        assertThat(control.snapshot().status()).isEqualTo(BalanceReaggregationControlPort.Status.OPEN);
        assertThat(control.snapshot().epoch()).isEqualTo(2L);
    }
}
