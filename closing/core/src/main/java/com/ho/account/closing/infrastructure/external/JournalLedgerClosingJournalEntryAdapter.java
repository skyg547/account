package com.ho.account.closing.infrastructure.external;

import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalEntryPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryResult;
import com.ho.account.closing.application.port.out.ClosingJournalLineage;
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
import java.util.HashMap;
import java.util.Map;

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
    public Map<String, String> preflightEclLineage(List<ClosingJournalLineage> expected) {
        Objects.requireNonNull(expected, "expected must not be null");
        Map<String, String> postedNoopReferences = new HashMap<>();
        for (ClosingJournalLineage lineage : expected) {
            journalQueryPort.findBySlipNo(lineage.slipNo()).ifPresent(existing -> {
                if (!lineage.slipNo().equals(existing.getSlipNo())
                        || !lineage.accountingDate().equals(existing.getSlipDate())
                        || !lineage.accountingDate().equals(existing.getAccountingDate())
                        || !lineage.currencyCode().equals(existing.getCurrencyCode())
                        || !"CLOSING_ADJUSTMENT".equals(existing.getEntryType())) {
                    throw new IllegalStateException("ECL slip has incompatible header: " + lineage.slipNo());
                }
                if (!"ECL_PROVISION".equals(existing.getLineageSourceType())
                        || existing.getLineageSourceId() == null
                        || !existing.getLineageSourceId().startsWith("ECLSNAP:")) {
                    throw new IllegalStateException("ECL slip has legacy or different snapshot lineage: "
                            + lineage.slipNo());
                }
                if (lineage.noAdjustment()
                        && lineage.snapshotReference().equals(existing.getLineageSourceId())) {
                    throw new IllegalStateException("ECL zero-delta operation has an unexpected existing slip: "
                            + lineage.slipNo());
                }
                if (!lineage.snapshotReference().equals(existing.getLineageSourceId())) {
                    // A posted adjustment can make a retry's calculated delta zero. Only its
                    // original immutable binding may authorize that no-op in the snapshot adapter.
                    if (!lineage.noAdjustment() || !"POSTED".equals(existing.getStatus())) {
                        throw new IllegalStateException("ECL slip has different snapshot lineage: "
                                + lineage.slipNo());
                    }
                    postedNoopReferences.put(lineage.operationKey(), existing.getLineageSourceId());
                }
            });
        }
        return Map.copyOf(postedNoopReferences);
    }

    @Override
    public ClosingJournalEntryResult createDraftAdjustment(ClosingJournalEntryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Optional<JournalSummary> existing = journalQueryPort.findBySlipNo(command.slipNo());
        if (existing.isPresent()) {
            JournalSummary summary = existing.get();
            List<JournalDetailSummary> details = journalQueryPort.getJournalDetails(summary.getId());
            requireSameClosingRequest(command, summary, details);
            return new ClosingJournalEntryResult(summary.getId(), summary.getSlipNo());
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
        if ("REJECTED".equals(persisted.getStatus()) || "REVERSED".equals(persisted.getStatus())) {
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
