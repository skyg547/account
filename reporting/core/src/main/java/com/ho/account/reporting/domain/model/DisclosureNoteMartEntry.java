package com.ho.account.reporting.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class DisclosureNoteMartEntry {

    private final String entryId;
    private final String martId;
    private final String statementId;
    private final FinancialStatement.StatementType statementType;
    private final LocalDateTime baseDate;
    private final String noteNumber;
    private final NoteCategory noteCategory;
    private final String sourceLineCode;
    private final String sourceLineLabel;
    private final MaturityBucket maturityBucket;
    private final RateType rateType;
    private final String currencyCode;
    private final RiskCategory riskCategory;
    private final BigDecimal currentAmount;
    private final BigDecimal previousAmount;
    private final String generatedBy;
    private final LocalDateTime generatedAt;

    public DisclosureNoteMartEntry(
            String entryId,
            String martId,
            String statementId,
            FinancialStatement.StatementType statementType,
            LocalDateTime baseDate,
            String noteNumber,
            NoteCategory noteCategory,
            String sourceLineCode,
            String sourceLineLabel,
            MaturityBucket maturityBucket,
            RateType rateType,
            String currencyCode,
            RiskCategory riskCategory,
            BigDecimal currentAmount,
            BigDecimal previousAmount,
            String generatedBy,
            LocalDateTime generatedAt) {
        this.entryId = requireText(entryId, "entryId is required.");
        this.martId = requireText(martId, "martId is required.");
        this.statementId = requireText(statementId, "statementId is required.");
        this.statementType = Objects.requireNonNull(statementType, "statementType must not be null");
        this.baseDate = Objects.requireNonNull(baseDate, "baseDate must not be null");
        this.noteNumber = requireText(noteNumber, "noteNumber is required.");
        this.noteCategory = Objects.requireNonNull(noteCategory, "noteCategory must not be null");
        this.sourceLineCode = requireText(sourceLineCode, "sourceLineCode is required.");
        this.sourceLineLabel = requireText(sourceLineLabel, "sourceLineLabel is required.");
        this.maturityBucket = Objects.requireNonNull(maturityBucket, "maturityBucket must not be null");
        this.rateType = Objects.requireNonNull(rateType, "rateType must not be null");
        this.currencyCode = normalizeCurrency(currencyCode);
        this.riskCategory = Objects.requireNonNull(riskCategory, "riskCategory must not be null");
        this.currentAmount = currentAmount == null ? BigDecimal.ZERO : currentAmount;
        this.previousAmount = previousAmount == null ? BigDecimal.ZERO : previousAmount;
        this.generatedBy = requireText(generatedBy, "generatedBy is required.");
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt must not be null");
    }

    static DisclosureNoteMartEntry fromLine(
            String martId,
            FinancialStatement statement,
            ReportLine line,
            String generatedBy,
            LocalDateTime generatedAt) {
        NoteCategory noteCategory = classifyCategory(line);
        return new DisclosureNoteMartEntry(
                UUID.randomUUID().toString(),
                martId,
                statement.getStatementId(),
                statement.getType(),
                statement.getBaseDate(),
                line.getNoteNumber(),
                noteCategory,
                line.getLineCode(),
                line.getLabel(),
                classifyMaturityBucket(noteCategory, line),
                classifyRateType(noteCategory, line),
                classifyCurrency(noteCategory, line),
                classifyRiskCategory(noteCategory, line),
                line.getCurrentAmount(),
                line.getPreviousAmount(),
                generatedBy,
                generatedAt);
    }

    private static NoteCategory classifyCategory(ReportLine line) {
        String note = normalize(line.getNoteNumber());
        String code = normalize(line.getLineCode());
        String label = normalize(line.getLabel());
        if ("8".equals(note) || containsAny(code, label, "DEPOSIT", "LIABILITY", "MATURITY", "DUE")) {
            return NoteCategory.MATURITY;
        }
        if ("12".equals(note) || "13".equals(note) || containsAny(code, label, "INTEREST", "RATE")) {
            return NoteCategory.INTEREST_RATE;
        }
        if ("3".equals(note) || containsAny(code, label, "CASH", "CURRENCY", "FX")) {
            return NoteCategory.CURRENCY;
        }
        if (containsAny(code, label, "RISK", "CREDIT", "ECL", "IMPAIRMENT", "ALLOWANCE")) {
            return NoteCategory.RISK;
        }
        return NoteCategory.GENERAL;
    }

    private static MaturityBucket classifyMaturityBucket(NoteCategory noteCategory, ReportLine line) {
        if (noteCategory != NoteCategory.MATURITY) {
            return MaturityBucket.NOT_APPLICABLE;
        }
        String code = normalize(line.getLineCode());
        if (containsAny(code, "", "DEPOSIT", "DEMAND")) {
            return MaturityBucket.ON_DEMAND;
        }
        return MaturityBucket.UNSPECIFIED;
    }

    private static RateType classifyRateType(NoteCategory noteCategory, ReportLine line) {
        if (noteCategory != NoteCategory.INTEREST_RATE) {
            return RateType.NOT_APPLICABLE;
        }
        String code = normalize(line.getLineCode());
        if (code.contains("EXPENSE")) {
            return RateType.FLOATING;
        }
        if (code.contains("INCOME")) {
            return RateType.FIXED;
        }
        return RateType.UNSPECIFIED;
    }

    private static String classifyCurrency(NoteCategory noteCategory, ReportLine line) {
        if (noteCategory != NoteCategory.CURRENCY) {
            return "N/A";
        }
        String code = normalize(line.getLineCode());
        if (code.contains("USD")) {
            return "USD";
        }
        if (code.contains("EUR")) {
            return "EUR";
        }
        return "KRW";
    }

    private static RiskCategory classifyRiskCategory(NoteCategory noteCategory, ReportLine line) {
        if (noteCategory != NoteCategory.RISK) {
            return RiskCategory.NOT_APPLICABLE;
        }
        String code = normalize(line.getLineCode());
        String label = normalize(line.getLabel());
        if (containsAny(code, label, "MARKET", "FX", "RATE")) {
            return RiskCategory.MARKET;
        }
        if (containsAny(code, label, "LIQUIDITY", "MATURITY")) {
            return RiskCategory.LIQUIDITY;
        }
        if (containsAny(code, label, "OPERATIONAL")) {
            return RiskCategory.OPERATIONAL;
        }
        return RiskCategory.CREDIT;
    }

    private static boolean containsAny(String code, String label, String... tokens) {
        for (String token : tokens) {
            if (code.contains(token) || label.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private static String normalizeCurrency(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? "N/A" : normalized;
    }

    public String getEntryId() {
        return entryId;
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

    public String getNoteNumber() {
        return noteNumber;
    }

    public NoteCategory getNoteCategory() {
        return noteCategory;
    }

    public String getSourceLineCode() {
        return sourceLineCode;
    }

    public String getSourceLineLabel() {
        return sourceLineLabel;
    }

    public MaturityBucket getMaturityBucket() {
        return maturityBucket;
    }

    public RateType getRateType() {
        return rateType;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public RiskCategory getRiskCategory() {
        return riskCategory;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public BigDecimal getPreviousAmount() {
        return previousAmount;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public enum NoteCategory {
        MATURITY,
        INTEREST_RATE,
        CURRENCY,
        RISK,
        GENERAL
    }

    public enum MaturityBucket {
        ON_DEMAND,
        LESS_THAN_3_MONTHS,
        THREE_TO_TWELVE_MONTHS,
        ONE_TO_FIVE_YEARS,
        OVER_FIVE_YEARS,
        UNSPECIFIED,
        NOT_APPLICABLE
    }

    public enum RateType {
        FIXED,
        FLOATING,
        NON_INTEREST_BEARING,
        UNSPECIFIED,
        NOT_APPLICABLE
    }

    public enum RiskCategory {
        CREDIT,
        LIQUIDITY,
        MARKET,
        OPERATIONAL,
        NOT_APPLICABLE
    }
}
