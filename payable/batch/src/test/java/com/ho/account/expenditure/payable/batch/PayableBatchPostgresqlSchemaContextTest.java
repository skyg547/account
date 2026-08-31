package com.ho.account.expenditure.payable.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.expenditure.domain.PayableStatus;
import com.ho.account.expenditure.infrastructure.persistence.entity.PayableJpaEntity;
import com.ho.account.expenditure.repository.PayableRepository;
import com.ho.account.expenditure.repository.PaymentRunRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        classes = PayableBatchApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=local",
                "spring.datasource.url=jdbc:h2:mem:payable-batch-pg-schema;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/payable-migration",
                "spring.flyway.clean-disabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.batch.job.enabled=false",
                "spring.batch.jdbc.initialize-schema=always",
                "spring.sql.init.mode=never",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "management.tracing.enabled=false"
        })
class PayableBatchPostgresqlSchemaContextTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PayableRepository payableRepository;

    @Autowired
    private PaymentRunRepository paymentRunRepository;

    @Test
    void batchMetadataCoexistsWithValidatedBusinessSchema() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE version = '1' AND success = TRUE
                """, Integer.class)).isOne();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = 'batch_job_instance'
                """, Integer.class)).isOne();
    }

    @Test
    @DisplayName("IN_PAYMENT, APPROVED, UNPAID 상태의 채무가 DB 제약조건을 만족하고 저장된다")
    void payableStatusTransitionsPersistSafely() {
        jdbcTemplate.update("""
                INSERT INTO purchase_invoices (invoice_no, vendor_code, issue_date, due_date,
                    total_amount, tax_amount, net_amount, status, created_by, created_at)
                VALUES ('INV-BATCH-001', 'V-BATCH', '2026-06-01', '2026-06-30',
                    1000.00, 100.00, 900.00, 'RECEIVED', 'SYSTEM', CURRENT_TIMESTAMP)
                """);

        PayableJpaEntity entity = new PayableJpaEntity();
        entity.setPurchaseInvoiceNo("INV-BATCH-001");
        entity.setPurchaseInvoiceVendorCode("V-BATCH");
        entity.setVendorCode("V-BATCH");
        entity.setOriginalAmount(new BigDecimal("1000.00"));
        entity.setOutstandingAmount(new BigDecimal("1000.00"));
        entity.setDueDate(LocalDate.of(2026, 6, 30));
        entity.setStatus(PayableStatus.IN_PAYMENT);

        PayableJpaEntity saved = payableRepository.save(entity);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(PayableStatus.IN_PAYMENT);

        assertThat(paymentRunRepository.findMatchingPaymentRuns(
                LocalDate.of(2026, 6, 30), "Batch Payment Run", "SYSTEM")).isEmpty();
    }
}
