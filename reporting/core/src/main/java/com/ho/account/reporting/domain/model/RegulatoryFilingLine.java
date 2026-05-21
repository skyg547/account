package com.ho.account.reporting.domain.model;

import java.math.BigDecimal;

public record RegulatoryFilingLine(
        String reportCode,
        String fieldCode,
        String fieldLabel,
        String sourceNoteNumber,
        String sourceLineCode,
        String sourceLineLabel,
        BigDecimal currentAmount,
        BigDecimal previousAmount,
        int displayOrder) {

    public RegulatoryFilingLine {
        reportCode = requireText(reportCode, "reportCode is required.");
        fieldCode = requireText(fieldCode, "fieldCode is required.");
        fieldLabel = requireText(fieldLabel, "fieldLabel is required.");
        sourceNoteNumber = requireText(sourceNoteNumber, "sourceNoteNumber is required.");
        sourceLineCode = requireText(sourceLineCode, "sourceLineCode is required.");
        sourceLineLabel = requireText(sourceLineLabel, "sourceLineLabel is required.");
        currentAmount = currentAmount == null ? BigDecimal.ZERO : currentAmount;
        previousAmount = previousAmount == null ? BigDecimal.ZERO : previousAmount;
        if (displayOrder < 1) {
            throw new IllegalArgumentException("displayOrder must be greater than zero.");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
