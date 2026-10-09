package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.application.port.out.LedgerBalancePersistencePort.BalanceAccount;
import com.ho.account.journalledger.infrastructure.persistence.repository.GlBalanceRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Connection;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Database transaction locks shared by JPA and JDBC writers. Seeded stripes also protect missing
 * balance rows and nullable SL dimensions; hash collisions only serialize unrelated accounts.
 * See journal-ledger/docs/posting-concurrency.md for transaction and retry boundaries.
 */
final class LedgerBalanceWriteLock {

    private static final int STRIPE_COUNT = 256;
    private static final List<Integer> ALL_STRIPES = IntStream.range(0, STRIPE_COUNT).boxed().toList();

    private LedgerBalanceWriteLock() {}

    static void lockAccounts(EntityManager entityManager, GlBalanceRepository repository,
                             List<BalanceAccount> accounts) {
        List<Integer> stripes = accounts.stream()
                .map(account -> Math.floorMod(
                        31 * account.accountCode().hashCode() + account.currencyCode().hashCode(), STRIPE_COUNT))
                .distinct().sorted().toList();
        lock(entityManager, repository, stripes);
    }

    static void lockAll(EntityManager entityManager, GlBalanceRepository repository) {
        lock(entityManager, repository, ALL_STRIPES);
    }

    private static void lock(EntityManager entityManager, GlBalanceRepository repository,
                             List<Integer> stripes) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            throw new IllegalStateException("Balance updates require an active write transaction");
        }
        int isolation = entityManager.unwrap(Session.class).doReturningWork(Connection::getTransactionIsolation);
        // A snapshot established before waiting can remain stale at REPEATABLE_READ/SERIALIZABLE.
        // Fail before any balance write instead of trusting refresh to advance that snapshot.
        if (isolation != Connection.TRANSACTION_READ_COMMITTED) {
            throw new IllegalStateException("Balance updates require READ_COMMITTED transaction isolation");
        }
        // Preserve earlier work in an ambient transaction before later refreshes. JDBC balance
        // reads are detached, so no stale managed balance can overwrite its prior bulk SQL.
        entityManager.flush();
        if (!stripes.isEmpty() && !repository.lockBalanceStripes(stripes).equals(stripes)) {
            throw new IllegalStateException("Ledger balance lock stripes are missing; apply migration V14");
        }
    }

    static <T> T refresh(EntityManager entityManager, T balance, boolean detach) {
        // The first-level cache may contain a value read before a competing writer committed.
        entityManager.refresh(balance);
        if (detach) {
            entityManager.detach(balance);
        }
        return balance;
    }
}
