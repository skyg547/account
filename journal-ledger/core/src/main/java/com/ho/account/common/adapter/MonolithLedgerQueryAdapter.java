package com.ho.account.common.adapter;

import com.ho.account.contracts.ledger.LedgerBalanceSummary;
import com.ho.account.contracts.ledger.LedgerQueryPort;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MonolithLedgerQueryAdapter implements LedgerQueryPort {

    private final LedgerService ledgerService;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final CurrencyPersistencePort currencyPersistencePort;

    @Override
    public List<LedgerBalanceSummary> getGlBalanceSummaries(LocalDate startDate,
                                                            LocalDate endDate,
                                                            String accountCode,
                                                            String currencyCode) {
        AccountSubject accountSubject = resolveAccountSubject(accountCode);
        Currency currency = resolveCurrency(currencyCode);

        if (accountCode != null && !accountCode.isBlank() && accountSubject == null) {
            return List.of();
        }
        if (currencyCode != null && !currencyCode.isBlank() && currency == null) {
            return List.of();
        }

        return ledgerService.getGlBalances(startDate, endDate, accountSubject, currency).stream()
                .map(this::mapToSummary)
                .toList();
    }

    private AccountSubject resolveAccountSubject(String accountCode) {
        if (accountCode == null || accountCode.isBlank()) {
            return null;
        }
        return accountSubjectPersistencePort.findByCode(accountCode.trim()).orElse(null);
    }

    private Currency resolveCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return null;
        }
        return currencyPersistencePort.findByCode(currencyCode.trim()).orElse(null);
    }

    private LedgerBalanceSummary mapToSummary(GlBalance balance) {
        LedgerBalanceSummary summary = new LedgerBalanceSummary();
        if (balance.getAccountSubject() != null) {
            summary.setAccountCode(balance.getAccountSubject().getCode());
        }
        if (balance.getCurrency() != null) {
            summary.setCurrencyCode(balance.getCurrency().getCode());
        }
        summary.setDebitAmount(balance.getDebitAmount());
        summary.setCreditAmount(balance.getCreditAmount());
        summary.setEndingBalance(balance.getEndingBalance());
        return summary;
    }
}
