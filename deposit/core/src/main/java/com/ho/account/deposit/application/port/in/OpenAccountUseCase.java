package com.ho.account.deposit.application.port.in;

import java.math.BigDecimal;

public interface OpenAccountUseCase {
    String openAccount(OpenAccountCommand command);

    record OpenAccountCommand(
        String customerCode,
        String productCode,
        String currencyCode,
        BigDecimal initialDeposit,
        BigDecimal interestRate
    ) {}
}
