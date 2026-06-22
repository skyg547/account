package com.ho.account.deposit.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DepositBatchUseCase {

    DepositAccountIntegrityResult validateActiveAccounts(LocalDate asOfDate);

    record DepositAccountIntegrityResult(int checkedCount, BigDecimal totalBalance) {
    }
}
