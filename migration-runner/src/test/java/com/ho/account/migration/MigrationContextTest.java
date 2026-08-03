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
    void contextsWithoutCleanBaselinesRemainFailClosed() {
        assertThat(MigrationContext.all())
                .filteredOn(context -> !context.cleanDatabaseReady())
                .extracting(MigrationContext::slug)
                .containsExactly(
                        "account-mart",
                        "ecl",
                        "reconciliation",
                        "tax");
        assertThat(MigrationContext.all())
                .filteredOn(context -> !context.cleanDatabaseReady())
                .allMatch(context -> context.blockerIssue() > 0);
        assertThat(MigrationContext.all())
                .filteredOn(MigrationContext::cleanDatabaseReady)
                .extracting(MigrationContext::slug)
                .containsExactly(
                        "asset-lease", "auth", "budget", "closing", "deposit", "expenditure-resolution",
                        "journal-ledger", "loan", "master-data", "payable", "receivable",
                        "reporting");

        MigrationContext closing = MigrationContext.require("closing");
        assertThat(closing.historyTable()).isEqualTo("flyway_schema_history_closing");
        assertThat(closing.legacyBaselineVersion()).isEqualTo("49");
    }
}
