package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.journalledger.application.port.out.BalanceReaggregationControlPort;
import com.ho.account.journalledger.application.port.out.LedgerBalancePersistencePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceReaggregationServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 24);

    @Mock LedgerBalancePersistencePort balances;
    @Mock BalanceReaggregationControlPort control;

    @Test
    void reconciliationMismatchDoesNotReleaseTheFailClosedBarrier() {
        var key = new BalanceReaggregationControlPort.GlKey("10100", "KRW");
        when(control.loadReconciliationData(DATE, DATE)).thenReturn(new BalanceReaggregationControlPort.ReconciliationData(
                List.of(new BalanceReaggregationControlPort.GlMovement(DATE, key, amount("10"), amount("0"))),
                List.of(),
                List.of(new BalanceReaggregationControlPort.GlActual(DATE, key,
                        amount("0"), amount("9"), amount("0"), amount("9"))),
                List.of(), List.of(), List.of()));
        BalanceReaggregationService service = new BalanceReaggregationService(balances, control);

        assertThatThrownBy(() -> service.reconcileAndRelease(767, DATE, DATE))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("amount mismatch");

        var ordered = inOrder(balances, control);
        ordered.verify(balances).lockAllBalanceAccounts();
        ordered.verify(control).assertOwner(767, DATE, DATE);
        verify(control, never()).release(767, DATE, DATE);
    }

    @Test
    void exactGlAndNullableSlKeysReleaseOnlyAfterReconciliation() {
        var glKey = new BalanceReaggregationControlPort.GlKey("10100", "KRW");
        var slNull = new BalanceReaggregationControlPort.SlKey("10100", null, null, "KRW");
        var slLiteral = new BalanceReaggregationControlPort.SlKey("10100", "NULL", null, "KRW");
        when(control.loadReconciliationData(DATE, DATE)).thenReturn(new BalanceReaggregationControlPort.ReconciliationData(
                List.of(new BalanceReaggregationControlPort.GlMovement(DATE, glKey, amount("10"), amount("0"))),
                List.of(
                        new BalanceReaggregationControlPort.SlMovement(DATE, slNull, amount("4"), amount("0")),
                        new BalanceReaggregationControlPort.SlMovement(DATE, slLiteral, amount("6"), amount("0"))),
                List.of(new BalanceReaggregationControlPort.GlActual(DATE, glKey,
                        amount("0"), amount("10"), amount("0"), amount("10"))),
                List.of(
                        new BalanceReaggregationControlPort.SlActual(DATE, slNull,
                                amount("0"), amount("4"), amount("0"), amount("4")),
                        new BalanceReaggregationControlPort.SlActual(DATE, slLiteral,
                                amount("0"), amount("6"), amount("0"), amount("6"))),
                List.of(), List.of()));
        BalanceReaggregationService service = new BalanceReaggregationService(balances, control);

        service.reconcileAndRelease(767, DATE, DATE);

        verify(control).release(767, DATE, DATE);
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
