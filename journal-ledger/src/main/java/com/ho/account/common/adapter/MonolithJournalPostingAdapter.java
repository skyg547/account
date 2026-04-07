package com.ho.account.common.adapter;

import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.CurrencyRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.service.JournalService;
import org.springframework.stereotype.Component;

@Component
public class MonolithJournalPostingAdapter implements JournalPostingPort {

    private final JournalService journalService;
    private final AccountSubjectRepository accountSubjectRepository;
    private final DepartmentRepository departmentRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final CurrencyRepository currencyRepository;

    public MonolithJournalPostingAdapter(
            JournalService journalService,
            AccountSubjectRepository accountSubjectRepository,
            DepartmentRepository departmentRepository,
            BusinessPartnerRepository businessPartnerRepository,
            CurrencyRepository currencyRepository) {
        this.journalService = journalService;
        this.accountSubjectRepository = accountSubjectRepository;
        this.departmentRepository = departmentRepository;
        this.businessPartnerRepository = businessPartnerRepository;
        this.currencyRepository = currencyRepository;
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
            entry.setCurrency(currencyRepository.findByCurrencyCode(command.currencyCode())
                    .orElseThrow(() -> new IllegalArgumentException("통화를 찾을 수 없습니다. code=" + command.currencyCode())));
        }

        for (JournalLineCommand line : command.lines()) {
            JournalDetail detail = new JournalDetail();
            detail.setDrcrType(line.drcrType());
            detail.setAccountSubject(accountSubjectRepository.findByCode(line.accountCode())
                    .orElseThrow(() -> new IllegalArgumentException("계정과목을 찾을 수 없습니다. code=" + line.accountCode())));
            detail.setAmount(line.amount());
            detail.setBaseAmount(line.baseAmount() != null ? line.baseAmount() : line.amount());
            detail.setDetailDescription(line.detailDescription());
            detail.setAuditUser(command.auditUser());

            if (line.departmentCode() != null && !line.departmentCode().isBlank()) {
                detail.setDepartment(departmentRepository.findByCode(line.departmentCode())
                        .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다. code=" + line.departmentCode())));
            }

            if (line.businessPartnerCode() != null && !line.businessPartnerCode().isBlank()) {
                detail.setBusinessPartner(businessPartnerRepository.findByBusinessPartnerCode(line.businessPartnerCode())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "거래처를 찾을 수 없습니다. code=" + line.businessPartnerCode())));
            }

            entry.addDetail(detail);
        }

        JournalEntry created = journalService.createJournalEntry(entry);
        return new JournalPostingResult(created.getId(), created.getSlipNo(), created.getStatus().name());
    }
}
