package com.ho.account.reporting.domain.model;

import com.ho.account.reporting.domain.model.DisclosureNoteMartEntry.NoteCategory;
import java.time.LocalDate;
import java.util.Objects;

public record RegulatoryReportMapping(
        FinancialStatement.StatementType statementType,
        String targetAgency,
        String reportCode,
        String fieldCode,
        String fieldLabel,
        NoteCategory sourceNoteCategory,
        String sourceNoteNumber,
        String sourceLineCode,
        boolean required,
        int displayOrder,
        LocalDate validFrom,
        LocalDate validTo) {

    public RegulatoryReportMapping {
        Objects.requireNonNull(statementType, "statementType must not be null");
        targetAgency = requireText(targetAgency, "targetAgency is required.");
        reportCode = requireText(reportCode, "reportCode is required.");
        fieldCode = requireText(fieldCode, "fieldCode is required.");
        fieldLabel = requireText(fieldLabel, "fieldLabel is required.");
        sourceNoteCategory = Objects.requireNonNull(sourceNoteCategory, "sourceNoteCategory must not be null");
        sourceNoteNumber = normalizeOptional(sourceNoteNumber);
        sourceLineCode = normalizeOptional(sourceLineCode);
        if (displayOrder < 1) {
            throw new IllegalArgumentException("displayOrder must be greater than zero.");
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
        return !date.isBefore(validFrom) && (validTo == null || !date.isAfter(validTo));
    }

    public boolean matches(DisclosureNoteMartEntry entry) {
        if (entry.getNoteCategory() != sourceNoteCategory) {
            return false;
        }
        if (!sourceNoteNumber.isBlank() && !sourceNoteNumber.equals(entry.getNoteNumber())) {
            return false;
        }
        return sourceLineCode.isBlank() || sourceLineCode.equals(entry.getSourceLineCode());
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}
