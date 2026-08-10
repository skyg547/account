package com.ho.account.shared.infrastructure;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Fails production startup unless the PostgreSQL server certificate and host are verified. */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProductionPostgresqlTlsGuard {

    public ProductionPostgresqlTlsGuard(
            @Value("${spring.datasource.url}") String datasourceUrl) {
        requireVerifyFull(datasourceUrl);
    }

    public static void requireVerifyFull(String datasourceUrl) {
        if (datasourceUrl == null
                || !datasourceUrl.regionMatches(true, 0, "jdbc:postgresql://", 0, 18)) {
            throw new IllegalStateException(
                    "Production datasource must be PostgreSQL with sslmode=verify-full");
        }
        int queryStart = datasourceUrl.indexOf('?');
        List<String> sslModes = queryStart < 0
                ? List.of()
                : Arrays.stream(datasourceUrl.substring(queryStart + 1).split("&"))
                        .map(parameter -> parameter.split("=", 2))
                        .filter(parts -> parts.length == 2 && parts[0].equalsIgnoreCase("sslmode"))
                        .map(parts -> parts[1])
                        .toList();
        if (sslModes.size() != 1 || !"verify-full".equalsIgnoreCase(sslModes.get(0))) {
            throw new IllegalStateException(
                    "Production datasource must be PostgreSQL with sslmode=verify-full");
        }
    }
}
