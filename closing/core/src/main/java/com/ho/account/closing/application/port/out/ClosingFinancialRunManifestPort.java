package com.ho.account.closing.application.port.out;

import java.util.List;
import java.util.function.Supplier;

/** Commits the complete prepared command set before the first remote Journal write. */
public interface ClosingFinancialRunManifestPort {
    boolean exists(String scope, Long batchId);
    List<ClosingJournalEntryCommand> loadOrCreate(String scope, Long batchId,
                                                   Supplier<List<ClosingJournalEntryCommand>> prepare);
}
