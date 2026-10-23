package com.ho.account.closing.application.port.out;

import com.ho.account.closing.application.service.FxValuationBalance;

import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * Streams the complete, posted-ledger FX valuation evidence for an accounting date.
 *
 * <p>The callback keeps the core independent of Spring Batch cursor and chunk APIs while allowing
 * an adapter to retain resource ownership for the duration of one traversal. Implementations must
 * invoke the consumer synchronously before this method returns and must never invoke it afterward.</p>
 */
public interface FxValuationEvidencePort {

    void forEachBalance(
            LocalDate valuationDate,
            String reportingCurrencyCode,
            Consumer<FxValuationBalance> consumer);
}
