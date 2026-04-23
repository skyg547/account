package com.ho.account.common.adapter;

import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.service.JournalService;
import org.springframework.stereotype.Component;

@Component
public class MonolithJournalPostingAdapter implements JournalPostingPort {

    private final JournalService journalService;
    private final accountSubjectPersistencePort accountSubjectPersistencePort;
    private final departmentPersistencePort departmentPersistencePort;
    private final businessPartnerPersistencePort businessPartnerPersistencePort;
    private final currencyPersistencePort currencyPersistencePort;

    public MonolithJournalPostingAdapter(
            JournalService journalService,
            accountSubjectPersistencePort accountSubjectPersistencePort,
            departmentPersistencePort departmentPersistencePort,
            businessPartnerPersistencePort businessPartnerPersistencePort,
            currencyPersistencePort currencyPersistencePort) {
        this.journalService = journalService;
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
        this.departmentPersistencePort = departmentPersistencePort;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.currencyPersistencePort = currencyPersistencePort;
    }

    @Override
    public JournalPostingResult createDraftEntry(JournalEntryCommand command) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(command.slipDate());
        entry.setAccountingDate(command.accountingDate());
        entry.setDescription(command.description());
        entry.setEntryType(command.entryType());
        entry.setExchangeRate(command.exchangeRate());
        entry.setCreatedBy(command.createdBy());
        entry.setAuditUser(command.auditUser());
        entry.setLineageSourceType(command.lineageSourceType());
        entry.setLineageSourceId(command.lineageSourceId());

        if (command.currencyCode() != null && !command.currencyCode().isBlank()) {
            entry.setCurrency(currencyPersistencePort.findByCurrencyCode(command.currencyCode())
                    .orElseThrow(() -> new IllegalArgumentException("?µí™”ë¥?ì°¾ì„ ???†ìŠµ?ˆë‹¤. code=" + command.currencyCode())));
        }

        for (JournalLineCommand line : command.lines()) {
            JournalDetail detail = new JournalDetail();
            detail.setDrcrType(line.drcrType());
            detail.setAccountSubject(accountSubjectPersistencePort.findByCode(line.accountCode())
                    .orElseThrow(() -> new IllegalArgumentException("ê³„ì •ê³¼ëª©??ì°¾ì„ ???†ìŠµ?ˆë‹¤. code=" + line.accountCode())));
            detail.setAmount(line.amount());
            detail.setBaseAmount(line.baseAmount() != null ? line.baseAmount() : line.amount());
            detail.setDetailDescription(line.detailDescription());
            detail.setAuditUser(command.auditUser());

            if (line.departmentCode() != null && !line.departmentCode().isBlank()) {
                detail.setDepartment(departmentPersistencePort.findByCode(line.departmentCode())
                        .orElseThrow(() -> new IllegalArgumentException("ë¶€?œë? ì°¾ì„ ???†ìŠµ?ˆë‹¤. code=" + line.departmentCode())));
            }

            if (line.businessPartnerCode() != null && !line.businessPartnerCode().isBlank()) {
                detail.setBusinessPartner(businessPartnerPersistencePort.findByBusinessPartnerCode(line.businessPartnerCode())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "ê±°ë˜ì²˜ë? ì°¾ì„ ???†ìŠµ?ˆë‹¤. code=" + line.businessPartnerCode())));
            }

            entry.addDetail(detail);
        }

        JournalEntry created = journalService.createJournalEntry(entry);
        return new JournalPostingResult(created.getId(), created.getSlipNo(), created.getStatus().name());
    }
}
