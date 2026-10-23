package com.ho.account.masterdata.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.ho.account.contracts.masterdata.FiscalPeriodClosingStatus;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionCommand;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionReceipt;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import com.ho.account.masterdata.core.infrastructure.adapter.MonolithFiscalPeriodControlAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.MasterDataAuditEventEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.FiscalPeriodMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.FiscalPeriodRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.FiscalPeriodTransitionRepository;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Real H2 transactions exercise committed receipts and same-period row-lock behavior. */
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"})
@ContextConfiguration(classes = FiscalPeriodTransitionPersistenceTest.TestApplication.class)
@Import({JpaFiscalPeriodPersistenceAdapter.class, FiscalPeriodMapper.class,
        MonolithFiscalPeriodControlAdapter.class, JpaMasterDataAuditAdapter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FiscalPeriodTransitionPersistenceTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ho.account.masterdata.core")
    static class TestApplication {}

    @Autowired private FiscalPeriodControlPort control;
    @Autowired private JpaFiscalPeriodPersistenceAdapter periods;
    @Autowired private FiscalPeriodRepository periodRepository;
    @Autowired private FiscalPeriodTransitionRepository transitions;
    @Autowired private PlatformTransactionManager transactionManager;
    @SpyBean private JpaMasterDataAuditAdapter auditAdapter;
    @PersistenceContext private EntityManager entityManager;

    private TransactionTemplate transaction;
    private Long periodId;

    @BeforeEach
    void createPeriod() {
        transaction = new TransactionTemplate(transactionManager);
        periodId = transaction.execute(status -> periods.save(FiscalPeriod.reconstitute(
                null, "2026", "11", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                FiscalPeriod.ClosingStatus.CLOSED, null, null, "prior")).getId());
    }

    @AfterEach
    void cleanFixture() {
        transaction.executeWithoutResult(status -> {
            transitions.deleteAll();
            periodRepository.deleteById(periodId);
        });
    }

    @Test
    void prepareIsDurableButDoesNotOpenPeriodAndExactRetryIsIdempotent() {
        FiscalPeriodTransitionCommand command = reopen("reopen-773");
        FiscalPeriodTransitionReceipt prepared = control.prepareClosingTransition(command);

        assertThat(prepared.phase()).isEqualTo(FiscalPeriodTransitionReceipt.Phase.PREPARED);
        assertThat(control.findTransitionReceipt(command.operationId())).contains(prepared);
        assertThat(periods.findById(periodId).orElseThrow().getClosingStatus())
                .isEqualTo(FiscalPeriod.ClosingStatus.CLOSED);
        assertThat(control.prepareClosingTransition(command)).isEqualTo(prepared);
        assertThatThrownBy(() -> control.prepareClosingTransition(new FiscalPeriodTransitionCommand(
                command.operationId(), periodId, FiscalPeriodClosingStatus.CLOSED,
                FiscalPeriodClosingStatus.OPEN, "another-actor")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> control.prepareClosingTransition(reopen("other-operation")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has a prepared transition");
        assertThatThrownBy(() -> control.updateClosingStatus(periodId, "OPEN", "legacy"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void maximumOperationIdentityFitsDurableReceiptColumn() {
        String operationId = "x".repeat(128);
        assertThat(control.prepareClosingTransition(reopen(operationId)).operationId())
                .isEqualTo(operationId);
        assertThat(control.findTransitionReceipt(operationId).orElseThrow().operationId())
                .isEqualTo(operationId);
    }

    @Test
    void applyCommitsStatusAuditAndReceiptTogetherAndLostResponseRetryDoesNotReplay() {
        FiscalPeriodTransitionCommand command = reopen("reopen-773");
        control.prepareClosingTransition(command);

        FiscalPeriodTransitionReceipt applied = control.applyClosingTransition(command.operationId());

        assertThat(applied.phase()).isEqualTo(FiscalPeriodTransitionReceipt.Phase.APPLIED);
        assertThat(control.findTransitionReceipt(command.operationId())).contains(applied);
        assertThat(periods.findById(periodId).orElseThrow().getClosingStatus())
                .isEqualTo(FiscalPeriod.ClosingStatus.OPEN);
        assertThat(control.findFiscalPeriodById(periodId).orElseThrow().closingStatus()).isEqualTo("CLOSED");
        assertThat(control.findFiscalPeriod("2026", "11").orElseThrow().closingStatus()).isEqualTo("CLOSED");
        assertThat(periods.findById(periodId).orElseThrow().getAuditUser()).isEqualTo("closing:alice");
        assertThat(auditRows()).hasSize(1);
        assertThat(auditRows().get(0).getBusinessActor()).isEqualTo("alice");
        assertThat(auditRows().get(0).getServiceActor()).isEqualTo("closing");
        assertThat(control.applyClosingTransition(command.operationId())).isEqualTo(applied);
        assertThat(auditRows()).hasSize(1);
        assertThatThrownBy(() -> control.prepareClosingTransition(new FiscalPeriodTransitionCommand(
                "next-close", periodId, FiscalPeriodClosingStatus.OPEN,
                FiscalPeriodClosingStatus.CLOSED, "alice")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Unconfirmed reopen");
        FiscalPeriodTransitionReceipt confirmed = control.confirmClosingTransition(command.operationId());
        assertThat(confirmed.phase()).isEqualTo(FiscalPeriodTransitionReceipt.Phase.CONFIRMED);
        assertThat(control.confirmClosingTransition(command.operationId())).isEqualTo(confirmed);
        assertThat(control.applyClosingTransition(command.operationId())).isEqualTo(confirmed);
        assertThat(control.findFiscalPeriodById(periodId).orElseThrow().closingStatus()).isEqualTo("OPEN");
        assertThat(control.findFiscalPeriod("2026", "11").orElseThrow().closingStatus()).isEqualTo("OPEN");
        assertThatThrownBy(() -> control.updateClosingStatus(periodId, "CLOSED", "legacy"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void failedAuditWriteRollsBackStatusAndReceipt() {
        FiscalPeriodTransitionCommand command = reopen("reopen-audit-failure");
        control.prepareClosingTransition(command);
        transaction.executeWithoutResult(status -> doThrow(new IllegalStateException("injected audit write failure"))
                .when(auditAdapter).append(any()));

        assertThatThrownBy(() -> control.applyClosingTransition(command.operationId()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("audit write failure");
        assertThat(periods.findById(periodId).orElseThrow().getClosingStatus())
                .isEqualTo(FiscalPeriod.ClosingStatus.CLOSED);
        assertThat(control.findTransitionReceipt(command.operationId()).orElseThrow().phase())
                .isEqualTo(FiscalPeriodTransitionReceipt.Phase.PREPARED);
        assertThat(auditRows()).isEmpty();
    }

    @Test
    void preparedCloseFencesPublicAdmissionBeforePersistedStatusChanges() {
        control.updateClosingStatus(periodId, "OPEN", "setup");
        FiscalPeriodTransitionCommand command = new FiscalPeriodTransitionCommand("close-fence", periodId,
                FiscalPeriodClosingStatus.OPEN, FiscalPeriodClosingStatus.CLOSED, "alice");
        control.prepareClosingTransition(command);

        assertThat(periods.findById(periodId).orElseThrow().getClosingStatus())
                .isEqualTo(FiscalPeriod.ClosingStatus.OPEN);
        assertThat(control.findFiscalPeriodById(periodId).orElseThrow().closingStatus()).isEqualTo("CLOSED");
        assertThat(control.findFiscalPeriod("2026", "11").orElseThrow().closingStatus()).isEqualTo("CLOSED");
        control.applyClosingTransition(command.operationId());
        assertThat(control.findFiscalPeriod("2026", "11").orElseThrow().closingStatus()).isEqualTo("CLOSED");
        assertThatThrownBy(() -> control.confirmClosingTransition(command.operationId()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("reopen");
        assertThat(control.findTransitionReceipt(command.operationId()).orElseThrow().phase())
                .isEqualTo(FiscalPeriodTransitionReceipt.Phase.APPLIED);
        assertThat(periods.findById(periodId).orElseThrow().getClosingStatus())
                .isEqualTo(FiscalPeriod.ClosingStatus.CLOSED);
        assertThat(control.applyClosingTransition(command.operationId()).phase())
                .isEqualTo(FiscalPeriodTransitionReceipt.Phase.APPLIED);
    }

    @Test
    void cancelTombstonesPreparedOperationAndFreesPeriodForNewIdentity() {
        FiscalPeriodTransitionCommand command = reopen("reopen-to-cancel");
        control.prepareClosingTransition(command);

        FiscalPeriodTransitionReceipt cancelled = control.cancelClosingTransition(command.operationId(), "recovery-admin");

        assertThat(cancelled.phase()).isEqualTo(FiscalPeriodTransitionReceipt.Phase.CANCELLED);
        assertThat(cancelled.auditUser()).isEqualTo("alice");
        String cancellationAuditActor = transaction.execute(status -> entityManager.createQuery(
                        "select e from MasterDataAuditEventEntity e where e.targetKey = :key "
                                + "and e.eventType = 'FISCAL_PERIOD_TRANSITION_CANCELLED'",
                        MasterDataAuditEventEntity.class)
                .setParameter("key", periodId.toString()).getSingleResult().getBusinessActor());
        assertThat(cancellationAuditActor).isEqualTo("recovery-admin");
        assertThat(control.cancelClosingTransition(command.operationId(), "recovery-admin")).isEqualTo(cancelled);
        assertThat(control.findTransitionReceipt(command.operationId())).contains(cancelled);
        assertThat(control.prepareClosingTransition(command)).isEqualTo(cancelled);
        assertThatThrownBy(() -> control.applyClosingTransition(command.operationId()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Cancelled");
        assertThatThrownBy(() -> control.confirmClosingTransition(command.operationId()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(periods.findById(periodId).orElseThrow().getClosingStatus())
                .isEqualTo(FiscalPeriod.ClosingStatus.CLOSED);
        assertThat(control.prepareClosingTransition(reopen("new-identity")).phase())
                .isEqualTo(FiscalPeriodTransitionReceipt.Phase.PREPARED);
    }

    @Test
    void confirmRejectsPreparedOperationWithoutOpeningAdmission() {
        FiscalPeriodTransitionCommand command = reopen("not-yet-applied");
        control.prepareClosingTransition(command);
        assertThatThrownBy(() -> control.confirmClosingTransition(command.operationId()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(control.findFiscalPeriodById(periodId).orElseThrow().closingStatus()).isEqualTo("CLOSED");
    }

    @Test
    void concurrentApplyAndCancelHaveOneDurableWinner() throws Exception {
        FiscalPeriodTransitionCommand command = reopen("reopen-race-" + UUID.randomUUID());
        control.prepareClosingTransition(command);
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var apply = pool.submit(() -> {
                start.await();
                return control.applyClosingTransition(command.operationId());
            });
            var cancel = pool.submit(() -> {
                start.await();
                return control.cancelClosingTransition(command.operationId(), "recovery-admin");
            });
            start.countDown();
            int successes = 0;
            for (var future : java.util.List.of(apply, cancel)) {
                try {
                    future.get(10, TimeUnit.SECONDS);
                    successes++;
                } catch (ExecutionException exception) {
                    assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
                }
            }
            assertThat(successes).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        var phase = control.findTransitionReceipt(command.operationId()).orElseThrow().phase();
        assertThat(phase).isIn(FiscalPeriodTransitionReceipt.Phase.APPLIED,
                FiscalPeriodTransitionReceipt.Phase.CANCELLED);
        assertThat(periods.findById(periodId).orElseThrow().getClosingStatus())
                .isEqualTo(phase == FiscalPeriodTransitionReceipt.Phase.APPLIED
                        ? FiscalPeriod.ClosingStatus.OPEN : FiscalPeriod.ClosingStatus.CLOSED);
    }

    @Test
    void failureBeforeCommitRollsBackPeriodAndReceiptAndRecoveryCanApply() {
        FiscalPeriodTransitionCommand command = reopen("reopen-773");
        control.prepareClosingTransition(command);

        assertThatThrownBy(() -> transaction.execute(status -> {
            control.applyClosingTransition(command.operationId());
            throw new IllegalStateException("injected failure before commit");
        })).isInstanceOf(IllegalStateException.class).hasMessageContaining("injected failure");

        assertThat(periods.findById(periodId).orElseThrow().getClosingStatus())
                .isEqualTo(FiscalPeriod.ClosingStatus.CLOSED);
        assertThat(control.findTransitionReceipt(command.operationId()).orElseThrow().phase())
                .isEqualTo(FiscalPeriodTransitionReceipt.Phase.PREPARED);
        assertThat(control.applyClosingTransition(command.operationId()).phase())
                .isEqualTo(FiscalPeriodTransitionReceipt.Phase.APPLIED);
    }

    @Test
    void concurrentSameOperationApplyReturnsOneCommittedOutcomeToBothCallers() throws Exception {
        FiscalPeriodTransitionCommand command = reopen("reopen-773-" + UUID.randomUUID());
        control.prepareClosingTransition(command);
        CountDownLatch start = new CountDownLatch(1);
        Callable<FiscalPeriodTransitionReceipt> apply = () -> {
            start.await();
            return control.applyClosingTransition(command.operationId());
        };
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(apply);
            var second = pool.submit(apply);
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS).phase())
                    .isEqualTo(FiscalPeriodTransitionReceipt.Phase.APPLIED);
            assertThat(second.get(10, TimeUnit.SECONDS).phase())
                .isEqualTo(FiscalPeriodTransitionReceipt.Phase.APPLIED);
        } finally {
            pool.shutdownNow();
        }
        assertThat(periods.findById(periodId).orElseThrow().getClosingStatus())
                .isEqualTo(FiscalPeriod.ClosingStatus.OPEN);
    }

    private FiscalPeriodTransitionCommand reopen(String operationId) {
        return new FiscalPeriodTransitionCommand(operationId, periodId,
                FiscalPeriodClosingStatus.CLOSED, FiscalPeriodClosingStatus.OPEN, "alice");
    }

    private java.util.List<MasterDataAuditEventEntity> auditRows() {
        return transaction.execute(status -> entityManager.createQuery(
                        "select e from MasterDataAuditEventEntity e where e.targetKey = :key "
                                + "and e.eventType = 'FISCAL_PERIOD_STATUS' order by e.id",
                        MasterDataAuditEventEntity.class)
                .setParameter("key", periodId.toString()).getResultList());
    }
}
