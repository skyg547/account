package com.ho.account.closing.batch.adapter.out;

import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalEntryPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryResult;
import com.ho.account.closing.application.port.out.ClosingJournalLineCommand;
import com.ho.account.closing.application.port.out.ClosingJournalSide;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class JournalLedgerClosingJournalEntryAdapter implements ClosingJournalEntryPort {

    private final JournalPostingPort journalPostingPort;
    private final JournalUseCase journalUseCase;

    @Override
    public ClosingJournalEntryResult createDraftAdjustment(ClosingJournalEntryCommand command) {
        var existing = journalUseCase.getJournalEntryBySlipNo(command.slipNo());
        if (existing.isPresent()) {
            JournalEntry persisted = journalUseCase.getJournalEntryWithDetails(existing.get().getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Existing closing journal cannot be loaded with details: " + command.slipNo()));
            requireSameClosingRequest(command, persisted);
            return new ClosingJournalEntryResult(persisted.getId(), persisted.getSlipNo());
        }

        JournalEntryCommand journalCommand = new JournalEntryCommand(
                command.slipDate(),
                command.accountingDate(),
                command.description(),
                command.entryType(),
                command.currencyCode(),
                BigDecimal.ONE,
                command.createdBy(),
                command.auditUser(),
                command.lineageSourceType(),
                command.lineageSourceId(),
                command.slipNo(),
                command.lines().stream().map(this::toJournalLine).toList());
        JournalPostingResult result = journalPostingPort.createDraftEntry(journalCommand);
        return new ClosingJournalEntryResult(result.journalEntryId(), result.slipNo());
    }

    @Override
    public void approveAndPost(Long journalEntryId, String actor) {
        JournalEntry entry = journalUseCase.getJournalEntry(journalEntryId)
                .orElseThrow(() -> new IllegalStateException(
                        "Closing journal entry not found: " + journalEntryId));
        JournalEntryStatus status = entry.getStatus();
        if (status == JournalEntryStatus.POSTED) {
            return;
        }
        if (status == JournalEntryStatus.DRAFT || status == JournalEntryStatus.REQUESTED) {
            journalUseCase.approveJournalEntry(journalEntryId, actor);
            journalUseCase.postJournalEntry(journalEntryId, actor);
            return;
        }
        if (status == JournalEntryStatus.APPROVED) {
            journalUseCase.postJournalEntry(journalEntryId, actor);
            return;
        }
        throw new IllegalStateException(
                "Closing journal cannot be posted from status " + status + ": " + journalEntryId);
    }

    private JournalLineCommand toJournalLine(ClosingJournalLineCommand line) {
        return new JournalLineCommand(
                line.side() == ClosingJournalSide.DEBIT ? "DEBIT" : "CREDIT",
                line.accountCode(),
                line.amount(),
                line.baseAmount(),
                null,
                null,
                line.description());
    }

    private void requireSameClosingRequest(
            ClosingJournalEntryCommand command,
            JournalEntry persisted) {
        boolean sameHeader = Objects.equals(command.slipNo(), persisted.getSlipNo())
                && Objects.equals(command.slipDate(), persisted.getSlipDate())
                && Objects.equals(command.accountingDate(), persisted.getAccountingDate())
                && Objects.equals(command.description(), persisted.getDescription())
                && Objects.equals(command.entryType(), persisted.getEntryType())
                && Objects.equals(command.currencyCode(), persisted.getCurrencyCode())
                && Objects.equals(command.lineageSourceType(), persisted.getLineageSourceType())
                && Objects.equals(command.lineageSourceId(), persisted.getLineageSourceId());
        List<String> requestedLines = command.lines().stream()
                .map(this::canonicalLine)
                .sorted()
                .toList();
        List<String> persistedLines = persisted.getDetails().stream()
                .map(this::canonicalLine)
                .sorted()
                .toList();
        if (!sameHeader || !requestedLines.equals(persistedLines)) {
            throw new IllegalStateException(
                    "Closing slip already exists with different business content: " + command.slipNo());
        }
        if (persisted.getStatus() == JournalEntryStatus.REJECTED
                || persisted.getStatus() == JournalEntryStatus.REVERSED) {
            throw new IllegalStateException(
                    "Closing slip exists in a non-reusable status " + persisted.getStatus()
                            + ": " + command.slipNo());
        }
    }

    private String canonicalLine(ClosingJournalLineCommand line) {
        return line.side().name()
                + "|" + line.accountCode()
                + "|" + decimal(line.amount())
                + "|" + decimal(line.baseAmount())
                + "|||" + optional(line.description());
    }

    private String canonicalLine(JournalDetail line) {
        return line.getSide().name()
                + "|" + line.getAccountCode()
                + "|" + decimal(line.getAmount())
                + "|" + decimal(line.getBaseAmount())
                + "|" + optional(line.getDepartmentCode())
                + "|" + optional(line.getBusinessPartnerCode())
                + "|" + optional(line.getDetailDescription());
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String optional(String value) {
        return value == null ? "" : value.trim();
    }
}
