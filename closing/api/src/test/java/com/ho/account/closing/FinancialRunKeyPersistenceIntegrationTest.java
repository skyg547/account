package com.ho.account.closing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.closing.domain.ProvisionBatch;
import com.ho.account.closing.domain.ValuationBatch;
import com.ho.account.closing.infrastructure.persistence.ProvisionBatchRepository;
import com.ho.account.closing.infrastructure.persistence.ValuationBatchRepository;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class FinancialRunKeyPersistenceIntegrationTest {

    @Autowired ValuationBatchRepository valuationBatches;
    @Autowired ProvisionBatchRepository provisionBatches;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void valuationKeyIsUniqueAndFindsOriginalRun() {
        ValuationBatch original = valuationBatches.saveAndFlush(valuation("same-key"));
        assertThat(valuationBatches.findByExecutionKey("same-key")).contains(original);
        assertThatThrownBy(() -> valuationBatches.saveAndFlush(valuation("same-key")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void provisionKeyIsUniqueAndFindsOriginalRun() {
        ProvisionBatch original = provisionBatches.saveAndFlush(provision("same-key"));
        assertThat(provisionBatches.findByExecutionKey("same-key")).contains(original);
        assertThatThrownBy(() -> provisionBatches.saveAndFlush(provision("same-key")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentFirstClaimsCannotCommitTwoValuationRuns() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstInserted = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> new TransactionTemplate(transactions).execute(status -> {
                ValuationBatch batch = valuationBatches.saveAndFlush(valuation("concurrent-key"));
                firstInserted.countDown();
                try {
                    if (!allowCommit.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("timed out");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
                return batch.getId();
            }));
            assertThat(firstInserted.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> new TransactionTemplate(transactions).execute(status ->
                    valuationBatches.saveAndFlush(valuation("concurrent-key")).getId()));
            allowCommit.countDown();
            Long firstId = first.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(DataIntegrityViolationException.class);
            Long persistedId = new TransactionTemplate(transactions).execute(status ->
                    valuationBatches.findByExecutionKey("concurrent-key").orElseThrow().getId());
            assertThat(persistedId)
                    .isEqualTo(firstId);
        } finally {
            allowCommit.countDown();
            executor.shutdownNow();
        }
    }

    private ValuationBatch valuation(String key) {
        ValuationBatch batch = new ValuationBatch();
        batch.setFiscalPeriodId(1L);
        batch.setValuationType(ValuationBatch.ValuationType.FX_RATE);
        batch.setRunDateTime(LocalDateTime.now());
        batch.setStatus(ValuationBatch.ValuationBatchStatus.RUNNING);
        batch.setExecutionKey(key);
        batch.setRunBy("ADMIN");
        return batch;
    }

    private ProvisionBatch provision(String key) {
        ProvisionBatch batch = new ProvisionBatch();
        batch.setFiscalPeriodId(1L);
        batch.setProvisionType(ProvisionBatch.ProvisionType.ECL);
        batch.setRunDateTime(LocalDateTime.now());
        batch.setStatus(ProvisionBatch.ProvisionBatchStatus.RUNNING);
        batch.setExecutionKey(key);
        batch.setRunBy("ADMIN");
        return batch;
    }
}
