package com.ho.account.closing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.closing.application.port.out.FinalCloseEvidencePersistencePort;
import com.ho.account.closing.domain.FinalCloseEvidenceControl;
import com.ho.account.closing.domain.FinalCloseEvidenceControl.Outcome;
import com.ho.account.closing.domain.FinalCloseEvidenceControl.Type;
import com.ho.account.closing.domain.FinalCloseEvidenceSet;
import com.ho.account.closing.domain.FinalCloseEvidenceTotal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import com.ho.account.closing.infrastructure.persistence.FinalCloseEvidencePersistenceTransactions;
import com.ho.account.closing.infrastructure.persistence.JpaFinalCloseEvidencePersistenceAdapter;

@ActiveProfiles("local")
@SpringBootTest(classes = FinalCloseEvidencePersistenceIntegrationTest.PersistenceTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.batch.job.enabled=false", "spring.cloud.config.enabled=false",
                "spring.cloud.vault.enabled=false", "spring.cloud.discovery.enabled=false", "eureka.client.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:final-close-evidence-persistence;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/closing-migration", "spring.flyway.table=flyway_schema_history_closing"})
class FinalCloseEvidencePersistenceIntegrationTest {
    @Autowired FinalCloseEvidencePersistencePort persistence;

    @Test
    void appendReloadsDeterministicOrderAndExactDecimalScale() {
        FinalCloseEvidenceSet input = evidence("set-a", Instant.parse("2026-02-01T01:00:00Z"), "a".repeat(64));

        FinalCloseEvidenceSet reloaded = persistence.append(input);

        assertThat(reloaded.controls()).extracting(FinalCloseEvidenceControl::type)
                .containsExactly(Type.AP_SUBLEDGER, Type.AR_SUBLEDGER);
        assertThat(reloaded.controls().get(0).totals()).extracting(FinalCloseEvidenceTotal::accountCode)
                .containsExactly("1000", "2000");
        assertThat(reloaded.controls().get(0).totals().get(0).sourceTotal())
                .isEqualTo(new BigDecimal("0.000000000000000000"));
        assertThat(reloaded.controls().get(0).totals().get(1).sourceTotal())
                .isEqualTo(new BigDecimal("123456789.123456789012345678"));
    }

    @Test
    void latestOrdersByObservedAtThenAppendSequenceAndRetriesAreIdempotent() {
        FinalCloseEvidenceSet older = evidence("older", Instant.parse("2026-02-01T00:00:00Z"), "b".repeat(64));
        FinalCloseEvidenceSet sameTimeFirst = evidence("same-time-first", Instant.parse("2026-02-01T01:00:00Z"), "c".repeat(64));
        FinalCloseEvidenceSet sameTimeLast = evidence("same-time-last", Instant.parse("2026-02-01T01:00:00Z"), "d".repeat(64));
        persistence.append(older);
        persistence.append(sameTimeFirst);
        persistence.append(sameTimeLast);

        assertThat(persistence.findLatestByCalendarId(10L)).get()
                .extracting(FinalCloseEvidenceSet::evidenceSetId).isEqualTo("same-time-last");
        assertThat(persistence.append(sameTimeLast)).isEqualTo(persistence.append(sameTimeLast));

        FinalCloseEvidenceSet conflicting = evidence("same-time-last", sameTimeLast.observedAt(), "e".repeat(64));
        assertThatThrownBy(() -> persistence.append(conflicting)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different content");
    }

    @Test
    void exactBoundIdLoadsOlderSnapshotEvenAfterNewerEvidenceAndMissingIdIsEmpty() {
        FinalCloseEvidenceSet bound = evidence("bound-older", Instant.parse("2026-02-02T00:00:00Z"), "1".repeat(64));
        FinalCloseEvidenceSet newer = evidence("newer", Instant.parse("2026-02-02T00:01:00Z"), "2".repeat(64));
        persistence.append(bound);
        persistence.append(newer);

        assertThat(persistence.findLatestByCalendarId(10L)).get()
                .extracting(FinalCloseEvidenceSet::evidenceSetId).isEqualTo("newer");
        assertThat(persistence.findByEvidenceSetId(bound.evidenceSetId())).contains(bound);
        assertThat(persistence.findByEvidenceSetId("no-such-bound-id")).isEmpty();
    }

    private FinalCloseEvidenceSet evidence(String id, Instant observedAt, String digest) {
        List<FinalCloseEvidenceTotal> totals = List.of(
                new FinalCloseEvidenceTotal("2000", "USD", new BigDecimal("123456789.123456789012345678"),
                        new BigDecimal("123456789.123456789012345678")),
                new FinalCloseEvidenceTotal("1000", "KRW", new BigDecimal("0.000000000000000000"),
                        new BigDecimal("0.000000000000000000")));
        return new FinalCloseEvidenceSet(id, 10L, 20L, "2026", "01", LocalDate.of(2026, 1, 31), observedAt,
                "provider", digest, List.of(
                        new FinalCloseEvidenceControl(Type.AR_SUBLEDGER, "receivable", "run-2", Outcome.PASS, 0, totals),
                        new FinalCloseEvidenceControl(Type.AP_SUBLEDGER, "payable", "run-1", Outcome.PASS, 0, totals)));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = {"com.ho.account.closing.domain", "com.ho.account.closing.infrastructure.persistence"})
    @EnableJpaRepositories(basePackages = "com.ho.account.closing.infrastructure.persistence")
    @Import({JpaFinalCloseEvidencePersistenceAdapter.class, FinalCloseEvidencePersistenceTransactions.class})
    static class PersistenceTestApplication { }
}
