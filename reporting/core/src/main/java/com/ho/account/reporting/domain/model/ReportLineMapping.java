package com.ho.account.reporting.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * SCD2 방식으로 관리되는 보고 라인과 계정 매핑 규칙.
 */
public record ReportLineMapping(
        FinancialStatement.StatementType statementType,
        String lineCode,
        String label,
        List<String> accountCodes,
        String noteNumber,
        int level,
        int displayOrder,
        LocalDate validFrom,
        LocalDate validTo) {

    public ReportLineMapping {
        Objects.requireNonNull(statementType, "statementType must not be null");
        lineCode = requireText(lineCode, "lineCode is required.");
        label = requireText(label, "label is required.");
        accountCodes = accountCodes == null ? List.of() : List.copyOf(accountCodes);
        if (accountCodes.isEmpty()) {
            throw new IllegalArgumentException("accountCodes is required.");
        }
        noteNumber = noteNumber == null ? "" : noteNumber.trim();
        if (level < 1) {
            throw new IllegalArgumentException("level must be greater than zero.");
        }
        if (validFrom == null) {
            throw new IllegalArgumentException("validFrom is required.");
        }
        if (validTo != null && validFrom.isAfter(validTo)) {
            throw new IllegalArgumentException("validFrom must be before validTo.");
        }
    }

    public boolean isEffectiveAt(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        boolean afterStart = !date.isBefore(validFrom);
        boolean beforeEnd = validTo == null || !date.isAfter(validTo);
        return afterStart && beforeEnd;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
