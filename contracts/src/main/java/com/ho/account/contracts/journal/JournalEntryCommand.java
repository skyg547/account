package com.ho.account.contracts.journal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record JournalEntryCommand(
        LocalDate slipDate,
        LocalDate accountingDate,
        String description,
        String entryType,
        String currencyCode,
        BigDecimal exchangeRate,
        String createdBy,
        String auditUser,
        String lineageSourceType,
        String lineageSourceId,
        List<JournalLineCommand> lines) {
}
