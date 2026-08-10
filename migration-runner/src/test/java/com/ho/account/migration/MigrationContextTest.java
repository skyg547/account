package com.ho.account.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MigrationContextTest {

    @Test
    void inventoryContainsEveryCurrentBusinessContext() {
        assertThat(MigrationContext.all())
                .extracting(MigrationContext::slug)
                .containsExactly(
                        "account-mart",
                        "asset-lease",
                        "auth",
                        "budget",
                        "closing",
                        "deposit",
                        "ecl",
                        "expenditure-resolution",
                        "internal-audit",
                        "journal-ledger",
                        "loan",
                        "master-data",
                        "payable",
                        "receivable",
                        "reconciliation",
                        "reporting",
                        "tax");
    }

    @Test
    void everyContextHasACleanPostgresqlBaseline() {
        assertThat(MigrationContext.all())
                .filteredOn(context -> !context.cleanDatabaseReady())
                .extracting(MigrationContext::slug)
                .isEmpty();
        assertThat(MigrationContext.all())
                .filteredOn(MigrationContext::cleanDatabaseReady)
                .extracting(MigrationContext::slug)
                .containsExactly(
                        "account-mart", "asset-lease", "auth", "budget", "closing", "deposit", "ecl",
                        "expenditure-resolution", "internal-audit",
                        "journal-ledger", "loan", "master-data", "payable", "receivable",
                        "reconciliation", "reporting", "tax");

        MigrationContext closing = MigrationContext.require("closing");
        assertThat(closing.historyTable()).isEqualTo("flyway_schema_history_closing");
        assertThat(closing.legacyBaselineVersion()).isEqualTo("49");
    }
}
