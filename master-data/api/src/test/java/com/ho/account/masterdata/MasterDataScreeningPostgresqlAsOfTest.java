package com.ho.account.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.BusinessPartnerScreening;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaBusinessPartnerScreeningAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.BusinessPartnerScreeningAuditRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Real PostgreSQL as-of replay against V11 audit rows in a fresh synthetic schema. */
@EnabledIfEnvironmentVariable(named = "MASTER_DATA_TEST_POSTGRES_URL", matches = "jdbc:postgresql:.*")
@SpringBootTest(classes = MasterDataScreeningPostgresqlAsOfTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
                "spring.config.name=screening-postgresql-as-of-test",
                "spring.profiles.active=local",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.clean-disabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.sql.init.mode=never",
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false"
        })
class MasterDataScreeningPostgresqlAsOfTest {

    private static final String SCHEMA = "screening_asof_923_" + UUID.randomUUID().toString().replace("-", "");
    private static final String CODE = "BP-923-ASOF";
    private static final LocalDate FIRST_DAY = LocalDate.of(2026, 10, 1);

    @Autowired private JpaBusinessPartnerScreeningAdapter adapter;
    @Autowired private BusinessPartnerScreeningAuditRepository audits;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("MASTER_DATA_TEST_POSTGRES_URL"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.hikari.schema", () -> SCHEMA);
        registry.add("spring.flyway.schemas", () -> SCHEMA);
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
    }

    @Test
    void replaysDecisionAndResubmissionAtDateEndWithDeterministicSameTimeTieBreak() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='11' AND success",
                Integer.class)).isOne();

        LocalDateTime firstSubmissionAt = FIRST_DAY.atTime(9, 0);
        BusinessPartnerScreening pending = BusinessPartnerScreening.submit(CODE, "KR",
                "BUSINESS_REGISTRATION", "1234567890", "evidence-original", FIRST_DAY.plusDays(1),
                "synthetic-submitter", firstSubmissionAt);
        BusinessPartnerScreening approved = pending.decide(BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.LOW, "Synthetic evidence reviewed", "synthetic-reviewer",
                firstSubmissionAt.plusHours(1));
        LocalDateTime resubmittedAt = FIRST_DAY.plusDays(2).atTime(9, 0);
        BusinessPartnerScreening resubmitted = BusinessPartnerScreening.submit(CODE, "KR",
                "BUSINESS_REGISTRATION", "1234567890", "evidence-resubmitted", FIRST_DAY.plusDays(10),
                "synthetic-submitter", resubmittedAt);
        BusinessPartnerScreening sameTimeSubmission = BusinessPartnerScreening.submit(CODE, "KR",
                "BUSINESS_REGISTRATION", "1234567890", "evidence-same-time-latest", FIRST_DAY.plusDays(10),
                "synthetic-submitter", resubmittedAt);

        // Persist through the real adapter so the historical query reads its own append-only snapshots.
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        for (BusinessPartnerScreening event : new BusinessPartnerScreening[] {
                pending, approved, resubmitted, sameTimeSubmission }) {
            transaction.executeWithoutResult(status -> {
                adapter.save(event);
                adapter.appendAudit(event);
            });
        }

        assertThat(adapter.findLatestAt(CODE, FIRST_DAY.minusDays(1))).isEmpty();
        BusinessPartnerScreening firstDay = adapter.findLatestAt(CODE, FIRST_DAY).orElseThrow();
        assertThat(firstDay.status()).isEqualTo(BusinessPartner.KycStatus.APPROVED);
        assertThat(firstDay.reviewedBy()).isEqualTo("synthetic-reviewer");
        assertThat(firstDay.decidedAt()).isEqualTo(firstSubmissionAt.plusHours(1));
        assertThat(firstDay.isEligible(FIRST_DAY)).isTrue();
        assertThat(firstDay.isEligible(FIRST_DAY.plusDays(2))).isFalse(); // Evidence expired on day 2.
        assertThat(adapter.findLatestAt(CODE, FIRST_DAY.plusDays(1)).orElseThrow().status())
                .isEqualTo(BusinessPartner.KycStatus.APPROVED);

        BusinessPartnerScreening thirdDay = adapter.findLatestAt(CODE, FIRST_DAY.plusDays(2)).orElseThrow();
        assertThat(thirdDay.status()).isEqualTo(BusinessPartner.KycStatus.PENDING);
        assertThat(thirdDay.evidenceReference()).isEqualTo("evidence-same-time-latest");
        assertThat(thirdDay.isEligible(FIRST_DAY.plusDays(2))).isFalse();
        assertThat(adapter.findByBusinessPartnerCode(CODE).orElseThrow().evidenceReference())
                .isEqualTo("evidence-same-time-latest");
        assertThat(audits.count()).isEqualTo(4);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = {
            "com.ho.account.masterdata.core.domain.model",
            "com.ho.account.masterdata.core.domain.changerequest",
            "com.ho.account.masterdata.core.infrastructure.persistence"
    })
    @EnableJpaRepositories(basePackages = "com.ho.account.masterdata.core.infrastructure.persistence")
    @Import(JpaBusinessPartnerScreeningAdapter.class)
    static class TestApplication {
    }
}
