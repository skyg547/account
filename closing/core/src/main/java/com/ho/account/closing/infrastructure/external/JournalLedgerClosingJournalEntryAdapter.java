package com.ho.account.closing.infrastructure.external;

import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalEntryPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryResult;
import com.ho.account.closing.application.port.out.ClosingJournalLineCommand;
import com.ho.account.closing.application.port.out.ClosingJournalSide;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSummary;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Batch-neutral adapter for deterministic Closing journal creation and safe retry reconciliation.
 *
 * <p>An existing deterministic slip is reusable only when its header, lineage and complete line set
 * equal the requested business content. This check prevents an API or Batch retry from accepting a
 * colliding slip that represents a different financial adjustment.</p>
 */
public final class JournalLedgerClosingJournalEntryAdapter implements ClosingJournalEntryPort {

    private final JournalPostingPort journalPostingPort;
    private final JournalQueryPort journalQueryPort;

    public JournalLedgerClosingJournalEntryAdapter(
            JournalPostingPort journalPostingPort,
            JournalQueryPort journalQueryPort) {
        this.journalPostingPort = Objects.requireNonNull(journalPostingPort, "journalPostingPort must not be null");
        this.journalQueryPort = Objects.requireNonNull(journalQueryPort, "journalQueryPort must not be null");
    }

    @Override
    public ClosingJournalEntryResult createDraftAdjustment(ClosingJournalEntryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Optional<JournalSummary> existing = journalQueryPort.findBySlipNo(command.slipNo());
        if (existing.isPresent()) {
            JournalSummary summary = existing.get();
            List<JournalDetailSummary> details = journalQueryPort.getJournalDetails(summary.getId());
            requireSameClosingRequest(command, summary, details);
            return new ClosingJournalEntryResult(summary.getId(), summary.getSlipNo(), summary.getStatus());
        }

        JournalEntryCommand journalCommand = new JournalEntryCommand(
                command.slipDate(),
                command.accountingDate(),
                command.description(),
                command.entryType(),
                command.currencyCode(),
                command.exchangeRate(),
                command.createdBy(),
                command.auditUser(),
                command.lineageSourceType(),
                command.lineageSourceId(),
                command.slipNo(),
                command.lines().stream().map(this::toJournalLine).toList());
        JournalPostingResult result = journalPostingPort.createDraftEntry(journalCommand);
        // Journal may deduplicate by lineage before honoring the requested slip. A reply for
        // another draft must not be recorded as this FX/ECL adjustment or auto-posted by ID.
        if (result == null || result.journalEntryId() == null || result.journalEntryId() < 1
                || !command.slipNo().equals(result.slipNo()) || !"DRAFT".equals(result.status())) {
            throw new IllegalStateException("Journal returned a different or invalid Closing draft");
        }
        return new ClosingJournalEntryResult(result.journalEntryId(), result.slipNo());
    }

    @Override
    public void approveAndPost(Long journalEntryId, String actor) {
        journalPostingPort.approveAndPost(journalEntryId, actor);
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
            JournalSummary persisted,
            List<JournalDetailSummary> details) {
        boolean sameHeader = Objects.equals(command.slipNo(), persisted.getSlipNo())
                && Objects.equals(command.slipDate(), persisted.getSlipDate())
                && Objects.equals(command.accountingDate(), persisted.getAccountingDate())
                && Objects.equals(command.description(), persisted.getDescription())
                && Objects.equals(command.entryType(), persisted.getEntryType())
                && Objects.equals(command.currencyCode(), persisted.getCurrencyCode())
                && Objects.equals(command.lineageSourceType(), persisted.getLineageSourceType())
                && Objects.equals(command.lineageSourceId(), persisted.getLineageSourceId());
        List<String> requestedLines = command.lines().stream().map(this::canonicalLine).sorted().toList();
        List<String> persistedLines = details.stream().map(this::canonicalLine).sorted().toList();
        if (!sameHeader || !requestedLines.equals(persistedLines)) {
            throw new IllegalStateException(
                    "Closing slip already exists with different business content: " + command.slipNo());
        }
        if (!"DRAFT".equals(persisted.getStatus()) && !"POSTED".equals(persisted.getStatus())) {
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

    private String canonicalLine(JournalDetailSummary line) {
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
