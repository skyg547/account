package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.ClosingMonetaryPrecision;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Posted allowance balances, credit-positive in explicitly named transaction and functional units.
 * Negative (debit) balances remain visible so reconciliation cannot silently discard them.
 */
public record AllowanceBalance(
        String transactionCurrencyCode,
        String functionalCurrencyCode,
        BigDecimal creditTransactionAmount,
        BigDecimal creditBaseAmount) {

    public AllowanceBalance {
        transactionCurrencyCode = requireCurrency(transactionCurrencyCode, "transactionCurrencyCode");
        functionalCurrencyCode = requireCurrency(functionalCurrencyCode, "functionalCurrencyCode");
        creditTransactionAmount = ClosingMonetaryPrecision.amount(creditTransactionAmount);
        creditBaseAmount = ClosingMonetaryPrecision.amount(creditBaseAmount);
    }

    private static String requireCurrency(String value, String name) {
        if (value == null || !value.trim().matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException(name + " must be a 3-letter currency code");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
