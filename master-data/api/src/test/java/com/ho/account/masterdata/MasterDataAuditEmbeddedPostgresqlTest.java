package com.ho.account.masterdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.masterdata.core.domain.audit.MasterDataAuditEvent;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaMasterDataAuditAdapter;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Synthetic PostgreSQL V1→V11 migration and committed audit evidence; never uses an external DB. */
@DataJpaTest(properties = {
        "spring.flyway.enabled=true", "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.target=11", "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = MasterDataAuditEmbeddedPostgresqlTest.TestApplication.class)
@Import(JpaMasterDataAuditAdapter.class)
class MasterDataAuditEmbeddedPostgresqlTest {
    private static final EmbeddedPostgres POSTGRES = startPostgres();

    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired JpaMasterDataAuditAdapter audit;
    @Autowired PlatformTransactionManager manager;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @AfterAll
    static void closePostgres() throws IOException { POSTGRES.close(); }

    @Test
    void v11MigratesAndCommittedEvidenceExportsWithIntegrityVerification() {
        assertThat(jdbc.queryForObject("""
                select count(*) from flyway_schema_history where version = '11' and success = true
                """, Integer.class)).isOne();

        // The writer receives no UPDATE grant. Successful append proves sealing precedes the INSERT.
        jdbc.execute("create role audit_924_writer nologin");
        jdbc.execute("grant usage on schema public to audit_924_writer");
        jdbc.execute("grant select, insert on master_data_audit_events to audit_924_writer");
        jdbc.execute("grant usage, select on sequence master_data_audit_event_ids to audit_924_writer");
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            jdbc.execute("set local role audit_924_writer");
            assertThat(entityManager.createNativeQuery("select current_user").getSingleResult())
                    .isEqualTo("audit_924_writer");
            audit.append(new MasterDataAuditEvent("APPLIED", Instant.parse("2026-10-20T00:00:00.123456789Z"),
                    MasterDataType.PRODUCT, "P-924", 924L, "source-924", null,
                    "synthetic-actor", "request:sha256:" + "a".repeat(64), 41L, 42L));
        });
        assertThat(jdbc.queryForObject("select has_table_privilege('audit_924_writer', 'master_data_audit_events', 'UPDATE')",
                Boolean.class)).isFalse();
        var exported = audit.exportAfterId(0, 10);
        assertThat(exported).hasSize(1);
        assertThat(exported.get(0).event().beforeVersionId()).isEqualTo(41L);
        assertThat(exported.get(0).event().afterVersionId()).isEqualTo(42L);
        assertThat(exported.get(0).event().sourceReference()).isEqualTo("source-924");
        assertThat(audit.exportAfterId(exported.get(0).id(), 10)).isEmpty();

        // Synthetic least-privilege role demonstrates grants needed by the deployment runbook.
        jdbc.execute("create role audit_924_reader nologin");
        jdbc.execute("grant select on master_data_audit_events to audit_924_reader");
        assertThat(jdbc.queryForObject("select has_table_privilege('audit_924_reader', 'master_data_audit_events', 'SELECT')",
                Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("select has_table_privilege('audit_924_reader', 'master_data_audit_events', 'UPDATE')",
                Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("select has_table_privilege('audit_924_reader', 'master_data_audit_events', 'DELETE')",
                Boolean.class)).isFalse();

        jdbc.update("update master_data_audit_events set business_actor = 'tampered' where id = ?",
                exported.get(0).id());
        assertThatThrownBy(() -> audit.exportAfterId(0, 10))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("integrity mismatch");
    }

    private static EmbeddedPostgres startPostgres() {
        try { return EmbeddedPostgres.builder().start(); }
        catch (IOException exception) { throw new IllegalStateException("Cannot start synthetic PostgreSQL", exception); }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ho.account.masterdata.core")
    static class TestApplication { }
}
