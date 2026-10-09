package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.domain.journal.repository.JournalDetailRepository;
import com.ho.account.journalledger.domain.ledger.repository.GlBalanceRepository;
import com.ho.account.journalledger.domain.ledger.repository.SlBalanceRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

/** The audited find-by-range/deleteAllInBatch path fails this repository interaction contract. */
class LedgerBalanceCleanupRegressionTest {

    @Test
    void cleanupUsesInclusiveBulkPredicatesForBothLedgersWithoutLoadingBalanceEntities() {
        GlBalanceRepository gl = mock(GlBalanceRepository.class);
        SlBalanceRepository sl = mock(SlBalanceRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        LedgerBalancePersistenceAdapter adapter = new LedgerBalancePersistenceAdapter(
                gl, sl, mock(JournalDetailRepository.class));
        boolean managesPersistenceContext = ReflectionUtils.findField(
                LedgerBalancePersistenceAdapter.class, "entityManager") != null;
        if (managesPersistenceContext) {
            ReflectionTestUtils.setField(adapter, "entityManager", entityManager);
        }
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);

        adapter.deleteBalancesBetween(start, end);

        // Inspect calls by name so this same test also compiles against the audited repositories,
        // where the bulk method does not exist. The old range read then deleteAllInBatch fails here.
        assertThat(mockingDetails(gl).getInvocations()).singleElement().satisfies(call -> {
            assertThat(call.getMethod().getName()).isEqualTo("deleteByBalanceDateBetweenBulk");
            assertThat(call.getArguments()).containsExactly(start, end);
        });
        assertThat(mockingDetails(sl).getInvocations()).singleElement().satisfies(call -> {
            assertThat(call.getMethod().getName()).isEqualTo("deleteByBalanceDateBetweenBulk");
            assertThat(call.getArguments()).containsExactly(start, end);
        });
        if (managesPersistenceContext) {
            var order = inOrder(entityManager);
            order.verify(entityManager).flush();
            order.verify(entityManager).clear();
            order.verifyNoMoreInteractions();
        }
    }
}
