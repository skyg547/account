package com.ho.account.journalledger.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class JournalEventQuarantineMigrationTest {

    @Test
    void cleanMigrationEnforcesBrokerIdentityAndResolutionState() {
        PostingTestDatabase database = PostingTestDatabase.create();
        Flyway.configure().dataSource(database.dataSource())
                .locations("classpath:db/journal-migration")
                .schemas(database.schema())
                .target("18")
                .load()
                .migrate();
        JdbcTemplate jdbc = new JdbcTemplate(database.dataSource());

        jdbc.update("""
                INSERT INTO journal_event_quarantine
                    (source_topic, source_partition, source_offset, payload_json, accounting_date,
                     status, reason_code, first_seen_at, replay_attempts, version)
                VALUES ('transaction-events', 0, 769, '{}', '2026-09-28',
                        'QUARANTINED', 'NO_MATCHING_RULE', CURRENT_TIMESTAMP, 0, 0)
                """);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO journal_event_quarantine
                    (source_topic, source_partition, source_offset, payload_json, accounting_date,
                     status, reason_code, first_seen_at, replay_attempts, version)
                VALUES ('transaction-events', 0, 769, '{}', '2026-09-28',
                        'QUARANTINED', 'NO_MATCHING_RULE', CURRENT_TIMESTAMP, 0, 0)
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE journal_event_quarantine SET status = 'REPLAYED'
                WHERE source_topic = 'transaction-events' AND source_partition = 0 AND source_offset = 769
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM journal_event_quarantine WHERE status = 'QUARANTINED'",
                Integer.class)).isOne();
    }
}
