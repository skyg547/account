package com.ho.account.closing.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record ClosingJournalEntryCommand(
        LocalDate slipDate,
        LocalDate accountingDate,
        String description,
        String entryType,
        String createdBy,
        String auditUser,
        String lineageSourceType,
        String lineageSourceId,
        String currencyCode,
        String slipNo,
        List<ClosingJournalLineCommand> lines) {

    public ClosingJournalEntryCommand {
        Objects.requireNonNull(slipDate, "slipDate must not be null");
        Objects.requireNonNull(accountingDate, "accountingDate must not be null");
        if (isBlank(description)) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (isBlank(entryType)) {
            throw new IllegalArgumentException("entryType must not be blank");
        }
        if (isBlank(createdBy)) {
            throw new IllegalArgumentException("createdBy must not be blank");
        }
        if (isBlank(auditUser)) {
            throw new IllegalArgumentException("auditUser must not be blank");
        }
        if (isBlank(lineageSourceType)) {
            throw new IllegalArgumentException("lineageSourceType must not be blank");
        }
        if (isBlank(lineageSourceId)) {
            throw new IllegalArgumentException("lineageSourceId must not be blank");
        }
        if (isBlank(currencyCode)) {
            throw new IllegalArgumentException("currencyCode must not be blank");
        }
        if (isBlank(slipNo)) {
            throw new IllegalArgumentException("slipNo must not be blank");
        }
        if (lines == null || lines.size() < 2) {
            throw new IllegalArgumentException("closing journal must have at least two lines");
        }
        description = description.trim();
        entryType = entryType.trim();
        createdBy = createdBy.trim();
        auditUser = auditUser.trim();
        lineageSourceType = lineageSourceType.trim();
        lineageSourceId = lineageSourceId.trim();
        currencyCode = currencyCode.trim();
        slipNo = slipNo.trim();
        lines = List.copyOf(lines);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}