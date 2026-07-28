package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryFiling;
import com.ho.account.reporting.domain.model.RegulatoryFilingLine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "rpt_regulatory_filing")
class RegulatoryFilingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "filing_id", nullable = false, length = 80)
    private String filingId;

    @Column(name = "submission_id", nullable = false, length = 80)
    private String submissionId;

    @Column(name = "statement_type", nullable = false, length = 40)
    private String statementType;

    @Column(name = "base_date", nullable = false)
    private LocalDateTime baseDate;

    @Column(name = "submission_version", nullable = false)
    private int submissionVersion;

    @Column(name = "target_agency", nullable = false, length = 40)
    private String targetAgency;

    @Column(name = "submitted_by", nullable = false, length = 80)
    private String submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "regulator_receipt_id", nullable = false, length = 120)
    private String regulatorReceiptId;

    @Column(name = "regulator_message", length = 500)
    private String regulatorMessage;

    @Column(name = "report_code", nullable = false, length = 80)
    private String reportCode;

    @Column(name = "field_code", nullable = false, length = 80)
    private String fieldCode;

    @Column(name = "field_label", nullable = false, length = 200)
    private String fieldLabel;

    @Column(name = "source_note_number", nullable = false, length = 40)
    private String sourceNoteNumber;

    @Column(name = "source_line_code", nullable = false, length = 80)
    private String sourceLineCode;

    @Column(name = "source_line_label", nullable = false, length = 200)
    private String sourceLineLabel;

    @Column(name = "current_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentAmount;

    @Column(name = "previous_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal previousAmount;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected RegulatoryFilingJpaEntity() {
    }

    static List<RegulatoryFilingJpaEntity> from(RegulatoryFiling filing) {
        return filing.getLines().stream()
                .map(line -> from(filing, line))
                .toList();
    }

    static RegulatoryFiling main(List<RegulatoryFilingJpaEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("rows must not be empty.");
        }
        RegulatoryFilingJpaEntity first = rows.get(0);
        return RegulatoryFiling.restored(
                first.filingId,
                first.submissionId,
                FinancialStatement.StatementType.valueOf(first.statementType),
                first.baseDate,
                first.submissionVersion,
                first.targetAgency,
                first.submittedBy,
                first.submittedAt,
                RegulatoryFiling.FilingStatus.valueOf(first.status),
                first.regulatorReceiptId,
                first.regulatorMessage,
                rows.stream()
                        .map(RegulatoryFilingJpaEntity::toLine)
                        .toList());
    }

    String filingId() {
        return filingId;
    }

    private static RegulatoryFilingJpaEntity from(RegulatoryFiling filing, RegulatoryFilingLine line) {
        RegulatoryFilingJpaEntity entity = new RegulatoryFilingJpaEntity();
        entity.filingId = filing.getFilingId();
        entity.submissionId = filing.getSubmissionId();
        entity.statementType = filing.getStatementType().name();
        entity.baseDate = filing.getBaseDate();
        entity.submissionVersion = filing.getSubmissionVersion();
        entity.targetAgency = filing.getTargetAgency();
        entity.submittedBy = filing.getSubmittedBy();
        entity.submittedAt = filing.getSubmittedAt();
        entity.status = filing.getStatus().name();
        entity.regulatorReceiptId = filing.getRegulatorReceiptId();
        entity.regulatorMessage = filing.getRegulatorMessage();
        entity.reportCode = line.reportCode();
        entity.fieldCode = line.fieldCode();
        entity.fieldLabel = line.fieldLabel();
        entity.sourceNoteNumber = line.sourceNoteNumber();
        entity.sourceLineCode = line.sourceLineCode();
        entity.sourceLineLabel = line.sourceLineLabel();
        entity.currentAmount = line.currentAmount();
        entity.previousAmount = line.previousAmount();
        entity.displayOrder = line.displayOrder();
        return entity;
    }

    private RegulatoryFilingLine toLine() {
        return new RegulatoryFilingLine(
                reportCode,
                fieldCode,
                fieldLabel,
                sourceNoteNumber,
                sourceLineCode,
                sourceLineLabel,
                currentAmount,
                previousAmount,
                displayOrder);
    }
}
