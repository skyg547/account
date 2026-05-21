package com.ho.account.reporting.domain.model;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class RegulatoryFiling {

    private final String filingId;
    private final String submissionId;
    private final FinancialStatement.StatementType statementType;
    private final LocalDateTime baseDate;
    private final int submissionVersion;
    private final String targetAgency;
    private final String submittedBy;
    private final LocalDateTime submittedAt;
    private final FilingStatus status;
    private final String regulatorReceiptId;
    private final String regulatorMessage;
    private final List<RegulatoryFilingLine> lines;

    private RegulatoryFiling(
            String filingId,
            String submissionId,
            FinancialStatement.StatementType statementType,
            LocalDateTime baseDate,
            int submissionVersion,
            String targetAgency,
            String submittedBy,
            LocalDateTime submittedAt,
            FilingStatus status,
            String regulatorReceiptId,
            String regulatorMessage,
            List<RegulatoryFilingLine> lines) {
        this.filingId = requireText(filingId, "filingId is required.");
        this.submissionId = requireText(submissionId, "submissionId is required.");
        this.statementType = Objects.requireNonNull(statementType, "statementType must not be null");
        this.baseDate = Objects.requireNonNull(baseDate, "baseDate must not be null");
        if (submissionVersion < 1) {
            throw new IllegalArgumentException("submissionVersion must be greater than zero.");
        }
        this.submissionVersion = submissionVersion;
        this.targetAgency = requireText(targetAgency, "targetAgency is required.");
        this.submittedBy = requireText(submittedBy, "submittedBy is required.");
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.regulatorReceiptId = requireText(regulatorReceiptId, "regulatorReceiptId is required.");
        this.regulatorMessage = regulatorMessage == null ? "" : regulatorMessage.trim();
        this.lines = List.copyOf(lines == null ? List.of() : lines);
        if (this.lines.isEmpty()) {
            throw new IllegalStateException("Regulatory filing requires at least one line.");
        }
    }

    public static RegulatoryFilingPackage packageFor(
            RegulatoryReportSubmission submission,
            DisclosureNoteMart mart,
            List<RegulatoryReportMapping> mappings,
            String targetAgency) {
        Objects.requireNonNull(submission, "submission must not be null");
        Objects.requireNonNull(mart, "mart must not be null");
        List<RegulatoryReportMapping> effectiveMappings = mappings == null ? List.of() : mappings.stream()
                .filter(mapping -> mapping.targetAgency().equals(targetAgency))
                .filter(mapping -> mapping.isEffectiveAt(submission.getBaseDate().toLocalDate()))
                .sorted(Comparator.comparingInt(RegulatoryReportMapping::displayOrder))
                .toList();
        if (effectiveMappings.isEmpty()) {
            throw new IllegalStateException("No regulatory report mappings are effective for the request.");
        }

        List<RegulatoryFilingLine> lines = effectiveMappings.stream()
                .map(mapping -> lineForMapping(mapping, mart))
                .toList();

        return new RegulatoryFilingPackage(
                UUID.randomUUID().toString(),
                submission.getSubmissionId(),
                submission.getStatementType(),
                submission.getBaseDate(),
                submission.getVersion(),
                targetAgency,
                lines);
    }

    public static RegulatoryFiling accepted(
            RegulatoryFilingPackage filingPackage,
            String submittedBy,
            LocalDateTime submittedAt,
            RegulatoryFilingReceipt receipt) {
        Objects.requireNonNull(filingPackage, "filingPackage must not be null");
        Objects.requireNonNull(receipt, "receipt must not be null");
        return new RegulatoryFiling(
                filingPackage.filingId(),
                filingPackage.submissionId(),
                filingPackage.statementType(),
                filingPackage.baseDate(),
                filingPackage.submissionVersion(),
                filingPackage.targetAgency(),
                submittedBy,
                submittedAt,
                FilingStatus.ACCEPTED,
                receipt.receiptId(),
                receipt.message(),
                filingPackage.lines());
    }

    public static RegulatoryFiling restored(
            String filingId,
            String submissionId,
            FinancialStatement.StatementType statementType,
            LocalDateTime baseDate,
            int submissionVersion,
            String targetAgency,
            String submittedBy,
            LocalDateTime submittedAt,
            FilingStatus status,
            String regulatorReceiptId,
            String regulatorMessage,
            List<RegulatoryFilingLine> lines) {
        return new RegulatoryFiling(
                filingId,
                submissionId,
                statementType,
                baseDate,
                submissionVersion,
                targetAgency,
                submittedBy,
                submittedAt,
                status,
                regulatorReceiptId,
                regulatorMessage,
                lines);
    }

    private static RegulatoryFilingLine lineForMapping(
            RegulatoryReportMapping mapping,
            DisclosureNoteMart mart) {
        DisclosureNoteMartEntry entry = mart.getEntries().stream()
                .filter(mapping::matches)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Required regulatory source is missing for field " + mapping.fieldCode()));
        return new RegulatoryFilingLine(
                mapping.reportCode(),
                mapping.fieldCode(),
                mapping.fieldLabel(),
                entry.getNoteNumber(),
                entry.getSourceLineCode(),
                entry.getSourceLineLabel(),
                entry.getCurrentAmount(),
                entry.getPreviousAmount(),
                mapping.displayOrder());
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    public String getFilingId() {
        return filingId;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public FinancialStatement.StatementType getStatementType() {
        return statementType;
    }

    public LocalDateTime getBaseDate() {
        return baseDate;
    }

    public int getSubmissionVersion() {
        return submissionVersion;
    }

    public String getTargetAgency() {
        return targetAgency;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public FilingStatus getStatus() {
        return status;
    }

    public String getRegulatorReceiptId() {
        return regulatorReceiptId;
    }

    public String getRegulatorMessage() {
        return regulatorMessage;
    }

    public List<RegulatoryFilingLine> getLines() {
        return lines;
    }

    public enum FilingStatus {
        ACCEPTED
    }
}
