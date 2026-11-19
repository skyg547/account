package com.ho.account.expenditure.payable.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.expenditure.repository.PayableRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
        classes = PayableApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=local",
                "spring.datasource.url=jdbc:h2:mem:payable-api-pg-schema;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/payable-migration",
                "spring.flyway.clean-disabled=true",
                "spring.flyway.baseline-on-migrate=false",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.sql.init.mode=never",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "management.tracing.enabled=false"
        })
class PayablePostgresqlSchemaContextTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PayableRepository repository;

    @Test
    void cleanBaselineMigratesValidatesAndSupportsRepository() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE version IN ('1', '2') AND success = TRUE
                """, Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('purchase_invoices', 'payables', 'payment_runs',
                                     'payments', 'advance_payments', 'payable_offset_operations')
                """, Integer.class)).isEqualTo(6);
        assertThat(repository.count()).isZero();
    }

    @Test
    @Transactional
    void offsetIdentityIsScopedToPayableInMigratedSchema() {
        jdbcTemplate.update("""
                INSERT INTO purchase_invoices (invoice_no, vendor_code, issue_date, due_date,
                    total_amount, tax_amount, net_amount, status, created_by, created_at)
                VALUES ('INV-OFFSET-100', 'V001', '2026-05-29', '2026-06-29',
                    500.00, 0.00, 500.00, 'RECEIVED', 'SYSTEM', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO purchase_invoices (invoice_no, vendor_code, issue_date, due_date,
                    total_amount, tax_amount, net_amount, status, created_by, created_at)
                VALUES ('INV-OFFSET-101', 'V001', '2026-05-29', '2026-06-29',
                    500.00, 0.00, 500.00, 'RECEIVED', 'SYSTEM', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO payables (id, purchase_invoice_invoice_no, purchase_invoice_vendor_code,
                    vendor_code, original_amount, outstanding_amount, due_date, status, created_at)
                VALUES (100, 'INV-OFFSET-100', 'V001', 'V001', 500.00, 500.00,
                    '2026-06-29', 'OPEN', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO payables (id, purchase_invoice_invoice_no, purchase_invoice_vendor_code,
                    vendor_code, original_amount, outstanding_amount, due_date, status, created_at)
                VALUES (101, 'INV-OFFSET-101', 'V001', 'V001', 500.00, 500.00,
                    '2026-06-29', 'OPEN', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO advance_payments (id, vendor_code, payment_date, amount,
                    outstanding_amount, status, created_at)
                VALUES (200, 'V001', '2026-05-29', 200.00, 200.00, 'ACTIVE', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO payable_offset_operations (payable_id, operation_id, advance_payment_id,
                    amount, accounting_date, journal_entry_id)
                VALUES (100, 'same-client-id', 200, 100.00, '2026-05-29', 40)
                """);
        jdbcTemplate.update("""
                INSERT INTO payable_offset_operations (payable_id, operation_id, advance_payment_id,
                    amount, accounting_date, journal_entry_id)
                VALUES (101, 'same-client-id', 200, 100.00, '2026-05-29', 41)
                """);

        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM payable_offset_operations WHERE operation_id = 'same-client-id'
                """, Integer.class)).isEqualTo(2);
    }
}
