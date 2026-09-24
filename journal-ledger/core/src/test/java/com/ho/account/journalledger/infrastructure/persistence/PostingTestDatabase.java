package com.ho.account.journalledger.infrastructure.persistence;

import java.util.UUID;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Only synthetic, disposable databases supplied explicitly to the test process are used. */
record PostingTestDatabase(String url, String username, String driver, String schema) {
    static PostingTestDatabase create() {
        String schema = "posting_" + UUID.randomUUID().toString().replace("-", "");
        String postgresUrl = System.getenv("JOURNAL_POSTING_TEST_POSTGRES_URL");
        if (postgresUrl == null || postgresUrl.isBlank()) {
            return new PostingTestDatabase("jdbc:h2:mem:" + schema
                    + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
                    "sa", "org.h2.Driver", "public");
        }
        return new PostingTestDatabase(postgresUrl
                + (postgresUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                "postgres", "org.postgresql.Driver", schema);
    }

    DriverManagerDataSource dataSource() {
        DriverManagerDataSource result = new DriverManagerDataSource(url, username, "");
        result.setDriverClassName(driver);
        return result;
    }
}
