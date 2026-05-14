package com.ho.account.common.adapter;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MonolithJournalPostingAdapter {

    private final JournalUseCase journalUseCase;

    public void post(MonolithJournalPostingCommand command) {
        JournalEntry entry = new JournalEntry();
        entry.setAccountingDate(command.accountingDate());
        entry.setSlipDate(command.accountingDate());
        entry.setDescription(command.description());

        String currencyCode = command.currencyCode() != null ? command.currencyCode() : "KRW";
        entry.setCurrencyCode(currencyCode);

        entry.setDetails(command.lines().stream().map(line -> {
            JournalDetail detail = new JournalDetail();

            detail.setAccountCode(line.accountCode());

            if (line.debitAmount() != null && line.debitAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                detail.setAmount(line.debitAmount());
                detail.setBaseAmount(line.debitAmount());
                detail.setSide(JournalSide.DEBIT);
            } else if (line.creditAmount() != null && line.creditAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                detail.setAmount(line.creditAmount());
                detail.setBaseAmount(line.creditAmount());
                detail.setSide(JournalSide.CREDIT);
            }

            if (line.departmentCode() != null) {
                detail.setDepartmentCode(line.departmentCode());
            }

            if (line.businessPartnerCode() != null) {
                detail.setBusinessPartnerCode(line.businessPartnerCode());
            }

            detail.setDetailDescription(line.description());
            detail.setJournalEntry(entry);
            return detail;
        }).collect(Collectors.toList()));

        journalUseCase.createJournalEntry(entry);
    }
}