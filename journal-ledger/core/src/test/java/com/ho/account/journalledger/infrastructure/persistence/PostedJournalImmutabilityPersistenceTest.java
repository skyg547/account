package com.ho.account.journalledger.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.journal.repository.JournalDetailRepository;
import com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Persistence-boundary regression for immutable POSTED accounting history.
 *
 * <p>Each expected flush failure owns a separate transaction because a JPA callback failure marks
 * that transaction unusable. Verification always reloads through a later transaction, proving the
 * database was unchanged rather than merely observing an in-memory object.</p>
 */
@SpringBootTest(classes = PostedJournalImmutabilityPersistenceTest.PersistenceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.name=posted-journal-immutability-test",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.vault.enabled=false",
        "eureka.client.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PostedJournalImmutabilityPersistenceTest {
    private static final LocalDate ORIGINAL_DATE = LocalDate.of(2026, 9, 25);
    private static final String ORIGINAL_DESCRIPTION = "committed posted fixture";
    private static final String ORIGINAL_AMOUNT = "100.00";
    private static final String DATABASE_NAME = "posted_immutability_"
            + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:" + DATABASE_NAME
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "public");
    }

    @Autowired private EntityManager entityManager;
    @Autowired private JournalEntryRepository entryRepository;
    @Autowired private JournalDetailRepository detailRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactions;

    @BeforeEach
    void configureTransactions() {
        transactions = new TransactionTemplate(transactionManager);
    }

    @Test
    void managedHeaderDirtyUpdateIsRejectedAndRolledBack() {
        PostedIds ids = seedPosted();
        StoredState before = readState(ids.entryId());

        assertPostedFlushRejected(() -> inTransaction(entry -> {
            ReflectionTestUtils.setField(entry, "accountingDate", ORIGINAL_DATE.plusDays(1));
            entityManager.flush();
        }, ids.entryId()));

        assertThat(readState(ids.entryId())).isEqualTo(before);
    }

    @Test
    void managedLineDirtyUpdateIsRejectedAndRolledBack() {
        PostedIds ids = seedPosted();
        StoredState before = readState(ids.entryId());

        assertPostedFlushRejected(() -> inTransaction(entry -> {
            ReflectionTestUtils.setField(entry.getDetails().get(0), "amount", new BigDecimal("101.00"));
            entityManager.flush();
        }, ids.entryId()));

        assertThat(readState(ids.entryId())).isEqualTo(before);
    }

    @Test
    void rawCollectionOrphanRemovalIsRejectedAndRolledBack() {
        PostedIds ids = seedPosted();
        StoredState before = readState(ids.entryId());

        assertPostedFlushRejected(() -> inTransaction(entry -> {
            mutableDetails(entry).remove(0);
            entityManager.flush();
        }, ids.entryId()));

        assertThat(readState(ids.entryId())).isEqualTo(before);
    }

    @Test
    void repositoryDetailDeleteIsRejectedAndRolledBack() {
        PostedIds ids = seedPosted();
        StoredState before = readState(ids.entryId());

        assertPostedFlushRejected(() -> transactions.executeWithoutResult(status -> {
            entityManager.clear();
            detailRepository.deleteById(ids.detailIds().get(0));
            detailRepository.flush();
        }));

        assertThat(readState(ids.entryId())).isEqualTo(before);
    }

    @Test
    void entryCascadeDeleteIsRejectedAndRolledBack() {
        PostedIds ids = seedPosted();
        StoredState before = readState(ids.entryId());

        assertPostedFlushRejected(() -> transactions.executeWithoutResult(status -> {
            entityManager.clear();
            entryRepository.deleteById(ids.entryId());
            entryRepository.flush();
        }));

        assertThat(readState(ids.entryId())).isEqualTo(before);
    }

    @ParameterizedTest(name = "header bulk delete variant: {0}")
    @EnumSource(BulkDeleteVariant.class)
    void everyBulkEntryDeleteVariantRejectsPostedHistoryAndRollsBack(BulkDeleteVariant variant) {
        PostedIds ids = seedPosted();
        StoredState before = readState(ids.entryId());

        assertPostedFlushRejected(() -> transactions.executeWithoutResult(status -> {
            entityManager.clear();
            JournalEntry entry = entryRepository.findByIdWithDetails(ids.entryId()).orElseThrow();
            bulkDeleteEntry(variant, entry);
            entryRepository.flush();
        }));

        assertThat(readState(ids.entryId())).isEqualTo(before);
    }

    @ParameterizedTest(name = "detail bulk delete variant: {0}")
    @EnumSource(BulkDeleteVariant.class)
    void everyBulkDetailDeleteVariantRejectsPostedHistoryAndRollsBack(BulkDeleteVariant variant) {
        PostedIds ids = seedPosted();
        StoredState before = readState(ids.entryId());

        assertPostedFlushRejected(() -> transactions.executeWithoutResult(status -> {
            entityManager.clear();
            JournalDetail detail = detailRepository.findById(ids.detailIds().get(0)).orElseThrow();
            bulkDeleteDetail(variant, detail);
            detailRepository.flush();
        }));

        assertThat(readState(ids.entryId())).isEqualTo(before);
    }

    @Test
    void insertingLineIntoPostedHeaderIsRejectedAndRolledBack() {
        PostedIds ids = seedPosted();
        StoredState before = readState(ids.entryId());

        assertPostedFlushRejected(() -> inTransaction(entry -> {
            JournalDetail inserted = detail(JournalSide.DEBIT, "10200", "1.00");
            ReflectionTestUtils.setField(inserted, "journalEntry", entry);
            mutableDetails(entry).add(inserted);
            entityManager.flush();
        }, ids.entryId()));

        assertThat(readState(ids.entryId())).isEqualTo(before);
    }

    @Test
    void detachedMergeOfChangedPostedHeaderIsRejectedAndRolledBack() {
        PostedIds ids = seedPosted();
        StoredState before = readState(ids.entryId());
        JournalEntry detached = transactions.execute(status -> {
            entityManager.clear();
            JournalEntry loaded = entryRepository.findByIdWithDetails(ids.entryId()).orElseThrow();
            loaded.getDetails().size();
            return loaded;
        });
        ReflectionTestUtils.setField(detached, "description", "detached overwrite");

        assertPostedFlushRejected(() -> transactions.executeWithoutResult(status -> {
            entityManager.clear();
            entityManager.merge(detached);
            entityManager.flush();
        }));

        assertThat(readState(ids.entryId())).isEqualTo(before);
    }

    @Test
    void draftHeaderAndLineUpdatesRemainPersistable() {
        PostedIds ids = seedDraft();

        transactions.executeWithoutResult(status -> {
            entityManager.clear();
            JournalEntry draft = entryRepository.findByIdWithDetails(ids.entryId()).orElseThrow();
            draft.setDescription("edited draft");
            draft.setAccountingDate(ORIGINAL_DATE.plusDays(2));
            draft.getDetails().get(0).setAmount(new BigDecimal("125.00"));
            draft.getDetails().get(1).setAmount(new BigDecimal("125.00"));
            entityManager.flush();
        });

        StoredState stored = readState(ids.entryId());
        assertThat(stored.status()).isEqualTo(JournalEntryStatus.DRAFT);
        assertThat(stored.accountingDate()).isEqualTo(ORIGINAL_DATE.plusDays(2));
        assertThat(stored.description()).isEqualTo("edited draft");
        assertThat(stored.lines()).extracting(StoredLine::amount)
                .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo("125.00"));
    }

    @Test
    void draftMembershipChangesAndOrphanRemovalRemainPersistable() {
        PostedIds ids = seedDraft();
        Long removedId = ids.detailIds().get(0);

        transactions.executeWithoutResult(status -> {
            entityManager.clear();
            JournalEntry draft = entryRepository.findByIdWithDetails(ids.entryId()).orElseThrow();
            draft.removeDetail(draft.getDetails().get(0));
            draft.addDetail(detail(JournalSide.DEBIT, "10200", ORIGINAL_AMOUNT));
            entityManager.flush();
        });

        StoredState stored = readState(ids.entryId());
        assertThat(stored.status()).isEqualTo(JournalEntryStatus.DRAFT);
        assertThat(stored.lines()).hasSize(2).extracting(StoredLine::accountCode)
                .containsExactlyInAnyOrder("40100", "10200");
        boolean removedStillExists = Boolean.TRUE.equals(
                transactions.execute(status -> detailRepository.existsById(removedId)));
        assertThat(removedStillExists).isFalse();
    }

    @Test
    void draftDetailAndEntryDeletesRemainPersistable() {
        PostedIds detailDelete = seedDraft();
        transactions.executeWithoutResult(status -> {
            detailRepository.deleteById(detailDelete.detailIds().get(0));
            detailRepository.flush();
        });
        boolean detailStillExists = Boolean.TRUE.equals(transactions.execute(
                status -> detailRepository.existsById(detailDelete.detailIds().get(0))));
        assertThat(detailStillExists).isFalse();

        PostedIds entryDelete = seedDraft();
        transactions.executeWithoutResult(status -> {
            entryRepository.deleteById(entryDelete.entryId());
            entryRepository.flush();
        });
        boolean entryStillExists = Boolean.TRUE.equals(
                transactions.execute(status -> entryRepository.existsById(entryDelete.entryId())));
        assertThat(entryStillExists).isFalse();
        assertThat(entryDelete.detailIds()).allSatisfy(id -> {
            boolean cascadedDetailStillExists = Boolean.TRUE.equals(
                    transactions.execute(status -> detailRepository.existsById(id)));
            assertThat(cascadedDetailStillExists).isFalse();
        });
    }

    @Test
    void approvedToPostedTransitionFlushesNormally() {
        PostedIds approved = seedApproved();

        transactions.executeWithoutResult(status -> {
            entityManager.clear();
            JournalEntry entry = entryRepository.findByIdWithDetails(approved.entryId()).orElseThrow();
            entry.post("poster");
            entityManager.flush();
        });

        StoredState stored = readState(approved.entryId());
        assertThat(stored.status()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(stored.description()).isEqualTo(ORIGINAL_DESCRIPTION);
        assertThat(stored.lines()).hasSize(2);
    }

    private PostedIds seedPosted() {
        PostedIds approved = seedApproved();
        transactions.executeWithoutResult(status -> {
            entityManager.clear();
            JournalEntry entry = entryRepository.findByIdWithDetails(approved.entryId()).orElseThrow();
            entry.post("poster");
            entityManager.flush();
            entityManager.clear();
        });
        return approved;
    }

    private PostedIds seedApproved() {
        return seed(JournalEntryStatus.APPROVED);
    }

    private PostedIds seedDraft() {
        return seed(JournalEntryStatus.DRAFT);
    }

    private PostedIds seed(JournalEntryStatus target) {
        return transactions.execute(status -> {
            JournalEntry entry = new JournalEntry();
            entry.setSlipNo("JE-758-" + UUID.randomUUID().toString().substring(0, 8));
            entry.setSlipDate(ORIGINAL_DATE);
            entry.setAccountingDate(ORIGINAL_DATE);
            entry.setDescription(ORIGINAL_DESCRIPTION);
            entry.setCurrencyCode("KRW");
            entry.setCreatedBy("maker");
            entry.initializeDraft();
            entry.addDetail(detail(JournalSide.DEBIT, "10100", ORIGINAL_AMOUNT));
            entry.addDetail(detail(JournalSide.CREDIT, "40100", ORIGINAL_AMOUNT));
            if (target == JournalEntryStatus.APPROVED) {
                entry.requestApproval("maker");
                entry.approve("checker");
            }
            entityManager.persist(entry);
            entityManager.flush();
            List<Long> detailIds = entry.getDetails().stream().map(JournalDetail::getId).toList();
            return new PostedIds(entry.getId(), detailIds);
        });
    }

    private void inTransaction(Consumer<JournalEntry> mutation, Long entryId) {
        transactions.executeWithoutResult(status -> {
            entityManager.clear();
            mutation.accept(entryRepository.findByIdWithDetails(entryId).orElseThrow());
        });
    }

    private StoredState readState(Long entryId) {
        return transactions.execute(status -> {
            entityManager.clear();
            JournalEntry entry = entryRepository.findByIdWithDetails(entryId).orElseThrow();
            List<StoredLine> lines = entry.getDetails().stream()
                    .map(detail -> new StoredLine(detail.getId(), detail.getSide(), detail.getAccountCode(),
                            detail.getAmount(), detail.getBaseAmount()))
                    .toList();
            return new StoredState(entry.getStatus(), entry.getAccountingDate(), entry.getDescription(), lines);
        });
    }

    @SuppressWarnings("unchecked")
    private static List<JournalDetail> mutableDetails(JournalEntry entry) {
        return (List<JournalDetail>) ReflectionTestUtils.getField(entry, "details");
    }

    private static JournalDetail detail(JournalSide side, String accountCode, String amount) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setAmount(new BigDecimal(amount));
        detail.setBaseAmount(new BigDecimal(amount));
        detail.setDepartmentCode("D-758");
        detail.setBusinessPartnerCode("BP-758");
        detail.setDetailDescription("fixture detail");
        detail.setAuditUser("fixture");
        return detail;
    }

    @SuppressWarnings("deprecation")
    private void bulkDeleteEntry(BulkDeleteVariant variant, JournalEntry entry) {
        switch (variant) {
            case ALL -> entryRepository.deleteAllInBatch();
            case ENTITIES -> entryRepository.deleteAllInBatch(List.of(entry));
            case IDS -> entryRepository.deleteAllByIdInBatch(List.of(entry.getId()));
            case DEPRECATED_ENTITIES -> entryRepository.deleteInBatch(List.of(entry));
        }
    }

    @SuppressWarnings("deprecation")
    private void bulkDeleteDetail(BulkDeleteVariant variant, JournalDetail detail) {
        switch (variant) {
            case ALL -> detailRepository.deleteAllInBatch();
            case ENTITIES -> detailRepository.deleteAllInBatch(List.of(detail));
            case IDS -> detailRepository.deleteAllByIdInBatch(List.of(detail.getId()));
            case DEPRECATED_ENTITIES -> detailRepository.deleteInBatch(List.of(detail));
        }
    }

    private static void assertPostedFlushRejected(Runnable operation) {
        Throwable failure;
        try {
            operation.run();
            throw new AssertionError("Expected POSTED immutability lifecycle rejection");
        } catch (Throwable caught) {
            failure = caught;
        }
        assertThat(causeChain(failure))
                .as("JPA failure must contain the POSTED immutability lifecycle exception, not an incidental FK error")
                .anySatisfy(cause -> {
                    assertThat(cause).isInstanceOf(IllegalStateException.class);
                    assertThat(cause.getMessage()).contains("POSTED");
                });
    }

    private static List<Throwable> causeChain(Throwable failure) {
        List<Throwable> causes = new ArrayList<>();
        Throwable current = failure;
        while (current != null && !causes.contains(current)) {
            causes.add(current);
            current = current.getCause();
        }
        return causes;
    }

    private record PostedIds(Long entryId, List<Long> detailIds) { }
    private record StoredState(JournalEntryStatus status, LocalDate accountingDate, String description,
                               List<StoredLine> lines) { }
    private record StoredLine(Long id, JournalSide side, String accountCode, BigDecimal amount,
                              BigDecimal baseAmount) { }
    private enum BulkDeleteVariant { ALL, ENTITIES, IDS, DEPRECATED_ENTITIES }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
            TransactionAutoConfiguration.class})
    @EntityScan(basePackageClasses = JournalEntry.class)
    @EnableJpaRepositories(basePackageClasses = JournalEntryRepository.class)
    static class PersistenceApplication { }
}
