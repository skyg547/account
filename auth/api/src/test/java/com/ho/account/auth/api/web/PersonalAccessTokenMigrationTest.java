package com.ho.account.auth.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

/** Checks V73-to-V74 PAT migration and owner limits on H2 and embedded PostgreSQL. */
class PersonalAccessTokenMigrationTest {

    @Test
    void postgresqlV74PreservesExistingTokenAndEnforcesTheEightyCharacterLimit() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder().start()) {
            String url = postgres.getJdbcUrl("postgres", "postgres");
            migrate(url, "postgres", "73");
            String existingId = UUID.randomUUID().toString();
            String existingOwner = "o".repeat(50);
            try (Connection connection = DriverManager.getConnection(url, "postgres", "")) {
                insertToken(connection, existingId, existingOwner);
            }

            migrate(url, "postgres", "74");
            try (Connection connection = DriverManager.getConnection(url, "postgres", "")) {
                assertThat(ownerFor(connection, existingId)).isEqualTo(existingOwner);
                for (int length : new int[] {51, 80}) {
                    String id = UUID.randomUUID().toString();
                    String owner = "o".repeat(length);
                    insertToken(connection, id, owner);
                    assertThat(ownerFor(connection, id)).isEqualTo(owner);
                }
                assertThatThrownBy(() -> insertToken(
                        connection, UUID.randomUUID().toString(), "o".repeat(81)))
                        .isInstanceOf(java.sql.SQLException.class)
                        .extracting("SQLState").isEqualTo("22001");
            }
        }
    }

    @Test
    void v74PreservesV73TokenAndWidensTheOwnerColumn() throws Exception {
        String url = "jdbc:h2:mem:pat-migration-" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        migrate(url, "73");
        String existingId = UUID.randomUUID().toString();
        String existingOwner = "o".repeat(50);
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            insertToken(connection, existingId, existingOwner);
        }

        migrate(url, "74");
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            assertThat(ownerFor(connection, existingId)).isEqualTo(existingOwner);
            for (int length : new int[] {51, 80}) {
                String id = UUID.randomUUID().toString();
                String owner = "o".repeat(length);
                insertToken(connection, id, owner);
                assertThat(ownerFor(connection, id)).isEqualTo(owner);
            }
        }
    }

    private void migrate(String url, String target) {
        migrate(url, "sa", target);
    }

    private void migrate(String url, String username, String target) {
        Flyway.configure()
                .dataSource(url, username, "")
                .locations("classpath:db/migration")
                .target(target)
                .load()
                .migrate();
    }

    private void insertToken(Connection connection, String id, String owner) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into auth_personal_access_tokens "
                        + "(id, username, token_name, token_prefix, token_hash, status, expires_at, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, id);
            statement.setString(2, owner);
            statement.setString(3, "Existing key");
            statement.setString(4, "pat_live_123...");
            statement.setString(5, UUID.randomUUID().toString());
            statement.setString(6, "ACTIVE");
            statement.setObject(7, LocalDateTime.now().plusDays(30));
            statement.setObject(8, LocalDateTime.now());
            statement.executeUpdate();
        }
    }

    private String ownerFor(Connection connection, String id) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "select username from auth_personal_access_tokens where id = ?")) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getString(1);
            }
        }
    }
}
