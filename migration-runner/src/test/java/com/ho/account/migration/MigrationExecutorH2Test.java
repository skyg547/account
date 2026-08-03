package com.ho.account.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

class MigrationExecutorH2Test {

    @Test
    void rejectsAProvisionedButIncompatibleLegacyBatchSchema() throws Exception {
        String url = "jdbc:h2:mem:legacy-batch;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/contexts/auth")
                .load()
                .migrate();
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE batch_job_instance (job_instance_id BIGINT PRIMARY KEY)");
        }
        MigrationConfiguration configuration = new MigrationConfiguration(url, "sa", "", "legacy_batch");

        assertThatThrownBy(() -> new MigrationExecutor().execute(
                MigrationContext.require("auth"),
                MigrationAction.MIGRATE,
                configuration))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("incompatible shape");
    }

    @ParameterizedTest
    @MethodSource("malformedBatchMetadataMutations")
    void validateRejectsMalformedBatchMetadataDefinitions(
            String databaseName,
            String mutation,
            String expectedMessage) throws Exception {
        String url = "jdbc:h2:mem:" + databaseName
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        MigrationConfiguration configuration = new MigrationConfiguration(url, "sa", "", databaseName);
        MigrationExecutor executor = new MigrationExecutor();
        MigrationContext context = MigrationContext.require("auth");
        executor.execute(context, MigrationAction.MIGRATE, configuration);
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            for (String sql : mutation.split(";;")) {
                statement.execute(sql);
            }
        }

        assertThatThrownBy(() -> executor.execute(context, MigrationAction.VALIDATE, configuration))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(expectedMessage);
    }

    @ParameterizedTest
    @MethodSource("postgresqlCompatibleContexts")
    void correctedMigrationsAndBatchMetadataMigrateAndValidateOnH2(
            String contextSlug,
            String expectedBusinessTable) throws Exception {
        String url = "jdbc:h2:mem:" + contextSlug
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        MigrationConfiguration configuration = new MigrationConfiguration(url, "sa", "", contextSlug);
        MigrationExecutor executor = new MigrationExecutor();
        MigrationContext context = MigrationContext.require(contextSlug);

        int migrationCount = executor.execute(context, MigrationAction.MIGRATE, configuration);
        int validationCount = executor.execute(context, MigrationAction.VALIDATE, configuration);

        assertThat(migrationCount).isGreaterThanOrEqualTo(2);
        assertThat(validationCount).isZero();
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(tableExists(statement, expectedBusinessTable)).isTrue();
            assertThat(tableExists(statement, "batch_job_instance")).isTrue();
            assertThat(tableExists(statement, context.historyTable())).isTrue();
            assertThat(tableExists(statement, "flyway_schema_history_batch")).isTrue();
        }
    }

    @Test
    void expenditureResolutionBaselineContainsOwnedTablesAndFinancialConstraints() throws Exception {
        String url = "jdbc:h2:mem:expenditure-resolution-parity"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        new MigrationExecutor().execute(
                MigrationContext.require("expenditure-resolution"),
                MigrationAction.MIGRATE,
                new MigrationConfiguration(url, "sa", "", "expenditure_resolution"));

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(Stream.of(
                            "expenditure_resolutions", "expenditure_details", "budgets",
                            "ap_invoices", "ap_payments")
                    .allMatch(table -> {
                        try {
                            return tableExists(statement, table);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })).isTrue();
            assertNumericColumn(connection, "expenditure_resolutions", "total_amount", 19, 2);
            assertNumericColumn(connection, "expenditure_details", "amount", 19, 2);
            assertNumericColumn(connection, "budgets", "assigned_amount", 19, 2);
            assertNumericColumn(connection, "ap_payments", "unapplied_amount", 19, 2);
            assertThat(hasForeignKey(
                    connection, "expenditure_details", "expenditure_resolution_id",
                    "expenditure_resolutions", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "ap_payments", "expenditure_resolution_id",
                    "expenditure_resolutions", "id")).isTrue();
            assertThat(hasUniqueIndexOnColumns(
                    connection, "budgets", List.of("year_month", "dept_code", "account_code"))).isTrue();
            assertThat(indexColumns(
                    connection, "expenditure_resolutions", "idx_expenditure_resolution_date_status"))
                    .containsExactly("resolution_date", "status");
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO expenditure_resolutions (
                        resolution_no, title, resolution_date, payment_date, total_amount, status)
                    VALUES ('INVALID', 'Invalid', DATE '2026-01-02', DATE '2026-01-01', -1.00, 'UNKNOWN')
                    """))
                    .hasMessageContaining("ck_expenditure_resolution");
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO ap_invoices (
                        invoice_no, vendor_code, invoice_date, due_date, currency_code, supply_amount,
                        tax_amount, total_amount, remaining_amount, status, created_at)
                    VALUES ('INVALID-TOTAL', 'VENDOR', DATE '2026-01-01', DATE '2026-01-02', 'KRW',
                        100.00, 10.00, 100.00, 100.00, 'OPEN', CURRENT_TIMESTAMP)
                    """))
                    .hasMessageContaining("ck_expenditure_ap_invoice_amounts");
        }
    }

    @Test
    void payableBaselineContainsOwnedTablesAndFinancialConstraints() throws Exception {
        String url = "jdbc:h2:mem:payable-parity"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        new MigrationExecutor().execute(
                MigrationContext.require("payable"),
                MigrationAction.MIGRATE,
                new MigrationConfiguration(url, "sa", "", "payable"));

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(Stream.of(
                            "purchase_invoices", "payables", "payment_runs", "payments",
                            "advance_payments")
                    .allMatch(table -> {
                        try {
                            return tableExists(statement, table);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })).isTrue();
            assertNumericColumn(connection, "purchase_invoices", "total_amount", 19, 2);
            assertNumericColumn(connection, "payables", "outstanding_amount", 19, 2);
            assertNumericColumn(connection, "payments", "amount", 19, 2);
            assertThat(hasForeignKey(connection, "payments", "payable_id", "payables", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "payments", "payment_run_id", "payment_runs", "id")).isTrue();
            assertThat(hasUniqueIndexOnColumns(
                    connection, "purchase_invoices", List.of("invoice_no", "vendor_code"))).isTrue();
            assertThat(hasCompositeForeignKey(
                    connection,
                    "payables",
                    List.of("purchase_invoice_invoice_no", "purchase_invoice_vendor_code"),
                    "purchase_invoices",
                    List.of("invoice_no", "vendor_code"))).isTrue();
            assertThat(indexColumns(connection, "payables", "idx_payable_vendor_due_status"))
                    .containsExactly("vendor_code", "due_date", "status");
            assertThat(indexColumns(connection, "payables", "idx_payable_due_status"))
                    .containsExactly("due_date", "status");
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO purchase_invoices (
                        invoice_no, vendor_code, issue_date, due_date,
                        total_amount, tax_amount, net_amount, status, created_by, created_at)
                    VALUES ('INVALID', 'VENDOR', DATE '2026-01-01', DATE '2026-01-02',
                        99.00, 10.00, 100.00, 'RECEIVED', 'tester', CURRENT_TIMESTAMP)
                    """))
                    .hasMessageContaining("ck_purchase_invoice_amounts");
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO purchase_invoices (
                        invoice_no, vendor_code, issue_date, due_date,
                        total_amount, tax_amount, net_amount, status, created_by, created_at)
                    VALUES ('ZERO', 'VENDOR', DATE '2026-01-01', DATE '2026-01-02',
                        0.00, 0.00, 0.00, 'RECEIVED', 'tester', CURRENT_TIMESTAMP)
                    """))
                    .hasMessageContaining("ck_purchase_invoice_amounts");
            statement.executeUpdate("""
                    INSERT INTO purchase_invoices (
                        invoice_no, vendor_code, issue_date, due_date,
                        total_amount, tax_amount, net_amount, status, created_by, created_at)
                    VALUES ('PI-1', 'VENDOR', DATE '2026-01-01', DATE '2026-01-02',
                        110.00, 10.00, 100.00, 'RECEIVED', 'tester', CURRENT_TIMESTAMP)
                    """);
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO payables (
                        purchase_invoice_invoice_no, purchase_invoice_vendor_code, vendor_code,
                        original_amount, outstanding_amount, due_date, status, created_at)
                    VALUES ('MISSING', 'VENDOR', 'VENDOR', 110.00, 110.00,
                        DATE '2026-01-02', 'OPEN', CURRENT_TIMESTAMP)
                    """))
                    .hasMessageContaining("fk_payable_purchase_invoice");
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO payables (
                        purchase_invoice_invoice_no, purchase_invoice_vendor_code, vendor_code,
                        original_amount, outstanding_amount, due_date, status, created_at)
                    VALUES ('PI-1', 'VENDOR', 'VENDOR', 110.00, 50.00,
                        DATE '2026-01-02', 'OPEN', CURRENT_TIMESTAMP)
                    """))
                    .hasMessageContaining("ck_payable_status");
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO payables (
                        purchase_invoice_invoice_no, purchase_invoice_vendor_code, vendor_code,
                        original_amount, outstanding_amount, due_date, status, created_at)
                    VALUES ('PI-1', 'VENDOR', 'OTHER-VENDOR', 110.00, 110.00,
                        DATE '2026-01-02', 'OPEN', CURRENT_TIMESTAMP)
                    """))
                    .hasMessageContaining("ck_payable_vendor_lineage");
        }
    }

    @Test
    void receivableBaselineContainsOwnedTablesAndFinancialConstraints() throws Exception {
        String url = "jdbc:h2:mem:receivable-parity"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        new MigrationExecutor().execute(
                MigrationContext.require("receivable"),
                MigrationAction.MIGRATE,
                new MigrationConfiguration(url, "sa", "", "receivable"));

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(Stream.of(
                            "sales_invoices", "receivables", "collections", "collection_allocations",
                            "matching_rules", "unmatched_collections")
                    .allMatch(table -> {
                        try {
                            return tableExists(statement, table);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })).isTrue();
            assertNumericColumn(connection, "sales_invoices", "total_amount", 19, 2);
            assertNumericColumn(connection, "receivables", "outstanding_amount", 19, 2);
            assertNumericColumn(connection, "collection_allocations", "matched_amount", 19, 2);
            assertThat(isNullable(connection, "receivables", "sales_invoice_id")).isFalse();
            assertThat(hasForeignKey(
                    connection, "receivables", "sales_invoice_id", "sales_invoices", "id")).isTrue();
            assertThat(hasCompositeForeignKey(
                    connection,
                    "receivables",
                    List.of("sales_invoice_id", "customer_code"),
                    "sales_invoices",
                    List.of("id", "customer_code"))).isTrue();
            assertThat(hasUniqueIndexOnColumns(
                    connection, "receivables", List.of("sales_invoice_id"))).isTrue();
            assertThat(hasForeignKey(
                    connection, "collection_allocations", "collection_id", "collections", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "collection_allocations", "receivable_id", "receivables", "id")).isTrue();
            assertThat(hasUniqueIndexOnColumns(
                    connection, "unmatched_collections", List.of("collection_id"))).isTrue();
            assertThat(indexColumns(
                    connection, "receivables", "idx_receivable_customer_due_status"))
                    .containsExactly("customer_code", "due_date", "status");
            assertThat(indexColumns(connection, "sales_invoices", "idx_sales_invoice_due_status"))
                    .containsExactly("due_date", "status");
            assertThat(indexColumns(connection, "receivables", "idx_receivable_due_status"))
                    .containsExactly("due_date", "status");
            assertThat(indexColumns(connection, "collections", "idx_collection_status"))
                    .containsExactly("status");
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO collections (
                        collection_date, customer_code, amount, matched_amount, status, created_at)
                    VALUES (DATE '2026-01-01', 'CUSTOMER', 100.00, 101.00, 'CANCELLED', CURRENT_TIMESTAMP)
                    """))
                    .hasMessageContaining("ck_collection_amounts");
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO collections (
                        collection_date, customer_code, amount, matched_amount, status, created_at)
                    VALUES (DATE '2026-01-01', 'CUSTOMER', 100.00, 50.00, 'MATCHED', CURRENT_TIMESTAMP)
                    """))
                    .hasMessageContaining("ck_collection_status");
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO sales_invoices (
                        invoice_no, customer_code, issue_date, due_date,
                        total_amount, tax_amount, net_amount, status, created_by, created_at)
                    VALUES ('ZERO', 'CUSTOMER', DATE '2026-01-01', DATE '2026-01-02',
                        0.00, 0.00, 0.00, 'ISSUED', 'tester', CURRENT_TIMESTAMP)
                    """))
                    .hasMessageContaining("ck_sales_invoice_amounts");
            statement.executeUpdate("""
                    INSERT INTO sales_invoices (
                        invoice_no, customer_code, issue_date, due_date,
                        total_amount, tax_amount, net_amount, status, created_by, created_at)
                    VALUES ('SI-1', 'CUSTOMER', DATE '2026-01-01', DATE '2026-01-02',
                        110.00, 10.00, 100.00, 'ISSUED', 'tester', CURRENT_TIMESTAMP)
                    """);
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO receivables (
                        sales_invoice_id, customer_code, original_amount,
                        outstanding_amount, due_date, status, created_at)
                    SELECT id, 'CUSTOMER', 110.00, 50.00, DATE '2026-01-02', 'OPEN', CURRENT_TIMESTAMP
                    FROM sales_invoices WHERE invoice_no = 'SI-1'
                    """))
                    .hasMessageContaining("ck_receivable_status");
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO receivables (
                        sales_invoice_id, customer_code, original_amount,
                        outstanding_amount, due_date, status, created_at)
                    SELECT id, 'OTHER-CUSTOMER', 110.00, 110.00, DATE '2026-01-02', 'OPEN', CURRENT_TIMESTAMP
                    FROM sales_invoices WHERE invoice_no = 'SI-1'
                    """))
                    .hasMessageContaining("fk_receivable_sales_invoice");
            statement.executeUpdate("""
                    INSERT INTO collections (
                        collection_date, customer_code, amount, matched_amount, status, created_at)
                    VALUES (DATE '2026-01-01', 'CUSTOMER', 100.00, 50.00, 'UNMATCHED', CURRENT_TIMESTAMP)
                    """);
        }
    }

    @Test
    void reconciliationBaselineContainsOwnedTablesLineageAndIdempotencyConstraints() throws Exception {
        String url = "jdbc:h2:mem:reconciliation-parity"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        new MigrationExecutor().execute(
                MigrationContext.require("reconciliation"),
                MigrationAction.MIGRATE,
                new MigrationConfiguration(url, "sa", "", "reconciliation"));

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(Stream.of(
                            "reconciliation_units", "difference_reason_codes", "bank_statements",
                            "recon_external_stage_record", "reconciliation_rules", "reconciliation_runs",
                            "reconciliation_differences", "reconciliation_stage_results")
                    .allMatch(table -> {
                        try {
                            return tableExists(statement, table);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })).isTrue();
            assertNumericColumn(connection, "reconciliation_rules", "tolerance_value", 19, 8);
            assertNumericColumn(connection, "reconciliation_runs", "unmatched_amount", 19, 2);
            assertNumericColumn(connection, "reconciliation_differences", "difference_amount", 19, 2);
            assertThat(hasForeignKey(
                    connection, "reconciliation_rules", "reconciliation_unit_id",
                    "reconciliation_units", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "reconciliation_runs", "reconciliation_unit_id",
                    "reconciliation_units", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "reconciliation_differences", "reconciliation_run_id",
                    "reconciliation_runs", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "reconciliation_stage_results", "reconciliation_run_id",
                    "reconciliation_runs", "id")).isTrue();
            assertThat(hasUniqueIndexOnColumns(
                    connection, "reconciliation_stage_results",
                    List.of("reconciliation_run_id", "stage_code"))).isTrue();
            assertThat(hasUniqueIndexOnColumns(
                    connection, "reconciliation_differences",
                    List.of("adjustment_journal_entry_id"))).isTrue();
            assertThat(indexColumns(
                    connection, "recon_external_stage_record", "idx_recon_external_stage_summary"))
                    .containsExactly(
                            "unit_id", "stage_code", "reconciliation_date",
                            "product_code", "currency_code", "legal_entity_code");
            assertThat(indexColumns(connection, "reconciliation_runs", "idx_reconciliation_run_date_status"))
                    .containsExactly("reconciliation_date", "status");

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO bank_statements (
                        bank_code, account_no, transaction_date, withdrawal_amount, deposit_amount,
                        reconciliation_status, create_date, update_date, audit_user)
                    VALUES ('BANK', 'ACCOUNT', DATE '2026-01-01', 100.00, 100.00,
                        'UNMATCHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'tester')
                    """))
                    .hasMessageContaining("ck_bank_statement_amounts");

            statement.executeUpdate("""
                    INSERT INTO reconciliation_units (
                        name, frequency, reconciliation_type, is_active)
                    VALUES ('Daily bank', 'DAILY', 'BANK_BOOK', TRUE)
                    """);
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO reconciliation_runs (
                        reconciliation_unit_id, reconciliation_date, run_start_time, status,
                        total_items_source, total_amount_source, total_items_target, total_amount_target,
                        matched_items_count, matched_amount, unmatched_items_count, unmatched_amount)
                    SELECT id, DATE '2026-01-01', CURRENT_TIMESTAMP, 'SUCCESS',
                        0, 0.00, 0, 0.00, 0, 0.00, 0, 0.00
                    FROM reconciliation_units WHERE name = 'Daily bank'
                    """))
                    .hasMessageContaining("ck_reconciliation_run_lifecycle");

            statement.executeUpdate("""
                    INSERT INTO recon_external_stage_record (
                        unit_id, stage_code, reconciliation_date, item_reference, amount,
                        create_date, update_date, audit_user)
                    VALUES ('1', 'SOURCE', DATE '2026-01-01', 'ITEM-1', 10.00,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'tester')
                    """);
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO recon_external_stage_record (
                        unit_id, stage_code, reconciliation_date, item_reference, amount,
                        create_date, update_date, audit_user)
                    VALUES ('1', 'SOURCE', DATE '2026-01-01', 'ITEM-1', 10.00,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'tester')
                    """))
                    .hasMessageContaining("uk_recon_external_stage_item");

            statement.executeUpdate("""
                    INSERT INTO recon_external_stage_record (
                        unit_id, stage_code, reconciliation_date, item_reference, amount,
                        create_date, update_date, audit_user)
                    VALUES ('1', 'INTERFACE', DATE '2026-01-01', NULL, 10.00,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'tester')
                    """);
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO recon_external_stage_record (
                        unit_id, stage_code, reconciliation_date, item_reference, amount,
                        create_date, update_date, audit_user)
                    VALUES ('1', 'INTERFACE', DATE '2026-01-01', NULL, 10.00,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'tester')
                    """))
                    .hasMessageContaining("uk_recon_external_stage_aggregate");
            statement.executeUpdate("""
                    INSERT INTO recon_external_stage_record (
                        unit_id, stage_code, reconciliation_date, item_reference,
                        product_code, currency_code, amount, create_date, update_date, audit_user)
                    VALUES ('1', 'INTERFACE', DATE '2026-01-01', NULL,
                        'P2', 'EUR', 20.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'tester')
                    """);

            statement.executeUpdate("""
                    INSERT INTO reconciliation_runs (
                        reconciliation_unit_id, reconciliation_date, run_start_time, run_end_time, status,
                        total_items_source, total_amount_source, total_items_target, total_amount_target,
                        matched_items_count, matched_amount, unmatched_items_count, unmatched_amount)
                    SELECT id, DATE '2026-01-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SUCCESS',
                        0, 0.00, 0, 0.00, 0, 0.00, 0, 0.00
                    FROM reconciliation_units WHERE name = 'Daily bank'
                    """);
            statement.executeUpdate("""
                    INSERT INTO reconciliation_differences (
                        reconciliation_run_id, difference_type, source_item_ref, target_item_ref,
                        difference_amount, status)
                    SELECT id, 'MISSING_TARGET', 'SOURCE-1', NULL, 10.00, 'PENDING'
                    FROM reconciliation_runs WHERE reconciliation_date = DATE '2026-01-01'
                    """);
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO reconciliation_differences (
                        reconciliation_run_id, difference_type, source_item_ref, target_item_ref,
                        difference_amount, status)
                    SELECT id, 'MISSING_TARGET', 'SOURCE-1', NULL, 10.00, 'PENDING'
                    FROM reconciliation_runs WHERE reconciliation_date = DATE '2026-01-01'
                    """))
                    .hasMessageContaining("uk_reconciliation_difference_lineage");
            String oversizedReference = "R".repeat(256);
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO reconciliation_differences (
                        reconciliation_run_id, difference_type, source_item_ref, target_item_ref,
                        difference_amount, status)
                    SELECT id, 'MISSING_TARGET', '%s', NULL, 10.00, 'PENDING'
                    FROM reconciliation_runs WHERE reconciliation_date = DATE '2026-01-01'
                    """.formatted(oversizedReference)))
                    .hasMessageContaining("Value too long");
        }
    }

    @Test
    void taxBaselineContainsFinancialAndCancellationConstraints() throws Exception {
        String url = "jdbc:h2:mem:tax-parity"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        new MigrationExecutor().execute(
                MigrationContext.require("tax"),
                MigrationAction.MIGRATE,
                new MigrationConfiguration(url, "sa", "", "tax"));

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(tableExists(statement, "tax_invoices")).isTrue();
            assertNumericColumn(connection, "tax_invoices", "supply_amount", 19, 2);
            assertNumericColumn(connection, "tax_invoices", "tax_amount", 19, 2);
            assertNumericColumn(connection, "tax_invoices", "total_amount", 19, 2);
            assertThat(hasUniqueIndexOnColumns(connection, "tax_invoices", List.of("issue_id"))).isTrue();
            assertThat(indexColumns(connection, "tax_invoices", "idx_tax_invoice_issue_date"))
                    .containsExactly("issue_date");

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO tax_invoices (
                        issue_id, type, issue_date, business_partner_code,
                        supply_amount, tax_amount, total_amount, status)
                    VALUES ('INVALID-AMOUNT', 'PURCHASE', DATE '2026-01-01', 'BP-1',
                        100.00, 10.00, 100.00, 'ACTIVE')
                    """))
                    .hasMessageContaining("ck_tax_invoice_amounts");
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO tax_invoices (
                        issue_id, type, issue_date, business_partner_code,
                        supply_amount, tax_amount, total_amount, status)
                    VALUES ('INVALID-CANCEL', 'PURCHASE', DATE '2026-01-01', 'BP-1',
                        100.00, 10.00, 110.00, 'CANCELLED')
                    """))
                    .hasMessageContaining("ck_tax_invoice_lifecycle");
        }
    }

    @Test
    void assetLeaseBaselineContainsEveryJpaOwnedTable() throws Exception {
        String url = "jdbc:h2:mem:asset-lease-parity"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        MigrationConfiguration configuration = new MigrationConfiguration(url, "sa", "", "asset_lease");

        new MigrationExecutor().execute(
                MigrationContext.require("asset-lease"),
                MigrationAction.MIGRATE,
                configuration);

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(Stream.of(
                            "fixed_assets",
                            "asset_histories",
                            "lease_contracts",
                            "right_of_use_assets",
                            "lease_liabilities",
                            "lease_payment_schedules")
                    .allMatch(table -> {
                        try {
                            return tableExists(statement, table);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    }))
                    .isTrue();
            assertNumericColumn(connection, "fixed_assets", "acquisition_cost", 19, 2);
            assertNumericColumn(connection, "lease_contracts", "monthly_payment", 19, 2);
            assertNumericColumn(connection, "lease_contracts", "discount_rate", 7, 4);
            assertNumericColumn(connection, "right_of_use_assets", "current_book_value", 19, 2);
            assertNumericColumn(connection, "lease_liabilities", "current_value", 19, 2);
            assertNumericColumn(connection, "lease_payment_schedules", "remaining_lease_liability", 19, 2);
            assertThat(isNullable(connection, "asset_histories", "asset_id")).isFalse();
            assertThat(isNullable(connection, "asset_histories", "history_type")).isFalse();
            assertThat(isNullable(connection, "asset_histories", "event_at")).isFalse();
            assertThat(isNullable(connection, "asset_histories", "audit_user")).isFalse();
            assertThat(isNullable(connection, "fixed_assets", "current_book_value")).isFalse();
            assertThat(isNullable(connection, "fixed_assets", "account_code")).isFalse();

            assertThat(hasIndex(connection, "fixed_assets", "status", false)).isTrue();
            assertThat(hasIndex(connection, "asset_histories", "asset_id", false)).isTrue();
            assertThat(hasIndex(connection, "lease_payment_schedules", "lease_contract_id", false)).isTrue();
            assertThat(hasIndex(connection, "right_of_use_assets", "lease_contract_id", true)).isTrue();
            assertThat(hasIndex(connection, "lease_liabilities", "lease_contract_id", true)).isTrue();

            assertThat(hasForeignKey(
                    connection, "asset_histories", "asset_id", "fixed_assets", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "right_of_use_assets", "lease_contract_id", "lease_contracts", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "lease_liabilities", "lease_contract_id", "lease_contracts", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "lease_payment_schedules", "lease_contract_id", "lease_contracts", "id")).isTrue();
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO lease_contracts (
                        contract_no, contract_name, start_date, end_date, monthly_payment,
                        payment_day, discount_rate, ifrs16_applicable, short_term_lease, low_value_lease)
                    VALUES ('INVALID-DAY', 'Invalid day', DATE '2026-01-01', DATE '2026-12-31',
                        100.00, 0, 10.0000, TRUE, FALSE, FALSE)
                    """))
                    .hasMessageContaining("ck_lease_contract_payment_day");
        }
    }

    @Test
    void journalLedgerBaselineContainsEveryJpaOwnedTableAndFinancialConstraint() throws Exception {
        String url = "jdbc:h2:mem:journal-ledger-parity"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        MigrationConfiguration configuration = new MigrationConfiguration(url, "sa", "", "journal_ledger");

        new MigrationExecutor().execute(
                MigrationContext.require("journal-ledger"),
                MigrationAction.MIGRATE,
                configuration);

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(Stream.of(
                            "journal_entries", "journal_details", "journal_rules",
                            "journal_rule_conditions", "journal_rule_details",
                            "gl_entries", "sl_entries", "gl_balances", "sl_balances",
                            "unsettled_items")
                    .allMatch(table -> {
                        try {
                            return tableExists(statement, table);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    }))
                    .isTrue();
            assertNumericColumn(connection, "journal_entries", "exchange_rate", 19, 8);
            assertNumericColumn(connection, "journal_details", "amount", 19, 2);
            assertNumericColumn(connection, "journal_details", "base_amount", 19, 2);
            assertNumericColumn(connection, "gl_entries", "dr_amount", 19, 2);
            assertNumericColumn(connection, "sl_entries", "base_cr_amount", 19, 2);
            assertNumericColumn(connection, "gl_balances", "ending_balance", 19, 2);
            assertNumericColumn(connection, "sl_balances", "ending_balance", 19, 2);
            assertThat(hasForeignKey(
                    connection, "journal_details", "journal_entry_id", "journal_entries", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "gl_entries", "journal_detail_id", "journal_details", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "sl_entries", "journal_detail_id", "journal_details", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "unsettled_items", "journal_detail_id", "journal_details", "id")).isTrue();
            assertThat(indexColumns(connection, "journal_entries", "idx_journal_entry_lineage"))
                    .containsExactly("lineage_source_type", "lineage_source_id");
            assertThat(indexColumns(connection, "gl_balances", "idx_gl_balance_lookup"))
                    .containsExactly("account_code", "currency_code", "balance_date");
            assertThat(hasUniqueIndexOnColumns(connection, "journal_entries", List.of("slip_no"))).isTrue();
            assertThat(hasUniqueIndexOnColumns(connection, "unsettled_items", List.of("management_no"))).isTrue();
            assertThat(hasUniqueIndexOnColumns(
                    connection,
                    "gl_balances",
                    List.of("account_code", "currency_code", "balance_date", "period"))).isTrue();
            statement.executeUpdate("""
                    INSERT INTO sl_balances (
                        balance_date, beginning_balance, debit_amount, credit_amount,
                        ending_balance, currency_code, account_code, bp_code, dept_code, period)
                    VALUES (DATE '2026-08-04', 0.00, 100.00, 0.00,
                        100.00, 'KRW', '101000', NULL, NULL, '2026-08')
                    """);
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO sl_balances (
                        balance_date, beginning_balance, debit_amount, credit_amount,
                        ending_balance, currency_code, account_code, bp_code, dept_code, period)
                    VALUES (DATE '2026-08-04', 0.00, 100.00, 0.00,
                        100.00, 'KRW', '101000', NULL, NULL, '2026-08')
                    """))
                    .isInstanceOf(java.sql.SQLException.class);
        }
    }

    @Test
    void journalLedgerPlaceholderHistoryUpgradesForwardToV11() throws Exception {
        String url = "jdbc:h2:mem:journal-ledger-v10-upgrade"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/contexts/journal-ledger")
                .target("10")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE sl_balances (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        balance_date DATE NOT NULL,
                        beginning_balance NUMERIC(19, 2) NOT NULL,
                        debit_amount NUMERIC(19, 2) NOT NULL,
                        credit_amount NUMERIC(19, 2) NOT NULL,
                        ending_balance NUMERIC(19, 2) NOT NULL,
                        currency_code VARCHAR(3) NOT NULL,
                        account_code VARCHAR(50) NOT NULL,
                        bp_code VARCHAR(50),
                        dept_code VARCHAR(50),
                        period VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP(6),
                        updated_at TIMESTAMP(6),
                        CONSTRAINT uk_sl_balance_key UNIQUE
                            (account_code, bp_code, dept_code, currency_code, balance_date, period)
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO sl_balances (
                        balance_date, beginning_balance, debit_amount, credit_amount,
                        ending_balance, currency_code, account_code, bp_code, dept_code, period)
                    VALUES (DATE '2026-08-04', 0.00, 100.00, 0.00,
                        100.00, 'KRW', '101000', NULL, NULL, '2026-08')
                    """);
        }

        MigrationConfiguration configuration =
                new MigrationConfiguration(url, "sa", "", "journal_ledger");
        MigrationExecutor executor = new MigrationExecutor();
        assertThat(executor.execute(
                MigrationContext.require("journal-ledger"), MigrationAction.MIGRATE, configuration))
                .isGreaterThanOrEqualTo(1);
        assertThat(executor.execute(
                MigrationContext.require("journal-ledger"), MigrationAction.VALIDATE, configuration)).isZero();
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(tableExists(statement, "journal_entries")).isTrue();
            assertThat(tableExists(statement, "batch_job_instance")).isTrue();
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO sl_balances (
                        balance_date, beginning_balance, debit_amount, credit_amount,
                        ending_balance, currency_code, account_code, bp_code, dept_code, period)
                    VALUES (DATE '2026-08-04', 0.00, 100.00, 0.00,
                        100.00, 'KRW', '101000', NULL, NULL, '2026-08')
                    """))
                    .isInstanceOf(java.sql.SQLException.class);
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT COUNT(*) FROM flyway_schema_history
                    WHERE version = '11' AND success = TRUE
                    """)) {
                resultSet.next();
                assertThat(resultSet.getInt(1)).isOne();
            }
        }
    }

    @Test
    void masterDataBaselineContainsEveryJpaOwnedTableAndConstraint() throws Exception {
        String url = "jdbc:h2:mem:master-data-parity"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        MigrationConfiguration configuration = new MigrationConfiguration(url, "sa", "", "master_data");

        new MigrationExecutor().execute(
                MigrationContext.require("master-data"),
                MigrationAction.MIGRATE,
                configuration);

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(Stream.of(
                            "account_subjects", "business_partners", "business_partner_accounts",
                            "currencies", "departments", "exchange_rates", "fiscal_periods",
                            "master_data_change_requests", "products", "tax_profiles")
                    .allMatch(table -> {
                        try {
                            return tableExists(statement, table);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    }))
                    .isTrue();
            assertNumericColumn(connection, "exchange_rates", "rate", 19, 8);
            assertNumericColumn(connection, "products", "price", 19, 4);
            assertNumericColumn(connection, "tax_profiles", "tax_rate", 7, 4);
            assertThat(hasForeignKey(
                    connection, "account_subjects", "parent_id", "account_subjects", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "departments", "parent_id", "departments", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection,
                    "business_partner_accounts",
                    "business_partner_id",
                    "business_partners",
                    "id")).isTrue();
            assertThat(indexColumns(connection, "account_subjects", "idx_account_code_valid"))
                    .containsExactly("code", "valid_from", "valid_to");
            assertThat(indexColumns(connection, "business_partners", "idx_bp_code_valid"))
                    .containsExactly("business_partner_code", "valid_from", "valid_to");
            assertThat(hasUniqueIndexOnColumns(
                    connection,
                    "exchange_rates",
                    List.of("from_currency_code", "to_currency_code", "effective_date"))).isTrue();
            assertThat(hasUniqueIndexOnColumns(
                    connection, "fiscal_periods", List.of("fiscal_year", "fiscal_period"))).isTrue();
            assertThat(hasUniqueIndexOnColumns(
                    connection, "master_data_change_requests", List.of("source_reference"))).isTrue();
        }
    }

    @Test
    void masterDataPublishedHistoryUpgradesForwardFromV5ToV6() throws Exception {
        String url = "jdbc:h2:mem:master-data-v5-upgrade"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/contexts/master-data")
                .target("5")
                .load()
                .migrate();

        MigrationConfiguration configuration = new MigrationConfiguration(url, "sa", "", "master_data");
        MigrationExecutor executor = new MigrationExecutor();
        assertThat(executor.execute(
                MigrationContext.require("master-data"), MigrationAction.MIGRATE, configuration))
                .isGreaterThanOrEqualTo(1);
        assertThat(executor.execute(
                MigrationContext.require("master-data"), MigrationAction.VALIDATE, configuration)).isZero();
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(tableExists(statement, "account_subjects")).isTrue();
            assertThat(tableExists(statement, "master_data_change_requests")).isTrue();
            assertThat(tableExists(statement, "batch_job_instance")).isTrue();
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT COUNT(*) FROM flyway_schema_history
                    WHERE version = '6' AND success = TRUE
                    """)) {
                resultSet.next();
                assertThat(resultSet.getInt(1)).isOne();
            }
        }
    }

    @Test
    void closingBaselineContainsEveryJpaOwnedTableAndRelationship() throws Exception {
        String url = "jdbc:h2:mem:closing-parity"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        MigrationConfiguration configuration = new MigrationConfiguration(url, "sa", "", "closing");

        new MigrationExecutor().execute(
                MigrationContext.require("closing"),
                MigrationAction.MIGRATE,
                configuration);

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(Stream.of(
                            "closing_calendars",
                            "closing_tasks",
                            "closing_gates",
                            "closing_audit_logs",
                            "period_locks",
                            "reopen_approvals",
                            "valuation_batches",
                            "provision_batches",
                            "closing_adjustments",
                            "daily_closing_status")
                    .allMatch(table -> {
                        try {
                            return tableExists(statement, table);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    }))
                    .isTrue();
            assertThat(isNullable(connection, "closing_calendars", "fiscal_year")).isFalse();
            assertThat(isNullable(connection, "closing_tasks", "calendar_id")).isFalse();
            assertThat(isNullable(connection, "closing_audit_logs", "action_user")).isFalse();
            assertThat(isNullable(connection, "valuation_batches", "run_date_time")).isFalse();
            assertThat(isNullable(connection, "daily_closing_status", "state")).isFalse();
            assertThat(hasIndex(connection, "closing_calendars", "fiscal_year", true)).isTrue();
            assertThat(hasIndex(connection, "closing_tasks", "calendar_id", false)).isTrue();
            assertThat(hasIndex(connection, "closing_gates", "calendar_id", false)).isTrue();
            assertThat(hasForeignKey(
                    connection, "closing_tasks", "calendar_id", "closing_calendars", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "closing_gates", "calendar_id", "closing_calendars", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "closing_audit_logs", "calendar_id", "closing_calendars", "id")).isTrue();
        }
    }

    @Test
    void existingClosingV49ShapeIsBaselinedAndUpgradedWithoutReapplyingBaseline() throws Exception {
        String url = "jdbc:h2:mem:closing-legacy-upgrade"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/contexts/closing")
                .table("flyway_schema_history_closing")
                .target("49")
                .load()
                .migrate();
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE flyway_schema_history_closing");
            statement.execute("DROP INDEX idx_closing_task_calendar_order");
            statement.executeUpdate("""
                    INSERT INTO daily_closing_status(date, is_closed, closed_by)
                    VALUES (DATE '2026-07-31', TRUE, 'legacy-operator')
                    """);
        }

        MigrationConfiguration configuration =
                new MigrationConfiguration(url, "sa", "", "closing");
        new MigrationExecutor().execute(
                MigrationContext.require("closing"),
                MigrationAction.MIGRATE,
                configuration);
        new MigrationExecutor().execute(
                MigrationContext.require("closing"),
                MigrationAction.VALIDATE,
                configuration);

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT state, created_by
                    FROM daily_closing_status
                    WHERE date = DATE '2026-07-31'
                    """)) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("state")).isEqualTo("CLOSED");
                assertThat(resultSet.getString("created_by")).isEqualTo("legacy-operator");
            }
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM flyway_schema_history_closing
                    WHERE version = '49' AND type = 'BASELINE'
                    """)) {
                resultSet.next();
                assertThat(resultSet.getInt(1)).isOne();
            }
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM flyway_schema_history_closing
                    WHERE version = '51' AND success = TRUE
                    """)) {
                resultSet.next();
                assertThat(resultSet.getInt(1)).isOne();
            }
            assertThat(indexColumns(
                    connection, "closing_tasks", "idx_closing_task_calendar_order"))
                    .containsExactly("calendar_id", "task_order");
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM flyway_schema_history_closing
                    WHERE version = '50' AND success = TRUE
                    """)) {
                resultSet.next();
                assertThat(resultSet.getInt(1)).isOne();
            }
        }
    }

    @Test
    void incompleteClosingLegacyShapeIsRejectedBeforeAutomaticBaseline() throws Exception {
        String url = "jdbc:h2:mem:closing-incomplete-legacy"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE closing_calendars (
                        id BIGINT PRIMARY KEY,
                        fiscal_year VARCHAR(4) NOT NULL,
                        fiscal_period VARCHAR(20) NOT NULL,
                        status VARCHAR(30) NOT NULL,
                        is_current_period BOOLEAN NOT NULL)
                    """);
        }

        assertThatThrownBy(() -> new MigrationExecutor().execute(
                MigrationContext.require("closing"),
                MigrationAction.MIGRATE,
                new MigrationConfiguration(url, "sa", "", "closing")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("complete version 49 table set")
                .hasMessageContaining("refusing automatic baseline");
    }

    @Test
    void incompleteClosingShapeWithBaselineHistoryIsRejectedBeforeV50() throws Exception {
        String url = "jdbc:h2:mem:closing-incomplete-with-history"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE closing_calendars (
                        id BIGINT PRIMARY KEY,
                        fiscal_year VARCHAR(4) NOT NULL,
                        fiscal_period VARCHAR(20) NOT NULL,
                        status VARCHAR(30) NOT NULL,
                        is_current_period BOOLEAN NOT NULL)
                    """);
        }
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/contexts/closing")
                .table("flyway_schema_history_closing")
                .baselineVersion("49")
                .load()
                .baseline();

        assertThatThrownBy(() -> new MigrationExecutor().execute(
                MigrationContext.require("closing"),
                MigrationAction.MIGRATE,
                new MigrationConfiguration(url, "sa", "", "closing")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("complete version 49 table set")
                .hasMessageContaining("refusing automatic baseline");
    }

    @Test
    void closingLegacyShapeMissingAnyJpaColumnIsRejected() throws Exception {
        String url = "jdbc:h2:mem:closing-missing-column"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/contexts/closing")
                .table("flyway_schema_history_closing")
                .target("49")
                .load()
                .migrate();
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE flyway_schema_history_closing");
            statement.execute("ALTER TABLE closing_tasks DROP COLUMN description");
        }

        assertThatThrownBy(() -> new MigrationExecutor().execute(
                MigrationContext.require("closing"),
                MigrationAction.MIGRATE,
                new MigrationConfiguration(url, "sa", "", "closing")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different column set for closing_tasks")
                .hasMessageContaining("description")
                .hasMessageContaining("refusing automatic baseline");
    }

    @Test
    void unrelatedNonEmptySchemaCannotBeMistakenForCleanClosingDatabase() throws Exception {
        String url = "jdbc:h2:mem:closing-unrelated-nonempty"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE unrelated_context_table (id BIGINT PRIMARY KEY)");
        }

        assertThatThrownBy(() -> new MigrationExecutor().execute(
                MigrationContext.require("closing"),
                MigrationAction.MIGRATE,
                new MigrationConfiguration(url, "sa", "", "closing")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non-empty")
                .hasMessageContaining("no version 49 Closing tables")
                .hasMessageContaining("refusing automatic baseline");
    }

    @ParameterizedTest
    @MethodSource("invalidClosingLegacyContracts")
    void closingLegacyTypeNullabilityAndKeysMustMatchBeforeBaseline(
            String databaseName,
            String mutation,
            String expectedMessage) throws Exception {
        String url = "jdbc:h2:mem:" + databaseName
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/contexts/closing")
                .table("flyway_schema_history_closing")
                .target("49")
                .load()
                .migrate();
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE flyway_schema_history_closing");
            for (String sql : mutation.split(";;")) {
                statement.execute(sql);
            }
        }

        assertThatThrownBy(() -> new MigrationExecutor().execute(
                MigrationContext.require("closing"),
                MigrationAction.MIGRATE,
                new MigrationConfiguration(url, "sa", "", "closing")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(expectedMessage)
                .hasMessageContaining("refusing automatic baseline");
    }

    private static Stream<Arguments> postgresqlCompatibleContexts() {
        return Stream.of(
                Arguments.of("asset-lease", "fixed_assets"),
                Arguments.of("auth", "auth_users"),
                Arguments.of("budget", "budget_plans"),
                Arguments.of("closing", "closing_calendars"),
                Arguments.of("deposit", "deposit_accounts"),
                Arguments.of("expenditure-resolution", "expenditure_resolutions"),
                Arguments.of("journal-ledger", "journal_entries"),
                Arguments.of("loan", "loans"),
                Arguments.of("master-data", "account_subjects"),
                Arguments.of("payable", "payables"),
                Arguments.of("receivable", "receivables"),
                Arguments.of("reconciliation", "reconciliation_runs"),
                Arguments.of("reporting", "rpt_snapshot_header"),
                Arguments.of("tax", "tax_invoices"));
    }

    private static Stream<Arguments> malformedBatchMetadataMutations() {
        return Stream.of(
                Arguments.of(
                        "bad-batch-column",
                        "ALTER TABLE batch_job_instance ALTER COLUMN job_key VARCHAR(64)",
                        "incompatible definition"),
                Arguments.of(
                        "bad-batch-unique",
                        "ALTER TABLE batch_job_instance DROP CONSTRAINT job_inst_un;;"
                                + "ALTER TABLE batch_job_instance ADD UNIQUE (job_name);;"
                                + "ALTER TABLE batch_job_instance ADD UNIQUE (job_key)",
                        "unique key"),
                Arguments.of(
                        "bad-batch-sequence",
                        "ALTER SEQUENCE batch_job_seq INCREMENT BY 2",
                        "incompatible definition"));
    }

    private static Stream<Arguments> invalidClosingLegacyContracts() {
        return Stream.of(
                Arguments.of(
                        "closing-nullable-state",
                        "ALTER TABLE daily_closing_status ALTER COLUMN is_closed DROP NOT NULL",
                        "incompatible nullability for daily_closing_status.is_closed"),
                Arguments.of(
                        "closing-invalid-type",
                        "ALTER TABLE closing_tasks ALTER COLUMN description BIGINT",
                        "incompatible type for closing_tasks.description"),
                Arguments.of(
                        "closing-invalid-length",
                        "ALTER TABLE closing_tasks ALTER COLUMN name VARCHAR(1)",
                        "incompatible length for closing_tasks.name"),
                Arguments.of(
                        "closing-unexpected-column",
                        "ALTER TABLE closing_tasks ADD legacy_required VARCHAR(10) NOT NULL",
                        "different column set for closing_tasks"),
                Arguments.of(
                        "closing-non-identity-id",
                        "ALTER TABLE period_locks ALTER COLUMN id DROP IDENTITY",
                        "non-identity primary key for period_locks.id"),
                Arguments.of(
                        "closing-missing-primary-key",
                        "ALTER TABLE daily_closing_status DROP PRIMARY KEY",
                        "incompatible primary key for daily_closing_status"),
                Arguments.of(
                        "closing-missing-foreign-key",
                        "ALTER TABLE closing_tasks DROP CONSTRAINT fk_closing_task_calendar",
                        "missing the required foreign key closing_tasks.calendar_id"),
                Arguments.of(
                        "closing-missing-period-unique",
                        "ALTER TABLE closing_calendars DROP CONSTRAINT uk_closing_calendar_period",
                        "missing the required unique key on closing_calendars"),
                Arguments.of(
                        "closing-wrong-operational-index",
                        "DROP INDEX idx_closing_task_calendar_order;;"
                                + "CREATE INDEX idx_closing_task_calendar_order ON closing_tasks(name)",
                        "incompatible index idx_closing_task_calendar_order"));
    }

    private boolean tableExists(Statement statement, String tableName) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = '" + tableName + "'")) {
            resultSet.next();
            return resultSet.getInt(1) == 1;
        }
    }

    private void assertNumericColumn(
            Connection connection,
            String tableName,
            String columnName,
            int precision,
            int scale) throws Exception {
        try (ResultSet resultSet = connection.getMetaData().getColumns(
                null, null, tableName, columnName)) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("COLUMN_SIZE")).isEqualTo(precision);
            assertThat(resultSet.getInt("DECIMAL_DIGITS")).isEqualTo(scale);
        }
    }

    private boolean isNullable(
            Connection connection,
            String tableName,
            String columnName) throws Exception {
        try (ResultSet resultSet = connection.getMetaData().getColumns(
                null, null, tableName, columnName)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
        }
    }

    private boolean hasIndex(
            Connection connection,
            String tableName,
            String columnName,
            boolean unique) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getIndexInfo(null, null, tableName, unique, false)) {
            while (resultSet.next()) {
                if (columnName.equals(resultSet.getString("COLUMN_NAME"))
                        && (!unique || !resultSet.getBoolean("NON_UNIQUE"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean hasForeignKey(
            Connection connection,
            String tableName,
            String columnName,
            String referencedTable,
            String referencedColumn) throws Exception {
        try (ResultSet resultSet = connection.getMetaData().getImportedKeys(
                null, null, tableName)) {
            while (resultSet.next()) {
                if (columnName.equals(resultSet.getString("FKCOLUMN_NAME"))
                        && referencedTable.equals(resultSet.getString("PKTABLE_NAME"))
                        && referencedColumn.equals(resultSet.getString("PKCOLUMN_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean hasCompositeForeignKey(
            Connection connection,
            String tableName,
            List<String> columnNames,
            String referencedTable,
            List<String> referencedColumns) throws Exception {
        Map<String, List<String>> localColumns = new LinkedHashMap<>();
        Map<String, List<String>> targetColumns = new LinkedHashMap<>();
        try (ResultSet resultSet = connection.getMetaData().getImportedKeys(
                null, null, tableName)) {
            while (resultSet.next()) {
                if (!referencedTable.equals(resultSet.getString("PKTABLE_NAME"))) {
                    continue;
                }
                String foreignKeyName = resultSet.getString("FK_NAME");
                int position = resultSet.getInt("KEY_SEQ");
                putAt(localColumns, foreignKeyName, position, resultSet.getString("FKCOLUMN_NAME"));
                putAt(targetColumns, foreignKeyName, position, resultSet.getString("PKCOLUMN_NAME"));
            }
        }
        return localColumns.entrySet().stream().anyMatch(entry -> {
            List<String> actualLocal = compact(entry.getValue());
            List<String> actualTarget = compact(targetColumns.get(entry.getKey()));
            return columnNames.equals(actualLocal) && referencedColumns.equals(actualTarget);
        });
    }

    private void putAt(
            Map<String, List<String>> values,
            String key,
            int position,
            String value) {
        List<String> ordered = values.computeIfAbsent(key, ignored -> new ArrayList<>());
        while (ordered.size() < position) {
            ordered.add(null);
        }
        ordered.set(position - 1, value);
    }

    private List<String> compact(List<String> values) {
        return values == null
                ? List.of()
                : values.stream().filter(java.util.Objects::nonNull).toList();
    }

    private List<String> indexColumns(
            Connection connection,
            String tableName,
            String indexName) throws Exception {
        List<String> columns = new ArrayList<>();
        try (ResultSet resultSet = connection.getMetaData().getIndexInfo(
                null, null, tableName, false, false)) {
            while (resultSet.next()) {
                if (indexName.equals(resultSet.getString("INDEX_NAME"))
                        && resultSet.getString("COLUMN_NAME") != null) {
                    int position = resultSet.getInt("ORDINAL_POSITION");
                    while (columns.size() < position) {
                        columns.add(null);
                    }
                    columns.set(position - 1, resultSet.getString("COLUMN_NAME"));
                }
            }
        }
        return columns.stream().filter(java.util.Objects::nonNull).toList();
    }

    private boolean hasUniqueIndexOnColumns(
            Connection connection,
            String tableName,
            List<String> expectedColumns) throws Exception {
        Map<String, List<String>> indexes = new LinkedHashMap<>();
        try (ResultSet resultSet = connection.getMetaData().getIndexInfo(
                null, null, tableName, true, false)) {
            while (resultSet.next()) {
                String indexName = resultSet.getString("INDEX_NAME");
                String columnName = resultSet.getString("COLUMN_NAME");
                if (indexName == null || columnName == null || resultSet.getBoolean("NON_UNIQUE")) {
                    continue;
                }
                List<String> columns = indexes.computeIfAbsent(indexName, ignored -> new ArrayList<>());
                int position = resultSet.getInt("ORDINAL_POSITION");
                while (columns.size() < position) {
                    columns.add(null);
                }
                columns.set(position - 1, columnName);
            }
        }
        return indexes.values().stream()
                .map(columns -> columns.stream().filter(java.util.Objects::nonNull).toList())
                .anyMatch(expectedColumns::equals);
    }
}
