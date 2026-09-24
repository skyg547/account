package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.journalledger.application.port.out.BalanceReaggregationControlPort;
import com.ho.account.journalledger.application.port.out.BalanceReaggregationControlPort.GlActual;
import com.ho.account.journalledger.application.port.out.BalanceReaggregationControlPort.GlKey;
import com.ho.account.journalledger.application.port.out.BalanceReaggregationControlPort.GlMovement;
import com.ho.account.journalledger.application.port.out.BalanceReaggregationControlPort.ReconciliationData;
import com.ho.account.journalledger.application.port.out.BalanceReaggregationControlPort.SlActual;
import com.ho.account.journalledger.application.port.out.BalanceReaggregationControlPort.SlKey;
import com.ho.account.journalledger.application.port.out.BalanceReaggregationControlPort.SlMovement;
import com.ho.account.journalledger.application.port.out.LedgerBalancePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Coordinates the persistent owner barrier; Spring Batch remains only the inbound orchestrator. */
@Service
@RequiredArgsConstructor
public class BalanceReaggregationService {

    private final LedgerBalancePersistencePort balances;
    private final BalanceReaggregationControlPort control;

    @Transactional
    public void start(long ownerJobInstanceId, LocalDate startDate, LocalDate endDate) {
        // Start takes every writer stripe before changing publication state. A writer that won a
        // stripe is included first; a writer that lost observes REBUILDING after it wakes.
        balances.lockAllBalanceAccounts();
        control.start(ownerJobInstanceId, startDate, endDate);
    }

    @Transactional
    public void clean(long ownerJobInstanceId, LocalDate startDate, LocalDate endDate) {
        balances.lockAllBalanceAccounts();
        control.assertOwner(ownerJobInstanceId, startDate, endDate);
        balances.deleteBalancesBetween(startDate, endDate);
    }

    @Transactional
    public void reconcileAndRelease(long ownerJobInstanceId, LocalDate startDate, LocalDate endDate) {
        // The final transaction waits for every committed chunk, then compares compact daily groups
        // while POSTED input is still frozen. Any mismatch rolls back and deliberately stays closed.
        balances.lockAllBalanceAccounts();
        control.assertOwner(ownerJobInstanceId, startDate, endDate);
        reconcile(control.loadReconciliationData(startDate, endDate));
        control.release(ownerJobInstanceId, startDate, endDate);
    }

    void reconcile(ReconciliationData data) {
        Map<GlKey, BigDecimal> glRunning = new LinkedHashMap<>();
        data.glPriors().forEach(prior -> glRunning.put(prior.key(), prior.ending()));
        Map<DatedGlKey, Amounts> expectedGl = new LinkedHashMap<>();
        for (GlMovement movement : data.glMovements()) {
            BigDecimal beginning = glRunning.getOrDefault(movement.key(), BigDecimal.ZERO);
            BigDecimal ending = beginning.add(movement.debit()).subtract(movement.credit());
            expectedGl.put(new DatedGlKey(movement.date(), movement.key()),
                    new Amounts(beginning, movement.debit(), movement.credit(), ending));
            glRunning.put(movement.key(), ending);
        }
        Map<DatedGlKey, Amounts> actualGl = new LinkedHashMap<>();
        for (GlActual actual : data.glActuals()) {
            putUnique(actualGl, new DatedGlKey(actual.date(), actual.key()),
                    new Amounts(actual.beginning(), actual.debit(), actual.credit(), actual.ending()), "GL");
        }
        compare(expectedGl, actualGl, "GL");

        Map<SlKey, BigDecimal> slRunning = new LinkedHashMap<>();
        data.slPriors().forEach(prior -> slRunning.put(prior.key(), prior.ending()));
        Map<DatedSlKey, Amounts> expectedSl = new LinkedHashMap<>();
        for (SlMovement movement : data.slMovements()) {
            BigDecimal beginning = slRunning.getOrDefault(movement.key(), BigDecimal.ZERO);
            BigDecimal ending = beginning.add(movement.debit()).subtract(movement.credit());
            expectedSl.put(new DatedSlKey(movement.date(), movement.key()),
                    new Amounts(beginning, movement.debit(), movement.credit(), ending));
            slRunning.put(movement.key(), ending);
        }
        Map<DatedSlKey, Amounts> actualSl = new LinkedHashMap<>();
        for (SlActual actual : data.slActuals()) {
            putUnique(actualSl, new DatedSlKey(actual.date(), actual.key()),
                    new Amounts(actual.beginning(), actual.debit(), actual.credit(), actual.ending()), "SL");
        }
        compare(expectedSl, actualSl, "SL");
    }

    private <K> void putUnique(Map<K, Amounts> target, K key, Amounts amounts, String ledger) {
        if (target.put(key, amounts) != null) {
            throw new IllegalStateException("Balance reaggregation reconciliation found duplicate " + ledger + " key: " + key);
        }
    }

    private <K> void compare(Map<K, Amounts> expected, Map<K, Amounts> actual, String ledger) {
        if (!expected.keySet().equals(actual.keySet())) {
            throw new IllegalStateException("Balance reaggregation reconciliation " + ledger
                    + " key mismatch; expected=" + expected.keySet() + ", actual=" + actual.keySet());
        }
        for (Map.Entry<K, Amounts> entry : expected.entrySet()) {
            if (!entry.getValue().financiallyEquals(actual.get(entry.getKey()))) {
                throw new IllegalStateException("Balance reaggregation reconciliation " + ledger
                        + " amount mismatch at " + entry.getKey());
            }
        }
    }

    private record DatedGlKey(LocalDate date, GlKey key) {}
    private record DatedSlKey(LocalDate date, SlKey key) {}
    private record Amounts(BigDecimal beginning, BigDecimal debit, BigDecimal credit, BigDecimal ending) {
        boolean financiallyEquals(Amounts other) {
            return other != null && same(beginning, other.beginning) && same(debit, other.debit)
                    && same(credit, other.credit) && same(ending, other.ending);
        }

        private boolean same(BigDecimal left, BigDecimal right) {
            return left != null && right != null && left.compareTo(right) == 0;
        }
    }
}
