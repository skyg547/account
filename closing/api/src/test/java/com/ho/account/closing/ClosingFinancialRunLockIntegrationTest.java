package com.ho.account.closing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.closing.application.port.out.ClosingFinancialRunLockPort;
import com.ho.account.closing.application.port.out.ClosingFinancialRunManifestPort;
import com.ho.account.closing.domain.ValuationBatch;
import com.ho.account.closing.infrastructure.persistence.ClosingFinancialRunLock;
import com.ho.account.closing.infrastructure.persistence.ClosingFinancialRunLockRepository;
import com.ho.account.closing.infrastructure.persistence.JpaClosingFinancialRunLockAdapter;
import com.ho.account.closing.infrastructure.persistence.JpaClosingFinancialRunManifestAdapter;
import com.ho.account.closing.infrastructure.persistence.ValuationBatchRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import({JpaClosingFinancialRunLockAdapter.class, JpaClosingFinancialRunManifestAdapter.class})
class ClosingFinancialRunLockIntegrationTest {
    @Autowired ClosingFinancialRunLockPort gate;
    @Autowired ClosingFinancialRunManifestPort manifest;
    @Autowired ClosingFinancialRunLockRepository locks;
    @Autowired ValuationBatchRepository batches;
    @Autowired PlatformTransactionManager transactions;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rolledBackGateReleasesLockAndPreservesRunAndManifestForReplay() {
        Long id = seed("crash-key");
        Map<Long, Long> journal = new java.util.HashMap<>();

        assertThatThrownBy(() -> gate.withExclusiveRun("VALUATION", "crash-key", () -> {
            manifest.loadOrCreate("VALUATION", id, List::of);
            journal.computeIfAbsent(id, ignored -> 901L);
            throw new IllegalStateException("process stopped after draft");
        })).hasMessageContaining("after draft");

        Long recovered = gate.withExclusiveRun("VALUATION", "crash-key", () -> {
            assertThat(manifest.loadOrCreate("VALUATION", id, List::of)).isEmpty();
            assertThat(jpaBatches().findById(id).orElseThrow().getStatus())
                    .isEqualTo(ValuationBatch.ValuationBatchStatus.RUNNING);
            Long draftId = journal.computeIfAbsent(id, ignored -> 999L);
            ValuationBatch batch = jpaBatches().findById(id).orElseThrow();
            batch.setGeneratedJournalEntryId(draftId);
            batch.setStatus(ValuationBatch.ValuationBatchStatus.PENDING_APPROVAL);
            jpaBatches().save(batch);
            return draftId;
        });

        assertThat(recovered).isEqualTo(901L);
        assertThat(journal).hasSize(1);
        Long persisted = new TransactionTemplate(transactions).execute(status ->
                jpaBatches().findById(id).orElseThrow().getGeneratedJournalEntryId());
        assertThat(persisted).isEqualTo(901L);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void sameKeyRetryCannotEnterWhileFirstWorkerHoldsDatabaseLock() throws Exception {
        seed("concurrent-key");
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> gate.withExclusiveRun("VALUATION", "concurrent-key", () -> {
                peak.accumulateAndGet(active.incrementAndGet(), Math::max);
                firstEntered.countDown();
                try {
                    if (!releaseFirst.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("timed out");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                } finally {
                    active.decrementAndGet();
                }
                return 1;
            }));
            assertThat(firstEntered.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> {
                secondStarted.countDown();
                return gate.withExclusiveRun("VALUATION", "concurrent-key", () -> {
                peak.accumulateAndGet(active.incrementAndGet(), Math::max);
                active.decrementAndGet();
                return 2;
                });
            });
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(java.util.concurrent.TimeoutException.class);
            releaseFirst.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo(1);
            assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo(2);
            assertThat(peak).hasValue(1);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void manySameKeyRetriesLeaveConnectionsForNestedManifestTransactions() throws Exception {
        Long id = seed("crowded-key");
        AtomicInteger active = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(12);
        try {
            List<java.util.concurrent.Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return gate.withExclusiveRun("VALUATION", "crowded-key", () -> {
                        peak.accumulateAndGet(active.incrementAndGet(), Math::max);
                        try {
                            manifest.loadOrCreate("VALUATION", id, List::of);
                            return 1;
                        } finally {
                            active.decrementAndGet();
                        }
                    });
                }));
            }
            start.countDown();
            for (var future : futures) assertThat(future.get(15, TimeUnit.SECONDS)).isEqualTo(1);
            assertThat(peak).hasValue(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private Long seed(String key) {
        return new TransactionTemplate(transactions).execute(status -> {
            locks.saveAndFlush(new ClosingFinancialRunLock("VALUATION", key));
            ValuationBatch batch = new ValuationBatch();
            batch.setFiscalPeriodId(1L);
            batch.setValuationType(ValuationBatch.ValuationType.FX_RATE);
            batch.setRunDateTime(LocalDateTime.now());
            batch.setStatus(ValuationBatch.ValuationBatchStatus.RUNNING);
            batch.setExecutionKey(key);
            batch.setRunBy("ADMIN");
            return batches.saveAndFlush(batch).getId();
        });
    }

    @SuppressWarnings("unchecked")
    private JpaRepository<ValuationBatch, Long> jpaBatches() {
        return (JpaRepository<ValuationBatch, Long>) batches;
    }
}
