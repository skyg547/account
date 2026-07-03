package com.ho.account.closing.batch.adapter.out;

import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalEntryPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryResult;
import com.ho.account.closing.application.port.out.ClosingJournalLineCommand;
import com.ho.account.closing.application.port.out.ClosingJournalSide;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JournalLedgerClosingJournalEntryAdapter implements ClosingJournalEntryPort {

    private final JournalUseCase journalUseCase;

    @Override
    public ClosingJournalEntryResult createDraftAdjustment(ClosingJournalEntryCommand command) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(command.slipDate());
        entry.setAccountingDate(command.accountingDate());
        entry.setDescription(command.description());
        entry.setStatus(JournalEntryStatus.DRAFT);
        entry.setEntryType(command.entryType());
        entry.setCreatedBy(command.createdBy());
        entry.setAuditUser(command.auditUser());
        entry.setLineageSourceType(command.lineageSourceType());
        entry.setLineageSourceId(command.lineageSourceId());
        entry.setCurrencyCode(command.currencyCode());
        entry.setSlipNo(command.slipNo());

        command.lines().stream()
                .map(this::toJournalDetail)
                .forEach(entry::addDetail);

        JournalEntry savedEntry = journalUseCase.createJournalEntry(entry);
        return new ClosingJournalEntryResult(savedEntry.getId(), savedEntry.getSlipNo());
    }

    @Override
    public void approveAndPost(Long journalEntryId, String actor) {
        journalUseCase.approveJournalEntry(journalEntryId, actor);
        journalUseCase.postJournalEntry(journalEntryId, actor);
    }

    private JournalDetail toJournalDetail(ClosingJournalLineCommand line) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(toJournalSide(line.side()));
        detail.setAccountCode(line.accountCode());
        detail.setAmount(line.amount());
        detail.setBaseAmount(line.baseAmount());
        detail.setDetailDescription(line.description());
        return detail;
    }

    private JournalSide toJournalSide(ClosingJournalSide side) {
        return side == ClosingJournalSide.DEBIT ? JournalSide.DEBIT : JournalSide.CREDIT;
    }
}