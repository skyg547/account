package com.ho.account.cashflow.core.application.port.out;

import java.math.BigDecimal;

public interface LedgerCashBalancePort {

    BigDecimal findBeginningCash(int fiscalYear, int fiscalPeriod, String currency);
}
