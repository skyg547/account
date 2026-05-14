package com.ho.account.common.adapter;

import com.ho.account.contracts.ledger.LedgerBalanceSummary;
import com.ho.account.contracts.ledger.LedgerQueryPort;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MonolithLedgerQueryAdapter implements LedgerQueryPort {

    private final LedgerService ledgerService;

    @Override
    public List<LedgerBalanceSummary> getGlBalanceSummaries(LocalDate startDate,
                                                            LocalDate endDate,
                                                            String accountCode,
                                                            String currencyCode) {
        return ledgerService.getGlBalances(startDate, endDate, accountCode, currencyCode).stream()
                .map(this::mapToSummary)
                .toList();
    }

    private LedgerBalanceSummary mapToSummary(GlBalance balance) {
        LedgerBalanceSummary summary = new LedgerBalanceSummary();
        summary.setAccountCode(balance.getAccountCode());
        summary.setCurrencyCode(balance.getCurrencyCode());
        summary.setDebitAmount(balance.getDebitAmount());
        summary.setCreditAmount(balance.getCreditAmount());
        summary.setEndingBalance(balance.getEndingBalance());
        return summary;
    }
}