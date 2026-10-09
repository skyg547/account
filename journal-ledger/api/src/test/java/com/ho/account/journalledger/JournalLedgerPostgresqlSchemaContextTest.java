package com.ho.account.journalledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.journalledger.infrastructure.persistence.repository.JournalEntryRepository;
import com.ho.account.journalledger.infrastructure.persistence.repository.GlBalanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        classes = JournalLedgerPostgresqlSchemaContextTest.SchemaValidationApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.config.name=schema-validation",
                "spring.profiles.active=local",
                "spring.datasource.url=jdbc:h2:mem:journal-api-pg-schema;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/journal-migration",
                "spring.flyway.clean-disabled=true",
                "spring.flyway.baseline-on-migrate=false",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.sql.init.mode=never",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false"
        })
class JournalLedgerPostgresqlSchemaContextTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private GlBalanceRepository glBalanceRepository;

    @Test
    void cleanBaselineMigratesValidatesAndSupportsCoreRepositories() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version IN ('1', '10', '11', '12', '13', '14', '15', '16', '17', '18')
                  AND success = TRUE
                """, Integer.class)).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ledger_balance_locks", Integer.class))
                .isEqualTo(256);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ledger_reaggregation_control WHERE control_id = 1 AND status = 'OPEN'",
                Integer.class)).isOne();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                    'journal_entries', 'journal_details', 'journal_rules',
                    'journal_rule_conditions', 'journal_rule_details',
                    'gl_entries', 'sl_entries', 'gl_balances', 'sl_balances', 'unsettled_items',
                    'unsettled_item_settlement_references', 'journal_reversal_operations',
                    'journal_event_quarantine')
                """, Integer.class)).isEqualTo(13);
        assertThat(journalEntryRepository.count()).isZero();
        assertThat(glBalanceRepository.count()).isZero();
    }

    @Test
    void v12BackfillIdempotentlyCopiesLastSettlementReference() {
        try {
            jdbcTemplate.update("""
                    INSERT INTO journal_entries (id, slip_no, slip_date, accounting_date, status)
                    VALUES (9999, 'SLIP-9999', '2026-09-07', '2026-09-07', 'DRAFT')
                    """);
            jdbcTemplate.update("""
                    INSERT INTO journal_details (id, journal_entry_id, side, account_code, amount, base_amount)
                    VALUES (9999, 9999, 'DEBIT', '11000', 100.00, 100.00)
                    """);
            jdbcTemplate.update("""
                    INSERT INTO unsettled_items (id, journal_detail_id, occurrence_date, original_amount, settled_amount, remaining_amount, resolved, status, account_code, management_no, last_settlement_reference)
                    VALUES (9999, 9999, '2026-09-07', 100.00, 40.00, 60.00, false, 'PARTIAL', '11000', 'UNS-9999', 'LEGACY-REF-001')
                    """);

            int inserted = jdbcTemplate.update("""
                    INSERT INTO unsettled_item_settlement_references (unsettled_item_id, settlement_reference)
                    SELECT ui.id, ui.last_settlement_reference
                    FROM unsettled_items ui
                    WHERE ui.last_settlement_reference IS NOT NULL
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unsettled_item_settlement_references uisr
                          WHERE uisr.unsettled_item_id = ui.id
                            AND uisr.settlement_reference = ui.last_settlement_reference
                      )
                    """);
            assertThat(inserted).isPositive();

            assertThat(jdbcTemplate.queryForObject("""
                    SELECT settlement_reference
                    FROM unsettled_item_settlement_references
                    WHERE unsettled_item_id = 9999
                    """, String.class)).isEqualTo("LEGACY-REF-001");

            int duplicateRun = jdbcTemplate.update("""
                    INSERT INTO unsettled_item_settlement_references (unsettled_item_id, settlement_reference)
                    SELECT ui.id, ui.last_settlement_reference
                    FROM unsettled_items ui
                    WHERE ui.last_settlement_reference IS NOT NULL
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unsettled_item_settlement_references uisr
                          WHERE uisr.unsettled_item_id = ui.id
                            AND uisr.settlement_reference = ui.last_settlement_reference
                      )
                    """);
            assertThat(duplicateRun).isZero();
        } finally {
            jdbcTemplate.update("DELETE FROM unsettled_item_settlement_references WHERE unsettled_item_id = 9999");
            jdbcTemplate.update("DELETE FROM unsettled_items WHERE id = 9999");
            jdbcTemplate.update("DELETE FROM journal_details WHERE id = 9999");
            jdbcTemplate.update("DELETE FROM journal_entries WHERE id = 9999");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ho.account.journalledger.infrastructure.persistence")
    @EnableJpaRepositories(basePackages = {
            "com.ho.account.journalledger.infrastructure.persistence.repository",
            "com.ho.account.journalledger.adapter.out.persistence"
    })
    static class SchemaValidationApplication {
    }
}
