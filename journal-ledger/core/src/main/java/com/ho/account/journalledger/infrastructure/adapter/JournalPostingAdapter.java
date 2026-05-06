package com.ho.account.journalledger.infrastructure.adapter;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
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

import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * 전표 전기 어댑터 (Journal Posting Adapter).
 * 
 * Contracts 모듈의 JournalPostingPort를 구현하여 
 * 타 모듈(매입, 매출, 지급 등)로부터의 전표 생성 요청을 처리합니다.
 */
@Component
@RequiredArgsConstructor
public class JournalPostingAdapter implements JournalPostingPort {

    private final JournalUseCase journalUseCase;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final DepartmentPersistencePort departmentPersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final CurrencyPersistencePort currencyPersistencePort;

    @Override
    public JournalPostingResult createDraftEntry(JournalEntryCommand command) {
        JournalEntry entry = new JournalEntry();
        entry.setAccountingDate(command.accountingDate());
        entry.setSlipDate(command.slipDate());
        entry.setDescription(command.description());

        // 기본 통화 KRW (Command에 없으면)
        String currencyCode = command.currencyCode() != null ? command.currencyCode() : "KRW";
        Currency currency = currencyPersistencePort.findByCode(currencyCode)
                .orElseThrow(() -> new IllegalArgumentException("Currency not found: " + currencyCode));
        entry.setCurrency(currency);

        entry.setDetails(command.lines().stream().map(line -> {
            JournalDetail detail = new JournalDetail();
            
            AccountSubject account = accountSubjectPersistencePort.findByCode(line.accountCode())
                    .orElseThrow(() -> new IllegalArgumentException("Account subject not found: " + line.accountCode()));
            detail.setAccountSubject(account);
            
            detail.setAmount(line.amount());
            detail.setSide("DEBIT".equalsIgnoreCase(line.drcrType()) ? JournalSide.DEBIT : JournalSide.CREDIT);
            
            if (line.departmentCode() != null) {
                departmentPersistencePort.findActiveByCode(line.departmentCode())
                        .ifPresent(detail::setDepartment);
            }
            
            if (line.businessPartnerCode() != null) {
                businessPartnerPersistencePort.findByBusinessPartnerCode(line.businessPartnerCode())
                        .ifPresent(detail::setBusinessPartner);
            }
            
            detail.setDetailDescription(line.detailDescription());
            detail.setJournalEntry(entry);
            return detail;
        }).collect(Collectors.toList()));

        JournalEntry savedEntry = journalUseCase.createJournalEntry(entry);
        
        return new JournalPostingResult(
                savedEntry.getId(),
                savedEntry.getSlipNo(),
                savedEntry.getStatus().name()
        );
    }
}
