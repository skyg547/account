package com.ho.account.closing.infrastructure.source;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Separate source connections; never registered as the Closing primary DataSource. */
public final class ClosingReadOnlySources implements AutoCloseable {
    private final HikariDataSource journal;
    private final HikariDataSource ecl;
    private final HikariDataSource masterData;

    public ClosingReadOnlySources(
            String journalUrl, String journalUsername, String journalPassword,
            String eclUrl, String eclUsername, String eclPassword,
            String masterUrl, String masterUsername, String masterPassword) {
        journal = source("journal", journalUrl, journalUsername, journalPassword);
        ecl = source("ecl", eclUrl, eclUsername, eclPassword);
        masterData = source("master-data", masterUrl, masterUsername, masterPassword);
    }

    public DataSource journalDataSource() { return journal; }
    public JdbcTemplate journalJdbcTemplate() { return new JdbcTemplate(journal); }
    public JdbcTemplate eclJdbcTemplate() { return new JdbcTemplate(ecl); }
    public JdbcTemplate masterDataJdbcTemplate() { return new JdbcTemplate(masterData); }

    private static HikariDataSource source(String name, String url, String username, String password) {
        if (url == null || !url.startsWith("jdbc:postgresql://")
                || username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Closing " + name + " source connection is required");
        }
        HikariDataSource source = new HikariDataSource();
        source.setPoolName("closing-source-" + name);
        source.setJdbcUrl(url);
        source.setUsername(username);
        source.setPassword(password);
        source.setReadOnly(true);
        // JDBC readOnly alone may not cover auto-commit SELECT connections on PostgreSQL.
        source.setConnectionInitSql("SET default_transaction_read_only = on");
        source.setMaximumPoolSize(1);
        source.setMinimumIdle(0);
        source.setConnectionTimeout(5000);
        source.setValidationTimeout(2000);
        source.setInitializationFailTimeout(-1);
        source.addDataSourceProperty("connectTimeout", "5");
        source.addDataSourceProperty("socketTimeout", "60");
        return source;
    }

    @Override
    public void close() {
        journal.close();
        ecl.close();
        masterData.close();
    }
}
