package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.in.FinancialClosingCalculation;
import com.ho.account.closing.application.port.in.FinancialClosingCalculationResult;
import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalEntryResult;
import com.ho.account.closing.application.port.out.FxValuationEvidencePort;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Coordinates evidence-backed FX and ECL calculations for synchronous Closing API runs.
 *
 * <p>Preparation finishes for the entire source set before the first Journal call. The API hard
 * caps are checked during that read, preventing an unexpectedly large request from producing a
 * partially posted run. Batch validation deliberately shares the financial validation but not the
 * synchronous API size limits.</p>
 */
@RequiredArgsConstructor
public class FinancialClosingCalculationService implements FinancialClosingCalculation {

    private final FxValuationEvidencePort fxValuationEvidencePort;
    private final FxValuationService fxValuationService;
    private final EclProvisionService eclProvisionService;
    private final ClosingAccountingProperties accountingProperties;

    @Override
    public FinancialClosingCalculationResult runFxValuation(
            LocalDate valuationDate,
            Long valuationBatchId) {
        List<ClosingJournalEntryCommand> commands = new ArrayList<>();
        forEachPreparedFxCommand(
                valuationDate,
                valuationBatchId,
                true,
                commands::add);
        List<ClosingJournalEntryCommand> immutableCommands = List.copyOf(commands);
        List<ClosingJournalEntryResult> results = immutableCommands.stream()
                .map(fxValuationService::postPreparedFxValuation)
                .toList();
        return toCalculationResult(results);
    }

    @Override
    public FinancialClosingCalculationResult runEclProvision(
            LocalDate closingDate,
            Long provisionBatchId) {
        requireDateAndPositiveId(closingDate, provisionBatchId, "closingDate", "provisionBatchId");
        List<ClosingJournalEntryCommand> commands = List.copyOf(
                eclProvisionService.prepareEclProvision(closingDate, provisionBatchId));
        requireWithinCommandCap(commands.size());
        return toCalculationResult(eclProvisionService.postPreparedEclProvision(commands));
    }

    @Override
    public void validateFxValuation(LocalDate valuationDate, Long valuationBatchId) {
        // Batch owns its own chunk/partition bounds. This method intentionally applies no API cap.
        forEachPreparedFxCommand(valuationDate, valuationBatchId, false, ignored -> { });
    }

    private void forEachPreparedFxCommand(
            LocalDate valuationDate,
            Long valuationBatchId,
            boolean enforceApiCaps,
            Consumer<ClosingJournalEntryCommand> commandConsumer) {
        requireDateAndPositiveId(valuationDate, valuationBatchId, "valuationDate", "valuationBatchId");
        Objects.requireNonNull(commandConsumer, "commandConsumer must not be null");

        String reportingCurrency = accountingProperties.requireFxValuationReportingCurrencyCode();
        int evidenceCap = enforceApiCaps
                ? accountingProperties.getApiFinancialRunMaxEvidenceRows()
                : Integer.MAX_VALUE;
        int commandCap = enforceApiCaps
                ? accountingProperties.getApiFinancialRunMaxJournalCommands()
                : Integer.MAX_VALUE;
        AtomicInteger evidenceRows = new AtomicInteger();
        AtomicInteger journalCommands = new AtomicInteger();

        // The adapter owns and traverses its cursor exactly once for this calculation.
        fxValuationEvidencePort.forEachBalance(valuationDate, reportingCurrency, balance -> {
            int currentEvidenceRows = evidenceRows.incrementAndGet();
            if (enforceApiCaps && currentEvidenceRows > evidenceCap) {
                throw new IllegalStateException(
                        "FX valuation source evidence exceeds API hard cap of " + evidenceCap + " rows");
            }
            fxValuationService.prepareFxValuationForAccount(balance, valuationDate, valuationBatchId)
                    .ifPresent(command -> {
                        int currentCommands = journalCommands.incrementAndGet();
                        if (enforceApiCaps && currentCommands > commandCap) {
                            throw new IllegalStateException(
                                    "FX valuation journal commands exceed API hard cap of " + commandCap);
                        }
                        commandConsumer.accept(command);
                    });
        });

        if (evidenceRows.get() == 0) {
            throw new IllegalStateException(
                    "FX valuation source evidence is empty for " + valuationDate);
        }
    }

    private void requireWithinCommandCap(int commandCount) {
        int commandCap = accountingProperties.getApiFinancialRunMaxJournalCommands();
        if (commandCount > commandCap) {
            throw new IllegalStateException(
                    "ECL provision journal commands exceed API hard cap of " + commandCap);
        }
    }

    private FinancialClosingCalculationResult toCalculationResult(
            List<ClosingJournalEntryResult> results) {
        Objects.requireNonNull(results, "results must not be null");
        List<ClosingJournalEntryResult> immutableResults = List.copyOf(results);
        List<Long> journalEntryIds = new ArrayList<>(immutableResults.size());
        for (ClosingJournalEntryResult result : immutableResults) {
            journalEntryIds.add(Objects.requireNonNull(result, "journal result must not be null").journalEntryId());
        }
        return FinancialClosingCalculationResult.fromJournalEntryIds(journalEntryIds);
    }

    private void requireDateAndPositiveId(
            LocalDate date,
            Long batchId,
            String dateName,
            String batchIdName) {
        Objects.requireNonNull(date, dateName + " must not be null");
        if (batchId == null || batchId <= 0) {
            throw new IllegalArgumentException(batchIdName + " must be positive");
        }
    }
}
