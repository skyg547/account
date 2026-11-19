package com.ho.account.auth.api.web;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GovernanceMigrationTest {
    @Test
    void h2V77MigratesExistingAuthAndStoresPortableTextAndEightyCharacterActors() throws Exception {
        verify("jdbc:h2:mem:governance-migration-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa");
    }

    @Test
    void postgresqlV77MigratesExistingAuthAndStoresPortableTextAndEightyCharacterActors() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder().start()) {
            verify(postgres.getJdbcUrl("postgres", "postgres"), "postgres");
        }
    }

    private void verify(String url, String user) throws Exception {
        Flyway.configure().dataSource(url, user, "").target("76").load().migrate();
        Flyway.configure().dataSource(url, user, "").target("77").load().migrate();
        try (Connection connection = DriverManager.getConnection(url, user, "")) {
            String actor = "a".repeat(80), payload = "payload".repeat(1000);
            try (var statement = connection.prepareStatement("insert into master_approval(master_type,master_key,request_type,payload,request_user,request_date,status,created_at,audit_user,approver_user) values('AUTH_USER_ROLE','fixture','UPDATE',?,?,CURRENT_TIMESTAMP,'PENDING',CURRENT_TIMESTAMP,?,?)")) {
                statement.setString(1, payload); statement.setString(2, actor); statement.setString(3, actor); statement.setString(4, actor);
                statement.executeUpdate();
            }
            try (var statement = connection.createStatement(); var rows = statement.executeQuery("select payload,request_user,approver_user from master_approval")) {
                assertThat(rows.next()).isTrue(); assertThat(rows.getString(1)).isEqualTo(payload);
                assertThat(rows.getString(2)).isEqualTo(actor); assertThat(rows.getString(3)).isEqualTo(actor);
            }
            try (var statement = connection.prepareStatement("insert into audit_log(event_type,event_date_time,user_id,target_entity,target_id,before_data,after_data,create_date,update_date,audit_user) values('FIXTURE',CURRENT_TIMESTAMP,?,'MASTER_APPROVAL','1',?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?)")) {
                statement.setString(1, actor); statement.setString(2, payload); statement.setString(3, payload); statement.setString(4, actor);
                statement.executeUpdate();
                statement.setString(1, "a".repeat(81));
                assertThatThrownBy(statement::executeUpdate).isInstanceOf(SQLException.class);
            }
        }
    }
}
