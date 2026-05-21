package com.ho.account.reporting.domain.model;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DisclosureNoteMart {

    private final String martId;
    private final String statementId;
    private final FinancialStatement.StatementType statementType;
    private final LocalDateTime baseDate;
    private final String generatedBy;
    private final LocalDateTime generatedAt;
    private final List<DisclosureNoteMartEntry> entries;

    private DisclosureNoteMart(
            String martId,
            String statementId,
            FinancialStatement.StatementType statementType,
            LocalDateTime baseDate,
            String generatedBy,
            LocalDateTime generatedAt,
            List<DisclosureNoteMartEntry> entries) {
        this.martId = requireText(martId, "martId is required.");
        this.statementId = requireText(statementId, "statementId is required.");
        this.statementType = Objects.requireNonNull(statementType, "statementType must not be null");
        this.baseDate = Objects.requireNonNull(baseDate, "baseDate must not be null");
        this.generatedBy = requireText(generatedBy, "generatedBy is required.");
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        this.entries = List.copyOf(entries == null ? List.of() : entries);
        if (this.entries.isEmpty()) {
            throw new IllegalStateException("Disclosure note mart requires at least one note entry.");
        }
    }

    public static DisclosureNoteMart fromStatement(
            FinancialStatement statement,
            String generatedBy,
            LocalDateTime generatedAt) {
        Objects.requireNonNull(statement, "statement must not be null");
        if (statement.getStatus() != FinancialStatement.StatementStatus.FINAL) {
            throw new IllegalStateException("Only FINAL statements can generate disclosure note marts.");
        }

        String martId = UUID.randomUUID().toString();
        List<DisclosureNoteMartEntry> entries = statement.getLines().stream()
                .filter(line -> line.getNoteNumber() != null && !line.getNoteNumber().isBlank())
                .map(line -> DisclosureNoteMartEntry.fromLine(martId, statement, line, generatedBy, generatedAt))
                .sorted(Comparator
                        .comparing(DisclosureNoteMartEntry::getNoteNumber)
                        .thenComparing(DisclosureNoteMartEntry::getSourceLineCode))
                .toList();

        return new DisclosureNoteMart(
                martId,
                statement.getStatementId(),
                statement.getType(),
                statement.getBaseDate(),
                generatedBy,
                generatedAt,
                entries);
    }

    public static DisclosureNoteMart restored(
            String martId,
            String statementId,
            FinancialStatement.StatementType statementType,
            LocalDateTime baseDate,
            String generatedBy,
            LocalDateTime generatedAt,
            List<DisclosureNoteMartEntry> entries) {
        return new DisclosureNoteMart(martId, statementId, statementType, baseDate, generatedBy, generatedAt, entries);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    public String getMartId() {
        return martId;
    }

    public String getStatementId() {
        return statementId;
    }

    public FinancialStatement.StatementType getStatementType() {
        return statementType;
    }

    public LocalDateTime getBaseDate() {
        return baseDate;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public List<DisclosureNoteMartEntry> getEntries() {
        return entries;
    }
}
