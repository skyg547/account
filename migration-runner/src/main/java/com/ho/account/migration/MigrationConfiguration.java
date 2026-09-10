package com.ho.account.migration;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

record MigrationConfiguration(String url, String user, String password, String expectedDatabase) {

    private static final Pattern DATABASE_IDENTIFIER = Pattern.compile("[a-z][a-z0-9_]*");

    static MigrationConfiguration from(
            Map<String, String> environment,
            MigrationContext context) {
        String url = required(environment, "MIGRATION_DB_URL");
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("MIGRATION_DB_URL must use jdbc:postgresql://");
        }
        rejectCredentialsInUrl(url);
        String targetEnvironment = required(environment, "MIGRATION_TARGET_ENV");
        if (!("development".equals(targetEnvironment) || "production".equals(targetEnvironment))) {
            throw new IllegalArgumentException(
                    "MIGRATION_TARGET_ENV must be development or production");
        }
        if ("production".equals(targetEnvironment)
                && !hasSingleCanonicalSslmode(url)) {
            throw new IllegalArgumentException(
                    "Production MIGRATION_DB_URL must use sslmode=verify-full");
        }
        String expectedDatabase = required(environment, "MIGRATION_EXPECTED_DATABASE");
        validateDatabaseBinding(url, expectedDatabase, context);
        return new MigrationConfiguration(
                url,
                required(environment, "MIGRATION_DB_USER"),
                requiredSecret(environment, "MIGRATION_DB_PASSWORD"),
                expectedDatabase);
    }

    static void requireMigrateApproval(Map<String, String> environment) {
        if (!"true".equals(environment.get("MIGRATION_ALLOW_MIGRATE"))) {
            throw new IllegalArgumentException("MIGRATION_ALLOW_MIGRATE must be exactly true");
        }
        required(environment, "MIGRATION_CHANGE_TICKET");
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    private static String requiredSecret(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static void rejectCredentialsInUrl(String url) {
        int authorityStart = "jdbc:postgresql://".length();
        int pathStart = url.indexOf('/', authorityStart);
        if (pathStart > authorityStart && url.substring(authorityStart, pathStart).contains("@")) {
            throw new IllegalArgumentException(
                    "MIGRATION_DB_URL must not contain user information");
        }
        int queryStart = url.indexOf('?');
        if (queryStart < 0) {
            return;
        }
        String[] parameters = url.substring(queryStart + 1).split("&");
        for (String parameter : parameters) {
            String name = parameter.split("=", 2)[0].toLowerCase(Locale.ROOT);
            if ("user".equals(name) || "password".equals(name)) {
                throw new IllegalArgumentException(
                        "MIGRATION_DB_URL must not contain user or password parameters");
            }
        }
    }

    private static void validateDatabaseBinding(
            String url,
            String expectedDatabase,
            MigrationContext context) {
        if (!DATABASE_IDENTIFIER.matcher(expectedDatabase).matches()) {
            throw new IllegalArgumentException(
                    "MIGRATION_EXPECTED_DATABASE must be a lowercase PostgreSQL identifier");
        }
        String token = context.databaseToken();
        if (!(expectedDatabase.equals(token) || expectedDatabase.startsWith(token + "_"))) {
            throw new IllegalArgumentException(
                    "MIGRATION_EXPECTED_DATABASE does not match the selected context");
        }
        URI uri = URI.create(url.substring("jdbc:".length()));
        String path = uri.getPath();
        String urlDatabase = path == null || path.length() < 2 ? "" : path.substring(1);
        if (!expectedDatabase.equals(urlDatabase)) {
            throw new IllegalArgumentException(
                    "MIGRATION_DB_URL database does not match MIGRATION_EXPECTED_DATABASE");
        }
    }

    private static boolean hasSingleCanonicalSslmode(String url) {
        int queryStart = url.indexOf('?');
        if (queryStart < 0) {
            return false;
        }
        // Read raw query entries without decoding or rewriting the URL passed to JDBC.
        int sslmodeCount = 0;
        for (String parameter : url.substring(queryStart + 1).split("&")) {
            String[] pair = parameter.split("=", 2);
            if ("sslmode".equalsIgnoreCase(pair[0])) {
                // Count even empty/bare or differently cased keys; a second key is ambiguous.
                sslmodeCount++;
                if (sslmodeCount > 1 || !"sslmode=verify-full".equals(parameter)) {
                    return false;
                }
            }
        }
        // Exactly one canonical entry makes the validator and JDBC agree on verify-full.
        return sslmodeCount == 1;
    }
}
