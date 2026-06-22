package com.ho.account.deposit.application.service;

import com.ho.account.deposit.application.port.in.DepositBatchUseCase;
import com.ho.account.deposit.application.port.out.DepositAccountPersistencePort;
import com.ho.account.deposit.domain.DepositAccount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DepositBatchService implements DepositBatchUseCase {

    private final DepositAccountPersistencePort depositAccountPersistencePort;

    public DepositBatchService(DepositAccountPersistencePort depositAccountPersistencePort) {
        this.depositAccountPersistencePort = depositAccountPersistencePort;
    }

    @Override
    public DepositAccountIntegrityResult validateActiveAccounts(LocalDate asOfDate) {
        List<DepositAccount> accounts = depositAccountPersistencePort.findActiveAccounts(asOfDate);
        BigDecimal totalBalance = BigDecimal.ZERO;
        for (DepositAccount account : accounts) {
            validateAccount(account, asOfDate);
            totalBalance = totalBalance.add(account.getBalance());
        }
        return new DepositAccountIntegrityResult(accounts.size(), totalBalance);
    }

    private void validateAccount(DepositAccount account, LocalDate asOfDate) {
        requireText(account.getAccountNumber(), "accountNumber");
        requireText(account.getCustomerCode(), "customerCode");
        requireText(account.getProductCode(), "productCode");
        requireText(account.getCurrencyCode(), "currencyCode");
        if (account.getBalance() == null || account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Deposit balance must be non-negative: " + account.getAccountNumber());
        }
        if (account.getInterestRate() == null || account.getInterestRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Deposit interest rate must be non-negative: " + account.getAccountNumber());
        }
        if (account.getValidFrom() == null || account.getValidTo() == null
                || account.getValidFrom().isAfter(account.getValidTo())
                || asOfDate.isBefore(account.getValidFrom())
                || asOfDate.isAfter(account.getValidTo())) {
            throw new IllegalStateException("Deposit validity range is invalid: " + account.getAccountNumber());
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Deposit " + field + " is required.");
        }
    }
}
