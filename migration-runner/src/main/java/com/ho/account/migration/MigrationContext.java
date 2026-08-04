package com.ho.account.migration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record MigrationContext(
        String slug,
        String databaseToken,
        String location,
        String historyTable,
        boolean cleanDatabaseReady,
        int blockerIssue) {

    private static final Map<String, MigrationContext> CONTEXTS = createContexts();

    static MigrationContext require(String slug) {
        MigrationContext context = CONTEXTS.get(slug);
        if (context == null) {
            throw new IllegalArgumentException("Unknown context: " + slug);
        }
        return context;
    }

    static List<MigrationContext> all() {
        return List.copyOf(CONTEXTS.values());
    }

    private static Map<String, MigrationContext> createContexts() {
        Map<String, MigrationContext> contexts = new LinkedHashMap<>();
        blocked(contexts, "account-mart", 255);
        blocked(contexts, "asset-lease", 254);
        ready(contexts, "auth");
        ready(contexts, "budget");
        blocked(contexts, "closing", 250, "flyway_schema_history_closing");
        ready(contexts, "deposit");
        blocked(contexts, "ecl", 255);
        blocked(contexts, "expenditure-resolution", 252);
        blocked(contexts, "journal-ledger", 251);
        ready(contexts, "loan");
        blocked(contexts, "master-data", 251);
        blocked(contexts, "payable", 252);
        blocked(contexts, "receivable", 252);
        blocked(contexts, "reconciliation", 253);
        ready(contexts, "reporting");
        blocked(contexts, "tax", 253);
        return Collections.unmodifiableMap(contexts);
    }

    private static void ready(Map<String, MigrationContext> contexts, String slug) {
        contexts.put(slug, new MigrationContext(
                slug,
                slug.replace('-', '_'),
                "classpath:db/contexts/" + slug,
                "flyway_schema_history",
                true,
                0));
    }

    private static void blocked(Map<String, MigrationContext> contexts, String slug, int issue) {
        blocked(contexts, slug, issue, "flyway_schema_history");
    }

    private static void blocked(
            Map<String, MigrationContext> contexts,
            String slug,
            int issue,
            String historyTable) {
        contexts.put(slug, new MigrationContext(
                slug,
                slug.replace('-', '_'),
                "classpath:db/contexts/" + slug,
                historyTable,
                false,
                issue));
    }
}
