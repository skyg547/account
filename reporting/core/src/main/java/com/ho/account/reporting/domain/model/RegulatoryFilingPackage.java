package com.ho.account.reporting.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record RegulatoryFilingPackage(
        String filingId,
        String submissionId,
        FinancialStatement.StatementType statementType,
        LocalDateTime baseDate,
        int submissionVersion,
        String targetAgency,
        List<RegulatoryFilingLine> lines) {

    public RegulatoryFilingPackage {
        filingId = requireText(filingId, "filingId is required.");
        submissionId = requireText(submissionId, "submissionId is required.");
        if (statementType == null) {
            throw new IllegalArgumentException("statementType is required.");
        }
        if (baseDate == null) {
            throw new IllegalArgumentException("baseDate is required.");
        }
        if (submissionVersion < 1) {
            throw new IllegalArgumentException("submissionVersion must be greater than zero.");
        }
        targetAgency = requireText(targetAgency, "targetAgency is required.");
        lines = List.copyOf(lines == null ? List.of() : lines);
        if (lines.isEmpty()) {
            throw new IllegalStateException("Regulatory filing package requires at least one line.");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
