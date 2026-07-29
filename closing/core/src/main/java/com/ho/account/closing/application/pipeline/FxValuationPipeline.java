package com.ho.account.closing.application.pipeline;

import com.ho.account.closing.application.service.FxValuationBalance;
import com.ho.account.closing.application.service.FxValuationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Chunk-level FX valuation pipeline.
 *
 * <p>The whole chunk is atomic. Every failed account is reported, then the exception rolls the
 * chunk back so Spring Batch does not checkpoint an incomplete financial valuation.</p>
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

        List<RuntimeException> failures = new ArrayList<>();
        for (FxValuationBalance balance : balances) {
            try {
                fxValuationService.processFxValuationForAccount(balance, valuationDate, valuationBatchId);
            } catch (RuntimeException exception) {
                failures.add(new IllegalStateException(
                        "FX valuation failed for " + balance.accountCode() + "|" + balance.currencyCode(),
                        exception));
            }
        }

        if (!failures.isEmpty()) {
            IllegalStateException aggregate = new IllegalStateException(
                    "FX valuation chunk failed for " + failures.size() + " balance(s)");
            failures.forEach(aggregate::addSuppressed);
            throw aggregate;
        }
    }
}
