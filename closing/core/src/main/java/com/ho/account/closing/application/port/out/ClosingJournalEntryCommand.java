package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.ClosingMonetaryPrecision;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
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
        BigDecimal exchangeRate,
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
        if (lineageSourceId.trim().length() > 100) {
            throw new IllegalArgumentException("lineageSourceId must not exceed 100 characters");
        }
        if (slipNo.trim().length() > 20) {
            throw new IllegalArgumentException("slipNo must not exceed 20 characters");
        }
        description = description.trim();
        entryType = entryType.trim();
        createdBy = createdBy.trim();
        auditUser = auditUser.trim();
        lineageSourceType = lineageSourceType.trim();
        lineageSourceId = lineageSourceId.trim();
        currencyCode = currencyCode.trim().toUpperCase(Locale.ROOT);
        if (!currencyCode.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("currencyCode must be a 3-letter currency code");
        }
        exchangeRate = ClosingMonetaryPrecision.exchangeRate(exchangeRate);
        slipNo = slipNo.trim();
        lines = List.copyOf(lines);
        validateBalanced(lines);
    }

    /** Existing functional-currency callers, including FX adjustments, post with a unit rate. */
    public ClosingJournalEntryCommand(
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
        this(slipDate, accountingDate, description, entryType, createdBy, auditUser,
                lineageSourceType, lineageSourceId, currencyCode, BigDecimal.ONE, slipNo, lines);
    }

    private static void validateBalanced(List<ClosingJournalLineCommand> lines) {
        BigDecimal debitAmount = sum(lines, ClosingJournalSide.DEBIT, false);
        BigDecimal creditAmount = sum(lines, ClosingJournalSide.CREDIT, false);
        BigDecimal debitBaseAmount = sum(lines, ClosingJournalSide.DEBIT, true);
        BigDecimal creditBaseAmount = sum(lines, ClosingJournalSide.CREDIT, true);

        if (debitAmount.signum() <= 0 || creditAmount.signum() <= 0) {
            throw new IllegalArgumentException("closing journal must contain positive debit and credit lines");
        }
        if (debitAmount.compareTo(creditAmount) != 0) {
            throw new IllegalArgumentException(
                    "closing journal amount is not balanced: debit=" + debitAmount + ", credit=" + creditAmount);
        }
        if (debitBaseAmount.compareTo(creditBaseAmount) != 0) {
            throw new IllegalArgumentException(
                    "closing journal baseAmount is not balanced: debit="
                            + debitBaseAmount + ", credit=" + creditBaseAmount);
        }
    }

    private static BigDecimal sum(
            List<ClosingJournalLineCommand> lines,
            ClosingJournalSide side,
            boolean baseAmount) {
        return lines.stream()
                .filter(Objects::nonNull)
                .filter(line -> line.side() == side)
                .map(line -> baseAmount ? line.baseAmount() : line.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
