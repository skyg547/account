package com.ho.account.masterdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Synthetic PostgreSQL proof for the forward migration and immutable screening audit. */
@EnabledIfEnvironmentVariable(named = "MASTER_DATA_TEST_POSTGRES_URL", matches = "jdbc:postgresql:.*")
class MasterDataScreeningPostgresqlMigrationTest {

    @Test
    void cleanV13CreatesScreeningTablesAndRejectsAuditMutation() {
        String schema = schema();
        migrate(schema, "13");
        JdbcTemplate jdbc = jdbc(schema);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='13' AND success",
                Integer.class)).isOne();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM business_partner_screenings", Integer.class)).isZero();

        jdbc.update("""
                INSERT INTO business_partner_screening_audit
                    (business_partner_code, jurisdiction, identifier_type, identifier,
                     evidence_reference, status, risk_rating, submitted_by, submitted_at)
                VALUES ('BP-923', 'KR', 'BUSINESS_REGISTRATION', '1234567890',
                        'synthetic-evidence', 'PENDING', 'LOW', 'synthetic-requester', CURRENT_TIMESTAMP)
                """);
        // History must remain append-only even if an SQL writer bypasses the application adapter.
        assertThat(sqlState(catchThrowable(() -> jdbc.update("""
                UPDATE business_partner_screening_audit
                   SET evidence_reference='tampered' WHERE business_partner_code='BP-923'
                """)))).isEqualTo("P0001");
        assertThat(sqlState(catchThrowable(() -> jdbc.update("""
                DELETE FROM business_partner_screening_audit WHERE business_partner_code='BP-923'
                """)))).isEqualTo("P0001");
        assertThat(jdbc.queryForObject("SELECT evidence_reference FROM business_partner_screening_audit",
                String.class)).isEqualTo("synthetic-evidence");
    }

    @Test
    void v12UpgradeKeepsLegacyPartnerButDoesNotGrantEligibility() {
        String schema = schema();
        migrate(schema, "12");
        JdbcTemplate jdbc = jdbc(schema);
        jdbc.update("""
                INSERT INTO business_partners
                    (business_partner_code, business_partner_name, use_yn, partner_type,
                     registration_number, risk_rating, kyc_status, valid_from, valid_to)
                VALUES ('BP-923', 'Synthetic partner', true, 'VENDOR',
                        '1234567890', 'LOW', 'APPROVED', DATE '2026-01-01', DATE '9999-12-31')
                """);

        migrate(schema, "13");
        assertThat(jdbc.queryForObject("SELECT kyc_status FROM business_partners WHERE business_partner_code='BP-923'",
                String.class)).isEqualTo("APPROVED");
        // A legacy label has no evidence or reviewer, so migration never invents an approval record.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM business_partner_screenings", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM business_partner_screening_audit", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='13' AND success",
                Integer.class)).isOne();
    }

    private static String schema() {
        return "screening_923_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static void migrate(String schema, String target) {
        Flyway.configure().dataSource(System.getenv("MASTER_DATA_TEST_POSTGRES_URL"), "postgres", "")
                .schemas(schema).defaultSchema(schema).locations("classpath:db/migration")
                .target(target).cleanDisabled(true).load().migrate();
    }

    private static JdbcTemplate jdbc(String schema) {
        String url = System.getenv("MASTER_DATA_TEST_POSTGRES_URL");
        return new JdbcTemplate(new DriverManagerDataSource(
                url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                "postgres", ""));
    }

    private static String sqlState(Throwable failure) {
        while (failure != null) {
            if (failure instanceof SQLException sql) {
                return sql.getSQLState();
            }
            failure = failure.getCause();
        }
        return null;
    }
}
