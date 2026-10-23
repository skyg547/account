package com.ho.account.journalledger;

import com.ho.account.journalledger.infrastructure.persistence.repository.UnsettledItemRepository;
import com.ho.account.journalledger.application.service.unsettled.UnsettledService;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.unsettled.UnsettledItem;
import com.ho.account.journalledger.infrastructure.persistence.UnsettledItemPersistenceAdapter;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@SpringBootTest(
        classes = UnsettledSettlementPersistenceTest.SettlementPersistenceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.config.name=unsettled-settlement-test",
                "spring.config.import=",
                "spring.profiles.active=local",
                "spring.datasource.url=jdbc:h2:mem:unsettled-settlement-precision;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/journal-migration",
                "spring.flyway.clean-disabled=true",
                "spring.flyway.baseline-on-migrate=false",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.sql.init.mode=never",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "spring.kafka.listener.auto-startup=false",
                "spring.batch.job.enabled=false"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UnsettledSettlementPersistenceTest {

    @Autowired
    private UnsettledService service;

    @Autowired
    private UnsettledItemPersistenceAdapter persistenceAdapter;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate newTransaction;

    @BeforeEach
    void setUp() {
        newTransaction = new TransactionTemplate(transactionManager);
        newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThat(AopUtils.isAopProxy(service)).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE version IN ('1', '10', '11', '12') AND success = TRUE
                """, Integer.class)).isEqualTo(4);
    }

    @Test
    void fractionalCentRejectionLeavesCommittedOpenRowAndReferenceRowsUnchanged() {
        Long id = persistOpenItem("REJECT");
        SettlementState before = readCommittedState(id);
        assertThat(before.original()).isEqualTo(new BigDecimal("1.00"));
        assertThat(before.settled()).isEqualTo(new BigDecimal("0.00"));
        assertThat(before.remaining()).isEqualTo(new BigDecimal("1.00"));
        assertThat(before.status()).isEqualTo("OPEN");
        assertThat(before.resolved()).isFalse();
        assertThat(before.references()).isEmpty();
        assertThat(before.lastReference()).isNull();
        assertThat(before.actor()).isNull();
        assertThat(before.settledAt()).isNull();

        assertThatIllegalArgumentException().isThrownBy(
                () -> service.settleItem(id, new BigDecimal("0.999"), "collector", "REJECTED-REF"));

        assertThat(readCommittedState(id)).isEqualTo(before);
    }

    @Test
    void exactCentSettlementsSurviveJpaRoundTripAndReplaysPreserveFinalState() {
        Long id = persistOpenItem("EXACT");

        service.settleItem(id, new BigDecimal("0.01"), "first-collector", "FIRST-REF");

        SettlementState partial = readCommittedState(id);
        assertThat(partial.original()).isEqualTo(new BigDecimal("1.00"));
        assertThat(partial.settled()).isEqualTo(new BigDecimal("0.01"));
        assertThat(partial.remaining()).isEqualTo(new BigDecimal("0.99"));
        assertThat(partial.status()).isEqualTo("PARTIAL");
        assertThat(partial.resolved()).isFalse();
        assertThat(partial.references()).containsExactly("FIRST-REF");
        assertThat(partial.lastReference()).isEqualTo("FIRST-REF");
        assertThat(partial.actor()).isEqualTo("first-collector");
        assertThat(partial.settledAt()).isNotNull();

        assertThatIllegalArgumentException().isThrownBy(
                () -> service.settleItem(id, new BigDecimal("0.001"), "rejected-collector", "REJECTED-REF"));
        assertThat(readCommittedState(id)).isEqualTo(partial);

        service.settleItem(id, new BigDecimal("0.99"), "last-collector", "LAST-REF");

        SettlementState cleared = readCommittedState(id);
        assertThat(cleared.original()).isEqualTo(new BigDecimal("1.00"));
        assertThat(cleared.settled()).isEqualTo(new BigDecimal("1.00"));
        assertThat(cleared.remaining()).isEqualTo(new BigDecimal("0.00"));
        assertThat(cleared.status()).isEqualTo("CLEARED");
        assertThat(cleared.resolved()).isTrue();
        assertThat(cleared.references()).containsExactlyInAnyOrder("FIRST-REF", "LAST-REF");
        assertThat(cleared.lastReference()).isEqualTo("LAST-REF");
        assertThat(cleared.actor()).isEqualTo("last-collector");
        assertThat(cleared.settledAt()).isNotNull();

        service.settleItem(id, new BigDecimal("0.999"), "replay-collector", "FIRST-REF");
        service.settleItem(id, null, "replay-collector", "LAST-REF");

        assertThat(readCommittedState(id)).isEqualTo(cleared);
    }

    private Long persistOpenItem(String suffix) {
        return newTransaction.execute(status -> {
            JournalEntry entry = new JournalEntry();
            entry.setSlipNo("PRECISION-" + suffix);
            entry.setSlipDate(LocalDate.of(2026, 9, 1));
            entry.setAccountingDate(LocalDate.of(2026, 9, 1));
            entry.setCurrencyCode("KRW");
            entry.setCreatedBy("fixture-collector");
            entry.initializeDraft();
            JournalDetail receivable = detail(JournalSide.DEBIT, "11000");
            entry.addDetail(receivable);
            entry.addDetail(detail(JournalSide.CREDIT, "41000"));
            entry.validateInvariants();
            entityManager.persist(entry);

            UnsettledItem item = new UnsettledItem();
            item.setManagementNo("UNS-PRECISION-" + suffix);
            item.setJournalDetail(receivable);
            item.setAccountCode("11000");
            item.setBusinessPartnerCode("BP-PRECISION");
            item.setOccurrenceDate(entry.getAccountingDate());
            item.setOriginalAmount(new BigDecimal("1.00"));
            service.registerUnsettledItem(item);
            entityManager.flush();
            return item.getId();
        });
    }

    private static JournalDetail detail(JournalSide side, String accountCode) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setAmount(new BigDecimal("1.00"));
        detail.setBaseAmount(new BigDecimal("1.00"));
        detail.setBusinessPartnerCode("BP-PRECISION");
        detail.setAuditUser("fixture-collector");
        return detail;
    }

    private SettlementState readCommittedState(Long id) {
        // 별도 트랜잭션과 비워진 영속성 컨텍스트로 조회하여 메모리 값이 아닌 저장 결과를 확인합니다.
        return newTransaction.execute(status -> {
            entityManager.flush();
            entityManager.clear();
            UnsettledItem item = persistenceAdapter.findById(id).orElseThrow();
            Set<String> referenceRows = new LinkedHashSet<>(jdbcTemplate.queryForList("""
                    SELECT settlement_reference FROM unsettled_item_settlement_references
                    WHERE unsettled_item_id = ?
                    """, String.class, id));
            assertThat(item.getSettlementReferences()).containsExactlyInAnyOrderElementsOf(referenceRows);
            return new SettlementState(item.getOriginalAmount(), item.getSettledAmount(),
                    item.getRemainingAmount(), item.getStatus(), item.isResolved(), referenceRows,
                    item.getLastSettlementReference(), item.getLastSettledBy(), item.getLastSettledAt());
        });
    }

    private record SettlementState(BigDecimal original, BigDecimal settled, BigDecimal remaining,
                                   String status, boolean resolved, Set<String> references,
                                   String lastReference, String actor, LocalDateTime settledAt) {
    }

    // DB/JPA/트랜잭션만 명시적으로 구성하며 외부 클라이언트·업무 컴포넌트는 스캔하지 않습니다.
    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class, FlywayAutoConfiguration.class, TransactionAutoConfiguration.class})
    @EntityScan(basePackages = "com.ho.account.journalledger.infrastructure.persistence")
    @EnableJpaRepositories(basePackageClasses = UnsettledItemRepository.class)
    @Import({UnsettledService.class, UnsettledItemPersistenceAdapter.class})
    static class SettlementPersistenceApplication {
    }
}
