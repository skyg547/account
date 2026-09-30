package com.ho.account.masterdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Real PostgreSQL upgrade gates preserve legacy rows when a forward migration rejects them. */
@EnabledIfEnvironmentVariable(named = "MASTER_DATA_TEST_POSTGRES_URL", matches = "jdbc:postgresql:.*")
class MasterDataScd2PostgresqlMigrationTest {

    private static final LocalDate JANUARY = LocalDate.of(2025, 1, 1);
    private static final LocalDate JANUARY_END = LocalDate.of(2025, 1, 31);
    private static final LocalDate FEBRUARY = LocalDate.of(2025, 2, 1);
    private static final LocalDate YEAR_END = LocalDate.of(2025, 12, 31);

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void v7ToV8RejectsReversedLegacyWindowWithoutChangingRows(MasterDataType type) {
        Database database = databaseAt("7");
        insert(database.jdbc(), type, "LEGACY-REVERSED", FEBRUARY, JANUARY, true);
        List<Map<String, Object>> before = rows(database.jdbc(), type);

        assertThatThrownBy(() -> migrate(database.schema(), "8")).isInstanceOf(FlywayException.class);

        assertThat(rows(database.jdbc(), type)).isEqualTo(before);
        assertVersionAbsent(database.jdbc(), "8");
        assertThat(database.jdbc().queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = ? AND table_name = 'master_data_business_key_locks'
                """, Integer.class, database.schema())).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void v8ToV9RejectsOverlappingLegacyRowsWithoutDeletingOrCompletingMigration(MasterDataType type) {
        Database database = databaseAt("8");
        insert(database.jdbc(), type, "LEGACY-OVERLAP", JANUARY, YEAR_END, true);
        // Inactive partner history still participates in temporal integrity; use_yn is not an escape.
        insert(database.jdbc(), type, "LEGACY-OVERLAP", FEBRUARY, YEAR_END, false);
        List<Map<String, Object>> before = rows(database.jdbc(), type);

        assertThatThrownBy(() -> migrate(database.schema(), "9")).isInstanceOf(FlywayException.class);

        assertThat(rows(database.jdbc(), type)).isEqualTo(before);
        assertVersionAbsent(database.jdbc(), "9");
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void validAdjacentLegacyWindowsUpgradeAndInclusiveEndpointOverlapIsRejected(MasterDataType type) {
        Database database = databaseAt("7");
        insert(database.jdbc(), type, "LEGACY-ADJACENT", JANUARY, JANUARY_END, true);
        insert(database.jdbc(), type, "LEGACY-ADJACENT", FEBRUARY, YEAR_END, true);
        List<Map<String, Object>> before = rows(database.jdbc(), type);

        migrate(database.schema(), "9");

        assertThat(rows(database.jdbc(), type)).isEqualTo(before);
        assertThat(database.jdbc().queryForObject("""
                SELECT count(*) FROM flyway_schema_history WHERE version IN ('8', '9') AND success
                """, Integer.class)).isEqualTo(2);
        Throwable overlap = catchThrowable(() -> insert(database.jdbc(), type, "LEGACY-ADJACENT",
                JANUARY_END, JANUARY_END, false));
        assertThat(sqlState(overlap)).isEqualTo("23P01");
        assertThat(rows(database.jdbc(), type)).isEqualTo(before);

        // A one-day interval itself is valid; only its overlap with the same key is rejected.
        insert(database.jdbc(), type, "SINGLE-DAY", JANUARY_END, JANUARY_END, true);
        assertThat(rows(database.jdbc(), type)).hasSize(3);
    }

    private static Database databaseAt(String target) {
        String schema = "scd2_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        migrate(schema, target);
        String url = System.getenv("MASTER_DATA_TEST_POSTGRES_URL");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema, "postgres", "");
        return new Database(schema, new JdbcTemplate(dataSource));
    }

    private static void migrate(String schema, String target) {
        Flyway.configure().dataSource(System.getenv("MASTER_DATA_TEST_POSTGRES_URL"), "postgres", "")
                .schemas(schema).defaultSchema(schema).locations("classpath:db/migration")
                .target(target).cleanDisabled(true).load().migrate();
    }

    private static void assertVersionAbsent(JdbcTemplate jdbc, String version) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version = ?",
                Integer.class, version)).isZero();
    }

    private static List<Map<String, Object>> rows(JdbcTemplate jdbc, MasterDataType type) {
        return jdbc.queryForList("SELECT * FROM " + table(type) + " ORDER BY id");
    }

    private static void insert(JdbcTemplate jdbc, MasterDataType type, String key,
            LocalDate from, LocalDate to, boolean active) {
        switch (type) {
            case ACCOUNT_SUBJECT -> jdbc.update("""
                    INSERT INTO account_subjects
                    (code, name, fixed_asset, unsettled, balance_type, valid_from, valid_to, created_at, updated_at)
                    VALUES (?, 'Synthetic account', false, false, 'DEBIT', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, key, from, to);
            case BUSINESS_PARTNER -> jdbc.update("""
                    INSERT INTO business_partners
                    (business_partner_code, business_partner_name, use_yn, partner_type, risk_rating,
                     kyc_status, valid_from, valid_to)
                    VALUES (?, 'Synthetic partner', ?, 'VENDOR', 'LOW', 'APPROVED', ?, ?)
                    """, key, active, from, to);
            case DEPARTMENT -> jdbc.update("""
                    INSERT INTO departments (code, name, valid_from, valid_to, created_at, updated_at)
                    VALUES (?, 'Synthetic department', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, key, from, to);
            case PRODUCT -> jdbc.update("""
                    INSERT INTO products
                    (product_code, name, price, product_type, valid_from, valid_to, created_at, updated_at)
                    VALUES (?, 'Synthetic product', 1, 'PHYSICAL', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, key, from, to);
            default -> throw new IllegalArgumentException("Unsupported fixture type");
        }
    }

    private static String table(MasterDataType type) {
        return switch (type) {
            case ACCOUNT_SUBJECT -> "account_subjects";
            case BUSINESS_PARTNER -> "business_partners";
            case DEPARTMENT -> "departments";
            case PRODUCT -> "products";
            default -> throw new IllegalArgumentException("Unsupported fixture type");
        };
    }

    private static String sqlState(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException) return sqlException.getSQLState();
        }
        return null;
    }

    private record Database(String schema, JdbcTemplate jdbc) { }
}
