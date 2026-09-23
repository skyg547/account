package com.ho.account.cashflow.core.infrastructure.memory;

import com.ho.account.cashflow.core.application.port.out.LedgerCashBalancePort;
import com.ho.account.cashflow.core.domain.CashflowAmounts;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryLedgerCashBalanceAdapter implements LedgerCashBalancePort {

    private final Map<BalanceKey, BigDecimal> balances = new ConcurrentHashMap<>();

    public void setBeginningCash(int fiscalYear, int fiscalPeriod, String currency, BigDecimal amount) {
        balances.put(
                new BalanceKey(fiscalYear, fiscalPeriod, CashflowAmounts.normalizeCurrency(currency)),
                CashflowAmounts.normalize(amount, "amount"));
    }

    @Override
    public BigDecimal findBeginningCash(int fiscalYear, int fiscalPeriod, String currency) {
        return balances.getOrDefault(
                new BalanceKey(fiscalYear, fiscalPeriod, CashflowAmounts.normalizeCurrency(currency)),
                CashflowAmounts.ZERO);
    }

    private record BalanceKey(int fiscalYear, int fiscalPeriod, String currency) {
    }
}
