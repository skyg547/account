package com.ho.account.common.adapter;

import com.ho.account.contracts.ledger.LedgerBalanceSummary;
import com.ho.account.contracts.ledger.LedgerQueryPort;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
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
                .map(this::mapGlToSummary)
                .toList();
    }

    @Override
    public List<LedgerBalanceSummary> getSlBalanceSummaries(LocalDate startDate,
                                                            LocalDate endDate,
                                                            String accountCode,
                                                            String businessPartnerCode,
                                                            String departmentCode,
                                                            String currencyCode) {
        return ledgerService.getSlBalances(
                startDate,
                endDate,
                accountCode,
                businessPartnerCode,
                departmentCode,
                currencyCode).stream()
                .map(this::mapSlToSummary)
                .toList();
    }

    private LedgerBalanceSummary mapGlToSummary(GlBalance balance) {
        LedgerBalanceSummary summary = new LedgerBalanceSummary();
        summary.setAccountCode(balance.getAccountCode());
        summary.setCurrencyCode(balance.getCurrencyCode());
        summary.setDebitAmount(balance.getDebitAmount());
        summary.setCreditAmount(balance.getCreditAmount());
        summary.setEndingBalance(balance.getEndingBalance());
        return summary;
    }

    private LedgerBalanceSummary mapSlToSummary(SlBalance balance) {
        LedgerBalanceSummary summary = new LedgerBalanceSummary();
        summary.setAccountCode(balance.getAccountCode());
        summary.setBusinessPartnerCode(balance.getBusinessPartnerCode());
        summary.setDepartmentCode(balance.getDepartmentCode());
        summary.setCurrencyCode(balance.getCurrencyCode());
        summary.setDebitAmount(balance.getDebitAmount());
        summary.setCreditAmount(balance.getCreditAmount());
        summary.setEndingBalance(balance.getEndingBalance());
        return summary;
    }
}
