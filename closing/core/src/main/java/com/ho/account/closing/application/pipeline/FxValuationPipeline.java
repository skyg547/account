package com.ho.account.closing.application.pipeline;

import com.ho.account.closing.application.service.FxValuationBalance;
import com.ho.account.closing.application.service.FxValuationService;
import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Chunk-level FX valuation pipeline.
 *
 * <p>Every balance in a chunk is prepared before the first journal effect. Preparation failures
 * are aggregated so Spring Batch cannot checkpoint an invalid financial valuation. Journal
 * adapters may be remote, so their idempotent lineage remains the recovery boundary.</p>
 */
@RequiredArgsConstructor
public class FxValuationPipeline {

    private final FxValuationService fxValuationService;

    @Transactional
    public void processChunk(
            List<? extends FxValuationBalance> balances,
            LocalDate valuationDate,
            Long valuationBatchId) {
        Objects.requireNonNull(balances, "balances must not be null");
        Objects.requireNonNull(valuationDate, "valuationDate must not be null");
        if (valuationBatchId == null || valuationBatchId <= 0) {
            throw new IllegalArgumentException("valuationBatchId must be positive");
        }

        List<ClosingJournalEntryCommand> commands = prepareChunk(
                balances, valuationDate, valuationBatchId);
        commands.forEach(fxValuationService::postPreparedFxValuation);
    }

    /** Runs the same chunk preflight without any Journal write. */
    public void validateChunk(
            List<? extends FxValuationBalance> balances,
            LocalDate valuationDate,
            Long valuationBatchId) {
        Objects.requireNonNull(balances, "balances must not be null");
        Objects.requireNonNull(valuationDate, "valuationDate must not be null");
        if (valuationBatchId == null || valuationBatchId <= 0) {
            throw new IllegalArgumentException("valuationBatchId must be positive");
        }
        prepareChunk(balances, valuationDate, valuationBatchId);
    }

    private List<ClosingJournalEntryCommand> prepareChunk(
            List<? extends FxValuationBalance> balances,
            LocalDate valuationDate,
            Long valuationBatchId) {
        List<RuntimeException> failures = new ArrayList<>();
        List<ClosingJournalEntryCommand> commands = new ArrayList<>();
        for (FxValuationBalance balance : balances) {
            try {
                fxValuationService.prepareFxValuationForAccount(balance, valuationDate, valuationBatchId)
                        .ifPresent(commands::add);
            } catch (RuntimeException exception) {
                failures.add(new IllegalStateException(
                        "FX valuation failed for " + balanceIdentity(balance),
                        exception));
            }
        }

        if (!failures.isEmpty()) {
            IllegalStateException aggregate = new IllegalStateException(
                    "FX valuation chunk failed for " + failures.size() + " balance(s)");
            failures.forEach(aggregate::addSuppressed);
            throw aggregate;
        }
        return List.copyOf(commands);
    }

    private String balanceIdentity(FxValuationBalance balance) {
        return balance == null ? "<null>" : balance.accountCode() + "|" + balance.currencyCode();
    }
}
