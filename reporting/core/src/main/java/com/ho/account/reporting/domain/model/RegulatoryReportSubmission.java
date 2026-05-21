package com.ho.account.reporting.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 감독보고 제출본 도메인 모델.
 *
 * <p>확정 재무제표 스냅샷을 외부 제출 가능한 버전 단위로 고정하고, 정정 제출과 기본 검증 규칙을 함께 관리합니다.
 */
public class RegulatoryReportSubmission {

    private final String submissionId;
    private final String statementId;
    private final FinancialStatement.StatementType statementType;
    private final LocalDateTime baseDate;
    private final int version;
    private final String submittedBy;
    private final LocalDateTime submittedAt;
    private final String correctionReason;
    private final SubmissionStatus status;
    private final List<String> validationMessages;

    private RegulatoryReportSubmission(
            String submissionId,
            String statementId,
            FinancialStatement.StatementType statementType,
            LocalDateTime baseDate,
            int version,
            String submittedBy,
            LocalDateTime submittedAt,
            String correctionReason,
            SubmissionStatus status,
            List<String> validationMessages) {
        this.submissionId = requireText(submissionId, "submissionId is required.");
        this.statementId = requireText(statementId, "statementId is required.");
        this.statementType = Objects.requireNonNull(statementType, "statementType must not be null");
        this.baseDate = Objects.requireNonNull(baseDate, "baseDate must not be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero.");
        }
        this.version = version;
        this.submittedBy = requireText(submittedBy, "submittedBy is required.");
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        this.correctionReason = correctionReason == null ? "" : correctionReason.trim();
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.validationMessages = List.copyOf(validationMessages == null ? List.of() : validationMessages);
    }

    public static RegulatoryReportSubmission ready(
            String submissionId,
            FinancialStatement statement,
            int version,
            String submittedBy,
            String correctionReason,
            LocalDateTime submittedAt) {
        Objects.requireNonNull(statement, "statement must not be null");
        if (version > 1 && !hasText(correctionReason)) {
            throw new IllegalArgumentException("correctionReason is required for corrected submissions.");
        }

        List<String> validationMessages = validateStatement(statement);
        if (!validationMessages.isEmpty()) {
            throw new IllegalStateException(
                    "Regulatory report validation failed: " + String.join("; ", validationMessages));
        }

        return new RegulatoryReportSubmission(
                submissionId,
                statement.getStatementId(),
                statement.getType(),
                statement.getBaseDate(),
                version,
                submittedBy,
                submittedAt,
                correctionReason,
                SubmissionStatus.READY,
                validationMessages);
    }

    public static RegulatoryReportSubmission restored(
            String submissionId,
            String statementId,
            FinancialStatement.StatementType statementType,
            LocalDateTime baseDate,
            int version,
            String submittedBy,
            LocalDateTime submittedAt,
            String correctionReason,
            SubmissionStatus status,
            List<String> validationMessages) {
        return new RegulatoryReportSubmission(
                submissionId,
                statementId,
                statementType,
                baseDate,
                version,
                submittedBy,
                submittedAt,
                correctionReason,
                status,
                validationMessages);
    }

    private static List<String> validateStatement(FinancialStatement statement) {
        List<String> messages = new ArrayList<>();
        if (statement.getStatus() != FinancialStatement.StatementStatus.FINAL) {
            messages.add("Only FINAL statements can be submitted.");
        }
        if (statement.getLines().isEmpty()) {
            messages.add("At least one report line is required.");
        }

        Set<String> lineCodes = new HashSet<>();
        for (ReportLine line : statement.getLines()) {
            if (!hasText(line.getLineCode())) {
                messages.add("lineCode is required.");
            } else if (!lineCodes.add(line.getLineCode())) {
                messages.add("Duplicate lineCode: " + line.getLineCode());
            }
            if (!hasText(line.getLabel())) {
                messages.add("label is required for lineCode " + line.getLineCode());
            }
            if (line.getCurrentAmount() == null) {
                messages.add("currentAmount is required for lineCode " + line.getLineCode());
            }
            if (line.getPreviousAmount() == null) {
                messages.add("previousAmount is required for lineCode " + line.getLineCode());
            }
            if (line.getLevel() < 1) {
                messages.add("line level must be greater than zero for lineCode " + line.getLineCode());
            }
        }

        if (statement.getType() == FinancialStatement.StatementType.BALANCE_SHEET) {
            validateBalanceSheetTotals(statement, messages);
        }
        if (statement.getType() == FinancialStatement.StatementType.INCOME_STATEMENT) {
            validateIncomeStatementTotals(statement, messages);
        }
        return messages;
    }

    private static void validateBalanceSheetTotals(FinancialStatement statement, List<String> messages) {
        Map<String, ReportLine> lines = statement.getLines().stream()
                .collect(Collectors.toMap(ReportLine::getLineCode, Function.identity(), (left, right) -> left));
        Optional<BigDecimal> totalAssets = amountOf(lines, "TOTAL_ASSETS");
        Optional<BigDecimal> totalLiabilitiesAndEquity = amountOf(lines, "TOTAL_LIABILITIES_EQUITY");
        if (totalAssets.isPresent()
                && totalLiabilitiesAndEquity.isPresent()
                && totalAssets.get().compareTo(totalLiabilitiesAndEquity.get()) != 0) {
            messages.add("Balance sheet total mismatch between TOTAL_ASSETS and TOTAL_LIABILITIES_EQUITY.");
        }
    }

    private static void validateIncomeStatementTotals(FinancialStatement statement, List<String> messages) {
        Map<String, ReportLine> lines = statement.getLines().stream()
                .collect(Collectors.toMap(ReportLine::getLineCode, Function.identity(), (left, right) -> left));
        Optional<BigDecimal> interestIncome = amountOf(lines, "INTEREST_INCOME");
        Optional<BigDecimal> interestExpense = amountOf(lines, "INTEREST_EXPENSE");
        Optional<BigDecimal> netIncome = amountOf(lines, "NET_INCOME");
        if (interestIncome.isPresent() && interestExpense.isPresent() && netIncome.isPresent()) {
            BigDecimal expectedNetIncome = interestIncome.get().subtract(interestExpense.get());
            if (expectedNetIncome.compareTo(netIncome.get()) != 0) {
                messages.add("Income statement NET_INCOME must equal INTEREST_INCOME minus INTEREST_EXPENSE.");
            }
        }
    }

    private static Optional<BigDecimal> amountOf(Map<String, ReportLine> lines, String lineCode) {
        return Optional.ofNullable(lines.get(lineCode)).map(ReportLine::getCurrentAmount);
    }

    private static String requireText(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public String getSubmissionId() {
        return submissionId;
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

    public int getVersion() {
        return version;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public String getCorrectionReason() {
        return correctionReason;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public List<String> getValidationMessages() {
        return validationMessages;
    }

    public enum SubmissionStatus {
        READY
    }
}
