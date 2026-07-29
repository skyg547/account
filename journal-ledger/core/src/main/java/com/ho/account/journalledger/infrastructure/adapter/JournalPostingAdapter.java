package com.ho.account.journalledger.infrastructure.adapter;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
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

    @Override
    public JournalPostingResult createDraftEntry(JournalEntryCommand command) {
        JournalEntry entry = new JournalEntry();
        entry.setAccountingDate(command.accountingDate());
        entry.setSlipDate(command.slipDate());
        entry.setDescription(command.description());
        entry.setEntryType(command.entryType());
        entry.setExchangeRate(command.exchangeRate() != null ? command.exchangeRate() : BigDecimal.ONE);
        entry.setCreatedBy(command.createdBy());
        entry.setAuditUser(command.auditUser());
        entry.setLineageSourceType(command.lineageSourceType());
        entry.setLineageSourceId(command.lineageSourceId());
        if (command.slipNo() != null && !command.slipNo().isBlank()) {
            entry.setSlipNo(command.slipNo().trim());
        }

        // 기본 통화 KRW (Command에 없으면)
        String currencyCode = command.currencyCode() != null ? command.currencyCode() : "KRW";
        entry.setCurrencyCode(currencyCode);

        entry.setDetails(command.lines().stream().map(line -> {
            JournalDetail detail = new JournalDetail();
            
            detail.setAccountCode(line.accountCode());
            detail.setAmount(line.amount());
            detail.setBaseAmount(line.baseAmount() != null ? line.baseAmount() : line.amount());
            detail.setSide("DEBIT".equalsIgnoreCase(line.drcrType()) ? JournalSide.DEBIT : JournalSide.CREDIT);
            
            if (line.departmentCode() != null) {
                detail.setDepartmentCode(line.departmentCode());
            }
            
            if (line.businessPartnerCode() != null) {
                detail.setBusinessPartnerCode(line.businessPartnerCode());
            }
            
            detail.setDetailDescription(line.detailDescription());
            detail.setAuditUser(command.auditUser());
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
