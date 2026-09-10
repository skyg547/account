package com.ho.account.masterdata.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod.ClosingStatus;
import com.ho.account.masterdata.core.infrastructure.adapter.MonolithFiscalPeriodControlAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.FiscalPeriodMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.FiscalPeriodRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 실제 H2 행과 Spring 프록시를 사용해 저장 결과 복원 실패가 정상 마감의 커밋을 막지 않는지 검증합니다.
 * PostgreSQL 잠금 경합/동시성 검증은 아니며, API나 Batch 실행 모듈에 의존하지 않습니다.
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@ContextConfiguration(classes = JpaFiscalPeriodPersistenceAdapterTest.TestApplication.class)
@Import({JpaFiscalPeriodPersistenceAdapter.class, FiscalPeriodMapper.class, MonolithFiscalPeriodControlAdapter.class})
// 기본 slice의 rollback 트랜잭션을 끄므로 control adapter가 반환할 때 실제 커밋까지 완료해야 합니다.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaFiscalPeriodPersistenceAdapterTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final LocalDate END = LocalDate.of(2026, 9, 30);
    private static final LocalDateTime CREATED = LocalDateTime.of(2025, 12, 1, 8, 15, 12);
    private static final LocalDateTime UPDATED = LocalDateTime.of(2026, 8, 31, 17, 45, 23);

    @Autowired
    private JpaFiscalPeriodPersistenceAdapter persistence;
    @Autowired
    private FiscalPeriodControlPort control;
    @Autowired
    private FiscalPeriodRepository repository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @PersistenceContext
    private EntityManager entityManager;

    private TransactionTemplate transaction;
    private final List<Long> fixtureIds = new ArrayList<>();

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ho.account.masterdata.core")
    static class TestApplication {
    }

    @BeforeEach
    void setUpTransactions() {
        transaction = new TransactionTemplate(transactionManager);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(AopUtils.isAopProxy(control)).isTrue();
    }

    @AfterEach
    void removeOnlyCommittedTestFixtures() {
        transaction.executeWithoutResult(status -> {
            fixtureIds.forEach(repository::deleteById);
            entityManager.flush();
            entityManager.clear();
        });
    }

    @ParameterizedTest
    @EnumSource(ClosingStatus.class)
    void commitsAndReadsEveryStoredStatusThroughAllThreePorts(ClosingStatus status) {
        FiscalPeriod stored = storeFixture(status);

        assertFreshReadsMatch(stored);
    }

    @Test
    void legalPermanentClosingCommitsThroughRealControlAdapterBeforeFreshReads() {
        FiscalPeriod closed = storeFixture(ClosingStatus.CLOSED);
        LocalDateTime beforeCommand = LocalDateTime.now();

        FiscalPeriodRef result = control.updateClosingStatus(closed.getId(), "PERMANENTLY_CLOSED", "  closing-command  ");

        // 프록시 바깥에 트랜잭션이 없으므로 여기서 성공 반환했다면 제어 어댑터의 커밋도 끝났습니다.
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(result.id()).isEqualTo(closed.getId());
        assertThat(result.closingStatus()).isEqualTo("PERMANENTLY_CLOSED");
        FiscalPeriod committed = readInFreshTransaction(() -> persistence.findById(closed.getId()).orElseThrow());
        assertThat(committed.getClosingStatus()).isEqualTo(ClosingStatus.PERMANENTLY_CLOSED);
        assertThat(committed.getAuditUser()).isEqualTo("closing-command");
        assertThat(committed.getCreatedAt()).isEqualTo(CREATED);
        // 실제 갱신에서는 기존 @PreUpdate가 시간을 찍습니다. 순수 Mapper의 시간 보존 계약과 구분합니다.
        assertThat(committed.getUpdatedAt()).isBetween(beforeCommand, LocalDateTime.now());
        assertBusinessIdentity(committed, closed);
        assertFreshReadsMatch(committed);
    }

    @ParameterizedTest
    @CsvSource({"OPEN, PERMANENTLY_CLOSED", "PERMANENTLY_CLOSED, OPEN", "PERMANENTLY_CLOSED, CLOSED"})
    void rejectedTransitionLeavesCommittedStateAndAuditUnchanged(ClosingStatus current, ClosingStatus next) {
        FiscalPeriod stored = storeFixture(current);

        assertThatThrownBy(() -> control.updateClosingStatus(stored.getId(), next.name(), "rejected-command"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertFreshReadsMatch(stored);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void rejectedActorLeavesCommittedStateAndAuditUnchanged(String actor) {
        FiscalPeriod stored = storeFixture(ClosingStatus.CLOSED);

        assertThatThrownBy(() -> control.updateClosingStatus(stored.getId(), "PERMANENTLY_CLOSED", actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Closing status audit user is required.");

        assertFreshReadsMatch(stored);
    }

    @Test
    void rejectedNullStatusLeavesCommittedStateAndAuditUnchanged() {
        FiscalPeriod stored = storeFixture(ClosingStatus.CLOSED);

        assertThatThrownBy(() -> control.updateClosingStatus(stored.getId(), null, "rejected-command"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Closing status is required.");

        assertFreshReadsMatch(stored);
    }

    private FiscalPeriod storeFixture(ClosingStatus status) {
        FiscalPeriod input = FiscalPeriod.reconstitute(
                null, "2026", "09", START, END, status, CREATED, UPDATED, "  stored-auditor  ");
        FiscalPeriod stored = transaction.execute(tx -> {
            FiscalPeriod saved = persistence.save(input);
            // IDENTITY 발급 ID만 추적하여 테스트가 중간에 실패해도 다른 행을 삭제하지 않습니다.
            fixtureIds.add(saved.getId());
            entityManager.flush();
            entityManager.clear();
            return saved;
        });
        assertThat(stored).isNotNull();
        assertThat(stored.getId()).isNotNull();
        assertThat(stored.getFiscalYear()).isEqualTo(input.getFiscalYear());
        assertThat(stored.getFiscalPeriod()).isEqualTo(input.getFiscalPeriod());
        assertThat(stored.getStartDate()).isEqualTo(START);
        assertThat(stored.getEndDate()).isEqualTo(END);
        assertThat(stored.getClosingStatus()).isEqualTo(status);
        // 명시한 fixture 값은 @PrePersist의 null 기본값을 사용하지 않습니다.
        assertThat(stored.getCreatedAt()).isEqualTo(CREATED);
        assertThat(stored.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(stored.getAuditUser()).isEqualTo("  stored-auditor  ");
        return stored;
    }

    private void assertFreshReadsMatch(FiscalPeriod expected) {
        FiscalPeriod byId = readInFreshTransaction(() -> persistence.findById(expected.getId()).orElseThrow());
        FiscalPeriod locked = readInFreshTransaction(() -> persistence.findByIdForUpdate(expected.getId()).orElseThrow());
        FiscalPeriod byYearAndPeriod = readInFreshTransaction(() -> persistence.findByFiscalYearAndFiscalPeriod(
                expected.getFiscalYear(), expected.getFiscalPeriod()).orElseThrow());

        for (FiscalPeriod actual : List.of(byId, locked, byYearAndPeriod)) {
            assertBusinessIdentity(actual, expected);
            assertThat(actual.getClosingStatus()).isEqualTo(expected.getClosingStatus());
            assertThat(actual.getCreatedAt()).isEqualTo(expected.getCreatedAt());
            assertThat(actual.getUpdatedAt()).isEqualTo(expected.getUpdatedAt());
            assertThat(actual.getAuditUser()).isEqualTo(expected.getAuditUser());
        }
    }

    private FiscalPeriod readInFreshTransaction(Supplier<FiscalPeriod> read) {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        return transaction.execute(tx -> {
            // 각 포트를 별도 트랜잭션/영속성 컨텍스트에서 읽고 잠금 조회도 트랜잭션 안에서만 실행합니다.
            entityManager.flush();
            entityManager.clear();
            return read.get();
        });
    }

    private void assertBusinessIdentity(FiscalPeriod actual, FiscalPeriod expected) {
        assertThat(actual.getId()).isEqualTo(expected.getId());
        assertThat(actual.getFiscalYear()).isEqualTo(expected.getFiscalYear());
        assertThat(actual.getFiscalPeriod()).isEqualTo(expected.getFiscalPeriod());
        assertThat(actual.getStartDate()).isEqualTo(expected.getStartDate());
        assertThat(actual.getEndDate()).isEqualTo(expected.getEndDate());
    }
}
