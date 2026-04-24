package com.ho.account.common.adapter;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.service.JournalService;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.masterdata.core.domain.model.Department;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 모놀리스 전표 기입 어댑터
 */
@Component
@RequiredArgsConstructor
public class MonolithJournalPostingAdapter {

    private final JournalService journalService;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final DepartmentPersistencePort departmentPersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final CurrencyPersistencePort currencyPersistencePort;

    public void post(com.ho.account.common.adapter.MonolithJournalPostingCommand command) {
        JournalEntry entry = new JournalEntry();
        entry.setAccountingDate(command.accountingDate());
        entry.setSlipDate(command.accountingDate());
        entry.setDescription(command.description());
        
        Currency currency = currencyPersistencePort.findByCode(command.currencyCode())
                .orElseThrow(() -> new IllegalArgumentException("Currency not found: " + command.currencyCode()));
        entry.setCurrency(currency);

        entry.setDetails(command.lines().stream().map(line -> {
            JournalDetail detail = new JournalDetail();
            AccountSubject account = accountSubjectPersistencePort.findByCode(line.accountCode())
                    .orElseThrow(() -> new IllegalArgumentException("Account subject not found: " + line.accountCode()));
            detail.setAccountSubject(account);
            
            if (line.debitAmount() != null && line.debitAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                detail.setAmount(line.debitAmount());
                detail.setDrcrType("DEBIT");
            } else if (line.creditAmount() != null && line.creditAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                detail.setAmount(line.creditAmount());
                detail.setDrcrType("CREDIT");
            }
            
            if (line.departmentCode() != null) {
                Department dept = departmentPersistencePort.findByCode(line.departmentCode())
                        .orElseThrow(() -> new IllegalArgumentException("Department not found: " + line.departmentCode()));
                detail.setDepartment(dept);
            }
            
            if (line.businessPartnerCode() != null) {
                BusinessPartner bp = businessPartnerPersistencePort.findByBusinessPartnerCode(line.businessPartnerCode())
                        .orElseThrow(() -> new IllegalArgumentException("Business partner not found: " + line.businessPartnerCode()));
                detail.setBusinessPartner(bp);
            }
            
            detail.setDetailDescription(line.description());
            detail.setJournalEntry(entry);
            return detail;
        }).collect(Collectors.toList()));

        journalService.createJournalEntry(entry);
    }
}
