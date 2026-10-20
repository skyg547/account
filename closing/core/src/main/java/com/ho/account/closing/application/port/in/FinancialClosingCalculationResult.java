package com.ho.account.closing.application.port.in;

import java.util.List;

/**
 * Journal outcome of one financial closing calculation.
 *
 * <p>A single journal ID is retained for the legacy batch history column. Zero or multiple
 * journals cannot be represented by that scalar column and therefore deliberately carry no ID.</p>
 */
public record FinancialClosingCalculationResult(int journalCount, Long singleJournalEntryId, boolean allPosted) {

    public FinancialClosingCalculationResult(int journalCount, Long singleJournalEntryId) {
        this(journalCount, singleJournalEntryId, false);
    }

    public FinancialClosingCalculationResult {
        if (journalCount < 0) {
            throw new IllegalArgumentException("journalCount must not be negative");
        }
        if (journalCount == 1 && singleJournalEntryId == null) {
            throw new IllegalArgumentException("singleJournalEntryId is required for exactly one journal");
        }
        if (journalCount != 1 && singleJournalEntryId != null) {
            throw new IllegalArgumentException("singleJournalEntryId is allowed only for exactly one journal");
        }
        if (singleJournalEntryId != null && singleJournalEntryId <= 0) {
            throw new IllegalArgumentException("singleJournalEntryId must be positive");
        }
    }

    public static FinancialClosingCalculationResult fromJournalEntryIds(
            List<Long> journalEntryIds) {
        if (journalEntryIds == null) {
            throw new IllegalArgumentException("journalEntryIds must not be null");
        }
        if (journalEntryIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("journalEntryIds must contain only positive IDs");
        }
        List<Long> immutableIds = List.copyOf(journalEntryIds);
        return new FinancialClosingCalculationResult(
                immutableIds.size(),
                immutableIds.size() == 1 ? immutableIds.get(0) : null);
    }
}
