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
        String legacyBaselineVersion,
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
        ready(contexts, "asset-lease");
        ready(contexts, "auth");
        ready(contexts, "budget");
        readyWithLegacyBaseline(
                contexts, "closing", "flyway_schema_history_closing", "49");
        ready(contexts, "deposit");
        blocked(contexts, "ecl", 255);
        ready(contexts, "expenditure-resolution");
        ready(contexts, "journal-ledger");
        ready(contexts, "loan");
        ready(contexts, "master-data");
        ready(contexts, "payable");
        ready(contexts, "receivable");
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
                null,
                true,
                0));
    }

    private static void readyWithLegacyBaseline(
            Map<String, MigrationContext> contexts,
            String slug,
            String historyTable,
            String legacyBaselineVersion) {
        contexts.put(slug, new MigrationContext(
                slug,
                slug.replace('-', '_'),
                "classpath:db/contexts/" + slug,
                historyTable,
                legacyBaselineVersion,
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
                null,
                false,
                issue));
    }
}
