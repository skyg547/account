package com.ho.account.loan.infrastructure.adapter;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.loan.application.port.out.LoanJournalPort;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Loan 전표 명령을 현재 모놀리스 journal-ledger 유즈케이스로 번역하는 출력 어댑터.
 */
@Component
public class LoanJournalAdapter implements LoanJournalPort {

    private final JournalUseCase journalUseCase;

    public LoanJournalAdapter(JournalUseCase journalUseCase) {
        this.journalUseCase = journalUseCase;
    }

    @Override
    public PostedJournal post(LoanJournalCommand command) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.now());
        entry.setAccountingDate(command.accountingDate());
        entry.setDescription(command.description());
        entry.setStatus(JournalEntryStatus.DRAFT);
        entry.setEntryType("NORMAL");
        entry.setCreatedBy(command.actor());
        entry.setAuditUser(command.actor());
        entry.setLineageSourceType(command.lineageSourceType());
        entry.setLineageSourceId(command.lineageSourceId());
        entry.setCurrencyCode(command.currencyCode());

        command.lines().forEach(line -> entry.addDetail(toDetail(line, command.actor())));
        JournalEntry saved = journalUseCase.createJournalEntry(entry);
        journalUseCase.approveJournalEntry(saved.getId(), command.actor());
        journalUseCase.postJournalEntry(saved.getId(), command.actor());
        JournalEntry posted = journalUseCase.getJournalEntryWithDetails(saved.getId()).orElse(saved);
        return new PostedJournal(posted.getId(), posted.getSlipNo());
    }

    private JournalDetail toDetail(LoanJournalLine line, String actor) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(JournalSide.valueOf(line.side()));
        detail.setAccountCode(line.accountCode());
        detail.setAmount(line.amount());
        detail.setBaseAmount(line.amount());
        detail.setDetailDescription(line.description());
        detail.setAuditUser(actor);
        return detail;
    }
}
